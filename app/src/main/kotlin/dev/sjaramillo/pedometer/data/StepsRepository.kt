package dev.sjaramillo.pedometer.data

import dev.sjaramillo.pedometer.util.DateUtil
import kotlin.math.max

// TODO Provide through DI
class StepsRepository(db: PedometerDatabase) {

    private val dailyStepsDao = db.dailyStepsDao()

    fun getAll(): List<DailySteps> {
        return dailyStepsDao.getAll()
    }

    fun getStepsToday(): Long {
        val today = DateUtil.getToday()
        return getSteps(today)
    }

    /**
     * Returns the steps taken on this day or 0 if date doesn't
     * exist in the database
     */
    fun getSteps(day: Long): Long {
        return dailyStepsDao.getSteps(day) ?: 0
    }

    fun getRecord(): DailySteps {
        return dailyStepsDao.getRecord()
    }

    fun getLastEntries(num: Int): List<DailySteps> {
        return dailyStepsDao.getLastEntries(num)
    }

    fun getStepsFromDayRange(start: Long, end: Long): Long {
        return dailyStepsDao.getStepsFromDayRange(start, end)
    }

    fun getTotalWithoutToday(): Long {
        return dailyStepsDao.getStepsFromDayRange(start = 0, end = DateUtil.getToday() - 1)
    }

    /**
     * Returns the current number of steps saved in the database or 0 if there is no entry
     */ // TODO Make private
    fun getStepsSinceBoot(): Long {
        return dailyStepsDao.getSteps(-1) ?: 0
    }

    fun getDays(): Long {
        return dailyStepsDao.getDaysWithoutToday(DateUtil.getToday()) + 1
    }

    /**
     * TODO
     */
    fun updateStepsSinceBoot(steps: Long): Long {
        val today = DateUtil.getToday()
        val todaySteps = dailyStepsDao.getSteps(today)
        val storedStepsSinceBoot = getStepsSinceBoot()

        // Make sure stepsDiff is at least 0. The only time when steps could be less than
        // storedStepsSinceBoot is when the phone reboots.
        val stepsDiff = max(steps - storedStepsSinceBoot, 0L)

        if (todaySteps == null) {
            // This is a new day, update last day and insert a new one
            addToLastEntry(stepsDiff)
            insertTodayEntry(today)
        } else {
            // Still same day, just add the steps diff
            addToLastEntry(stepsDiff)
        }

        // Update copy of steps since boot in db
        val stepsSinceBoot = DailySteps(day = -1, steps = steps)
        dailyStepsDao.insert(stepsSinceBoot)

        return getSteps(today)
    }

    private fun addToLastEntry(steps: Long) {
        dailyStepsDao.addToLastEntry(steps)
    }

    /**
     * TODO
     *
     * @param today  the Epoc Day, where day 0 is 1970-01-01
     */
    private fun insertTodayEntry(today: Long) {
        if (dailyStepsDao.getSteps(today) == null) {
            val todaySteps = DailySteps(day = today, steps = 0)
            dailyStepsDao.insert(todaySteps)
        }
    }

    /**
     * Inserts a new entry in the database, overwriting any existing entry for the given date.
     * Use this method for restoring data from a backup.
     *
     * @param day   the Epoc Day, where day 0 is 1970-01-01
     * @param steps the step value for 'date'; must be >= 0
     * @return true if a new entry was created, false if there was already an
     * entry for 'date' (and it was overwritten)
     */
    fun insertDayFromBackup(day: Long, steps: Long): Boolean {
        val dailySteps = DailySteps(day = day, steps = steps)
        val updatedRows = dailyStepsDao.update(dailySteps)
        if (updatedRows == 0) {
            dailyStepsDao.insert(dailySteps)
            return true
        }
        return false
    }
}
