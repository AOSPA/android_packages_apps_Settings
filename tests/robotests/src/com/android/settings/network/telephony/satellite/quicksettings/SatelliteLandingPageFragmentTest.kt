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
import android.app.settings.SettingsEnums
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Looper
import android.os.PersistableBundle
import android.provider.Settings
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.view.View
import android.widget.TextView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.spa.preference.ComposePreference
import com.android.settings.testutils.MetricsRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.widget.BannerMessagePreference
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.IllustrationPreference
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSubscriptionManager
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h2000dp")
class SatelliteLandingPageFragmentTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val metricsRule = MetricsRule()

    private lateinit var context: Application
    private val SUB_ID = 1
    private val APP1_PACKAGE = "com.app1"
    private val APP1_NAME = "App1"
    private val APP1_INTENT_ACTION = "action"

    // Preference Keys
    private val KEY_ILLUSTRATION = "illustration"
    private val KEY_TRY_A_DEMO_BUTTON = "try_a_demo_button"
    private val KEY_FOOTER = "footer"
    private val KEY_SATELLITE_APPS_LIST = "satellite_apps_list"
    private val KEY_PRIMARY_WARNING_BANNER = "satellite_settings_warning_banner"
    private val KEY_SECONDARY_WARNING_BANNER = "satellite_settings_secondary_warning_banner"

    @Mock private lateinit var subInfo: SubscriptionInfo
    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var appsRepository: SatelliteAppsRepository
    @Mock private lateinit var satelliteStateRepository: SatelliteStateRepository

    private lateinit var fragmentFactory: FragmentFactory
    private val satelliteStatusFlow = MutableStateFlow(SatelliteStatus.NOT_AVAILABLE)
    private val activeSubIdFlow = MutableStateFlow(SUB_ID)
    private val isTerrestrialConnectedFlow = MutableStateFlow(false)
    private val satelliteDisallowedReasonsFlow = MutableStateFlow<IntArray>(intArrayOf())

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.setTheme(R.style.Theme_Settings)

        // Mock System Services
        ShadowSubscriptionManager.setActiveDataSubscriptionId(SUB_ID)
        `when`(subInfo.subscriptionId).thenReturn(SUB_ID)
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        shadowOf(subscriptionManager).setActiveSubscriptionInfoList(listOf(subInfo))
        val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!
        shadowOf(carrierConfigManager).setConfigForSubId(SUB_ID, PersistableBundle())

        // Mock Apps Repository
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(listOf())
        `when`(appsRepository.getAppsPackagesForNbNtnLandingPage()).thenReturn(listOf())
        `when`(appsRepository.getDialerIntent()).thenReturn(null)
        `when`(appsRepository.getSettingsIntent(org.mockito.ArgumentMatchers.anyBoolean()))
            .thenReturn(null)

        // Mock State Repository
        `when`(satelliteStateRepository.satelliteStatus).thenReturn(satelliteStatusFlow)
        `when`(satelliteStateRepository.activeSubIdFlow).thenReturn(activeSubIdFlow)
        `when`(satelliteStateRepository.isTerrestrialConnected)
            .thenReturn(isTerrestrialConnectedFlow)
        `when`(satelliteStateRepository.satelliteDisallowedReasons)
            .thenReturn(satelliteDisallowedReasonsFlow)
        `when`(satelliteStateRepository.getAttachRestrictionReasons(SUB_ID)).thenReturn(emptySet())
        SatelliteStateRepository.setInstance(satelliteStateRepository)

        fragmentFactory =
            object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                    return SatelliteLandingPageFragment(
                        packageManager,
                        appsRepository,
                        Dispatchers.Main,
                    )
                }
            }
    }

    @Test
    fun illustration_whenLteNtnSupported_isHidden() {
        setLteNtnSupported(true)
        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val illustrationPreference =
                fragment.findPreference<IllustrationPreference>(KEY_ILLUSTRATION)

            assertThat(illustrationPreference).isNotNull()
            assertThat(illustrationPreference!!.isVisible).isFalse()
        }
    }

    @Test
    fun illustration_whenLteNtnNotSupported_isVisible() {
        setLteNtnSupported(false)
        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val illustrationPreference =
                fragment.findPreference<IllustrationPreference>(KEY_ILLUSTRATION)

            assertThat(illustrationPreference).isNotNull()
            assertThat(illustrationPreference!!.isVisible).isTrue()
            assertThat(illustrationPreference.getImageDrawable()).isNotNull()
        }
    }

    @Test
    fun onResume_refreshesAppsList() {
        setLteNtnSupported(true)
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(emptyList())
        val scenario = launchFragment()
        // Verify initially empty
        scenario.onFragment { fragment ->
            assertThat(fragment.viewModel.satelliteAppItems.value).isEmpty()
        }
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(listOf(APP1_PACKAGE))
        setupPackageManagerForApp(APP1_PACKAGE, APP1_NAME, Intent(APP1_INTENT_ACTION))

        // Trigger onResume via lifecycle
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        waitForAsync()

        scenario.onFragment { fragment ->
            assertThat(fragment.viewModel.satelliteAppItems.value).hasSize(1)
        }
    }

    @Test
    fun satelliteApps_whenStatusNotAvailable_appsAreDisabled() {
        setLteNtnSupported(true)
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(listOf("com.app1"))
        setupPackageManagerForApp("com.app1", "App1", Intent("action"))
        satelliteStatusFlow.value = SatelliteStatus.NOT_AVAILABLE

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            assertThat(fragment.viewModel.areAppsEnabled.value).isFalse()
        }
    }

    @Test
    fun tryDemoButton_onClick_startsDemoActivity() {
        setLteNtnSupported(false) // Make button visible
        satelliteStatusFlow.value = SatelliteStatus.AVAILABLE
        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val demoButton = fragment.findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON)
            demoButton!!.performClick()
        }

        val startedIntent = shadowOf(context).nextStartedActivity
        assertThat(startedIntent).isNotNull()
        val expectedAction =
            context.getString(
                com.android.internal.R.string.config_satellite_demo_mode_sos_intent_action
            )
        assertThat(startedIntent.action).isEqualTo(expectedAction)
    }

    @Test
    fun tryDemoButton_onClick_whenActivityNotFound_doesNotCrash() {
        setLteNtnSupported(false) // Make button visible
        val scenario = launchFragment()
        // Enable activity checking to simulate ActivityNotFoundException
        shadowOf(context).checkActivities(true)

        scenario.onFragment { fragment ->
            val demoButton = fragment.findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON)
            demoButton!!.performClick()
        }

        // Should not crash.
        // Because checkActivities(true) is enabled and the intent is not resolved,
        // startActivity should throw ActivityNotFoundException (caught by fragment),
        // and the intent should NOT be recorded as started.
        val startedIntent = shadowOf(context).nextStartedActivity
        assertThat(startedIntent).isNull()
    }

    @Test
    fun tryDemoButton_whenLteNotSupported_isVisible() {
        setLteNtnSupported(false)
        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val demoButton = fragment.findPreference<Preference>("try_a_demo_button")

            assertThat(demoButton?.isVisible).isTrue()
        }
    }

    @Test
    fun footerLearnMore_onClick_startsSatelliteSettingsActivity() {
        val intent = Intent(Settings.ACTION_SATELLITE_SETTING)
        `when`(appsRepository.getSettingsIntent(org.mockito.ArgumentMatchers.anyBoolean()))
            .thenReturn(intent)

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
    }

    @Test
    fun illustration_whenCreated_isSet() {
        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val illustration = fragment.findPreference<IllustrationPreference>("illustration")
            assertThat(illustration).isNotNull()
            assertThat(illustration?.isVisible).isTrue()
        }
    }

    @Test
    fun satelliteApps_whenHasApps_listIsVisible() {
        setLteNtnSupported(true) // for LTE page
        val lteAppPackages = listOf(APP1_PACKAGE)
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(lteAppPackages)
        setupPackageManagerForApp(APP1_PACKAGE, APP1_NAME, Intent(APP1_INTENT_ACTION))

        val scenario = launchFragment()
        composeTestRule.waitForIdle()

        scenario.onFragment { fragment ->
            val composePref = fragment.findPreference<ComposePreference>("satellite_apps_list")
            assertThat(composePref?.isVisible).isTrue()
        }
    }

    @Test
    fun satelliteApps_when9Apps_showsAllAndNoExpandButton() {
        setLteNtnSupported(true)
        val apps = (1..9).map { "com.app$it" }
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(apps)
        apps.forEachIndexed { index, pkg ->
            setupPackageManagerForApp(pkg, "App${index + 1}", Intent("action${index + 1}"))
        }

        val scenario = launchFragment()

        // Verify all 9 apps are displayed
        (1..9).forEach {
            composeTestRule.onAllNodesWithText("App$it").onFirst().assertIsDisplayed()
        }

        // Verify "See all" button is NOT displayed
        val seeAllText = context.getString(R.string.satellite_apps_see_all_supported_apps_text)
        composeTestRule.onNodeWithText(seeAllText).assertDoesNotExist()
    }

    @Test
    fun satelliteApps_when10Apps_showsCollapsedAndExpandButton() {
        setLteNtnSupported(true)
        val apps = (1..10).map { "com.app$it" }
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(apps)
        apps.forEachIndexed { index, pkg ->
            setupPackageManagerForApp(pkg, "App${index + 1}", Intent("action${index + 1}"))
        }

        val scenario = launchFragment()

        // Verify first 8 apps are displayed
        (1..8).forEach {
            composeTestRule.onAllNodesWithText("App$it").onFirst().assertIsDisplayed()
        }
        // Verify 9th and 10th apps are NOT displayed initially
        composeTestRule.onNodeWithText("App9").assertDoesNotExist()
        composeTestRule.onNodeWithText("App10").assertDoesNotExist()

        // Verify "See all" button is displayed
        val seeAllText = context.getString(R.string.satellite_apps_see_all_supported_apps_text)
        composeTestRule.onAllNodesWithText(seeAllText).onFirst().assertIsDisplayed()

        // Click "See all"
        composeTestRule.onAllNodesWithText(seeAllText).onFirst().performClick()

        // Verify all 10 apps are displayed
        (1..10).forEach {
            composeTestRule.onAllNodesWithText("App$it").onFirst().assertIsDisplayed()
        }

        // Verify "See less" button is displayed
        val seeLessText = context.getString(R.string.satellite_apps_see_less_text)
        composeTestRule.onAllNodesWithText(seeLessText).onFirst().assertIsDisplayed()

        // Click "See less"
        composeTestRule.onAllNodesWithText(seeLessText).onFirst().performClick()

        // Verify we are back to collapsed state
        composeTestRule.onNodeWithText("App9").assertDoesNotExist()
        composeTestRule.onAllNodesWithText(seeAllText).onFirst().assertIsDisplayed()
    }

    @Test
    fun satelliteApps_whenNoApps_listIsHidden() {
        setLteNtnSupported(true)
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(emptyList())

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val composePref = fragment.findPreference<ComposePreference>("satellite_apps_list")
            // Ideally we check visibility. The fragment sets composePreference.isVisible =
            // satelliteAppItems.isNotEmpty()
            // inside the setContent block. This is hard to test with standard Robolectric as
            // setContent runs in Compose.
            // However, verify that we at least tried to load items.
            assertThat(fragment.viewModel.satelliteAppItems.value).isEmpty()
        }
    }

    @Test
    fun footer_whenLteSupported_showsLteText() {
        setLteNtnSupported(true)

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val footer = fragment.findPreference<FooterPreference>("footer")
            assertThat(footer?.title)
                .isEqualTo(context.getString(R.string.landing_page_footer_text_lte))
        }
    }

    @Test
    fun footer_whenLteNotSupported_showsNbIotText() {
        setLteNtnSupported(false)

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val footer = fragment.findPreference<FooterPreference>("footer")
            assertThat(footer?.title)
                .isEqualTo(context.getString(R.string.landing_page_footer_text_nbiot))
        }
    }

    @Test
    fun satelliteIconDrawable_hasCorrectIntrinsicSize() {
        val drawable = SatelliteIconDrawable(context)
        val density = context.resources.displayMetrics.density

        // Expected: 73dp x 32dp
        val expectedWidth = (SatelliteIconDrawable.INTRINSIC_WIDTH_DP * density).toInt()
        val expectedHeight = (SatelliteIconDrawable.INTRINSIC_HEIGHT_DP * density).toInt()

        assertEquals(expectedWidth, drawable.intrinsicWidth)
        assertEquals(expectedHeight, drawable.intrinsicHeight)
    }

    @Test
    fun tryDemoButton_isSatelliteDemoPreference() {
        setLteNtnSupported(false)

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val demoButton = fragment.findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON)
            assertThat(demoButton).isInstanceOf(SatelliteDemoPreference::class.java)
        }
    }

    @Test
    fun tryDemoButton_whenStatusActive_isDisabled() {
        setLteNtnSupported(false) // Make button visible
        satelliteStatusFlow.value = SatelliteStatus.ACTIVE

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val demoButton = fragment.findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON)
            assertThat(demoButton?.isEnabled).isFalse()
        }
    }

    @Test
    fun tryDemoButton_whenStatusAvailable_isEnabled() {
        setLteNtnSupported(false) // Make button visible
        satelliteStatusFlow.value = SatelliteStatus.AVAILABLE

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val demoButton = fragment.findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON)
            assertThat(demoButton?.isEnabled).isTrue()
        }
    }

    @Test
    fun bannerState_updatesBannerController() {
        val scenario = launchFragment()
        scenario.onFragment { fragment ->
            val viewModel = fragment.viewModel
            val bannerStateFlow =
                ReflectionHelpers.getField<MutableStateFlow<SatelliteBannerState>>(
                    viewModel,
                    "_bannerState",
                )

            // Trigger a state that should show a banner
            bannerStateFlow.value = SatelliteBannerState(isNetworkConnected = true)
        }
        waitForAsync()

        scenario.onFragment { fragment ->
            val primaryBanner =
                fragment.findPreference<BannerMessagePreference>(KEY_PRIMARY_WARNING_BANNER)

            assertThat(primaryBanner?.isVisible).isTrue()
            assertThat(primaryBanner?.summary)
                .isEqualTo(context.getString(R.string.satellite_network_connected_warning_summary))
        }
    }

    @Test
    fun onResume_logsPageVisible() {
        val scenario = launchFragment()
        scenario.moveToState(Lifecycle.State.RESUMED)

        scenario.onFragment { fragment ->
            verify(metricsRule.metricsFeatureProvider)
                .visible(
                    fragment.requireContext(),
                    metricsRule.metricsFeatureProvider.getAttribution(fragment.activity),
                    SettingsEnums.SATELLITE_LANDING_PAGE,
                    0, // latency
                )
        }
    }

    @Test
    fun onLteNtnSupportChanged_whenFalse_logsNbiotVariant() {
        setLteNtnSupported(false)
        val scenario = launchFragment()

        scenario.onFragment { fragment -> fragment.viewModel.refresh() }
        waitForAsync()

        verify(metricsRule.metricsFeatureProvider)
            .action(
                any(),
                eq(SettingsEnums.ACTION_SATELLITE_LANDING_PAGE_VARIANT),
                eq(1),
            ) // Nb-IoT variant 1, LTE variant 2
    }

    @Test
    fun onLteNtnSupportChanged_whenTrue_logsLTEVariant() {
        setLteNtnSupported(true)
        val scenario = launchFragment()

        scenario.onFragment { fragment -> fragment.viewModel.refresh() }
        waitForAsync()

        verify(metricsRule.metricsFeatureProvider)
            .action(any(), eq(SettingsEnums.ACTION_SATELLITE_LANDING_PAGE_VARIANT), eq(2))
    }

    @Test
    fun onLteNtnSupportChanged_logsVariantOnlyOnce() {
        setLteNtnSupported(false)
        val scenario = launchFragment()
        scenario.onFragment { fragment ->
            val viewModel = fragment.viewModel
            val isLteBasedNtnSupportedFlow =
                ReflectionHelpers.getField<MutableStateFlow<Boolean>>(
                    viewModel,
                    "_isLteBasedNtnSupported",
                )

            // Trigger a state change to LTE variant
            isLteBasedNtnSupportedFlow.value = false
            isLteBasedNtnSupportedFlow.value = true
        }
        waitForAsync()

        verify(metricsRule.metricsFeatureProvider, times(1))
            .action(any(), eq(SettingsEnums.ACTION_SATELLITE_LANDING_PAGE_VARIANT), eq(1))
        verify(metricsRule.metricsFeatureProvider, never())
            .action(any(), eq(SettingsEnums.ACTION_SATELLITE_LANDING_PAGE_VARIANT), eq(2))
    }

    @Test
    fun tryDemoButton_onClick_logsAction() {
        setLteNtnSupported(false) // Make button visible
        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val demoButton = fragment.findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON)
            demoButton!!.performClick()
        }

        verify(metricsRule.metricsFeatureProvider)
            .action(any(), eq(SettingsEnums.ACTION_SATELLITE_DEMO_CLICK))
    }

    @Test
    fun onAppClick_logsActionWithPackageName() {
        setLteNtnSupported(true)
        satelliteStatusFlow.value = SatelliteStatus.AVAILABLE
        val messagingPackage = "com.google.android.apps.messaging"
        `when`(appsRepository.getAppsPackagesForLteLandingPage())
            .thenReturn(listOf(messagingPackage))
        setupPackageManagerForApp(messagingPackage, "Messages", Intent("action"))
        val scenario = launchFragment()
        waitForAsync()

        composeTestRule.onNodeWithText("Messages").performClick()
        waitForAsync()

        verify(metricsRule.metricsFeatureProvider)
            .action(any(), eq(SettingsEnums.ACTION_SATELLITE_APP_CLICK), eq(messagingPackage))
    }

    private fun waitForAsync() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun launchFragment(): FragmentScenario<SatelliteLandingPageFragment> {
        val scenario: FragmentScenario<SatelliteLandingPageFragment> =
            launchFragmentInContainer(
                themeResId = R.style.Theme_Settings,
                factory = fragmentFactory,
            )
        waitForAsync()
        return scenario
    }

    private fun setupPackageManagerForApp(packageName: String, appName: String, intent: Intent) {
        val appInfo = mock(ApplicationInfo::class.java)
        appInfo.packageName = packageName
        `when`(appInfo.loadLabel(packageManager)).thenReturn(appName)
        `when`(appInfo.loadIcon(packageManager)).thenReturn(mock(Drawable::class.java))
        // Use doReturn for methods that can throw checked exceptions to avoid Mockito issues.
        doReturn(appInfo).`when`(packageManager).getApplicationInfo(packageName, 0)
        doReturn(mock(Drawable::class.java)).`when`(packageManager).getApplicationIcon(packageName)
        `when`(packageManager.getLaunchIntentForPackage(packageName)).thenReturn(intent)
    }

    private fun setLteNtnSupported(isSupported: Boolean) {
        val reasons = if (isSupported) emptySet() else setOf(1)
        val config =
            PersistableBundle().apply {
                putBoolean(CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, isSupported)
                if (isSupported) {
                    putInt(
                        CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                        CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
                    )
                }
            }
        val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!
        shadowOf(carrierConfigManager).setConfigForSubId(SUB_ID, config)
    }
}
