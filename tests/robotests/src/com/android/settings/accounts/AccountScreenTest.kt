/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.settings.accounts

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.widget.theme.flags.Flags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountScreenTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private val preferenceScreen = AccountScreen()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreen.key).isEqualTo(AccountScreen.KEY)
    }

    @Test
    @EnableFlags(FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun getIcon_isExpressiveTheme() {
        assertThat(preferenceScreen.getIcon(context)).isEqualTo(R.drawable.ic_homepage_accounts)
    }

    @Test
    @DisableFlags(FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun getIcon_notExpressiveTheme() {
        assertThat(preferenceScreen.getIcon(context))
            .isEqualTo(R.drawable.ic_settings_passwords_filled)
    }
}
