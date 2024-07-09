/*
 * Copyright (C) 2019 The SuperiorOS Project
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

package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

public class SuperiorVersionPreferenceController extends BasePreferenceController {

    @VisibleForTesting
    static final String SUPERIOR_VERSION_PROPERTY = "ro.modversion";
    static final String SUPERIOR_RELEASETYPE_PROPERTY = "ro.superior.releasetype";
    static final String SUPERIOR_ZIPTYPE_PROPERTY = "ro.superior.edition";

    public SuperiorVersionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return !TextUtils.isEmpty(SystemProperties.get(SUPERIOR_VERSION_PROPERTY)) && !TextUtils.isEmpty(SystemProperties.get(SUPERIOR_RELEASETYPE_PROPERTY)) && !TextUtils.isEmpty(SystemProperties.get(SUPERIOR_ZIPTYPE_PROPERTY))
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        String superiorVersion = SystemProperties.get(SUPERIOR_VERSION_PROPERTY);
        String superiorReleaseType = SystemProperties.get(SUPERIOR_RELEASETYPE_PROPERTY);
        String superiorZipType = SystemProperties.get(SUPERIOR_ZIPTYPE_PROPERTY);
        if (!superiorVersion.isEmpty() && !superiorReleaseType.isEmpty() && !superiorZipType.isEmpty()) {
            return superiorVersion + " | " + superiorReleaseType + " | " + superiorZipType;
        } else {
            return
                mContext.getString(R.string.device_info_default);
        }
    }
}
