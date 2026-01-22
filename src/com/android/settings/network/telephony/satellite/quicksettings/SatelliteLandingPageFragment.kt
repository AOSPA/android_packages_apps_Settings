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

package com.android.settings.network.telephony.satellite.quicksettings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import com.android.internal.telephony.flags.Flags
import com.android.settings.R
import com.android.settings.spa.preference.ComposePreference
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.widget.BannerMessagePreference
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.IllustrationPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * A fragment that displays the satellite landing page.
 *
 * This fragment is displayed when the user taps on the satellite quick settings tile. It displays a
 * list of satellite-optimized apps and a footer with more information about satellite services. The
 * content of the landing page is determined by the NTN type.
 */
class SatelliteLandingPageFragment : SettingsBasePreferenceFragment {

    private lateinit var packageManager: PackageManager

    private var appsRepository: SatelliteAppsRepository? = null
    private var backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default

    @VisibleForTesting
    val viewModel: SatelliteLandingPageViewModel by viewModels {
        SatelliteLandingPageViewModelFactory(
            requireContext(),
            appsRepository ?: SatelliteAppsRepository(requireContext()),
            packageManager,
            backgroundDispatcher,
        )
    }

    /**
     * Test-only constructor to inject dependencies for testing.
     *
     * @param packageManager The [PackageManager] to use for testing.
     * @param appsRepository The [SatelliteAppsRepository] to use for testing.
     * @param backgroundDispatcher The [CoroutineDispatcher] to use for background work in
     *   ViewModel.
     */
    @VisibleForTesting
    constructor(
        packageManager: PackageManager,
        appsRepository: SatelliteAppsRepository,
        backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) : super() {
        this.packageManager = packageManager
        this.appsRepository = appsRepository
        this.backgroundDispatcher = backgroundDispatcher
    }

    constructor() : super()

    private var activeSubId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (!::packageManager.isInitialized) {
            packageManager = requireContext().packageManager
        }
        setPreferencesFromResource(R.layout.satellite_landing_page_pref, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateActiveSubId()
        Log.d(TAG, "onViewCreated: activeSubId: $activeSubId")

        observeViewModel()
        setUpSatelliteAppsContent()
        setUpTryADemoButtonListener()
    }

    override fun onResume() {
        super.onResume()
        updateActiveSubId()
        updateLandingPageContent()
    }

