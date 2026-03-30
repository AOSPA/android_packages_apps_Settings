/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.testutils.shadow;

import android.hardware.display.DisplayManager;
import android.util.SparseArray;
import android.util.SparseDoubleArray;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(DisplayManager.class)
public class ShadowDisplayManager extends org.robolectric.shadows.ShadowDisplayManager {
    private final SparseArray<SparseDoubleArray> displayBrightnessByUnit = new SparseArray<>();

    @Implementation
    protected float getBrightness(int displayId, int unit) {
      return (float) getDisplayBrightnessByUnit(displayId).get(unit, 0.0);
    }

    @Implementation
    protected void setBrightness(int displayId, float value, int unit) {
      getDisplayBrightnessByUnit(displayId).put(unit, value);
    }

    private synchronized SparseDoubleArray getDisplayBrightnessByUnit(int displayId) {
      SparseDoubleArray brightnessByUnit = displayBrightnessByUnit.get(displayId);
      if (brightnessByUnit == null) {
        brightnessByUnit = new SparseDoubleArray();
        displayBrightnessByUnit.put(displayId, brightnessByUnit);
      }
      return brightnessByUnit;
    }
}
