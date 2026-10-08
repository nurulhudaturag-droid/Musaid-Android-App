package qiubzen.musaid.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "goal_steps",
    foreignKeys = [
        ForeignKey(
            entity = Goal::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goalId"])]
)
@JsonClass(generateAdapter = true)
data class GoalStep(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int,
    val title: String,
    val targetDateEpochDay: Long,
    val orderIndex: Int,
    val isCompleted: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderTimeMillis: Long? = null
)
