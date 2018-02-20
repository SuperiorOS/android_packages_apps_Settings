/*
 * Copyright (c) 2018 AOSiP
 * Contributed by Projekt Substratum
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
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;

import java.util.List;


public class ThemeUtils {

    private static final String substratumVersionMetadata = "Substratum_Version";

    private static boolean isSubstratumOverlay(
            Context mContext,
            String packageName) {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    packageName, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                int returnMetadata = appInfo.metaData.getInt(substratumVersionMetadata);
                if (String.valueOf(returnMetadata) != null) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean isSubstratumOverlayInstalled(Context mContext) {
        try {
            OverlayManager mOverlayManager = new OverlayManager();
            List<OverlayInfo> overlayInfoList =
                    mOverlayManager.getOverlayInfosForFramework();
            for (int i = 0; i < overlayInfoList.size(); i++) {
                if (isSubstratumOverlay(mContext, overlayInfoList.get(i).packageName))
                    return true;
            }
        } catch (RemoteException ignored) {
        }
        return false;
    }

    static class OverlayManager {
        private final IOverlayManager mService;

        OverlayManager() {
            mService = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));
        }

        List<OverlayInfo> getOverlayInfosForFramework()
                throws RemoteException {
            return mService.getOverlayInfosForTarget("android", UserHandle.myUserId());
        }
    }
}
