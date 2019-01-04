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

import com.android.customization.model.CustomizationManager;

public class ThemeManager implements CustomizationManager<ThemeBundle> {

    private final ThemeBundleProvider mProvider;

    public ThemeManager(ThemeBundleProvider provider) {
        mProvider = provider;
    }

    @Override
    public boolean isAvailable() {
        return mProvider.isAvailable();
    }

    @Override
    public void apply(ThemeBundle theme) {
        // TODO(santie) implement
    }

    @Override
    public void fetchOptions(OptionsFetchedListener<ThemeBundle> callback) {
        mProvider.fetch(callback, false);
    }
}
