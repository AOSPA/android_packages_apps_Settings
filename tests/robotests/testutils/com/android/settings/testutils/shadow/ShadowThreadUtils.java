/*
 * Copyright (C) 2017 The Android Open Source Project
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

import com.android.settingslib.utils.ThreadUtils;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@Implements(ThreadUtils.class)
public class ShadowThreadUtils {
    private static final String TAG = "ShadowThreadUtils";

    private static boolean sIsMainThread = true;
    private static ListeningExecutorService sExecutorService = null;

    @Resetter
    public static void reset() {
        sIsMainThread = true;
        if (sExecutorService != null) {
            sExecutorService.shutdownNow();
            sExecutorService = null;
        }
    }

    @Implementation
    protected static void postOnBackgroundThread(Runnable runnable) {
        runnable.run();
    }

    @Implementation
    protected static void postOnMainThread(Runnable runnable) {
        runnable.run();
    }

    @Implementation
    protected static boolean isMainThread() {
        return sIsMainThread;
    }

    /** Use the direct executor so that scheduled tasks will be dispatched immediately. */
    @Implementation
    public static synchronized ListeningExecutorService getBackgroundExecutor() {
        if (sExecutorService == null) {
            sExecutorService = MoreExecutors.newDirectExecutorService();
        }
        return sExecutorService;
    }

    public static void setIsMainThread(boolean isMainThread) {
        sIsMainThread = isMainThread;
    }

    public static void setExecutorService(ListeningExecutorService executorService) {
        sExecutorService = executorService;
    }
}
