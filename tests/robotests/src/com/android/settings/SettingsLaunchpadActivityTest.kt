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

package com.android.settings

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.launch
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.testutils.shadow.ShadowActivityEmbeddingUtils
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.FixedArrayMap
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.PreferenceScreenMetadata.Companion.EXTRA_LAUNCH_SCREEN
import com.android.settingslib.metadata.PreferenceScreenMetadataFactory
import com.android.settingslib.metadata.PreferenceScreenMetadataParameterizedFactory
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Disallowed
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowActivityEmbeddingUtils::class])
class SettingsLaunchpadActivityTest {

    companion object {
        const val TEST_SCREEN_KEY = "test_screen_key"
        const val API_SCREEN_KEY = "api_screen_key"

        var preconditionsAreMet = true
    }

    private lateinit var context: Context
    private lateinit var fakeFactory: FakeParameterizedFactory
    private lateinit var fakeApiFactory: PreferenceScreenMetadataFactory

    // Dummy class for testing fragment launching
    class TestFragment : Fragment()

    /**
     * A fake [PreferencesApiScreen] for testing the category mapping logic.
     * It contains screen preconditions which by default are met, but they can be set through
     * [preconditionsAreMet] variable in tests.
     */
    class FakePreferencesApiScreen :
        PreferencesApiScreen(
            key = API_SCREEN_KEY,
            topLevelSettingsCategory = Category.APPS,
            fragment = TestFragment::class,
            purpose = 0,
        ) {
        init {
            preconditions("Test preconditions") {
                if (preconditionsAreMet) {
                    Allowed
                } else {
                    Disallowed("Test preconditions not met")
                }
            }
        }
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()

        // default value for preconditions variable
        preconditionsAreMet = true

        fakeFactory = FakeParameterizedFactory()
        fakeApiFactory = PreferenceScreenMetadataFactory { FakePreferencesApiScreen() }

        PreferenceScreenRegistry.preferenceScreenMetadataFactories =
            FixedArrayMap(2) {
                it.put(API_SCREEN_KEY, fakeApiFactory)
                it.put(TEST_SCREEN_KEY, fakeFactory)
            }
    }

    @Test
    fun onCreate_missingScreenKey_shouldFinish() {
        val intent = Intent(context, SettingsLaunchpadActivity::class.java)
        val activity =
            Robolectric.buildActivity(SettingsLaunchpadActivity::class.java, intent).create().get()

        assertThat(activity.isFinishing).isTrue()
        assertThat(shadowOf(activity).nextStartedActivity).isNull()
    }

    @Test
    fun onCreate_screenNotFound_shouldFinish() {
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(SettingsLaunchpadActivity.EXTRA_SCREEN_KEY, "non_existent_key")
            }
        val activity =
            Robolectric.buildActivity(SettingsLaunchpadActivity::class.java, intent).create().get()

