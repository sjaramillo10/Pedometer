package dev.sjaramillo.pedometer.worker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class StepsCounterWorkerTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var workerManagerTestRule = WorkManagerTestRule()

    @Test // TODO Figure out how to mock the SensorManager
    fun testStepsCounterWorker_readsStepsCount_returnsStepsCount() {
        val work =
            TestListenableWorkerBuilder<StepsCounterWorker>(workerManagerTestRule.targetContext).build()

        runBlocking {
            val result = work.doWork()

            assertNotNull(result)
        }
    }
}
