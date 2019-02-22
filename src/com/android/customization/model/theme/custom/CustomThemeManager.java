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
package com.android.customization.model.theme.custom;

import com.android.customization.model.CustomizationManager;

public class CustomThemeManager implements CustomizationManager<ThemeComponentOption> {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void apply(ThemeComponentOption option, Callback callback) {

    }

    @Override
    public void fetchOptions(OptionsFetchedListener<ThemeComponentOption> callback) {

    }
}
