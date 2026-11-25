package dev.sjaramillo.nemi.data

import kotlin.test.Test
import kotlin.test.assertEquals

class PhoneStepsDataSourceTest {
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
