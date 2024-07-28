/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.gestures;

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;

import android.content.Context;
import android.provider.DeviceConfig;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Configures behaviour of long press home button to invoke assistant app gesture.
 */
public class ButtonNavigationSettingsAssistController extends TogglePreferenceController {

    static final String LONG_PRESS_HOME_BUTTON_TO_SEARCH = "long_press_home_button_to_search";
    static final String NAMESPACE_LAUNCHER = "launcher";

    public ButtonNavigationSettingsAssistController(Context context, String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        boolean onByDefault = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_assistLongPressHomeEnabledDefault);
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_LONG_PRESS_HOME_ENABLED, onByDefault ? 1 : 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ASSIST_LONG_PRESS_HOME_ENABLED, isChecked ? 1 : 0);
    }

    @Override
    public int getAvailabilityStatus() {
        if (SystemNavigationPreferenceController.isOverlayPackageAvailable(mContext,
                NAV_BAR_MODE_2BUTTON_OVERLAY)
                || SystemNavigationPreferenceController.isOverlayPackageAvailable(mContext,
                NAV_BAR_MODE_3BUTTON_OVERLAY)) {
            return AVAILABLE;
        }        
        else if (isFlagEnabled()) {
            return 3;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

    public static boolean isFlagEnabled() {
        return DeviceConfig.getBoolean(NAMESPACE_LAUNCHER, LONG_PRESS_HOME_BUTTON_TO_SEARCH, true);
    }
}
