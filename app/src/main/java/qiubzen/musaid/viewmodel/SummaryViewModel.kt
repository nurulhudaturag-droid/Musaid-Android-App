package qiubzen.musaid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import qiubzen.musaid.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth

data class MonthlySummary(
    val totalReportDays: Int,
    val successRate: Float,
    val itemBreakdown: List<ItemSummary>
)

data class ItemSummary(
    val title: String,
    val completionRate: Float
)

data class ActiveGoalSummary(
    val id: Int,
    val title: String,
    val completedSteps: Int,
    val totalSteps: Int,
    val progress: Float,
    val daysRemaining: Long
)

class SummaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val dao = AppDatabase.getDatabase(application).appDao()
        repository = AppRepository(dao)
    }

    val activeGoals: Flow<List<ActiveGoalSummary>> = combine(
        repository.allGoals,
        repository.allGoalSteps,
        todayEpochDayFlow()
    ) { goals, allSteps, todayEpochDay ->
        val stepsByGoal = allSteps.groupBy { it.goalId }
        goals.filter { it.status == Goal.STATUS_IN_PROGRESS }.map { goal ->
            val steps = stepsByGoal[goal.id].orEmpty()
            val total = steps.size
            val completed = steps.count { it.isCompleted }
            val progress = if (total > 0) completed.toFloat() / total else 0f
            val daysRemaining = (goal.targetDateEpochDay - todayEpochDay).coerceAtLeast(0)
            ActiveGoalSummary(goal.id, goal.title, completed, total, progress, daysRemaining)
        }
    }

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    val summaryData: Flow<MonthlySummary?> = _selectedMonth.flatMapLatest { month ->
        val startDay = month.atDay(1).toEpochDay()
        val endDay = month.atEndOfMonth().toEpochDay()
        
        repository.getCompletionRecordsInDateRange(startDay, endDay).map { records ->
            if (records.isEmpty()) return@map null
            
            val daysWithReports = records.map { it.dateEpochDay }.distinct().size
            val totalItems = records.size
            val completedItems = records.count { it.isCompleted }
            
            val overallSuccessRate = if (totalItems > 0) completedItems.toFloat() / totalItems else 0f
            
            val itemBreakdown = records.groupBy { it.itemTitleSnapshot }.map { (title, itemRecords) ->
                val total = itemRecords.size
                val completed = itemRecords.count { it.isCompleted }
                ItemSummary(title, if (total > 0) completed.toFloat() / total else 0f)
            }.sortedByDescending { it.completionRate }
            
            MonthlySummary(daysWithReports, overallSuccessRate, itemBreakdown)
        }
    }
}
