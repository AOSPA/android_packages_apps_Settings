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

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/** A {@link ViewModelProvider.Factory} that creates {@link AppExclusionViewModel} instances. */
public class AppExclusionViewModelFactory implements ViewModelProvider.Factory {
    private final Application mApplication;
    private final int mUserId;
    private final String mVpnPackage;

    public AppExclusionViewModelFactory(Application application, int userId, String vpnPackage) {
        mApplication = application;
        mUserId = userId;
        mVpnPackage = vpnPackage;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AppExclusionViewModel.class)) {
            return modelClass.cast(new AppExclusionViewModel(mApplication, mUserId, mVpnPackage));
        }
        throw new IllegalArgumentException("Unsupported model class: " + modelClass);
    }
}
