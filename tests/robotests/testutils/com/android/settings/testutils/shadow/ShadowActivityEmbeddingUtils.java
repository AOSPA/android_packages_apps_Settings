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

package com.android.settings.testutils.shadow;

import android.app.Activity;
import android.content.Context;

import com.android.settings.activityembedding.ActivityEmbeddingUtils;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

/**
 * Shadow class for {@link ActivityEmbeddingUtils} to test embedding activity features.
 */
@Implements(ActivityEmbeddingUtils.class)
public class ShadowActivityEmbeddingUtils {
    private static boolean sIsEmbeddingActivityEnabled;
    private static boolean sIsAlreadyEmbedded;

    @Implementation
    public static boolean isEmbeddingActivityEnabled(Context context) {
        return sIsEmbeddingActivityEnabled;
    }

    /**
     * Shadow for {@link ActivityEmbeddingUtils#isAlreadyEmbedded(Activity)}.
     */
    @Implementation
    public static boolean isAlreadyEmbedded(Activity activity) {
        return sIsAlreadyEmbedded;
    }

    public static void setIsEmbeddingActivityEnabled(boolean isEmbeddingActivityEnabled) {
        sIsEmbeddingActivityEnabled = isEmbeddingActivityEnabled;
    }

    public static void setIsAlreadyEmbedded(boolean isAlreadyEmbedded) {
        sIsAlreadyEmbedded = isAlreadyEmbedded;
    }

    /**
     * Resets the static states for this shadow class.
     */
    @Resetter
    public static void reset() {
        sIsEmbeddingActivityEnabled = false;
        sIsAlreadyEmbedded = false;
    }
}
