package dev.sjaramillo.pedometer.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HealthConnectSyncCoordinatorTest {
    @Test
    fun `missing permission does not synchronize`() =
        runBlocking {
            val cache = RecordingCache()
            val coordinator = HealthConnectSyncCoordinator(TestProvider(StepsAccess.PermissionRequired), cache)

            coordinator.refresh()

            assertEquals(HealthConnectSyncState.PermissionRequired, coordinator.state.value)
            assertFalse(cache.synced)
        }

    @Test
    fun `unavailable provider does not synchronize`() =
        runBlocking {
            val cache = RecordingCache()
            val coordinator = HealthConnectSyncCoordinator(TestProvider(StepsAccess.Unavailable), cache)

            coordinator.refresh()

            assertEquals(HealthConnectSyncState.Unavailable, coordinator.state.value)
            assertFalse(cache.synced)
        }

    @Test
    fun `available provider synchronizes before becoming ready`() =
        runBlocking {
            val cache = RecordingCache()
            val coordinator = HealthConnectSyncCoordinator(TestProvider(StepsAccess.Available), cache)

            coordinator.refresh()

            assertTrue(cache.synced)
            assertEquals(HealthConnectSyncState.Ready, coordinator.state.value)
        }

    @Test
    fun `access failures become errors`() =
        runBlocking {
            val coordinator = HealthConnectSyncCoordinator(TestProvider(failure = IllegalStateException()), RecordingCache())

            coordinator.refresh()

            assertEquals(HealthConnectSyncState.Error, coordinator.state.value)
        }

    @Test
    fun `sync failures become errors`() =
        runBlocking {
            val coordinator = HealthConnectSyncCoordinator(TestProvider(), RecordingCache(IllegalStateException()))

            coordinator.refresh()

            assertEquals(HealthConnectSyncState.Error, coordinator.state.value)
        }

    @Test
    fun `cancellation propagates`() =
        runBlocking {
            val coordinator = HealthConnectSyncCoordinator(TestProvider(), RecordingCache(CancellationException()))
            var cancelled = false

            try {
                coordinator.refresh()
            } catch (error: CancellationException) {
                cancelled = true
            }

            assertTrue(cancelled)
        }

    @Test
    fun `provider change waits for prior cache commit`() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val finish = CompletableDeferred<Unit>()
            var fake = false
            val commits = mutableListOf<Boolean>()
            val cache =
                object : StepCacheSynchronizer {
                    override suspend fun sync() {
                        if (!fake) {
                            started.complete(Unit)
                            finish.await()
                        }
                        commits.add(fake)
                    }
                }
            val coordinator = HealthConnectSyncCoordinator(TestProvider(), cache)
            val refresh = launch { coordinator.refresh() }
            started.await()
            val switch =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    coordinator.refreshAfter { fake = true }
                }
            assertFalse(fake)
            finish.complete(Unit)
            refresh.join()
            switch.join()

            assertEquals(listOf(false, true), commits)
            assertEquals(HealthConnectSyncState.Ready, coordinator.state.value)
        }

    private class TestProvider(
        private val access: StepsAccess = StepsAccess.Available,
        private val failure: Exception? = null,
    ) : StepsProvider {
        override suspend fun access(): StepsAccess {
            failure?.let { throw it }
            return access
        }

        override suspend fun readDailySteps(
            start: LocalDate,
            end: LocalDate,
        ): Map<LocalDate, Long> = emptyMap()
    }

    private class RecordingCache(private val failure: Exception? = null) : StepCacheSynchronizer {
        var synced = false

        override suspend fun sync() {
            failure?.let { throw it }
            synced = true
        }
    }
}
