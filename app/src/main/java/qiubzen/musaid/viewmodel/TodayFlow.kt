package qiubzen.musaid.viewmodel

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Emits today's epoch day and re-emits whenever the local date rolls over, so flows
 * that key queries on "today" never stay frozen across midnight when a composition
 * keeps collecting (e.g. the app left open overnight).
 */
internal fun todayEpochDayFlow() = flow {
    while (true) {
        val today = LocalDate.now()
        emit(today.toEpochDay())
        val nextMidnight = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val millisUntil = Duration.between(Instant.now(), nextMidnight).toMillis()
        delay(millisUntil.coerceAtLeast(1_000L))
    }
}
