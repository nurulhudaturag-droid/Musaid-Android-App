package qiubzen.musaid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import qiubzen.musaid.alarm.AlarmScheduler
import qiubzen.musaid.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GoalWithStepStats(
    val goal: Goal,
    val totalSteps: Int,
    val completedSteps: Int,
    val workProgress: Float,
    val timeProgress: Float,
    val isBehind: Boolean
)

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository
    private val alarmScheduler: AlarmScheduler

    init {
        val dao = AppDatabase.getDatabase(application).appDao()
        repository = AppRepository(dao)
        alarmScheduler = AlarmScheduler(application)
    }

    private val _statusFilter = MutableStateFlow<String?>("IN_PROGRESS") // null means ALL
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    fun setStatusFilter(status: String?) {
        _statusFilter.value = status
    }

    val goalsWithStats: StateFlow<List<GoalWithStepStats>> = combine(
        repository.allGoals,
        repository.allGoalSteps,
        _statusFilter,
        todayEpochDayFlow()
    ) { goals, allSteps, filter, todayEpochDay ->
        val stepsByGoal = allSteps.groupBy { it.goalId }
        val filteredGoals = if (filter != null) {
            goals.filter { it.status == filter }
        } else {
            goals
        }

        filteredGoals.map { goal ->
            val steps = stepsByGoal[goal.id].orEmpty()
            val totalSteps = steps.size
            val completedSteps = steps.count { it.isCompleted }
            val workProgress = if (totalSteps > 0) completedSteps.toFloat() / totalSteps else 0f

            val totalDays = (goal.targetDateEpochDay - goal.startDateEpochDay).coerceAtLeast(1)
            val elapsedDays = (todayEpochDay - goal.startDateEpochDay).coerceAtLeast(0)
            val timeProgress = (elapsedDays.toFloat() / totalDays).coerceIn(0f, 1f)
            val isBehind = totalSteps > 0 && workProgress < timeProgress && goal.status == Goal.STATUS_IN_PROGRESS

            GoalWithStepStats(
                goal = goal,
                totalSteps = totalSteps,
                completedSteps = completedSteps,
                workProgress = workProgress,
                timeProgress = timeProgress,
                isBehind = isBehind
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Goal CRUD
    fun addGoal(title: String, motivation: String, startDateEpochDay: Long, targetDateEpochDay: Long) {
        viewModelScope.launch {
            val goal = Goal(
                title = title.trim(),
                motivationNote = motivation.trim(),
                startDateEpochDay = startDateEpochDay,
                targetDateEpochDay = targetDateEpochDay,
                status = Goal.STATUS_IN_PROGRESS
            )
            repository.insertGoal(goal)
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            repository.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            val steps = repository.getStepsForGoalOnce(goal.id)
            steps.forEach { step ->
                alarmScheduler.cancelGoalStepAlarm(step.id)
            }
            repository.deleteGoalById(goal.id)
        }
    }

    // Detail flow for single goal
    fun getGoalFlow(goalId: Int): Flow<Goal?> = repository.getGoalById(goalId)

    fun getStepsFlow(goalId: Int): Flow<List<GoalStep>> = repository.getStepsForGoal(goalId)

    // Step CRUD
    fun addStep(
        goalId: Int,
        title: String,
        targetDateEpochDay: Long,
        reminderEnabled: Boolean,
        reminderTimeMillis: Long?
    ) {
        viewModelScope.launch {
            val existingSteps = repository.getStepsForGoalOnce(goalId)
            val nextOrder = if (existingSteps.isEmpty()) 0 else (existingSteps.maxOf { it.orderIndex } + 1)
            val step = GoalStep(
                goalId = goalId,
                title = title.trim(),
                targetDateEpochDay = targetDateEpochDay,
                orderIndex = nextOrder,
                isCompleted = false,
                reminderEnabled = reminderEnabled,
                reminderTimeMillis = reminderTimeMillis
            )
            val stepId = repository.insertGoalStep(step)
            val savedStep = step.copy(id = stepId.toInt())
            if (reminderEnabled) {
                alarmScheduler.scheduleGoalStepAlarm(savedStep)
            }

            // Check if goal was previously marked completed and now a new step makes it in progress
            val goal = repository.getGoalByIdOnce(goalId)
            if (goal != null && goal.status == Goal.STATUS_COMPLETED) {
                repository.updateGoal(goal.copy(status = Goal.STATUS_IN_PROGRESS))
            }
        }
    }

    fun updateStep(step: GoalStep) {
        viewModelScope.launch {
            repository.updateGoalStep(step)
            if (step.reminderEnabled && !step.isCompleted) {
                alarmScheduler.scheduleGoalStepAlarm(step)
            } else {
                alarmScheduler.cancelGoalStepAlarm(step.id)
            }
        }
    }

    fun deleteStep(step: GoalStep) {
        viewModelScope.launch {
            alarmScheduler.cancelGoalStepAlarm(step.id)
            repository.deleteGoalStepById(step.id)
            // Deletion must never auto-complete a goal (and pop the congratulations
            // event): completing is only reachable through explicit toggles.
            checkGoalCompletion(step.goalId, allowComplete = false)
        }
    }

    private val _congratulationsEvent = MutableSharedFlow<Goal>()
    val congratulationsEvent: SharedFlow<Goal> = _congratulationsEvent.asSharedFlow()

    fun toggleStepCompletion(step: GoalStep, isCompleted: Boolean) {
        viewModelScope.launch {
            val updatedStep = step.copy(isCompleted = isCompleted)
            repository.updateGoalStep(updatedStep)

            if (isCompleted) {
                alarmScheduler.cancelGoalStepAlarm(step.id)
            } else if (step.reminderEnabled) {
                alarmScheduler.scheduleGoalStepAlarm(updatedStep)
            }

            checkGoalCompletion(step.goalId)
        }
    }

    private suspend fun checkGoalCompletion(goalId: Int, allowComplete: Boolean = true) {
        val steps = repository.getStepsForGoalOnce(goalId)
        val goal = repository.getGoalByIdOnce(goalId) ?: return

        if (steps.isNotEmpty() && steps.all { it.isCompleted }) {
            if (allowComplete && goal.status != Goal.STATUS_COMPLETED) {
                val completedGoal = goal.copy(status = Goal.STATUS_COMPLETED)
                repository.updateGoal(completedGoal)
                _congratulationsEvent.emit(completedGoal)
            }
        } else {
            if (goal.status == Goal.STATUS_COMPLETED) {
                // All steps done (empty after delete) or at least one still open: the
                // goal is no longer complete, regardless of allowComplete.
                repository.updateGoal(goal.copy(status = Goal.STATUS_IN_PROGRESS))
            }
        }
    }

    fun moveStepUp(step: GoalStep, allSteps: List<GoalStep>) {
        val currentIndex = allSteps.indexOfFirst { it.id == step.id }
        if (currentIndex > 0) {
            val prevStep = allSteps[currentIndex - 1]
            viewModelScope.launch {
                repository.swapSteps(step, prevStep)
            }
        }
    }

    fun moveStepDown(step: GoalStep, allSteps: List<GoalStep>) {
        val currentIndex = allSteps.indexOfFirst { it.id == step.id }
        if (currentIndex >= 0 && currentIndex < allSteps.size - 1) {
            val nextStep = allSteps[currentIndex + 1]
            viewModelScope.launch {
                repository.swapSteps(step, nextStep)
            }
        }
    }
}
