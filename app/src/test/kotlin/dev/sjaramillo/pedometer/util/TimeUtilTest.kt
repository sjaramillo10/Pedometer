package dev.sjaramillo.pedometer.util

import org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class TimeUtilTest {

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun `Given today is Oct 24th, 2021 - unix day should be 18924`() {
        val today = LocalDate.of(2021, 10, 24) // Oct 24th, 2021
        val unixDays = TimeUtil.getUnixDay(today)

        assertEquals(18924L, unixDays)
    }
}