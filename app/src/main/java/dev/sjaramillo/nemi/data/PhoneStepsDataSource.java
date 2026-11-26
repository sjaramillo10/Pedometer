package dev.sjaramillo.nemi.data;

import static android.health.connect.HealthPermissions.READ_STEPS;
import static android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.health.connect.AggregateRecordsGroupedByPeriodResponse;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.DeviceDataSource;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.LocalTimeRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.os.Build;
import android.os.OutcomeReceiver;
import android.os.ext.SdkExtensions;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class PhoneStepsDataSource {
    enum Access { Available, PermissionRequired, Unavailable, UpdateRequired, NoStepCounter }

    private final Context context;

    public PhoneStepsDataSource(Context context) {
        this.context = context;
    }

    Access access() {
        var sensors = context.getSystemService(SensorManager.class);
        return access(context.getSystemService(HealthConnectManager.class) != null,
                extensionVersion(),
                sensors != null && sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null,
                context.checkSelfPermission(READ_STEPS) == PackageManager.PERMISSION_GRANTED);
    }

    static Access access(boolean hasHealthConnect, int extensionVersion, boolean hasStepCounter,
            boolean hasPermission) {
        if (!hasHealthConnect) {
            return Access.Unavailable;
        }
        if (extensionVersion < 20) {
            return Access.UpdateRequired;
        }
        if (!hasStepCounter) {
            return Access.NoStepCounter;
        }
        return hasPermission ? Access.Available : Access.PermissionRequired;
    }

    CompletableFuture<Map<LocalDate, Long>> readDailySteps(LocalDate start, LocalDate end) {
        var manager = Objects.requireNonNull(context.getSystemService(HealthConnectManager.class));
        var range = new LocalTimeRangeFilter.Builder()
                .setStartTime(start.atStartOfDay())
                .setEndTime(end.plusDays(1).atStartOfDay())
                .build();
        var request = new AggregateRecordsRequest.Builder<Long>(range)
                .addAggregationType(STEPS_COUNT_TOTAL);
        var origin = supportsCurrentDeviceOrigin(Build.VERSION.SDK_INT, extensionVersion())
                ? currentDeviceOrigin(manager) : CompletableFuture.<String>completedFuture(null);
        return origin.thenCompose(packageName -> {
            for (String entry : originPackages(packageName)) {
                request.addDataOriginsFilter(new DataOrigin.Builder().setPackageName(entry).build());
            }
            var future = new CompletableFuture<List<AggregateRecordsGroupedByPeriodResponse<Long>>>();
            manager.aggregateGroupByPeriod(request.build(), Period.ofDays(1),
                    context.getMainExecutor(), receiver(future));
            return future;
        }).thenApply(buckets -> {
            var totals = new HashMap<LocalDate, Long>();
            for (var bucket : buckets) {
                Long steps = bucket.get(STEPS_COUNT_TOTAL);
                if (steps != null) {
                    totals.put(bucket.getStartTime().toLocalDate(), steps);
                }
            }
            return totals;
        });
    }

    @SuppressLint("NewApi")
    private CompletableFuture<String> currentDeviceOrigin(HealthConnectManager manager) {
        var future = new CompletableFuture<DeviceDataSource>();
        try {
            manager.getCurrentDeviceDataSource(context.getMainExecutor(), receiver(future));
        } catch (Exception error) {
            future.completeExceptionally(error);
        }
        return future.thenApply(source -> source.getDeviceDataOrigin().getPackageName())
                .exceptionally(error -> null);
    }

    private static <T> OutcomeReceiver<T, HealthConnectException> receiver(
            CompletableFuture<T> future) {
        return new OutcomeReceiver<>() {
            @Override public void onResult(T result) {
                future.complete(result);
            }

            @Override public void onError(HealthConnectException error) {
                future.completeExceptionally(error);
            }
        };
    }

    private static int extensionVersion() {
        return SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE);
    }

    static boolean supportsCurrentDeviceOrigin(int sdkInt, int extensionVersion) {
        return sdkInt >= 37 || extensionVersion >= 22;
    }

    static Set<String> originPackages(String currentDeviceOrigin) {
        var packages = new LinkedHashSet<String>();
        packages.add("android");
        if (currentDeviceOrigin != null) {
            packages.add(currentDeviceOrigin);
        }
        return packages;
    }
}
