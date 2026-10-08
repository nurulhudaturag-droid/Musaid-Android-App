package qiubzen.musaid.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "checklist_items")
@JsonClass(generateAdapter = true)
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isActive: Boolean = true
)