        assertThat(activity.isFinishing).isTrue()
        assertThat(shadowOf(activity).nextStartedActivity).isNull()
    }

    @Test
    fun onCreate_preconditionsNotMet_shouldFinish() {
        preconditionsAreMet = false

        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(SettingsLaunchpadActivity.EXTRA_SCREEN_KEY, API_SCREEN_KEY)
            }
        val activity =
            Robolectric.buildActivity(SettingsLaunchpadActivity::class.java, intent).create().get()

        assertThat(activity.isFinishing).isTrue()
        assertThat(shadowOf(activity).nextStartedActivity).isNull()
    }

    @Test
    fun launch_intentFromSearch_shouldLaunchSubSettingsWithCorrectKeyToHighlight() {
        // Set device is in one-pane mode
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(false)
        val highlightKeyValue = "preference_to_highlight"
        val intent =
            Intent(
                    ApplicationProvider.getApplicationContext(),
                    SettingsLaunchpadActivity::class.java,
                )
                .apply {
                    putExtra(EXTRA_FRAGMENT_ARG_KEY, "CS:$TEST_SCREEN_KEY/$highlightKeyValue")
                }

        ActivityScenario.launch<SettingsLaunchpadActivity>(intent).use { scenario ->
            // Verify the SettingsLaunchpadActivity is finished
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        }

        val nextIntent = shadowOf(context as ContextWrapper).nextStartedActivity
        assertThat(nextIntent).isNotNull()
        val expectedComponent = ComponentName(context, SubSettings::class.java)
        assertThat(nextIntent.component).isEqualTo(expectedComponent)
        assertThat(nextIntent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
            .isEqualTo(TestFragment::class.java.name)
        val fragmentArgs = nextIntent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
        assertThat(fragmentArgs).isNotNull()
        assertThat(fragmentArgs?.getString(EXTRA_FRAGMENT_ARG_KEY)).isEqualTo(highlightKeyValue)
    }

    @Test
    fun launch_onePane_shouldLaunchSubSettingsDirectly() {
        // Arrange: Device is in one-pane mode
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(false)

        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(SettingsLaunchpadActivity.EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
            }

        // Act
        val activity =
            Robolectric.buildActivity(SettingsLaunchpadActivity::class.java, intent).create().get()

        // Assert
        val nextActivity = shadowOf(activity).nextStartedActivity
        assertThat(nextActivity).isNotNull()
        assertThat(nextActivity.component?.className).isEqualTo(SubSettings::class.java.name)
        assertThat(nextActivity.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
            .isEqualTo(TestFragment::class.java.name)
        assertThat(activity.isFinishing).isTrue()
    }

    @Test
    fun launch_twoPane_shouldLaunchTrampoline() {
        // Arrange: Device is in two-pane mode
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(true)
        ShadowActivityEmbeddingUtils.setIsAlreadyEmbedded(false)

        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(SettingsLaunchpadActivity.EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
            }

        // Act
        val activity =
            Robolectric.buildActivity(SettingsLaunchpadActivity::class.java, intent).create().get()

        // Assert
        val nextActivity = shadowOf(activity).nextStartedActivity
        assertThat(nextActivity).isNotNull()
        assertThat(nextActivity.action).isEqualTo(Settings.ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY)
        assertThat(activity.isFinishing).isTrue()
    }

    @Test
    fun launch_withArgs_shouldPassArgsToFragment() {
        // Arrange
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(
            false
        ) // Test with one-pane for simplicity

        val fakeFactory = FakeParameterizedFactory()
        PreferenceScreenRegistry.preferenceScreenMetadataFactories =
            FixedArrayMap(1) { it.put(TEST_SCREEN_KEY, fakeFactory) }

        val screenArgs = Bundle().apply { putString("test_arg_key", "test_arg_value") }

        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(SettingsLaunchpadActivity.EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
                putExtra(SettingsLaunchpadActivity.EXTRA_SCREEN_ARGS, screenArgs)
            }

        // Act
        val activity =
            Robolectric.buildActivity(SettingsLaunchpadActivity::class.java, intent).create().get()

        // Assert
        val nextActivity = shadowOf(activity).nextStartedActivity
        assertThat(nextActivity).isNotNull()

        assertThat(fakeFactory.hasParameters()).isTrue()
        assertThat(fakeFactory.getParameter("test_arg_key")).isEqualTo("test_arg_value")

        val fragmentArgs =
            nextActivity.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
        assertThat(fragmentArgs).isNotNull()
        assertThat(nextActivity.component?.className).isEqualTo(SubSettings::class.java.name)
        assertThat(nextActivity.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
            .isEqualTo(TestFragment::class.java.name)
        assertThat(activity.isFinishing).isTrue()
    }

    @Test
    fun launch_withHighlightKey_shouldPassKeyToFragment() {
        // Arrange
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(false) // One-pane is simplest
        val highlightKeyValue = "preference_to_highlight"

        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(SettingsLaunchpadActivity.EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
                putExtra(SettingsLaunchpadActivity.EXTRA_HIGHLIGHT_KEY, highlightKeyValue)
            }

        // Act
        val activity =
            Robolectric.buildActivity(SettingsLaunchpadActivity::class.java, intent).create().get()

        // Assert
        val nextActivity = shadowOf(activity).nextStartedActivity
        assertThat(nextActivity).isNotNull()

        val fragmentArgs =
            nextActivity.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
        assertThat(fragmentArgs).isNotNull()
        assertThat(fragmentArgs!!.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY))
            .isEqualTo(highlightKeyValue)
    }

    @Test
    fun onCreate_fragmentClassNameIsNull_shouldFinish() {
        // Arrange: Configure the fake to return a null fragment class
        fakeFactory.fragmentClassToReturn = null // You will need to add this field to your fake

        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(SettingsLaunchpadActivity.EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
            }

        // Act
        val activity =
            Robolectric.buildActivity(SettingsLaunchpadActivity::class.java, intent).create().get()

        // Assert
        assertThat(activity.isFinishing).isTrue()
        assertThat(shadowOf(activity).nextStartedActivity).isNull()
    }

    @Test
    fun launch_twoPane_withPreferenceScreenMixin_usesHighlightMenuKey() {
        // Arrange: Device is in two-pane mode
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(true)
        ShadowActivityEmbeddingUtils.setIsAlreadyEmbedded(false)

        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(SettingsLaunchpadActivity.EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
            }

        // Act
        val activity =
            Robolectric.buildActivity(SettingsLaunchpadActivity::class.java, intent).create().get()

        // Assert
        val nextActivity = shadowOf(activity).nextStartedActivity
        assertThat(nextActivity).isNotNull()
        assertThat(nextActivity.action).isEqualTo(Settings.ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY)

        val expectedMenuKey = context.getString(R.string.menu_key_display)
        assertThat(
                nextActivity.getStringExtra(
                    Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_HIGHLIGHT_MENU_KEY
                )
            )
            .isEqualTo(expectedMenuKey)

        assertThat(activity.isFinishing).isTrue()
    }

    @Test
    fun launch_twoPane_withApiScreen_usesCategoryMap() {
        // Arrange: Device is in two-pane mode
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(true)
        ShadowActivityEmbeddingUtils.setIsAlreadyEmbedded(false)

        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                // Use the key for our new PreferencesApiScreen fake
                putExtra(SettingsLaunchpadActivity.EXTRA_SCREEN_KEY, API_SCREEN_KEY)
            }

        // Act
        val activity =
            Robolectric.buildActivity(SettingsLaunchpadActivity::class.java, intent).create().get()

        // Assert
        val nextActivity = shadowOf(activity).nextStartedActivity
        assertThat(nextActivity).isNotNull()
        assertThat(nextActivity.action).isEqualTo(Settings.ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY)

        val expectedMenuKey = context.getString(R.string.menu_key_apps)
        assertThat(
                nextActivity.getStringExtra(
                    Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_HIGHLIGHT_MENU_KEY
                )
            )
            .isEqualTo(expectedMenuKey)

        assertThat(activity.isFinishing).isTrue()
    }

    @Test
    fun launch_withLaunchScreenExtra_shouldPassExtrasToFragment() {
        // Arrange: Use one-pane mode for direct verification of SubSettings launch
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(false)
        val extraKey1 = "package"
        val extraValue1 = "com.google.android.gm"
        val extraKey2 = "id"
        val extraValue2 = 2737
        val launchScreenExtra =
            Bundle(2).apply {
                putString(extraKey1, extraValue1)
                putInt(extraKey2, extraValue2)
            }

        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(SettingsLaunchpadActivity.EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
                putExtra(EXTRA_LAUNCH_SCREEN, launchScreenExtra)
            }

        // Act: Create the activity
        val activity =
            Robolectric.buildActivity(SettingsLaunchpadActivity::class.java, intent).create().get()

        // Assert: Verify that the next activity receives the extras in its fragment arguments
        val nextActivity = shadowOf(activity).nextStartedActivity
        assertThat(nextActivity).isNotNull()
        val fragmentArgs =
            nextActivity.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
        assertThat(fragmentArgs).isNotNull()
        assertThat(fragmentArgs!!.getString(extraKey1)).isEqualTo(extraValue1)
        assertThat(fragmentArgs.getInt(extraKey2)).isEqualTo(extraValue2)
        assertThat(activity.isFinishing).isTrue()
    }

    class FakeParameterizedFactory :
        PreferenceScreenMetadataParameterizedFactory, PreferenceScreenMixin {
        var fragmentClassToReturn: Class<out Fragment>? = TestFragment::class.java

        private var receivedBundle: Bundle? = null
        private var receivedKeyParameters: ValidatedKeyParameters? = null

        override fun create(context: Context, args: Bundle): PreferenceScreenMetadata {
            receivedBundle = args
            return this
        }

        override fun acceptEmptyArguments(): Boolean = true

        override val key: String
            get() = TEST_SCREEN_KEY

        override val title: Int
            get() = 0

        override val highlightMenuKey: Int
            get() = R.string.menu_key_display

        override fun getMetricsCategory(): Int = 0

        override fun fragmentClass(): Class<out Fragment>? = fragmentClassToReturn

        override val purpose: Int
            get() = 0

        override fun createWithKeyParameters(
            context: Context,
            keyParameters: ValidatedKeyParameters,
        ): PreferenceScreenMetadata {
            this.receivedKeyParameters = keyParameters
            return this
        }

        override fun getPreferenceHierarchy(
            context: Context,
            coroutineScope: kotlinx.coroutines.CoroutineScope,
        ): com.android.settingslib.metadata.PreferenceHierarchy {
            throw NotImplementedError("Not needed for this test")
        }

        override fun parameters(context: Context): Flow<Bundle> {
            throw NotImplementedError("Not needed for this test")
        }

        override fun keyParameters(context: Context): Flow<ValidatedKeyParameters> {
            throw NotImplementedError("Not needed for this test")
        }

        override val parametersSchema: KeyParametersSchema
            get() = KeyParametersSchema { parameter("test_arg_key", "The test argument") }

        fun hasParameters(): Boolean =
            if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                receivedKeyParameters != null
            } else {
                receivedBundle != null
            }

        fun getParameter(key: String): String? =
            if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                receivedKeyParameters?.get(key)
            } else {
                receivedBundle?.getString(key)
            }
    }
}
