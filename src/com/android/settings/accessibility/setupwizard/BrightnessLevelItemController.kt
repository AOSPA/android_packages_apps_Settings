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

package com.android.settings.accessibility.setupwizard

import android.app.ActivityOptions
import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.android.settings.display.BrightnessLevelPreference
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.google.android.setupdesign.items.Item

/** Controller for managing the brightness level item in the Setup Wizard. */
class BrightnessLevelItemController(
    private val context: Context,
    item: Item,
    private val brightnessLevelDataStore: KeyValueStore =
        BrightnessLevelPreference().storage(context),
) : BaseItemController(item) {

    private val brightnessLevelMetadata: BrightnessLevelPreference = BrightnessLevelPreference()
    private var brightnessObserver: KeyedObserver<String>? = null

    override fun bindData(item: Item) = updateSummary()

    override fun onStart() {
        super.onStart()
        val observer = KeyedObserver<String> { _, _ -> updateSummary() }
        brightnessObserver = observer

        brightnessLevelDataStore.addObserver(
            BrightnessLevelPreference.KEY,
            observer,
            context.mainExecutor,
        )
    }

    override fun onStop() {
        brightnessObserver?.let {
            brightnessLevelDataStore.removeObserver(BrightnessLevelPreference.KEY, it)
            brightnessObserver = null
        }
    }

    override fun onItemSelected(activity: FragmentActivity) {
        brightnessLevelMetadata.intent(context)?.let { intent ->
            val options =
                ActivityOptions.makeCustomAnimation(
                    context,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                )
            activity.startActivityForResult(intent, 0, options.toBundle())
        }
    }

    private fun updateSummary() {
        targetItem.summary = brightnessLevelMetadata.getSummary(context)
    }
}
