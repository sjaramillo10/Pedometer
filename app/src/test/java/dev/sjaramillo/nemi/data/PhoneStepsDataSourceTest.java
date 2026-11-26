package dev.sjaramillo.nemi.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.Set;

public class PhoneStepsDataSourceTest {
    @Test
    public void givenExtensionsBelow20OrMissingPlatformFeatures_whenCheckingAccess_thenReportsTheReason() {
        // Given
        var requiredExtension = 20;

        // When
        var serviceMissing = PhoneStepsDataSource.access(false, requiredExtension, true, false);
        var extensionOld = PhoneStepsDataSource.access(true, requiredExtension - 1, true, true);
        var sensorMissing = PhoneStepsDataSource.access(true, requiredExtension, false, false);

        // Then
        assertEquals(PhoneStepsDataSource.Access.Unavailable, serviceMissing);
        assertEquals(PhoneStepsDataSource.Access.UpdateRequired, extensionOld);
        assertEquals(PhoneStepsDataSource.Access.NoStepCounter, sensorMissing);
    }

    @Test
    public void givenExtension20AndAStepCounter_whenCheckingAccess_thenRequiresTheStepsPermission() {
        // Given
        int extensionVersion = 20;

        // When
        var beforeGrant = PhoneStepsDataSource.access(true, extensionVersion, true, false);
        var afterGrant = PhoneStepsDataSource.access(true, extensionVersion, true, true);

        // Then
        assertEquals(PhoneStepsDataSource.Access.PermissionRequired, beforeGrant);
        assertEquals(PhoneStepsDataSource.Access.Available, afterGrant);
    }

    @Test
    public void givenAndroid14Through16_whenCheckingCurrentOriginSupport_thenRequiresExtension22() {
        // Given
        var androidVersions = java.util.stream.IntStream.rangeClosed(34, 36).toArray();

        // When
        boolean legacy = java.util.Arrays.stream(androidVersions)
                .anyMatch(version -> PhoneStepsDataSource.supportsCurrentDeviceOrigin(version, 21));
        boolean updated = java.util.Arrays.stream(androidVersions)
                .allMatch(version -> PhoneStepsDataSource.supportsCurrentDeviceOrigin(version, 22));

        // Then
        assertFalse(legacy);
        assertTrue(updated);
    }

    @Test
    public void givenAndroid17OrNewer_whenCheckingCurrentOriginSupport_thenAcceptsThePlatformApi() {
        // Given
        int extensionVersion = 20;

        // When
        var android17 = PhoneStepsDataSource.supportsCurrentDeviceOrigin(37, extensionVersion);
        var newer = PhoneStepsDataSource.supportsCurrentDeviceOrigin(38, extensionVersion);

        // Then
        assertFalse(PhoneStepsDataSource.supportsCurrentDeviceOrigin(36, extensionVersion));
        assertTrue(android17);
        assertTrue(newer);
    }

    @Test
    public void givenACurrentDeviceOrigin_whenBuildingThePhoneFilter_thenBothOriginsAreIncluded() {
        // Given
        var deviceOrigin = "com.android.healthconnect.phone.test";

        // When
        var origins = PhoneStepsDataSource.originPackages(deviceOrigin);

        // Then
        assertEquals(Set.of("android", deviceOrigin), origins);
    }

    @Test
    public void givenNoCurrentDeviceOrigin_whenBuildingThePhoneFilter_thenOnlyTheLegacyOriginIsIncluded() {
        // Given
        String deviceOrigin = null;

        // When
        var origins = PhoneStepsDataSource.originPackages(deviceOrigin);

        // Then
        assertEquals(Set.of("android"), origins);
    }

    @Test
    public void givenTheLegacyOriginAsTheDeviceOrigin_whenBuildingThePhoneFilter_thenItIsIncludedOnce() {
        // Given
        var deviceOrigin = "android";

        // When
        var origins = PhoneStepsDataSource.originPackages(deviceOrigin);

        // Then
        assertEquals(Set.of("android"), origins);
    }
}
