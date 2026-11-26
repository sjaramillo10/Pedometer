package dev.sjaramillo.nemi.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneStepsDataSourceTest {
    @Test
    fun `given extensions below 20 or missing platform features when checking access then reports the reason`() {
        // Given
        val requiredExtension = 20

        // When
        val serviceMissing = phoneStepsAccess(false, requiredExtension, true, false)
        val extensionOld = phoneStepsAccess(true, requiredExtension - 1, true, true)
        val sensorMissing = phoneStepsAccess(true, requiredExtension, false, false)

        // Then
        assertEquals(HealthConnectAccess.Unavailable, serviceMissing)
        assertEquals(HealthConnectAccess.UpdateRequired, extensionOld)
        assertEquals(HealthConnectAccess.NoStepCounter, sensorMissing)
    }

    @Test
    fun `given extension 20 and a step counter when checking access then requires the steps permission`() {
        // Given
        val extensionVersion = 20

        // When
        val beforeGrant = phoneStepsAccess(true, extensionVersion, true, false)
        val afterGrant = phoneStepsAccess(true, extensionVersion, true, true)

        // Then
        assertEquals(HealthConnectAccess.PermissionRequired, beforeGrant)
        assertEquals(HealthConnectAccess.Available, afterGrant)
    }

    @Test
    fun `given Android 14 through 16 when checking current origin support then requires extension 22`() {
        // Given
        val androidVersions = 34..36

        // When
        val legacy = androidVersions.map { supportsCurrentDeviceOrigin(it, 21) }
        val updated = androidVersions.map { supportsCurrentDeviceOrigin(it, 22) }

        // Then
        assertTrue(legacy.none { it })
        assertTrue(updated.all { it })
    }

    @Test
    fun `given Android 17 or newer when checking current origin support then accepts the platform API`() {
        // Given
        val extensionVersion = 20

        // When
        val android17 = supportsCurrentDeviceOrigin(37, extensionVersion)
        val newer = supportsCurrentDeviceOrigin(38, extensionVersion)

        // Then
        assertFalse(supportsCurrentDeviceOrigin(36, extensionVersion))
        assertTrue(android17)
        assertTrue(newer)
    }

    @Test
    fun `given a current device origin when building the phone filter then both origins are included`() {
        // Given
        val deviceOrigin = "com.android.healthconnect.phone.test"

        // When
        val origins = phoneStepOriginPackages(deviceOrigin)

        // Then
        assertEquals(setOf("android", deviceOrigin), origins)
    }

    @Test
    fun `given no current device origin when building the phone filter then only the legacy origin is included`() {
        // Given
        val deviceOrigin = null

        // When
        val origins = phoneStepOriginPackages(deviceOrigin)

        // Then
        assertEquals(setOf("android"), origins)
    }

    @Test
    fun `given the legacy origin as the device origin when building the phone filter then it is included once`() {
        // Given
        val deviceOrigin = "android"

        // When
        val origins = phoneStepOriginPackages(deviceOrigin)

        // Then
        assertEquals(setOf("android"), origins)
    }
}
