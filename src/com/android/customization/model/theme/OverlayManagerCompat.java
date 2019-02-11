/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.customization.model.theme;

import android.content.Context;
import android.content.om.OverlayInfo;
import android.content.om.OverlayManager;
import android.os.UserHandle;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper over {@link OverlayManager} that abstracts away its internals and can be mocked for
 * testing.
 */
public class OverlayManagerCompat {
    private final OverlayManager mOverlayManager;

    public OverlayManagerCompat(Context context) {
        mOverlayManager = context.getSystemService(OverlayManager.class);
    }

    /**
     * Enables the overlay provided by the given package for the given user Id
     * @return true if the operation succeeded
     */
    public boolean setEnabledExclusiveInCategory(String packageName, int userId) {
        return mOverlayManager.setEnabledExclusiveInCategory(packageName, userId);
    }

    /**
     * Disables the overlay provided by the given package for the given user Id
     * @return true if the operation succeeded
     */
    public boolean disableOverlay(String packageName, int userId) {
        return mOverlayManager.setEnabled(packageName, false, userId);
    }

    /**
     * @return the package name of the currently enabled overlay for the given target package, in
     * the given category, or {@code null} if none is currently enabled.
     */
    @Nullable
    public String getEnabledPackageName(String targetPackageName, String category) {
        List<OverlayInfo> overlayInfos = getOverlayInfosForTarget(targetPackageName,
                UserHandle.myUserId());
        for (OverlayInfo overlayInfo : overlayInfos) {
            if (category.equals(overlayInfo.category) && overlayInfo.isEnabled()) {
                return overlayInfo.packageName;
            }
        }
        return null;
    }

    /**
     * @return a Map of Category -> PackageName of all the overlays enabled for the given target
     * packages. It might be empty if no overlay is enabled for those targets.
     */
    public Map<String, String> getEnabledOverlaysForTargets(String... targetPackages) {
        Map<String, String> overlays = new HashMap<>();
        for (String target : targetPackages) {
            addAllEnabledOverlaysForTarget(overlays, target);
        }
        return overlays;
    }

    private List<OverlayInfo> getOverlayInfosForTarget(String targetPackageName, int userId) {
        return mOverlayManager.getOverlayInfosForTarget(targetPackageName, userId);
    }

    private void addAllEnabledOverlaysForTarget(Map<String, String> overlays, String target) {
        for (OverlayInfo overlayInfo : getOverlayInfosForTarget(target,
                UserHandle.myUserId())) {
            if (overlayInfo.isEnabled()) {
                overlays.put(overlayInfo.category, overlayInfo.packageName);
            }
        }
    }
}