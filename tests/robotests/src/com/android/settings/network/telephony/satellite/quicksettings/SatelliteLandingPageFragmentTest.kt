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
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.PersistableBundle
import android.provider.Settings
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.satellite.SatelliteManager
import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.IllustrationPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.shadows.ShadowNetworkInfo
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
    private lateinit var shadowApplication: ShadowApplication
    private lateinit var shadowSatelliteManager: ShadowSatelliteManager

    @Mock private lateinit var subInfo: SubscriptionInfo
    private val SUB_ID = 1

    private companion object {
        private const val KEY_ILLUSTRATION = "illustration"
        private const val KEY_TRY_A_DEMO_BUTTON = "try_a_demo_button"
        private const val KEY_FOOTER = "footer"
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext<Application>()
        context.setTheme(R.style.Theme_Settings)
        shadowSatelliteManager =
            Shadow.extract(context.getSystemService(SatelliteManager::class.java))
        ShadowSubscriptionManager.setActiveDataSubscriptionId(SUB_ID)
        `when`(subInfo.subscriptionId).thenReturn(SUB_ID)
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        shadowOf(subscriptionManager).setActiveSubscriptionInfoList(listOf(subInfo))
        shadowApplication = shadowOf(context)
        val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!
        shadowOf(carrierConfigManager).setConfigForSubId(SUB_ID, PersistableBundle())
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

        val startedIntent = shadowApplication.nextStartedActivity
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

        val startedIntent = shadowApplication.nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.action).isEqualTo(Settings.ACTION_SATELLITE_SETTING)
        assertThat(startedIntent.hasExtra("sub_id")).isTrue()
        assertThat(startedIntent.getBooleanExtra(":settings:show_fragment_as_subsetting", false))
            .isTrue()
    }

    private fun launchFragment(): FragmentScenario<SatelliteLandingPageFragment> {
        return launchFragmentInContainer<SatelliteLandingPageFragment>(
            themeResId = R.style.Theme_Settings
        )
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

    /**
     * Configures the test environment to simulate Wi-Fi connection status.
     *
     * @param isConnected `true` to simulate that Wi-Fi is connected, `false` otherwise.
     */
    private fun setWifiConnected(isConnected: Boolean) {
        val connectivityManager: ConnectivityManager =
            context.getSystemService(ConnectivityManager::class.java)!!
        val shadowConnectivityManager = shadowOf(connectivityManager)
        if (isConnected) {
            val networkInfo =
                ShadowNetworkInfo.newInstance(
                    NetworkInfo.DetailedState.CONNECTED,
                    ConnectivityManager.TYPE_WIFI,
                    0, /* subType */
                    true, /* isAvailable */
                    true, /* isConnected */
                )
            shadowConnectivityManager.setActiveNetworkInfo(networkInfo)
            val activeNetwork = connectivityManager.activeNetwork

            val capabilities = ShadowNetworkCapabilities.newInstance()
            shadowOf(capabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            shadowOf(capabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            shadowConnectivityManager.setNetworkCapabilities(activeNetwork!!, capabilities)
        } else {
            shadowConnectivityManager.setActiveNetworkInfo(null)
        }
    }
}
