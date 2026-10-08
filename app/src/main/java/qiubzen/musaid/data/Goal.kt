package qiubzen.musaid.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "goals")
@JsonClass(generateAdapter = true)
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val motivationNote: String = "",
    val startDateEpochDay: Long,
    val targetDateEpochDay: Long,
    val status: String = STATUS_IN_PROGRESS
) {
    companion object {
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_PAUSED = "PAUSED"
    }
}
