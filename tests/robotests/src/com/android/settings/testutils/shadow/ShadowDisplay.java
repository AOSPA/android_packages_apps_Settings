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

import android.view.Display;
import android.util.SparseArray;
import android.util.SparseDoubleArray;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(Display.class)
public class ShadowDisplay extends org.robolectric.shadows.ShadowDisplay {

    public Display.Mode mPreferredMode = null;

    @Implementation
    public void setUserPreferredDisplayMode(Display.Mode mode) {
        setWidth(mode.getPhysicalWidth());
        setHeight(mode.getPhysicalHeight());
        setRefreshRate(mode.getRefreshRate());

        mPreferredMode = mode;
    }

    @Implementation
    public Display.Mode getMode() {
        return mPreferredMode != null ? mPreferredMode : super.getDefaultDisplay().getMode();
    }
}
