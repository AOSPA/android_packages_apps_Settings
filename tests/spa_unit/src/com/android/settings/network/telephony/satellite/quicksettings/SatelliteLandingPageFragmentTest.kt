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

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
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
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settings.fuelgauge.PowerUsageFeatureProvider
import com.android.settings.network.telephony.TelephonyFeatureProvider
import com.android.settings.network.telephony.satellite.SatelliteSettingsRepository
import com.android.settings.overlay.FeatureFactory
import com.android.settings.spa.preference.ComposePreference
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider
import com.android.settingslib.widget.BannerMessagePreference
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.IllustrationPreference
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoSession
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class SatelliteLandingPageFragmentTest {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    private lateinit var context: Context
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

    private lateinit var mockitoSession: MockitoSession

    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var appsRepository: SatelliteAppsRepository
    @Mock private lateinit var satelliteStateRepository: SatelliteStateRepository
    @Mock private lateinit var mockMetricsFeatureProvider: MetricsFeatureProvider
    @Mock private lateinit var mockFeatureFactory: FeatureFactory
    @Mock private lateinit var mockPowerUsageFeatureProvider: PowerUsageFeatureProvider
    @Mock private lateinit var mockTelephonyFeatureProvider: TelephonyFeatureProvider
    @Mock private lateinit var mockSatelliteSettingsRepository: SatelliteSettingsRepository

    private lateinit var fragmentFactory: FragmentFactory
    private val satelliteStatusFlow = MutableStateFlow(SatelliteStatus.NOT_AVAILABLE)
    private val activeSubIdFlow = MutableStateFlow(SUB_ID)
    private val isTerrestrialConnectedFlow = MutableStateFlow(false)
    private val satelliteDisallowedReasonsFlow = MutableStateFlow<IntArray>(intArrayOf())

    @Before
    fun setUp() {
        mockitoSession =
            ExtendedMockito.mockitoSession()
                .initMocks(this)
                .mockStatic(SatelliteUtils::class.java)
                .mockStatic(RestrictedLockUtilsInternal::class.java)
                .strictness(Strictness.LENIENT)
                .startMocking()

        context = ApplicationProvider.getApplicationContext()
        Intents.init()
        intending(not(hasAction(Intent.ACTION_MAIN)))
            .respondWith(ActivityResult(Activity.RESULT_OK, null))

        // Mock FeatureFactory
        `when`(mockFeatureFactory.metricsFeatureProvider).thenReturn(mockMetricsFeatureProvider)
        `when`(mockFeatureFactory.powerUsageFeatureProvider)
            .thenReturn(mockPowerUsageFeatureProvider)
        `when`(mockFeatureFactory.telephonyFeatureProvider).thenReturn(mockTelephonyFeatureProvider)
        `when`(mockTelephonyFeatureProvider.satelliteSettingsRepository)
            .thenReturn(mockSatelliteSettingsRepository)
        FeatureFactory.setFactory(context, mockFeatureFactory)

        // Mock SatelliteUtils
        // Default to not supported
        whenever(SatelliteUtils.isLteBasedNtnSupported(anyInt())).thenReturn(false)
        whenever(SatelliteUtils.isLteBasedNtnSupported(any(), anyInt())).thenReturn(false)
        whenever(SatelliteUtils.isCarrierRoamingNtnSupported(anyInt())).thenReturn(false)

        // Mock RestrictedLockUtilsInternal
        whenever(
                RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                    any(),
                    anyInt(),
                    anyInt(),
                )
            )
            .thenReturn(null)

        // Mock Apps Repository
        `when`(appsRepository.getAppsPackagesForLteLandingPage(anyBoolean())).thenReturn(listOf())
        `when`(appsRepository.getAppsPackagesForNbNtnLandingPage()).thenReturn(listOf())
        `when`(appsRepository.getDialerIntent()).thenReturn(null)
        doReturn(null).`when`(appsRepository).getSettingsIntent(anyBoolean())

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

    @After
    fun tearDown() {
        Intents.release()
        mockitoSession.finishMocking()
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
        `when`(appsRepository.getAppsPackagesForLteLandingPage(anyBoolean()))
            .thenReturn(emptyList())
        val scenario = launchFragment()
        // Verify initially empty
        scenario.onFragment { fragment ->
            assertThat(fragment.viewModel.satelliteAppItems.value).isEmpty()
        }
        `when`(appsRepository.getAppsPackagesForLteLandingPage(anyBoolean()))
            .thenReturn(listOf(APP1_PACKAGE))
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
        `when`(appsRepository.getAppsPackagesForLteLandingPage(anyBoolean()))
            .thenReturn(listOf("com.app1"))
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

        val expectedAction =
            context.getString(
                com.android.internal.R.string.config_satellite_demo_mode_sos_intent_action
            )
        val recordedIntents = Intents.getIntents()
        val found = recordedIntents.any { it.action == expectedAction }
        assertThat(found).isTrue()
    }

    @Test
    fun tryDemoButton_onClick_whenActivityNotFound_doesNotCrash() {
        setLteNtnSupported(false) // Make button visible
        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val demoButton = fragment.findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON)
            demoButton!!.performClick()
        }
        // If we are here, it didn't crash.
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

            // Access private mLearnMoreListener using reflection
            val listener = getField<View.OnClickListener>(footer, "mLearnMoreListener")
            listener.onClick(learnMoreView)
        }

        val recordedIntents = Intents.getIntents()
        val found = recordedIntents.any { it.action == Settings.ACTION_SATELLITE_SETTING }
        assertThat(found).isTrue()
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
        `when`(appsRepository.getAppsPackagesForLteLandingPage(anyBoolean()))
            .thenReturn(lteAppPackages)
        setupPackageManagerForApp(APP1_PACKAGE, APP1_NAME, Intent(APP1_INTENT_ACTION))

        val scenario = launchFragment()
        scenario.onFragment { fragment -> fragment.viewModel.refresh() }
        waitForAsync()

        scenario.onFragment { fragment ->
            val composePref = fragment.findPreference<ComposePreference>("satellite_apps_list")
            assertThat(composePref?.isVisible).isTrue()
        }
    }

    @Test
    fun satelliteApps_when9Apps_showsAllAndNoExpandButton() {
        setLteNtnSupported(true)
        val apps = (1..9).map { "com.app$it" }
        `when`(appsRepository.getAppsPackagesForLteLandingPage(anyBoolean())).thenReturn(apps)
        apps.forEachIndexed { index, pkg ->
            setupPackageManagerForApp(pkg, "App${index + 1}", Intent("action${index + 1}"))
        }

        val scenario = launchFragment()

        // Verify all 9 apps are displayed
        (1..9).forEach { composeTestRule.onAllNodesWithText("App$it").onFirst().assertExists() }

        // Verify "See all" button is NOT displayed
        val seeAllText = context.getString(R.string.satellite_apps_see_all_supported_apps_text)
        composeTestRule.onNodeWithText(seeAllText).assertDoesNotExist()
    }

    @Test
    fun satelliteApps_when10Apps_showsCollapsedAndExpandButton() {
        setLteNtnSupported(true)
        val apps = (1..10).map { "com.app$it" }
        `when`(appsRepository.getAppsPackagesForLteLandingPage(anyBoolean())).thenReturn(apps)
        apps.forEachIndexed { index, pkg ->
            setupPackageManagerForApp(pkg, "App${index + 1}", Intent("action${index + 1}"))
        }

        val scenario = launchFragment()

        // Verify first 8 apps are displayed
        (1..8).forEach { composeTestRule.onAllNodesWithText("App$it").onFirst().assertExists() }
        // Verify 9th and 10th apps are NOT displayed initially
        composeTestRule.onNodeWithText("App9").assertDoesNotExist()
        composeTestRule.onNodeWithText("App10").assertDoesNotExist()

        // Verify "See all" button is displayed
        val seeAllText = context.getString(R.string.satellite_apps_see_all_supported_apps_text)
        composeTestRule.onAllNodesWithText(seeAllText).onFirst().assertExists()

        // Click "See all"
        composeTestRule.onAllNodesWithText(seeAllText).onFirst().performClick()

        // Verify all 10 apps are displayed
        (1..10).forEach { composeTestRule.onAllNodesWithText("App$it").onFirst().assertExists() }

        // Verify "See less" button is displayed
        val seeLessText = context.getString(R.string.satellite_apps_see_less_text)
        composeTestRule.onAllNodesWithText(seeLessText).onFirst().assertExists()

        // Click "See less"
        composeTestRule.onAllNodesWithText(seeLessText).onFirst().performClick()

        // Verify we are back to collapsed state
        composeTestRule.onNodeWithText("App9").assertDoesNotExist()
        composeTestRule.onAllNodesWithText(seeAllText).onFirst().assertExists()
    }

    @Test
    fun satelliteApps_whenNoApps_listIsHidden() {
        setLteNtnSupported(true)
        `when`(appsRepository.getAppsPackagesForLteLandingPage(anyBoolean()))
            .thenReturn(emptyList())

        val scenario = launchFragment()

        scenario.onFragment { fragment ->
            val composePref = fragment.findPreference<ComposePreference>("satellite_apps_list")
            assertThat(fragment.viewModel.satelliteAppItems.value).isEmpty()
        }
    }

    @Test
    fun footer_whenLteSupported_showsLteText() {
        setLteNtnSupported(true)

        val scenario = launchFragment()
        scenario.onFragment { fragment -> fragment.viewModel.refresh() }
        waitForAsync()

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
        scenario.onFragment { fragment -> fragment.viewModel.refresh() }
        waitForAsync()

        scenario.onFragment { fragment ->
            val footer = fragment.findPreference<FooterPreference>("footer")
            assertThat(footer?.title)
                .isEqualTo(context.getString(R.string.landing_page_footer_text_nbiot))
        }
    }

    @Test
    fun satelliteIconDrawable_hasCorrectIntrinsicSize() {
        composeTestRule.runOnUiThread {
            val drawable = SatelliteIconDrawable(context)
            val density = context.resources.displayMetrics.density

            // Expected: 73dp x 32dp
            val expectedWidth = (SatelliteIconDrawable.INTRINSIC_WIDTH_DP * density).toInt()
            val expectedHeight = (SatelliteIconDrawable.INTRINSIC_HEIGHT_DP * density).toInt()

            assertEquals(expectedWidth, drawable.intrinsicWidth)
            assertEquals(expectedHeight, drawable.intrinsicHeight)
        }
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
            // Access private field _bannerState using reflection
            val bannerStateFlow =
                getField<MutableStateFlow<SatelliteBannerState>>(viewModel, "_bannerState")

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
            verify(mockMetricsFeatureProvider)
                .visible(
                    eq(fragment.requireContext()),
                    any(), // attribution
                    eq(SettingsEnums.SATELLITE_LANDING_PAGE),
                    eq(0), // latency
                )
        }
    }

    @Test
    fun onLteNtnSupportChanged_whenFalse_logsNbiotVariant() {
        setLteNtnSupported(false)
        val scenario = launchFragment()

        scenario.onFragment { fragment -> fragment.viewModel.refresh() }
        waitForAsync()

        verify(mockMetricsFeatureProvider)
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

        scenario.onFragment { fragment ->
            // Reset the metric log flag to ensure refresh() triggers the log
            setField(fragment, "isVariantMetricLogged", false)
            fragment.viewModel.refresh()
        }
        waitForAsync()

        verify(mockMetricsFeatureProvider)
            .action(any(), eq(SettingsEnums.ACTION_SATELLITE_LANDING_PAGE_VARIANT), eq(2))
    }

    @Test
    fun onLteNtnSupportChanged_logsVariantOnlyOnce() {
        setLteNtnSupported(false)
        val scenario = launchFragment()
        scenario.onFragment { fragment ->
            val viewModel = fragment.viewModel
            val isLteBasedNtnSupportedFlow =
                getField<MutableStateFlow<Boolean>>(viewModel, "_isLteBasedNtnSupported")

            // Trigger a state change to LTE variant
            isLteBasedNtnSupportedFlow.value = false
            isLteBasedNtnSupportedFlow.value = true
        }
        waitForAsync()

        verify(mockMetricsFeatureProvider, times(1))
            .action(any(), eq(SettingsEnums.ACTION_SATELLITE_LANDING_PAGE_VARIANT), eq(1))
        verify(mockMetricsFeatureProvider, never())
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

        verify(mockMetricsFeatureProvider)
            .action(any(), eq(SettingsEnums.ACTION_SATELLITE_DEMO_CLICK))
    }

    @Test
    fun onAppClick_logsActionWithPackageName() {
        setLteNtnSupported(true)
        satelliteStatusFlow.value = SatelliteStatus.AVAILABLE
        val messagingPackage = "com.google.android.apps.messaging"
        `when`(appsRepository.getAppsPackagesForLteLandingPage(anyBoolean()))
            .thenReturn(listOf(messagingPackage))
        setupPackageManagerForApp(messagingPackage, "Messages", Intent("action"))
        val scenario = launchFragment()
        waitForAsync()

        composeTestRule.onAllNodesWithText("Messages").onFirst().performClick()
        waitForAsync()

        verify(mockMetricsFeatureProvider)
            .action(any(), eq(SettingsEnums.ACTION_SATELLITE_APP_CLICK), eq(messagingPackage))
    }

    private fun waitForAsync() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        composeTestRule.waitForIdle()
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
        // Mock SatelliteUtils to return the desired support status
        whenever(SatelliteUtils.isLteBasedNtnSupported(anyInt())).thenReturn(isSupported)
        whenever(SatelliteUtils.isLteBasedNtnSupported(any(), anyInt())).thenReturn(isSupported)
    }

    private fun Preference.inflateViewHolder(): PreferenceViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(layoutResource, null)
        val constructor = PreferenceViewHolder::class.java.getDeclaredConstructor(View::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(view)
    }

    private fun <T> getField(target: Any, fieldName: String): T {
        val field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(target) as T
    }

    private fun setField(target: Any, fieldName: String, value: Any?) {
        val field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}
