package dev.sjaramillo.pedometer.data

import java.time.LocalDate
import javax.inject.Inject

class FakeStepsProvider @Inject constructor() : StepsProvider {
    override val isFake = true

    override suspend fun access(): StepsAccess = StepsAccess.Available

    override suspend fun readDailySteps(
        start: LocalDate,
        end: LocalDate,
    ): Map<LocalDate, Long> =
        if (end < start) {
            emptyMap()
        } else {
            generateSequence(start) { it.plusDays(1).takeIf { day -> day <= end } }
                .associateWith { SAMPLE_COUNTS[Math.floorMod(it.toEpochDay(), SAMPLE_COUNTS.size.toLong()).toInt()] }
        }

    private companion object {
        val SAMPLE_COUNTS = longArrayOf(1500, 4500, 7500, 10500, 14000)
    }
}
