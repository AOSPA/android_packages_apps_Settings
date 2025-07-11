/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.biometrics

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class IdentityCheckPromoCardActivityTest {

    private val mContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun launchActivity_showsFragment() {
        ActivityScenario.launch<IdentityCheckPromoCardActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertThat(activity.isFinishing).isFalse()
                assertThat(countIdentityCheckPromoCardBottomSheetFragments(activity))
                    .isEqualTo(1)

                activity.finish()
            }
        }
        assertThat(hasPromoCardBeenShown()).isTrue()
    }

    private val intent: Intent
        get() = Intent(mContext, IdentityCheckPromoCardActivity::class.java)

    private fun hasPromoCardBeenShown(): Boolean {
        return Settings.Global.getInt(
            mContext.contentResolver,
            Settings.Global.IDENTITY_CHECK_PROMO_CARD_SHOWN,
            0
        ) == 1
    }

    private fun countIdentityCheckPromoCardBottomSheetFragments(
        activity: IdentityCheckPromoCardActivity
    ): Int {
        var count = 0
        for (fragment in activity.supportFragmentManager.fragments) {
            if (fragment.javaClass == IdentityCheckPromoCardFragment::class.java) {
                count++
            }
        }
        return count
    }
}