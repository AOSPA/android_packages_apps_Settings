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

package com.android.settings.datetime.timezone

import android.app.Activity
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settings.dashboard.RestrictedDashboardFragment
import com.android.settings.datetime.timezone.model.TimeZoneData
import com.android.settings.datetime.timezone.model.TimeZoneDataLoader
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import java.util.Date
import java.util.Locale

@SearchIndexable
open class HomeTimeZoneSettings :
    RestrictedDashboardFragment(UserManager.DISALLOW_CONFIG_DATE_TIME) {

    private var mLocale: Locale? = null
    @get:VisibleForTesting internal var mTimeZoneData: TimeZoneData? = null
    @get:VisibleForTesting internal var mSelectedTimeZoneId: String? = null
    private var mTimeZoneInfoFormatter: TimeZoneInfo.Formatter? = null
    private var mPendingZonePickerRequestResult: Intent? = null

    override fun getMetricsCategory(): Int {
        return SettingsEnums.DATE_TIME_HOME_TIME_ZONE
    }

    override fun getLogTag(): String {
        return TAG
    }

    override fun getPreferenceScreenResId(): Int {
        return R.xml.home_time_zone_prefs
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mLocale = context.resources.configuration.locales[0]
        mTimeZoneInfoFormatter = TimeZoneInfo.Formatter(mLocale, Date())
        use(ClearHomeTimeZonePreferenceController::class.java)?.setParentFragment(this)
        use(SuggestionsTimeZonePreferenceController::class.java)?.setParentFragment(this)
        use(HomeRegionPreferenceController::class.java)?.let {
            it.setParentFragment(this)
            it.setOnClickListener { startRegionPicker() }
        }
        use(HomeRegionZonePreferenceController::class.java)?.let {
            it.setParentFragment(this)
            it.setOnClickListener { startTimeZonePicker() }
        }

        loaderManager.initLoader(
            /* id= */ 0,
            /* args= */ null,
            /* callback= */ TimeZoneDataLoader.LoaderCreator(context, ::onTimeZoneDataReady),
        )
    }

    private fun onTimeZoneDataReady(timeZoneData: TimeZoneData?) {
        if (mTimeZoneData == null && timeZoneData != null) {
            mTimeZoneData = timeZoneData
            setupForCurrentTimeZone()
            if (mPendingZonePickerRequestResult != null) {
                onZonePickerRequestResult(mPendingZonePickerRequestResult!!)
                mPendingZonePickerRequestResult = null
            }
        }
    }

    private fun setupForCurrentTimeZone() {
        mSelectedTimeZoneId =
            Settings.Global.getString(
                requireContext().contentResolver,
                Settings.Global.HOME_TIME_ZONE_ID,
            )
        updateUi()
    }

    private fun updateUi() {
        val selectedTimeZoneId = mSelectedTimeZoneId
        val regionId = getRegionIdForTimeZone(selectedTimeZoneId)
        val tzInfo =
            if (!selectedTimeZoneId.isNullOrEmpty() && mTimeZoneInfoFormatter != null) {
                mTimeZoneInfoFormatter!!.format(selectedTimeZoneId)
            } else {
                null
            }

        val regionController = use(HomeRegionPreferenceController::class.java)
        val regionZoneController = use(HomeRegionZonePreferenceController::class.java)

        regionController?.setRegionId(regionId)

        if (regionZoneController != null) {
            regionZoneController.timeZoneInfo = tzInfo

            if (mTimeZoneData != null) {
                val countryTimeZones = mTimeZoneData!!.lookupCountryTimeZones(regionId)
                regionZoneController.setClickable(
                    countryTimeZones != null && countryTimeZones.preferredTimeZoneIds.size > 1
                )
            }
        }
        updatePreferenceStates()
    }

    private fun startRegionPicker() {
        if (mTimeZoneData == null) {
            return
        }
        val args = Bundle()
        val regionId = getRegionIdForTimeZone(mSelectedTimeZoneId)
        if (regionId != null) {
            args.putString(RegionSearchPicker.EXTRA_CURRENT_REGION_ID, regionId)
        }
        startPickerFragment(RegionSearchPicker::class.java, args, REQUEST_CODE_REGION_PICKER)
    }

    private fun startTimeZonePicker() {
        if (mTimeZoneData == null) {
            return
        }
        val args = Bundle()
        val regionId = getRegionIdForTimeZone(mSelectedTimeZoneId)
        if (regionId != null) {
            args.putString(RegionZonePicker.EXTRA_REGION_ID, regionId)
        }
        startPickerFragment(RegionZonePicker::class.java, args, REQUEST_CODE_ZONE_PICKER)
    }

    private fun getRegionIdForTimeZone(timeZoneId: String?): String {
        if (mTimeZoneData == null || timeZoneId.isNullOrEmpty()) {
            return ""
        }
        return mTimeZoneData!!.lookupCountryCodesForZoneId(timeZoneId).firstOrNull() ?: ""
    }

    private fun startPickerFragment(
        fragmentClass: Class<out BaseTimeZonePicker>,
        args: Bundle,
        resultRequestCode: Int,
    ) {
        SubSettingLauncher(requireContext())
            .setDestination(fragmentClass.canonicalName)
            .setArguments(args)
            .setSourceMetricsCategory(metricsCategory)
            .setResultListener(this, resultRequestCode)
            .launch()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return
        }

        when (requestCode) {
            REQUEST_CODE_REGION_PICKER,
            REQUEST_CODE_ZONE_PICKER -> {
                if (mTimeZoneData == null) {
                    mPendingZonePickerRequestResult = data
                } else {
                    onZonePickerRequestResult(data)
                }
            }
        }
    }

    private fun onZonePickerRequestResult(data: Intent) {
        val regionId = data.getStringExtra(BaseTimeZonePicker.EXTRA_RESULT_REGION_ID)
        val tzId = data.getStringExtra(BaseTimeZonePicker.EXTRA_RESULT_TIME_ZONE_ID)

        // Ignore the result if user didn't change the region or time zone.
        if (
            regionId == use(HomeRegionPreferenceController::class.java)?.regionId &&
                tzId == mSelectedTimeZoneId
        ) {
            return
        }

        val countryTimeZones = mTimeZoneData?.lookupCountryTimeZones(regionId)
        if (countryTimeZones == null || countryTimeZones.preferredTimeZoneIds.isEmpty()) {
            Log.e(TAG, "Region has no time zones: " + regionId)
            return
        } else if (!countryTimeZones.preferredTimeZoneIds.contains(tzId)) {
            Log.e(TAG, "Unknown time zone id is selected: " + tzId)
            return
        }

        if (!tzId.isNullOrEmpty()) {
            setHomeTimeZone(tzId)
        }

        updateUi()
    }

    open fun setHomeTimeZone(tzId: String?) {
        if (tzId == mSelectedTimeZoneId) {
            return
        }
        mSelectedTimeZoneId = tzId
        Settings.Global.putString(
            requireContext().contentResolver,
            Settings.Global.HOME_TIME_ZONE_ID,
            tzId,
        )

        val intent = Intent(Intent.ACTION_HOME_TIME_ZONE_CHANGED)
        intent.putExtra(Intent.EXTRA_HOME_TIME_ZONE_ID, tzId)
        requireContext().sendBroadcast(intent)

        if (tzId.isNullOrEmpty()) {
            updateUi()
        } else {
            finish()
        }
    }

    open fun isHomeTimeZoneEnabled(): Boolean {
        // Null means the feature is not initialized, but it is enabled.
        // Empty means the feature is disabled.
        val selectedTimeZoneId = mSelectedTimeZoneId
        return selectedTimeZoneId == null || selectedTimeZoneId.isNotEmpty()
    }

    companion object {
        private const val TAG = "HomeTimeZoneSettings"

        private const val REQUEST_CODE_REGION_PICKER = 1
        private const val REQUEST_CODE_ZONE_PICKER = 2

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            BaseSearchIndexProvider(R.xml.home_time_zone_prefs)
    }
}
