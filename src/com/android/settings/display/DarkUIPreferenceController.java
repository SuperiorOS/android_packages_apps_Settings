/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.content.ContentResolver;
import android.os.Bundle;
import android.text.TextUtils;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import libcore.util.Objects;
import java.util.ArrayList;
import java.util.List;

import com.android.settings.R;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

public class DarkUIPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {
    private static final String SYSTEM_THEME_STYLE = "system_theme_style";
    private ListPreference mSystemThemeStyle;
    public DarkUIPreferenceController(Context context) {
        super(context);
    }
     @Override
    public String getPreferenceKey() {
        return SYSTEM_THEME_STYLE;
    }
     @Override
    public boolean isAvailable() {
        return true;
    }
     @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSystemThemeStyle = (ListPreference) screen.findPreference(SYSTEM_THEME_STYLE);
        int systemThemeStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SYSTEM_THEME_STYLE, 0);
        int valueIndex = mSystemThemeStyle.findIndexOfValue(String.valueOf(systemThemeStyle));
        mSystemThemeStyle.setValueIndex(valueIndex >= 0 ? valueIndex : 0);
        mSystemThemeStyle.setSummary(mSystemThemeStyle.getEntry());
        mSystemThemeStyle.setOnPreferenceChangeListener(this);
    }
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSystemThemeStyle) {
            String value = (String) newValue;
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.SYSTEM_THEME_STYLE, Integer.valueOf(value));
            int valueIndex = mSystemThemeStyle.findIndexOfValue(value);
            mSystemThemeStyle.setSummary(mSystemThemeStyle.getEntries()[valueIndex]);

            Toast.makeText(mContext, mContext.getString(R.string.ui_style_applied_toast),
                Toast.LENGTH_LONG).show();
            Intent goHome = new Intent(Intent.ACTION_MAIN);
            goHome.addCategory(Intent.CATEGORY_HOME);
            goHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(goHome);   
        }
        return true;
    }
}
