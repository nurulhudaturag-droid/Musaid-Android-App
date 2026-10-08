package qiubzen.musaid.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val dao: AppDao) {

    val allRoutines: Flow<List<RoutineItem>> = dao.getAllRoutines()
    val allChecklists: Flow<List<ChecklistItem>> = dao.getAllChecklists()
    val activeChecklists: Flow<List<ChecklistItem>> = dao.getActiveChecklists()

    suspend fun insertRoutine(routine: RoutineItem): Long = dao.insertRoutine(routine)
    suspend fun updateRoutine(routine: RoutineItem) = dao.updateRoutine(routine)
    suspend fun deleteRoutineById(id: Int) = dao.deleteRoutineById(id)
    suspend fun getRoutineById(id: Int): RoutineItem? = dao.getRoutineById(id)

    suspend fun insertChecklist(checklist: ChecklistItem): Long = dao.insertChecklist(checklist)
    suspend fun updateChecklist(checklist: ChecklistItem) = dao.updateChecklist(checklist)
    suspend fun deleteChecklistById(id: Int) = dao.deleteChecklistById(id)

    fun getCompletionRecordsByDate(epochDay: Long): Flow<List<CompletionRecord>> = dao.getCompletionRecordsByDate(epochDay)
    
    fun getCompletionRecordsInDateRange(startEpochDay: Long, endEpochDay: Long): Flow<List<CompletionRecord>> = 
        dao.getCompletionRecordsInDateRange(startEpochDay, endEpochDay)

    suspend fun getAllCompletionRecordsOnce(): List<CompletionRecord> = dao.getAllCompletionRecordsOnce()

    suspend fun insertCompletionRecord(record: CompletionRecord) = dao.insertCompletionRecord(record)
    
    suspend fun getCompletionRecord(epochDay: Long, type: String, itemId: Int): CompletionRecord? = 
        dao.getCompletionRecord(epochDay, type, itemId)

    // Goals
    val allGoals: Flow<List<Goal>> = dao.getAllGoals()
    fun getGoalById(id: Int): Flow<Goal?> = dao.getGoalById(id)
    suspend fun getGoalByIdOnce(id: Int): Goal? = dao.getGoalByIdOnce(id)
    suspend fun insertGoal(goal: Goal): Long = dao.insertGoal(goal)
    suspend fun updateGoal(goal: Goal) = dao.updateGoal(goal)
    suspend fun deleteGoalById(id: Int) = dao.deleteGoalById(id)

    // Goal Steps
    fun getStepsForGoal(goalId: Int): Flow<List<GoalStep>> = dao.getStepsForGoal(goalId)
    suspend fun getStepsForGoalOnce(goalId: Int): List<GoalStep> = dao.getStepsForGoalOnce(goalId)
    fun getStepsDueOn(epochDay: Long): Flow<List<GoalStep>> = dao.getStepsDueOn(epochDay)
    suspend fun getAllActiveStepReminders(): List<GoalStep> = dao.getAllActiveStepReminders()
    suspend fun insertGoalStep(step: GoalStep): Long = dao.insertGoalStep(step)
    suspend fun updateGoalStep(step: GoalStep) = dao.updateGoalStep(step)
    suspend fun deleteGoalStepById(id: Int) = dao.deleteGoalStepById(id)
    suspend fun getAllGoalStepsOnce(): List<GoalStep> = dao.getAllGoalStepsOnce()
    val allGoalSteps: Flow<List<GoalStep>> = dao.getAllGoalSteps()
    suspend fun swapSteps(a: GoalStep, b: GoalStep) = dao.swapSteps(a, b)

    suspend fun replaceAllData(
        routines: List<RoutineItem>,
        checklists: List<ChecklistItem>,
        goals: List<Goal> = emptyList(),
        steps: List<GoalStep> = emptyList(),
        completionRecords: List<CompletionRecord> = emptyList()
    ) {
        dao.replaceAllData(routines, checklists, goals, steps, completionRecords)
    }
}
