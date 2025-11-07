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

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import android.provider.Settings
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.satellite.SatelliteManager
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.spa.preference.ComposePreference
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.IllustrationPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSatelliteManager
import org.robolectric.shadows.ShadowSubscriptionManager
import org.robolectric.util.ReflectionHelpers

/**
 * Test suite for [SatelliteLandingPageFragment].
 *
 * This class tests the behavior of the satellite landing page under various conditions, such as
 * network availability and satellite feature support.
 */
@RunWith(RobolectricTestRunner::class)
class SatelliteLandingPageFragmentTest {
    private lateinit var context: Application
    private lateinit var shadowSatelliteManager: ShadowSatelliteManager
    private val SUB_ID = 1

    @Mock private lateinit var subInfo: SubscriptionInfo
    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var appsRepository: SatelliteAppsRepository

    private lateinit var fragmentFactory: FragmentFactory

    private companion object {
        private const val KEY_ILLUSTRATION = "illustration"
        private const val KEY_TRY_A_DEMO_BUTTON = "try_a_demo_button"
        private const val KEY_FOOTER = "footer"
        private const val APP1_PACKAGE = "com.app1"
        private const val APP1_NAME = "App1"
        private const val APP1_INTENT_ACTION = "app1.intent.action"
        private const val APP2_PACKAGE = "com.app2"
        private const val APP2_NAME = "App2"
        private const val APP2_INTENT_ACTION = "app2.intent.action"
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.getApplication()
        context.setTheme(R.style.Theme_Settings)

        shadowSatelliteManager =
            Shadow.extract(context.getSystemService(SatelliteManager::class.java))
        ShadowSubscriptionManager.setActiveDataSubscriptionId(SUB_ID)
        `when`(subInfo.subscriptionId).thenReturn(SUB_ID)
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        shadowOf(subscriptionManager).setActiveSubscriptionInfoList(listOf(subInfo))

        val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!
        shadowOf(carrierConfigManager).setConfigForSubId(SUB_ID, PersistableBundle())

        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(listOf())
        `when`(appsRepository.getAppsPackagesForNbNtnLandingPage()).thenReturn(listOf())
        `when`(appsRepository.getEmergencySosIntent()).thenReturn(null)
        `when`(appsRepository.getSettingsIntent()).thenReturn(null)

        fragmentFactory =
            object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                    return SatelliteLandingPageFragment(packageManager, appsRepository)
                }
            }
    }

    @Test
    fun onCreate_loadsPreferencesAndSetsIllustration() {
        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val illustrationPreference =
                fragment.findPreference<IllustrationPreference>(KEY_ILLUSTRATION)

            assertThat(illustrationPreference).isNotNull()
            assertThat(illustrationPreference?.getImageDrawable()).isNotNull()
        }
    }

    @Test
    fun tryDemoButton_whenLteNtnNotSupported_isVisible() {
        setLteNtnSupported(false)

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val demoButton = fragment.findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON)
            assertThat(demoButton).isNotNull()
            assertThat(demoButton!!.isVisible).isTrue()
        }
    }

    @Test
    fun tryDemoButton_whenLteNtnSupported_isHidden() {
        setLteNtnSupported(true)

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val demoButton = fragment.findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON)
            assertThat(demoButton).isNotNull()
            assertThat(demoButton!!.isVisible).isFalse()
        }
    }

    @Test
    fun tryDemoButton_onClick_startsDemoActivity() {
        setLteNtnSupported(false) // Make button visible
        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val demoButton = fragment.findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON)
            demoButton!!.performClick()
        }

        val startedIntent = shadowOf(context).nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.action)
            .isEqualTo("com.google.android.apps.stargate.ACTION_ESOS_DEMO")
    }

    @Test
    fun footerText_whenLteNtnSupported_isLteText() {
        setLteNtnSupported(true)

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val footer = fragment.findPreference<FooterPreference>(KEY_FOOTER)
            assertThat(footer!!.title)
                .isEqualTo(context.getString(R.string.landing_page_footer_text_lte))
        }
    }

    @Test
    fun footerText_whenLteNtnNotSupported_isNbiotText() {
        setLteNtnSupported(false)

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val footer = fragment.findPreference<FooterPreference>(KEY_FOOTER)
            assertThat(footer!!.title)
                .isEqualTo(context.getString(R.string.landing_page_footer_text_nbiot))
        }
    }

    @Test
    fun footerLearnMore_onClick_startsSatelliteSettingsActivity() {
        val scenario = launchFragment()
        scenario.onFragment { fragment ->
            val footer = fragment.findPreference<FooterPreference>(KEY_FOOTER)
            val prefViewHolder = footer!!.inflateViewHolder()
            val learnMoreView =
                prefViewHolder.itemView.findViewById<TextView>(R.string.satellite_more_info_text)

            ReflectionHelpers.getField<View.OnClickListener>(footer, "mLearnMoreListener")
                .onClick(learnMoreView)
        }

        val startedIntent = shadowOf(context).nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.action).isEqualTo(Settings.ACTION_SATELLITE_SETTING)
        assertThat(startedIntent.hasExtra("sub_id")).isTrue()
        assertThat(startedIntent.getBooleanExtra(":settings:show_fragment_as_subsetting", false))
            .isTrue()
    }

    @Test
    fun satelliteApps_whenHasApps_listIsVisible() {
        setLteNtnSupported(true) // for LTE page
        val lteAppPackages = listOf(APP1_PACKAGE)
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(lteAppPackages)
        setupPackageManagerForApp(APP1_PACKAGE, APP1_NAME, Intent(APP1_INTENT_ACTION))

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val appsList: ComposePreference? =
                fragment.findPreference<ComposePreference>("satellite_apps_list")
            assertThat(appsList).isNotNull()
            assertThat(appsList!!.isVisible).isTrue()
        }
    }

    @Test
    fun satelliteApps_whenHasApps_listHasCorrectItems() {
        setLteNtnSupported(true) // for LTE page
        val lteAppPackages = listOf(APP1_PACKAGE, APP2_PACKAGE)
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(lteAppPackages)
        setupPackageManagerForApp(APP1_PACKAGE, APP1_NAME, Intent(APP1_INTENT_ACTION))
        setupPackageManagerForApp(APP2_PACKAGE, APP2_NAME, Intent(APP2_INTENT_ACTION))

        val scenario = launchFragment()

        scenario.onFragment { fragment: SatelliteLandingPageFragment ->
            val satelliteAppItems: List<SatelliteAppItem> =
                fragment.viewModel.satelliteAppItems.value
            assertThat(satelliteAppItems).hasSize(2)
            assertThat(satelliteAppItems[0].getAppLabel(packageManager)).isEqualTo(APP1_NAME)
            assertThat(satelliteAppItems[1].getAppLabel(packageManager)).isEqualTo(APP2_NAME)
        }
    }

    @Test
    fun satelliteApps_whenNoApps_listIsHidden() {
        setLteNtnSupported(true) // for LTE page
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(emptyList())
        // Also ensure SOS and Settings apps are not available
        `when`(appsRepository.getEmergencySosIntent()).thenReturn(null)
        `when`(appsRepository.getSettingsIntent()).thenReturn(null)

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val appsList = fragment.findPreference<ComposePreference>("satellite_apps_list")
            assertThat(appsList).isNotNull()
            assertThat(appsList!!.isVisible).isFalse()
        }
    }

    @Test
    fun satelliteApps_appItemHasCorrectIntent() {
        setLteNtnSupported(true) // for LTE page
        val lteAppPackages = listOf(APP1_PACKAGE)
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(lteAppPackages)
        val app1Intent = Intent(APP1_INTENT_ACTION)
        setupPackageManagerForApp(APP1_PACKAGE, APP1_NAME, app1Intent)

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val satelliteAppItems = fragment.viewModel.satelliteAppItems.value
            assertThat(satelliteAppItems).hasSize(1)
            assertThat(satelliteAppItems[0].intent).isEqualTo(app1Intent)
        }
    }

    @Test
    fun onResume_updatesContentWhenAppListChanges() {
        setLteNtnSupported(true) // for LTE page
        // Setup initial app (App1) details for PackageManager
        setupPackageManagerForApp(APP1_PACKAGE, APP1_NAME, Intent(APP1_INTENT_ACTION))
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(listOf(APP1_PACKAGE))
        val scenario = launchFragment()
        // Verify initial UI
        scenario.onFragment { fragment -> assertAppsListContent(fragment, listOf(APP1_NAME)) }
        // Change the underlying data by re-mocking the repository to return App2
        setupPackageManagerForApp(APP2_PACKAGE, APP2_NAME, Intent(APP2_INTENT_ACTION))
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(listOf(APP2_PACKAGE))

        // Trigger onResume lifecycle method to force UI update
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)

        // Verify UI is updated
        scenario.onFragment { fragment -> assertAppsListContent(fragment, listOf(APP2_NAME)) }
    }

    private fun assertAppsListContent(
        fragment: SatelliteLandingPageFragment,
        expectedAppNames: List<String>,
    ) {
        val appsList = fragment.findPreference<ComposePreference>("satellite_apps_list")!!
        assertThat(appsList.isVisible).isEqualTo(expectedAppNames.isNotEmpty())
        val satelliteAppItems = fragment.viewModel.satelliteAppItems.value
        val actualAppNames = satelliteAppItems.map { it.getAppLabel(packageManager) }
        assertThat(actualAppNames).containsExactlyElementsIn(expectedAppNames).inOrder()
    }

    private fun launchFragment(): FragmentScenario<SatelliteLandingPageFragment> {
        return launchFragmentInContainer(
            themeResId = R.style.Theme_Settings,
            factory = fragmentFactory,
        )
    }

    private fun setupPackageManagerForApp(packageName: String, appName: String, intent: Intent) {
        val appInfo = mock(ApplicationInfo::class.java)
        `when`(appInfo.loadLabel(packageManager)).thenReturn(appName)
        `when`(appInfo.loadIcon(packageManager)).thenReturn(mock(Drawable::class.java))
        // Use doReturn for methods that can throw checked exceptions to avoid Mockito issues.
        doReturn(appInfo).`when`(packageManager).getApplicationInfo(packageName, 0)
        doReturn(mock(Drawable::class.java)).`when`(packageManager).getApplicationIcon(packageName)
        `when`(packageManager.getLaunchIntentForPackage(packageName)).thenReturn(intent)
    }

    /**
     * Configures the test environment to simulate whether LTE NTN is supported.
     *
     * @param isSupported `true` to simulate that LTE NTN is supported, `false` otherwise.
     */
    private fun setLteNtnSupported(isSupported: Boolean) {
        // A non-empty set of restriction reasons means that attach is restricted.
        val reasons = if (isSupported) emptySet() else setOf(1)
        shadowSatelliteManager.setAttachRestrictionReasonsForCarrier(SUB_ID, reasons)

        val config =
            PersistableBundle().apply {
                putBoolean(CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, isSupported)
                putInt(
                    CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                    CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
                )
            }
        val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!
        shadowOf(carrierConfigManager).setConfigForSubId(SUB_ID, config)
    }
}
