package qiubzen.musaid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import qiubzen.musaid.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReportItem(
    val type: String,
    val id: Int,
    val title: String,
    val isCompleted: Boolean,
    val existingRecordId: Int? = null
)

enum class PastReportStatus {
    SUBMITTED,
    PARTIAL,
    MISSED,
    NOT_APPLICABLE
}

data class PastDaySummary(
    val date: LocalDate,
    val status: PastReportStatus,
    val recordedCount: Int,
    val totalApplicable: Int
)

class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val dao = AppDatabase.getDatabase(application).appDao()
        repository = AppRepository(dao)
    }

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    fun setDate(date: LocalDate) {
        val today = LocalDate.now()
        // Do not allow selecting future dates
        if (!date.isAfter(today)) {
            _selectedDate.value = date
        }
    }

    fun canGoToNextDay(): Boolean {
        return _selectedDate.value.isBefore(LocalDate.now())
    }

    fun nextDay() {
        if (canGoToNextDay()) {
            _selectedDate.value = _selectedDate.value.plusDays(1)
        }
    }

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun resetToToday() {
        _selectedDate.value = LocalDate.now()
    }

    val reportItems: Flow<List<ReportItem>> = _selectedDate.flatMapLatest { date ->
        val epochDay = date.toEpochDay()
        val currentDay = if (date.dayOfWeek.value == 7) 0 else date.dayOfWeek.value
        
        combine(
            repository.allRoutines,
            repository.activeChecklists,
            repository.getCompletionRecordsByDate(epochDay)
        ) { routines, checklists, records ->
            val items = mutableListOf<ReportItem>()
            
            // Routines applicable on this date
            routines.filter { it.daysOfWeek.contains(currentDay) }.forEach { routine ->
                val record = records.find { it.itemType == "ROUTINE" && it.itemId == routine.id }
                items.add(ReportItem("ROUTINE", routine.id, routine.title, record?.isCompleted ?: false, record?.id))
            }
            
            // Current active checklists
            checklists.forEach { checklist ->
                val record = records.find { it.itemType == "CHECKLIST" && it.itemId == checklist.id }
                items.add(ReportItem("CHECKLIST", checklist.id, checklist.title, record?.isCompleted ?: false, record?.id))
            }
            
            items
        }
    }

    // Past 30 days status summary — keyed to a midnight-ticking "today" so a screen
    // kept open across midnight cannot keep querying yesterday's window.
    val past30DaysSummary: Flow<List<PastDaySummary>> = todayEpochDayFlow().flatMapLatest { todayEpochDay ->
        val today = LocalDate.ofEpochDay(todayEpochDay)
        val startDate = today.minusDays(29)

        combine(
            repository.allRoutines,
            repository.activeChecklists,
            repository.getCompletionRecordsInDateRange(startDate.toEpochDay(), today.toEpochDay())
        ) { routines, checklists, allRecords ->
            val list = mutableListOf<PastDaySummary>()
            val recordsByDate = allRecords.groupBy { it.dateEpochDay }

            for (i in 0..29) {
                val date = today.minusDays(i.toLong())
                val epochDay = date.toEpochDay()
                val currentDay = if (date.dayOfWeek.value == 7) 0 else date.dayOfWeek.value

                // Applicable set is derived per (past) day: routines by their weekly
                // schedule, checklists by current active flag (historical active
                // state is not stored).
                val applicableRoutineIds = routines
                    .filter { it.daysOfWeek.contains(currentDay) }
                    .map { it.id }
                    .toSet()
                val applicableChecklistIds = checklists.map { it.id }.toSet()
                val totalApplicable = applicableRoutineIds.size + applicableChecklistIds.size

                val dateRecords = recordsByDate[epochDay] ?: emptyList()
                // Only completed ticks count; unchecking an item writes isCompleted=false.
                val recordedCount = dateRecords.count { record ->
                    record.isCompleted && (
                        (record.itemType == "ROUTINE" && record.itemId in applicableRoutineIds) ||
                            (record.itemType == "CHECKLIST" && record.itemId in applicableChecklistIds)
                        )
                }

                val status = when {
                    totalApplicable == 0 -> PastReportStatus.NOT_APPLICABLE
                    recordedCount >= totalApplicable -> PastReportStatus.SUBMITTED
                    recordedCount > 0 -> PastReportStatus.PARTIAL
                    else -> PastReportStatus.MISSED
                }

                list.add(PastDaySummary(date, status, recordedCount, totalApplicable))
            }
            list
        }
    }

    fun toggleItemCompletion(item: ReportItem) {
        viewModelScope.launch {
            val date = _selectedDate.value.toEpochDay()
            val newCompleted = !item.isCompleted
            
            // Check if existing record already exists for this itemId and type and date
            val existing = repository.getCompletionRecord(date, item.type, item.id)
            val record = CompletionRecord(
                id = existing?.id ?: item.existingRecordId ?: 0,
                dateEpochDay = date,
                itemType = item.type,
                itemId = item.id,
                itemTitleSnapshot = item.title,
                isCompleted = newCompleted
            )
            repository.insertCompletionRecord(record)
        }
    }
}
