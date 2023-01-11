/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.superior.android.settings.fuelgauge;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.util.SparseIntArray;

import com.android.internal.util.ArrayUtils;
import com.android.settingslib.fuelgauge.Estimate;

import com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl;
import com.android.settings.R;

import com.android.settingslib.utils.PowerUtil;

import java.time.Duration;
import java.util.List;

public class PowerUsageFeatureProviderSuperiorImpl extends PowerUsageFeatureProviderImpl {

    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PACKAGE_GCS = "com.google.android.apps.gcs";
    private static final String[] PACKAGES_SERVICE = {PACKAGE_GMS, PACKAGE_GCS};

    static final String AVERAGE_BATTERY_LIFE_COL = "average_battery_life";
    static final String BATTERY_ESTIMATE_BASED_ON_USAGE_COL = "is_based_on_usage";
    static final String BATTERY_ESTIMATE_COL = "battery_estimate";
    static final String BATTERY_LEVEL_COL = "battery_level";
    static final String IS_EARLY_WARNING_COL = "is_early_warning";
    static final String TIMESTAMP_COL = "timestamp_millis";
    static final int CUSTOMIZED_TO_USER = 1;
    static final int NEED_EARLY_WARNING = 1;

    public PowerUsageFeatureProviderSuperiorImpl(Context context) {
        super(context);
    }

    @Override
    public String getAdvancedUsageScreenInfoString() {
        return mContext.getString(R.string.advanced_battery_graph_subtext);
    }

    @Override
    public boolean isEnhancedBatteryPredictionEnabled(Context context) {
        try {
            return mPackageManager.getPackageInfo("com.google.android.apps.turbo",
                                    PackageManager.MATCH_DISABLED_COMPONENTS).applicationInfo.enabled;
        } catch (PackageManager.NameNotFoundException unused) {
            return false;
        }
    }


    @Override
    public SparseIntArray getEnhancedBatteryPredictionCurve(Context context, long zeroTime) {
        SparseIntArray curve = new SparseIntArray();
        Uri uri = this.getEnhancedBatteryPredictionCurveUri();
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);

        // Return null if cursor is null or empty
        if (cursor == null || !cursor.moveToFirst())
            return null;

        // Get time/battery data indicies
        int timestamp = cursor.getColumnIndex(TIMESTAMP_COL);
        int batteryLevel = cursor.getColumnIndex(BATTERY_LEVEL_COL);

        // Add time/battery data to a SparseIntArray and shift time data relative to starting time
        while (cursor.moveToNext()) {
            curve.append((int)(cursor.getLong(timestamp) - zeroTime), cursor.getInt(batteryLevel));
        }

        // Cleanup
        try {
            cursor.close();
        } catch (NullPointerException nullPointerException) {
            // We already checked if cursor is null, so it shouldn't be dereferenced yet.
        }
        return curve;
    }

    private Uri getEnhancedBatteryPredictionCurveUri() {
        return new Uri.Builder().scheme("content")
                                .authority("com.google.android.apps.turbo.estimated_time_remaining")
                                .appendPath("discharge_curve").build();
    }

    private Uri getEnhancedBatteryPredictionUri() {
        return new Uri.Builder().scheme("content")
                                .authority("com.google.android.apps.turbo.estimated_time_remaining")
                                .appendPath("time_remaining").build();
    }

    @Override
    public Estimate getEnhancedBatteryPrediction(Context context) {
        long dischargeTime = -1L;
        boolean basedOnUsage = false;
        Uri uri = this.getEnhancedBatteryPredictionUri();
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);

        // Return null if cursor is null or empty
        if (cursor == null || !cursor.moveToFirst())
            return null;

        // Check if estimate is usage based
        int colIndex = cursor.getColumnIndex(BATTERY_ESTIMATE_BASED_ON_USAGE_COL);
        if (colIndex != -1)
            basedOnUsage = cursor.getInt(colIndex) == 1;

        // Calculate average discharge time based on average battery life
        colIndex = cursor.getColumnIndex(AVERAGE_BATTERY_LIFE_COL);
        if (colIndex != -1) {
            long avgBattery = cursor.getLong(colIndex);
            if (avgBattery != -1L) {
                dischargeTime = Duration.ofMinutes(15L).toMillis();
                if (Duration.ofMillis(avgBattery).compareTo(Duration.ofDays(1L)) >= 0)
                    dischargeTime = Duration.ofHours(1L).toMillis();
                dischargeTime = PowerUtil.roundTimeToNearestThreshold(avgBattery, dischargeTime);
            }
        }

        colIndex = cursor.getColumnIndex(BATTERY_ESTIMATE_COL);
        Estimate enhancedEstimate = new Estimate(cursor.getLong(colIndex),
                                                 basedOnUsage, dischargeTime);
        cursor.close();
        return enhancedEstimate;
    }

    @Override
    public boolean isTypeService(int uid) {
        final String[] packages = mPackageManager.getPackagesForUid(uid);
        if (packages != null) {
            for (final String packageName : packages) {
                if (ArrayUtils.contains(PACKAGES_SERVICE, packageName)) {
                    return true;
                }
            }
        }
        return false;
    }
}