    private fun updateActiveSubId() {
        activeSubId = SubscriptionManager.getActiveDataSubscriptionId()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLteBasedNtnSupported.collectLatest { isLte ->
                        if (isLte != null) {
                            addIllustrationPreference(isLte)
                            updateTryADemoButtonVisibility(isLte)
                            setUpFooterPreference(isLte)
                        }
                    }
                }
                launch {
                    viewModel.isCarrierRoamingNtnSupported.collectLatest { isCarrier ->
                        updateTryADemoButtonIcon(isCarrier)
                    }
                }
                launch {
                    viewModel.bannerState.collect { bannerState ->
                        updateWarningBanners(bannerState)
                    }
                }
                launch {
                    viewModel.isTryADemoButtonEnabled.collectLatest { isEnabled ->
                        updateTryADemoButtonEnabledState(isEnabled)
                    }
                }
            }
        }
    }

    /**
     * Updates the content of the landing page.
     *
     * This method triggers a refresh in the ViewModel, which in turn updates the satellite app list
     * and support status. It should be called when the fragment becomes visible or when the
     * subscription ID might have changed.
     */
    private fun updateLandingPageContent() {
        viewModel.refresh(activeSubId)
    }

    /**
     * Adds the satellite landing page PUI illustration.
     *
     * The illustration is not shown if LTE NTN is supported.
     */
    private fun addIllustrationPreference(isLteBasedNtnSupported: Boolean) {
        findPreference<IllustrationPreference>(KEY_ILLUSTRATION)?.apply {
            isVisible = !isLteBasedNtnSupported
            if (isVisible) {
                setImageDrawable(context?.getDrawable(R.drawable.ill_satellite_landing))
            }
        }
    }

    private fun setUpTryADemoButtonListener() {
        val demoButtonPreference = findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON) ?: return
        demoButtonPreference.setOnPreferenceClickListener {
            val action =
                getString(
                    com.android.internal.R.string.config_satellite_demo_mode_sos_intent_action
                )
            startActivitySafely(
                Intent(action)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            true
        }
    }

    /**
     * Sets up the "Try a demo" button visibility.
     *
     * The button should only be visible if NTN LTE is not supported.
     */
    private fun updateTryADemoButtonVisibility(isLteBasedNtnSupported: Boolean) {
        val demoButtonPreference = findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON) ?: return
        // Don't show the button if LTE NTN is supported.
        demoButtonPreference.isVisible = !isLteBasedNtnSupported
    }

    private fun updateTryADemoButtonIcon(isCarrierSupported: Boolean) {
        val demoButtonPreference = findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON) ?: return
        if (isCarrierSupported && Flags.newSatelliteIcon()) {
            demoButtonPreference.icon = SatelliteIconDrawable(requireContext())
        } else {
            demoButtonPreference.icon = requireContext().getDrawable(R.drawable.ic_satellite_demo)
        }
    }

    private fun updateTryADemoButtonEnabledState(isEnabled: Boolean) {
        val demoButtonPreference = findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON) ?: return
        demoButtonPreference.isEnabled = isEnabled
    }

    /** Populates the list of satellite-enabled apps by observing the ViewModel. */
    private fun setUpSatelliteAppsContent() {
        val composePreference = findPreference<ComposePreference>(KEY_SATELLITE_APPS_LIST)
        composePreference?.setContent {
            val satelliteAppItems by viewModel.satelliteAppItems.collectAsState()
            val areAppsEnabled by viewModel.areAppsEnabled.collectAsState()

            composePreference.isVisible = satelliteAppItems.isNotEmpty()
            Column {
                satelliteAppItems.forEach { item ->
                    val appListItemModel =
                        AppListItemModel(
                            record = item,
                            label = item.getAppLabel(packageManager),
                            summary = { "" },
                        )
                    appListItemModel.SatelliteAppListItem(
                        enabled = areAppsEnabled,
                        onClick = { item.intent?.let { startActivitySafely(it) } },
                    )
                }
            }
        }
    }

    /** Sets up the footer preference based on the NTN type. */
    private fun setUpFooterPreference(isLteBasedNtnSupported: Boolean) {
        val footerPreference: FooterPreference? = findPreference(KEY_FOOTER)
        val footerTextResId =
            if (isLteBasedNtnSupported) {
                R.string.landing_page_footer_text_lte
            } else {
                R.string.landing_page_footer_text_nbiot
            }
        footerPreference?.setTitle(footerTextResId)
        footerPreference?.setLearnMoreText(getString(R.string.satellite_more_info_text))
        footerPreference?.setLearnMoreAction {
            viewModel.getSettingsIntent()?.let { startActivitySafely(it) }
        }
    }

    /**
     * Updates the warning banners based on the provided [SatelliteBannerState].
     *
     * This method prioritizes the banners to display. Only the first two applicable banners are
     * shown.
     *
     * @param bannerState The current state of satellite conditions affecting banner display.
     */
    private fun updateWarningBanners(bannerState: SatelliteBannerState) {
        val primaryBanner = findPreference<BannerMessagePreference>(KEY_PRIMARY_WARNING_BANNER)
        val secondaryBanner = findPreference<BannerMessagePreference>(KEY_SECONDARY_WARNING_BANNER)

        // Hide banners by default, they will be shown if a warning is applicable.
        primaryBanner?.isVisible = false
        secondaryBanner?.isVisible = false

        val bannersToShow = mutableListOf<BannerMessagePreference.() -> Unit>()

        // Priority 1: Network connection is active (Satellite is not available).
        if (bannerState.isNetworkConnected) {
            bannersToShow.add { setupNetworkConnectedBanner(this) }
        }

        // Priority 2: Satellite service is not available in the current region.
        if (!bannerState.isSatelliteAvailableInRegion) {
            bannersToShow.add { setupSatelliteUnavailableInRegionBanner(this) }
        }

        // Priority 3: User is not entitled to satellite service.
        if (!bannerState.isEntitled) {
            bannersToShow.add { setupNotEntitledBanner(this) }
        }

        // Priority 4: Default messaging app is not set correctly.
        if (!bannerState.isDefaultMessagingApp) {
            bannersToShow.add { setupNotDefaultMessagingAppBanner(this) }
        }

        // Priority 5: Satellite is enabled by carrier (Informational).
        if (bannerState.isSatelliteEnabledByCarrier) {
            bannersToShow.add { setupSatelliteEnabledByCarrierBanner(this) }
        }

        // Priority 6: Satellite is not allowed (Unspecified generic warning).
        if (!bannerState.isSatelliteAllowed) {
            bannersToShow.add { setupSatelliteGenericUnavailableBanner(this) }
        }

        val availableBanners = listOfNotNull(primaryBanner, secondaryBanner)
        bannersToShow.zip(availableBanners).forEach { (bannerSetup, banner) ->
            banner.apply(bannerSetup)
        }
    }

    private fun setupNetworkConnectedBanner(banner: BannerMessagePreference?) {
        setupWarningBannerPreference(
            banner,
            titleRes = R.string.satellite_not_available_warning_title,
            summaryRes = R.string.satellite_network_connected_warning_summary,
            buttonTextRes = 0,
            intent = null,
        )
    }

    private fun setupSatelliteUnavailableInRegionBanner(banner: BannerMessagePreference?) {
        setupWarningBannerPreference(
            banner,
            titleRes = R.string.satellite_not_available_warning_title,
            summaryRes = R.string.satellite_unavailable_in_region_warning_summary,
            buttonTextRes = R.string.satellite_view_coverage_button,
            // TODO(b/465479769): Create an intent to view available locations.
            intent = null,
        )
    }

    private fun setupSatelliteGenericUnavailableBanner(banner: BannerMessagePreference?) {
        setupWarningBannerPreference(
            banner,
            titleRes = R.string.satellite_not_available_warning_title,
            summaryRes = R.string.satellite_unavailable_generic_warning_summary,
            buttonTextRes = 0,
            intent = null,
        )
    }

    private fun setupNotEntitledBanner(banner: BannerMessagePreference?) {
        setupWarningBannerPreference(
            banner,
            titleRes = R.string.satellite_not_available_warning_title,
            summaryRes = R.string.satellite_plan_warning_summary,
            buttonTextRes = 0,
            intent = null,
        )
    }

    private fun setupNotDefaultMessagingAppBanner(banner: BannerMessagePreference?) {
        setupWarningBannerPreference(
            banner,
            titleRes = R.string.satellite_not_available_warning_title,
            summaryRes = R.string.satellite_default_app_warning_summary,
            buttonTextRes = R.string.satellite_change_default_app_button,
            // TODO(b/465479769): Create intent to change default messaging app.
            intent = null,
        )
    }

    private fun setupSatelliteEnabledByCarrierBanner(banner: BannerMessagePreference?) {
        setupWarningBannerPreference(
            banner,
            titleRes = 0,
            summaryRes = R.string.satellite_carrier_enabled_info_summary,
            buttonTextRes = R.string.satellite_view_carrier_settings_button,
            // TODO(b/465479769): Create intent to view carrier settings.
            intent = null,
        )
    }

    /**
     * Configures a [BannerMessagePreference] with the given parameters.
     *
     * @param banner The banner preference to configure.
     * @param titleRes The resource ID for the banner title. If 0, the title is hidden.
     * @param summaryRes The resource ID for the banner summary.
     * @param buttonTextRes The resource ID for the button text. If 0, the button is hidden.
     * @param intent The intent to launch when the button is clicked. If null, the button does
     *   nothing.
     * @param iconRes The resource ID for the banner icon. Defaults to an info icon.
     */
    private fun setupWarningBannerPreference(
        banner: BannerMessagePreference?,
        @StringRes titleRes: Int,
        @StringRes summaryRes: Int,
        @StringRes buttonTextRes: Int,
        intent: Intent?,
        @DrawableRes iconRes: Int = R.drawable.ic_info_outline_24,
    ) {
        banner?.apply {
            isVisible = true
            setAttentionLevel(BannerMessagePreference.AttentionLevel.NORMAL)

            if (titleRes != 0) {
                setTitle(titleRes)
            } else {
                title = null
            }

            setSummary(summaryRes)
            setIcon(iconRes)
            setPositiveButtonText(null)
            setPositiveButtonOnClickListener(null)

            if (buttonTextRes != 0) {
                setNegativeButtonText(getString(buttonTextRes))
                setNegativeButtonOnClickListener {
                    if (intent != null) {
                        startActivitySafely(intent)
                    }
                }
            } else {
                setNegativeButtonText(null)
                setNegativeButtonOnClickListener(null)
            }
        }
    }

    private fun startActivitySafely(intent: Intent) {
        try {
            context?.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Failed to start activity: ${intent.action ?: intent.component}", e)
        }
    }

    companion object {
        private const val TAG = "SatelliteLandingPageFragment"
        private const val KEY_PRIMARY_WARNING_BANNER = "satellite_settings_warning_banner"
        private const val KEY_SECONDARY_WARNING_BANNER =
            "satellite_settings_secondary_warning_banner"
        private const val KEY_ILLUSTRATION = "illustration"
        private const val KEY_TRY_A_DEMO_BUTTON = "try_a_demo_button"
        private const val KEY_FOOTER = "footer"
        private const val KEY_SATELLITE_APPS_LIST = "satellite_apps_list"
        private const val EXTRA_SHOW_FRAGMENT_AS_SUBSETTING =
            ":settings:show_fragment_as_subsetting"
        private const val EXTRA_SUB_ID = "sub_id"
    }
}
