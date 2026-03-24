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

import android.app.settings.SettingsEnums
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import com.android.internal.telephony.flags.Flags
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settings.spa.preference.ComposePreference
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.Preference as SpaPreference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
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
    private var isVariantMetricLogged = false

    private var appsRepository: SatelliteAppsRepository? = null
    private var backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default
    private var bannerController: SatelliteBannerController? = null

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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (!::packageManager.isInitialized) {
            packageManager = requireContext().packageManager
        }
        setPreferencesFromResource(R.layout.satellite_landing_page_pref, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bannerController =
            SatelliteBannerController(
                requireContext(),
                findPreference(KEY_PRIMARY_WARNING_BANNER),
                findPreference(KEY_SECONDARY_WARNING_BANNER),
            )

        observeViewModel()
        setUpSatelliteAppsContent()
        setUpTryADemoButtonListener()
    }

    override fun onResume() {
        super.onResume()
        FeatureFactory.featureFactory.metricsFeatureProvider.visible(
            context,
            FeatureFactory.featureFactory.metricsFeatureProvider.getAttribution(activity),
            SettingsEnums.SATELLITE_LANDING_PAGE,
            0, // latency
        )
        updateLandingPageContent()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLteBasedNtnSupported.collectLatest { isLte ->
                        if (isLte != null) {
                            if (!isVariantMetricLogged) {
                                val subType = if (isLte) 2 else 1 // 2 for LTE, 1 for NB-NTN
                                FeatureFactory.featureFactory.metricsFeatureProvider.action(
                                    requireContext(),
                                    SettingsEnums.ACTION_SATELLITE_LANDING_PAGE_VARIANT,
                                    subType,
                                )
                                isVariantMetricLogged = true
                            }
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
                        bannerController?.update(bannerState)
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
     * and support status. It should be called when the fragment becomes visible.
     */
    private fun updateLandingPageContent() {
        viewModel.refresh()
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
            FeatureFactory.featureFactory.metricsFeatureProvider.action(
                requireContext(),
                SettingsEnums.ACTION_SATELLITE_DEMO_CLICK,
            )
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
            var isExpanded by rememberSaveable { mutableStateOf(false) }
            val maxVisible = 8

            composePreference.isVisible = satelliteAppItems.isNotEmpty()

            // Optimization: If only 1 extra app exists, just show it instead of a button.
            val showExpandButton = satelliteAppItems.size > maxVisible + 1
            val visibleItems =
                if (isExpanded || !showExpandButton) {
                    satelliteAppItems
                } else {
                    satelliteAppItems.take(maxVisible)
                }

            Column {
                visibleItems.forEach { item ->
                    val appListItemModel =
                        AppListItemModel(
                            record = item,
                            label = item.getAppLabel(packageManager),
                            summary = { item.summary ?: "" },
                        )
                    appListItemModel.SatelliteAppListItem(
                        enabled = areAppsEnabled,
                        onClick = { handleSatelliteAppClick(item) },
                    )
                }

                if (showExpandButton) {
                    val seeAllTitle =
                        stringResource(R.string.satellite_apps_see_all_supported_apps_text)
                    val seeLessTitle = stringResource(R.string.satellite_apps_see_less_text)
                    SpaPreference(
                        remember(isExpanded, seeAllTitle, seeLessTitle) {
                            object : PreferenceModel {
                                override val title = if (isExpanded) seeLessTitle else seeAllTitle
                                override val icon =
                                    @Composable {
                                        Icon(
                                            painter =
                                                if (isExpanded)
                                                    painterResource(
                                                        R.drawable.ic_settings_expand_less
                                                    )
                                                else
                                                    painterResource(
                                                        R.drawable.ic_settings_expand_more
                                                    ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SettingsDimension.itemIconSize),
                                        )
                                    }
                                override val onClick = { isExpanded = !isExpanded }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun handleSatelliteAppClick(item: SatelliteAppItem) {
        // Log the package name of the app that was clicked. No sensitive
        // data is logged as this is only for satellite optimized apps.
        val payload = item.app.packageName
        FeatureFactory.featureFactory.metricsFeatureProvider.action(
            requireContext(),
            SettingsEnums.ACTION_SATELLITE_APP_CLICK,
            payload,
        )
        item.intent?.let { startActivitySafely(it) }
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

    private fun startActivitySafely(intent: Intent) {
        try {
            context?.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Failed to start activity: ${intent.action ?: intent.component}", e)
        } catch (e: SecurityException) {
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
