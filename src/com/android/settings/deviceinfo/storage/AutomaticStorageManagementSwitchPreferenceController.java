/**
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.storage;

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.GenericSwitchController;
import com.android.settings.widget.PrimarySwitchPreference;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.Utils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class AutomaticStorageManagementSwitchPreferenceController extends
        BasePreferenceController implements LifecycleObserver, OnResume,
        SwitchWidgetController.OnSwitchChangeListener {
    @VisibleForTesting
    static final String STORAGE_MANAGER_ENABLED_BY_DEFAULT_PROPERTY = "ro.storage_manager.enabled";
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private PrimarySwitchPreference mSwitch;
    private GenericSwitchController mSwitchController;
    private FragmentManager mFragmentManager;

    public AutomaticStorageManagementSwitchPreferenceController(Context context, String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    public AutomaticStorageManagementSwitchPreferenceController setFragmentManager(
            FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
        return this;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSwitch = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mContext.getResources().getBoolean(R.bool.config_show_smart_storage_toggle)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return !ActivityManager.isLowRamDeviceStatic() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onResume() {
        if (!isAvailable()) {
            return;
        }
        mSwitch.setChecked(Utils.isStorageManagerEnabled(mContext));

        if (mSwitch != null) {
            mSwitchController = new GenericSwitchController(mSwitch);
            mSwitchController.setListener(this);
            mSwitchController.startListening();
        }
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_TOGGLE_STORAGE_MANAGER, isChecked);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED,
                isChecked ? 1 : 0);

        return true;
    }
}
