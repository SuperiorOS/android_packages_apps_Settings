/*
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright (C) 2020 The "Best Improved Cherry Picked Rom" Project
 * Copyright (C) 2020 Project Fluid
 * Copyright (C) 2021 ShapeShiftOS
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
package com.android.settings.utils;

import android.os.SystemProperties;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.view.Display;
import android.view.WindowManager;
import android.util.DisplayMetrics;
import android.graphics.Point;
import androidx.annotation.VisibleForTesting;

import com.android.internal.os.PowerProfile;
import com.android.internal.util.MemInfoReader;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.android.settings.R;

public class SuperiorSpecUtils {
    private static final String DEVICE_NAME_MODEL_PROPERTY = "ro.product.system.model";
    private static final String SUPERIOR_CPU_MODEL_PROPERTY = "ro.superior.cpu";
    private static final String FALLBACK_CPU_MODEL_PROPERTY = "ro.board.platform";
    private static final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";
    private static final BigDecimal GB2MB = new BigDecimal(1024);
    static String aproxStorage;

    public static String getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        double total = (totalBlocks * blockSize)/ 1073741824;
        int lastval = (int) Math.round(total);
            if ( lastval > 0  && lastval <= 16){
                aproxStorage = "16";
            } else if (lastval > 16 && lastval <=32) {
                aproxStorage = "32";
            } else if (lastval > 32 && lastval <=64) {
                aproxStorage = "64";
            } else if (lastval > 64 && lastval <=128) {
                aproxStorage = "128";
            } else if (lastval > 128 && lastval <= 256) {
                aproxStorage = "256";
            } else if (lastval > 256 && lastval <= 512) {
                aproxStorage = "512";
            } else if (lastval > 512) {
                aproxStorage = "512+";
            } else aproxStorage = "null";
        return aproxStorage;
    }

    public static String getTotalRAM() {
        MemInfoReader memReader = new MemInfoReader();
        memReader.readMemInfo();
        long totalMem = memReader.getTotalSize();
        long totalMemMiB = totalMem / (1024 * 1024);
        BigDecimal rawVal = new BigDecimal(totalMemMiB);
        return rawVal.divide(GB2MB, 0, RoundingMode.UP) + " GB";
    }

    public static String getDeviceName() {
        String deviceModel = SystemProperties.get(DEVICE_NAME_MODEL_PROPERTY);
        if (!deviceModel.isEmpty())
            return deviceModel;
        else
            return "unknown";
    }

    public static String getProcessorModel() {
        String cpuModelSuperior = SystemProperties.get(SUPERIOR_CPU_MODEL_PROPERTY);
        String cpuModelFallback = SystemProperties.get(FALLBACK_CPU_MODEL_PROPERTY);
        if (!cpuModelSuperior.isEmpty()) {
            return cpuModelSuperior;
        } else if (!cpuModelFallback.isEmpty()) {
            return cpuModelFallback;
        } else {
            return "unknown";
        }
    }

    public static String getScreenRes(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y + getNavigationBarHeight(windowManager);
        return width + " x " + height;
    }

    private static int getNavigationBarHeight(WindowManager wm) {
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        wm.getDefaultDisplay().getRealMetrics(metrics);
        int realHeight = metrics.heightPixels;
        if (realHeight > usableHeight)
            return realHeight - usableHeight;
        else
            return 0;
    }

    public static int getBatteryCapacity(Context context) {
        Object powerProfile = null;

        double batteryCapacity = 0;
        try {
            powerProfile = Class.forName(POWER_PROFILE_CLASS)
                            .getConstructor(Context.class).newInstance(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            batteryCapacity = (Double) Class
                            .forName(POWER_PROFILE_CLASS)
                            .getMethod("getAveragePower", java.lang.String.class)
                            .invoke(powerProfile, "battery.capacity");

        } catch (Exception e) {
            e.printStackTrace();
        }

        String str = Double.toString(batteryCapacity);
        String strArray[] = str.split("\\.");
        int batteryCapacityInt = Integer.parseInt(strArray[0]);

        return batteryCapacityInt;
    }
}
