package qiubzen.musaid.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "routines")
@JsonClass(generateAdapter = true)
data class RoutineItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val timeMillis: Long,           // milliseconds since midnight
    val alarmMessage: String = "",
    val isAlarmEnabled: Boolean = true,
    val daysOfWeek: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6) // 0=Sunday ... 6=Saturday
)
