package qiubzen.musaid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import qiubzen.musaid.alarm.AlarmScheduler
import qiubzen.musaid.data.*
import qiubzen.musaid.widget.RoutineWidget
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository
    private val alarmScheduler: AlarmScheduler

    init {
        val dao = AppDatabase.getDatabase(application).appDao()
        repository = AppRepository(dao)
        alarmScheduler = AlarmScheduler(application)
    }

    val routines: StateFlow<List<RoutineItem>> = repository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val checklists: StateFlow<List<ChecklistItem>> = repository.allChecklists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayDueSteps: StateFlow<List<Pair<GoalStep, String>>> = todayEpochDayFlow()
        .flatMapLatest { today -> repository.getStepsDueOn(today) }
        .combine(repository.allGoals) { steps, goals ->
            val titlesById = goals.associate { it.id to it.title }
            steps.map { step -> step to (titlesById[step.goalId] ?: "টার্গেট") }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Routines
    fun addRoutine(title: String, timeMillis: Long, alarmMessage: String, daysOfWeek: List<Int>) {
        viewModelScope.launch {
            val routine = RoutineItem(
                title = title,
                timeMillis = timeMillis,
                alarmMessage = alarmMessage,
                daysOfWeek = daysOfWeek,
                isAlarmEnabled = true
            )
            val id = repository.insertRoutine(routine)
            alarmScheduler.scheduleRoutineAlarm(routine.copy(id = id.toInt()))
            RoutineWidget().updateAll(getApplication())
        }
    }

    fun updateRoutine(routine: RoutineItem) {
        viewModelScope.launch {
            repository.updateRoutine(routine)
            if (routine.isAlarmEnabled) {
                alarmScheduler.scheduleRoutineAlarm(routine)
            } else {
                alarmScheduler.cancelRoutineAlarm(routine.id)
            }
            RoutineWidget().updateAll(getApplication())
        }
    }

    fun toggleRoutineAlarm(routine: RoutineItem, isEnabled: Boolean) {
        val updated = routine.copy(isAlarmEnabled = isEnabled)
        updateRoutine(updated)
    }

    fun deleteRoutine(routine: RoutineItem) {
        viewModelScope.launch {
            repository.deleteRoutineById(routine.id)
            alarmScheduler.cancelRoutineAlarm(routine.id)
            RoutineWidget().updateAll(getApplication())
        }
    }

    // Checklists
    fun addChecklist(title: String) {
        viewModelScope.launch {
            repository.insertChecklist(ChecklistItem(title = title))
        }
    }

    fun updateChecklist(checklist: ChecklistItem) {
        viewModelScope.launch {
            repository.updateChecklist(checklist)
        }
    }

    fun deleteChecklist(checklist: ChecklistItem) {
        viewModelScope.launch {
            repository.deleteChecklistById(checklist.id)
        }
    }

    fun toggleChecklistActive(checklist: ChecklistItem, isActive: Boolean) {
        updateChecklist(checklist.copy(isActive = isActive))
    }
}
