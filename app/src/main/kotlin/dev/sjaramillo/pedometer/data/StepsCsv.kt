package dev.sjaramillo.pedometer.data

internal data class StepsCsvImport(
    val dailySteps: List<DailySteps>,
    val ignoredRows: Int,
)

internal fun encodeStepsCsv(dailySteps: List<DailySteps>): String =
    buildString {
        dailySteps
            .filter { it.day > 0 }
            .sortedBy(DailySteps::day)
            .forEach { dailySteps ->
                append(dailySteps.day)
                append(',')
                append(dailySteps.steps.coerceAtLeast(0))
                append('\n')
            }
    }

internal fun decodeStepsCsv(csv: String): StepsCsvImport {
    var ignoredRows = 0
    val stepsByDay = mutableMapOf<Long, DailySteps>()

    csv.lineSequence().forEach { line ->
        if (line.isBlank()) return@forEach

        val values = line.split(',', limit = 3)
        val day = values.getOrNull(0)?.trim()?.toLongOrNull()
        val steps = values.getOrNull(1)?.trim()?.toLongOrNull()
        if (values.size != 2 || day == null || day <= 0 || steps == null || steps < 0) {
            ignoredRows++
        } else {
            stepsByDay[day] = DailySteps(day, steps)
        }
    }

    return StepsCsvImport(stepsByDay.values.sortedBy(DailySteps::day), ignoredRows)
}
