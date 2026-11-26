package dev.sjaramillo.nemi.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HealthConnectSyncCoordinatorTest {
    @Test
    fun `given missing permission when refreshing then skips cache synchronization`() =
        runBlocking {
            // Given
            var synced = false
            val coordinator = HealthConnectSyncCoordinator({ HealthConnectAccess.PermissionRequired }, { synced = true })

            // When
            coordinator.refresh()

            // Then
            assertEquals(HealthConnectSyncState.PermissionRequired, coordinator.state.value)
            assertFalse(synced)
        }

    @Test
    fun `given unsupported phone step tracking when refreshing then reports the reason and skips synchronization`() =
        runBlocking {
            // Given
            val unsupported =
                mapOf(
                    HealthConnectAccess.Unavailable to HealthConnectSyncState.Unavailable,
                    HealthConnectAccess.UpdateRequired to HealthConnectSyncState.UpdateRequired,
                    HealthConnectAccess.NoStepCounter to HealthConnectSyncState.NoStepCounter,
                )

            for ((access, expectedState) in unsupported) {
                var synced = false
                val coordinator = HealthConnectSyncCoordinator({ access }, { synced = true })

                // When
                coordinator.refresh()

                // Then
                assertEquals(expectedState, coordinator.state.value)
                assertFalse(synced)
            }
        }

    @Test
    fun `given available Health Connect when refreshing then syncs cache and becomes ready`() =
        runBlocking {
            // Given
            var synced = false
            val coordinator = HealthConnectSyncCoordinator({ HealthConnectAccess.Available }, { synced = true })

            // When
            coordinator.refresh()

            // Then
            assertTrue(synced)
            assertEquals(HealthConnectSyncState.Ready, coordinator.state.value)
        }

    @Test
    fun `given an access failure when refreshing then reports an error`() =
        runBlocking {
            // Given
            val coordinator = HealthConnectSyncCoordinator({ error("Access failed") }, {})

            // When
            coordinator.refresh()

            // Then
            assertEquals(HealthConnectSyncState.Error, coordinator.state.value)
        }

    @Test
    fun `given a sync failure when refreshing then reports an error`() =
        runBlocking {
            // Given
            val coordinator = HealthConnectSyncCoordinator({ HealthConnectAccess.Available }, { error("Sync failed") })

            // When
            coordinator.refresh()

            // Then
            assertEquals(HealthConnectSyncState.Error, coordinator.state.value)
        }

    @Test
    fun `given a cancelled sync when refreshing then propagates cancellation`() =
        runBlocking {
            // Given
            val cancellation = CancellationException()
            val coordinator = HealthConnectSyncCoordinator({ HealthConnectAccess.Available }, { throw cancellation })

            // When
            val failure = runCatching { coordinator.refresh() }.exceptionOrNull()

            // Then
            assertSame(cancellation, failure)
        }
}
