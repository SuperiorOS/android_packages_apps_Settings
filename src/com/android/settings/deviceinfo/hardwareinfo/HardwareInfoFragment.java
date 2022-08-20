/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.hardwareinfo;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.BluetoothAddressPreferenceController;
import com.android.settings.deviceinfo.IpAddressPreferenceController;
import com.android.settings.deviceinfo.WifiMacAddressPreferenceController;
import com.android.settings.deviceinfo.imei.ImeiInfoPreferenceController;
import com.android.settings.deviceinfo.simstatus.EidStatus;
import com.android.settings.deviceinfo.simstatus.SimEidPreferenceController;
import com.android.settings.deviceinfo.simstatus.SimStatusPreferenceController;
import com.android.settings.deviceinfo.simstatus.SlotSimStatus;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@SearchIndexable
public class HardwareInfoFragment extends DashboardFragment {

  private static final String LOG_TAG = "HardwareInfoFragment";
  private static final String KEY_EID_INFO = "eid_info";

  private final BroadcastReceiver mSimStateReceiver = new BroadcastReceiver() {
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
        String state = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
        Log.d(LOG_TAG, "Received ACTION_SIM_STATE_CHANGED: " + state);
        updatePreferenceStates();
      }
    }
  };

  public static final String TAG = "HardwareInfo";

  @Override
  public int getMetricsCategory() {
    return SettingsEnums.DIALOG_SETTINGS_HARDWARE_INFO;
  }

  @Override
  protected int getPreferenceScreenResId() {
    return R.xml.hardware_info;
  }

  @Override
  protected String getLogTag() {
    return TAG;
  }

  @Override
  protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
    final List<AbstractPreferenceController> controllers = new ArrayList<>();
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final Lifecycle lifecycle = getSettingsLifecycle();
    final SlotSimStatus slotSimStatus = new SlotSimStatus(context, executor, lifecycle);

    controllers.add(new IpAddressPreferenceController(context, lifecycle));
    controllers.add(new WifiMacAddressPreferenceController(context, lifecycle));
    controllers.add(new BluetoothAddressPreferenceController(context, lifecycle));

    Consumer<String> imeiInfoList = imeiKey -> {
      ImeiInfoPreferenceController imeiRecord =
          new ImeiInfoPreferenceController(context, imeiKey);
      imeiRecord.init(this, slotSimStatus);
      controllers.add(imeiRecord);
    };

    imeiInfoList.accept(ImeiInfoPreferenceController.DEFAULT_KEY);

    for (int slotIndex = 0; slotIndex < slotSimStatus.size(); slotIndex ++) {
      SimStatusPreferenceController slotRecord =
          new SimStatusPreferenceController(context,
          slotSimStatus.getPreferenceKey(slotIndex));
      slotRecord.init(this, slotSimStatus);
      controllers.add(slotRecord);

      imeiInfoList.accept(ImeiInfoPreferenceController.DEFAULT_KEY + (1 + slotIndex));
    }

    EidStatus eidStatus = new EidStatus(slotSimStatus, context, executor);
    SimEidPreferenceController simEid = new SimEidPreferenceController(context, KEY_EID_INFO);
    simEid.init(slotSimStatus, eidStatus);
    controllers.add(simEid);

    executor.shutdown();
    return controllers;
  }

  @Override
  public void onResume() {
    super.onResume();
    Context context = getContext();
    if (context != null) {
      context.registerReceiver(mSimStateReceiver,
          new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED));
    } else {
      Log.i(LOG_TAG, "context is null, not registering SimStateReceiver");
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    Context context = getContext();
    if (context != null) {
      context.unregisterReceiver(mSimStateReceiver);
    } else {
      Log.i(LOG_TAG, "context already null, not unregistering SimStateReceiver");
    }
  }

  public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
      new BaseSearchIndexProvider(R.xml.hardware_info) {
        @Override
        protected boolean isPageSearchEnabled(Context context) {
          return context.getResources().getBoolean(R.bool.config_show_device_model);
        }
      };
}
