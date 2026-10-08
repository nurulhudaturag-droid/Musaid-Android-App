package qiubzen.musaid.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "completion_records",
    indices = [Index(
        value = ["dateEpochDay", "itemType", "itemId"],
        unique = true,
        name = "index_completion_records_date_type_item"
    )]
)
@JsonClass(generateAdapter = true)
data class CompletionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateEpochDay: Long,          // LocalDate.toEpochDay(), for easy date-range queries
    val itemType: String,            // "ROUTINE" or "CHECKLIST"
    val itemId: Int,
    val itemTitleSnapshot: String,
    val isCompleted: Boolean
)
