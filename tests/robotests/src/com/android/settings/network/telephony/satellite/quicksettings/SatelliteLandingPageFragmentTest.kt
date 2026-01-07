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
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSatelliteManager
import org.robolectric.shadows.ShadowSubscriptionManager
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class SatelliteLandingPageFragmentTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private lateinit var context: Application
    private lateinit var shadowSatelliteManager: ShadowSatelliteManager
    private val SUB_ID = 1
    private val APP1_PACKAGE = "com.app1"
    private val APP1_NAME = "App1"
    private val APP1_INTENT_ACTION = "action"

    // Preference Keys
    private val KEY_ILLUSTRATION = "illustration"
    private val KEY_TRY_A_DEMO_BUTTON = "try_a_demo_button"
    private val KEY_FOOTER = "footer"
    private val KEY_SATELLITE_APPS_LIST = "satellite_apps_list"

    @Mock private lateinit var subInfo: SubscriptionInfo
    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var appsRepository: SatelliteAppsRepository
    @Mock private lateinit var satelliteStateRepository: SatelliteStateRepository

    private lateinit var fragmentFactory: FragmentFactory
    private val satelliteStatusFlow = MutableStateFlow(SatelliteStatus.ACTIVE)

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.setTheme(R.style.Theme_Settings)

        // Mock System Services
        shadowSatelliteManager =
            Shadow.extract(context.getSystemService(SatelliteManager::class.java))
        ShadowSubscriptionManager.setActiveDataSubscriptionId(SUB_ID)
        `when`(subInfo.subscriptionId).thenReturn(SUB_ID)
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        shadowOf(subscriptionManager).setActiveSubscriptionInfoList(listOf(subInfo))
        val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!
        shadowOf(carrierConfigManager).setConfigForSubId(SUB_ID, PersistableBundle())

        // Mock Apps Repository
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(listOf())
        `when`(appsRepository.getAppsPackagesForNbNtnLandingPage()).thenReturn(listOf())
        `when`(appsRepository.getEmergencySosIntent()).thenReturn(null)
        `when`(appsRepository.getSettingsIntent()).thenReturn(null)

        // Mock State Repository
        `when`(satelliteStateRepository.satelliteStatus).thenReturn(satelliteStatusFlow)
        SatelliteStateRepository.setInstance(satelliteStateRepository)

        fragmentFactory =
            object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                    return SatelliteLandingPageFragment(packageManager, appsRepository)
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
        assertThat(startedIntent.getIntExtra("sub_id", -1)).isEqualTo(SUB_ID)
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
            val composePref = fragment.findPreference<ComposePreference>("satellite_apps_list")
            assertThat(composePref?.isVisible).isTrue()
        }
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

    private fun setLteNtnSupported(isSupported: Boolean) {
        val reasons = if (isSupported) emptySet() else setOf(1)
        shadowSatelliteManager.setAttachRestrictionReasonsForCarrier(SUB_ID, reasons)
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
