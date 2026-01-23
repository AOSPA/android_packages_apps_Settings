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

package com.android.settings.vpn2;

import android.content.Context;

import com.android.settings.R;

/** A preference for an app that is already in and can be removed from the exclusion list. */
public class AppExclusionSelectedPreference extends AppExclusionBasePreference {
    public AppExclusionSelectedPreference(
            Context context,
            String packageName,
            int userId,
            AppExclusionActionCallback actionCallback) {
        super(context, packageName, userId, actionCallback);
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_delete;
    }

    @Override
    public String getActionIconContentDescription() {
        return getContext().getResources().getString(R.string.remove);
    }

    @Override
    public AppExclusionActionCallback.Action getClickAction() {
        return AppExclusionActionCallback.Action.REMOVE;
    }
}
