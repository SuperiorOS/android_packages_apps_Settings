/*
 * Copyright (C) 2018 AospExtended ROM
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import com.superior.settings.preferences.SystemSettingSwitchPreference;
import com.android.settings.display.AccentPickerPreferenceController;
import com.android.settings.display.DarkUIPreferenceController;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class ForceAllowThemePreferenceController extends AbstractPreferenceController implements
PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_FORCE_ALLOW_SYSTEM_THEMES = "force_allow_system_themes";
    private static DarkUIPreferenceController mUIStylePreference;
    private static AccentPickerPreferenceController mAccentPickerPreference;

    public ForceAllowThemePreferenceController(Context context, DarkUIPreferenceController uiStylePreference, AccentPickerPreferenceController accentPickerPreference) {
        super(context);
        mUIStylePreference = uiStylePreference;
        mAccentPickerPreference = accentPickerPreference;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_FORCE_ALLOW_SYSTEM_THEMES;
    }


    @Override
    public void updateState(Preference preference) {
        int value = Settings.System.getInt(mContext.getContentResolver(), Settings.System.FORCE_ALLOW_SYSTEM_THEMES, 0);
        ((SystemSettingSwitchPreference) preference).setChecked(value == 1);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean val = (boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.FORCE_ALLOW_SYSTEM_THEMES, val ? 1 : 0);
                if(val) {
                  new AlertDialog.Builder(mContext)
                          .setTitle(R.string.force_theme_warning_title)
                          .setMessage(R.string.force_theme_warning_message)
                          .setPositiveButton(R.string.dlg_ok, null)
                          .show();

                }
        // Update preferences states
        mUIStylePreference.onResume();
        mAccentPickerPreference.onResume();
        return true;
    }


}
