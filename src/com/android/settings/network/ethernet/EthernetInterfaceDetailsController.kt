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

package com.android.settings.network.ethernet

import android.content.Context
import android.net.EthernetManager
import android.widget.ImageView
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.widget.EntityHeaderController
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.widget.LayoutPreference

class EthernetInterfaceDetailsController(
    context: Context,
    private val fragment: PreferenceFragmentCompat,
    private val preferenceId: String,
) : AbstractPreferenceController(context) {
    private val KEY_HEADER = "ethernet_details"

    private val ethernetManager = context.getSystemService(EthernetManager::class.java)
    private val ethernetInterface =
        EthernetTrackerImpl.getInstance(context).getInterface(preferenceId)

    override fun isAvailable(): Boolean {
        return true
    }

    override fun getPreferenceKey(): String? {
        return KEY_HEADER
    }

    override fun displayPreference(screen: PreferenceScreen) {
        val headerPref: LayoutPreference? = screen.findPreference(KEY_HEADER)

        val mEntityHeaderController =
            EntityHeaderController.newInstance(
                fragment.getActivity(),
                fragment,
                headerPref?.findViewById(R.id.entity_header),
            )

        val iconView: ImageView? = headerPref?.findViewById(R.id.entity_header_icon)

        iconView?.setScaleType(ImageView.ScaleType.CENTER_INSIDE)

        mEntityHeaderController
            .setLabel("Ethernet")
            .setSummary(
                if (ethernetInterface?.getInterfaceState() == EthernetManager.STATE_LINK_UP) {
                    mContext.getString(R.string.network_connected)
                } else {
                    mContext.getString(R.string.network_disconnected)
                }
            )
            .setSecondSummary("")
            .setIcon(mContext.getDrawable(R.drawable.ic_settings_ethernet))
            .done(true /* rebind */)
    }
}
