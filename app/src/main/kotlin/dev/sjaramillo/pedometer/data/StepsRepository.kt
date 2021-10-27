package dev.sjaramillo.pedometer.data

import dev.sjaramillo.pedometer.util.DateUtil

// TODO Provide through DI
class StepsRepository(db: PedometerDatabase) {

    private val dailyStepsDao = db.dailyStepsDao()

    /**
     * If date is Util.getToday(), this method returns the offset which needs to
     * be added to the value returned by getCurrentSteps() to get today's steps.
     *
     * Returns the steps taken on this date or Long.MIN_VALUE if date doesn't
     * exist in the database
     */
    fun getSteps(day: Long): Long {
        return dailyStepsDao.getSteps(day) ?: Long.MIN_VALUE
    }

    fun getLastEntries(num: Int): List<DailySteps> {
        return dailyStepsDao.getLastEntries(num)
    }

    fun getTotalWithoutToday(): Long {
        return dailyStepsDao.getStepsFromDayRange(start = 0, end = DateUtil.getToday())
    }

    /**
     * Returns the current number of steps saved in the database or 0 if there is no entry
     */
    fun getStepsSinceBoot(): Long {
        return dailyStepsDao.getSteps(-1) ?: 0
    }

    fun getDays(): Long {
        return dailyStepsDao.getDaysWithoutToday(DateUtil.getToday()) + 1
    }

    fun updateStepsSinceBoot(steps: Long) {
        val stepsSinceBoot = DailySteps(day = -1, steps = steps)
        dailyStepsDao.insert(stepsSinceBoot)
    }

    fun addToLastEntry(steps: Long) {
        dailyStepsDao.addToLastEntry(steps)
    }

    /**
     * Inserts a new entry in the database, if there is no entry for the given
     * date yet. Steps should be the current number of steps and it's negative
     * value will be used as offset for the new date. Also adds 'steps' steps to
     * the previous day, if there is an entry for that date.
     *
     *
     * This method does nothing if there is already an entry for 'date' - use
     * [.saveCurrentSteps] in this case.
     *
     *
     * To restore data from a backup, use [.insertDayFromBackup]
     *
     * @param day  the Epoc Day, where day 0 is 1970-01-01
     * @param steps the current step value to be used as negative offset for the
     * new day; must be >= 0
     */
    fun insertNewDay(day: Long, steps: Long) {
        if (dailyStepsDao.getSteps(day) == null) {
            addToLastEntry(steps)

            val today = DailySteps(day = day, steps = -steps)
            dailyStepsDao.insert(today)
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

    // This method might no longer be necessary if last entry does not store a negative value
    fun removeNegativeEntries() {
        dailyStepsDao.removeNegativeEntries()
    }
}
