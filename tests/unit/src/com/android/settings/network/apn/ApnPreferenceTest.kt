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

package com.android.settings.network.apn

import android.content.Context
import android.view.LayoutInflater
import android.widget.RadioButton
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ApnPreferenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var preference: TestApnPreference
    private lateinit var viewHolder: PreferenceViewHolder
    private val mockRadioButton = mock<RadioButton>()

    // Test subclass to make notifyChanged() visible
    open class TestApnPreference(context: Context) : ApnPreference(context) {
        var notifyChangedCalled = false

        public override fun notifyChanged() {
            notifyChangedCalled = true
            super.notifyChanged()
        }
    }

    @Before
    fun setUp() {
        preference = TestApnPreference(context)

        // Inflate the layout and create a ViewHolder
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(preference.layoutResource, null)
        viewHolder = spy(PreferenceViewHolder.createInstanceForTests(view))

        viewHolder.stub { on { findViewById(android.R.id.checkbox) } doReturn mockRadioButton }
    }

    @Test
    fun onBindViewHolder_setIsCheckedTrue_verifyRadioButtonSetCheckedTrue() {
        preference.setIsChecked(true)

        preference.onBindViewHolder(viewHolder)

        verify(mockRadioButton).isChecked = true
    }

    @Test
    fun onBindViewHolder_setIsCheckedFalse_verifyRadioButtonSetCheckedFalse() {
        preference.setIsChecked(false)

        preference.onBindViewHolder(viewHolder)

        verify(mockRadioButton).isChecked = false
    }

    @Test
    fun setIsChecked_valueNotChange_noNotifyChanged() {
        preference.setIsChecked(false) // Initial state
        preference.notifyChangedCalled = false // Reset flag

        preference.setIsChecked(false) // Set to same value

        assertThat(preference.notifyChangedCalled).isFalse()
    }

    @Test
    fun setIsChecked_valueChanged_notifyChanged() {
        preference.setIsChecked(false) // Initial state
        preference.notifyChangedCalled = false // Reset flag

        preference.setIsChecked(true) // Set to different value

        assertThat(preference.notifyChangedCalled).isTrue()
    }
}
