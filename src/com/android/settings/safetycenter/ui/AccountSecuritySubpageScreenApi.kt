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

package com.android.settings.safetycenter.ui

import com.android.settings.R
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.category.Category

/**
 * API definition for the Account Security Subpage screen. This class exposes the screen to the
 * Settings API framework.
 */

// LINT.IfChange
@ProvidePreferenceScreen(AccountSecuritySubpageScreenApi.KEY)
class AccountSecuritySubpageScreenApi :
    SubpageScreenApi(
        key = KEY,
        topLevelSettingsCategory = Category.SAFETY_CENTER,
        fragment = AccountSecuritySubpageFragment::class,
        purpose = R.string.account_security_subpage_screen_purpose,
        subpageRegistryKey = SafetyCenterSubpageRegistry.ACCOUNT_SECURITY_SUBPAGE_KEY,
    ) {

    companion object {
        const val KEY = "account_security_subpage_screen"
    }
}
// LINT.ThenChange(AccountSecuritySubpageFragment.kt)
