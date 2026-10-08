package qiubzen.musaid.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // Routines
    @Query("SELECT * FROM routines ORDER BY timeMillis ASC")
    fun getAllRoutines(): Flow<List<RoutineItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineItem): Long

    @Update
    suspend fun updateRoutine(routine: RoutineItem)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutineById(id: Int)

    // Checklists
    @Query("SELECT * FROM checklist_items")
    fun getAllChecklists(): Flow<List<ChecklistItem>>
    
    @Query("SELECT * FROM checklist_items WHERE isActive = 1")
    fun getActiveChecklists(): Flow<List<ChecklistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: ChecklistItem): Long

    @Update
    suspend fun updateChecklist(checklist: ChecklistItem)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteChecklistById(id: Int)

    // Completion Records
    @Query("SELECT * FROM completion_records WHERE dateEpochDay = :epochDay")
    fun getCompletionRecordsByDate(epochDay: Long): Flow<List<CompletionRecord>>

    @Query("SELECT * FROM completion_records WHERE dateEpochDay >= :startEpochDay AND dateEpochDay <= :endEpochDay")
    fun getCompletionRecordsInDateRange(startEpochDay: Long, endEpochDay: Long): Flow<List<CompletionRecord>>

    @Query("SELECT * FROM completion_records")
    suspend fun getAllCompletionRecordsOnce(): List<CompletionRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletionRecord(record: CompletionRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletionRecords(records: List<CompletionRecord>)

    @Query("SELECT * FROM completion_records WHERE dateEpochDay = :epochDay AND itemType = :type AND itemId = :itemId LIMIT 1")
    suspend fun getCompletionRecord(epochDay: Long, type: String, itemId: Int): CompletionRecord?

    @Query("DELETE FROM routines")
    suspend fun deleteAllRoutines()

    @Query("DELETE FROM completion_records")
    suspend fun deleteAllCompletionRecords()

    @Query("DELETE FROM checklist_items")
    suspend fun deleteAllChecklists()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutines(routines: List<RoutineItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklists(checklists: List<ChecklistItem>)
    
    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Int): RoutineItem?

    // Goals
    @Query("SELECT * FROM goals ORDER BY targetDateEpochDay ASC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id")
    fun getGoalById(id: Int): Flow<Goal?>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalByIdOnce(id: Int): Goal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Int)

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<Goal>)

    // Goal Steps
    @Query("SELECT * FROM goal_steps WHERE goalId = :goalId ORDER BY orderIndex ASC")
    fun getStepsForGoal(goalId: Int): Flow<List<GoalStep>>

    @Query("SELECT * FROM goal_steps WHERE goalId = :goalId ORDER BY orderIndex ASC")
    suspend fun getStepsForGoalOnce(goalId: Int): List<GoalStep>

    @Query("SELECT * FROM goal_steps WHERE targetDateEpochDay = :epochDay AND isCompleted = 0")
    fun getStepsDueOn(epochDay: Long): Flow<List<GoalStep>>

    @Query("SELECT * FROM goal_steps WHERE reminderEnabled = 1 AND isCompleted = 0")
    suspend fun getAllActiveStepReminders(): List<GoalStep>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalStep(step: GoalStep): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalSteps(steps: List<GoalStep>)

    @Update
    suspend fun updateGoalStep(step: GoalStep)

    @Query("DELETE FROM goal_steps WHERE id = :id")
    suspend fun deleteGoalStepById(id: Int)

    @Query("SELECT * FROM goal_steps")
    suspend fun getAllGoalStepsOnce(): List<GoalStep>

    @Query("SELECT * FROM goal_steps")
    fun getAllGoalSteps(): Flow<List<GoalStep>>

    /**
     * Atomic reorder: both rows update in one transaction, so a concurrent write can
     * never observe a half-swapped orderIndex pair (two sequential @Update calls could
     * interleave and duplicate/skip an order).
     */
    @Transaction
    suspend fun swapSteps(a: GoalStep, b: GoalStep) {
        updateGoalStep(a.copy(orderIndex = b.orderIndex))
        updateGoalStep(b.copy(orderIndex = a.orderIndex))
    }

    @Query("DELETE FROM goal_steps")
    suspend fun deleteAllGoalSteps()

    /**
     * Full replace used by JSON import. Runs as a single Room transaction so a crash
     * or cancellation mid-import rolls back to the previous data instead of leaving
     * the database half-wiped.
     */
    @Transaction
    suspend fun replaceAllData(
        routines: List<RoutineItem>,
        checklists: List<ChecklistItem>,
        goals: List<Goal>,
        steps: List<GoalStep>,
        completionRecords: List<CompletionRecord>
    ) {
        deleteAllRoutines()
        deleteAllChecklists()
        deleteAllGoalSteps()
        deleteAllGoals()
        // The imported backup carries its own records (same ids as its items), so old
        // records are dropped together with the old items — no id re-attribution.
        deleteAllCompletionRecords()
        insertRoutines(routines)
        insertChecklists(checklists)
        if (goals.isNotEmpty()) insertGoals(goals)
        if (steps.isNotEmpty()) insertGoalSteps(steps)
        if (completionRecords.isNotEmpty()) insertCompletionRecords(completionRecords)
    }
}
