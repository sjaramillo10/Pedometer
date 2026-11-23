package dev.sjaramillo.pedometer.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `given unavailable Health Connect when refreshing then skips cache synchronization`() =
        runBlocking {
            // Given
            var synced = false
            val coordinator = HealthConnectSyncCoordinator({ HealthConnectAccess.Unavailable }, { synced = true })

            // When
            coordinator.refresh()

            // Then
            assertEquals(HealthConnectSyncState.Unavailable, coordinator.state.value)
            assertFalse(synced)
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
