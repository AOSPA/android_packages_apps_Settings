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

import android.content.Context;
import android.telecom.TelecomManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

/**
 * Shadow for {@link com.android.internal.telecom.TelecomDependencies}.
 */
@Implements(className = "com.android.internal.telecom.TelecomDependencies")
public class ShadowTelecomDependencies {
    @Implementation
    public static TelecomManager createTelecomManager(Context context) {
        return ReflectionHelpers.callConstructor(TelecomManager.class,
                ClassParameter.from(Context.class, context));
    }
}
