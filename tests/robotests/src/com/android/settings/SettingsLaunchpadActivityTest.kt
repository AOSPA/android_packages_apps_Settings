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

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.fragment.app.Fragment
import androidx.test.core.app.ApplicationProvider
import com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY
import com.android.settings.SettingsLaunchpadActivityTest.Companion.preconditionsAreMet
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.spa.SpaActivity
import com.android.settings.testutils.shadow.ShadowActivityEmbeddingUtils
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.EXTRA_BINDING_SCREEN_KEY
import com.android.settingslib.metadata.FixedArrayMap
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.PreferenceScreenMetadata.Companion.EXTRA_LAUNCH_SCREEN
import com.android.settingslib.metadata.PreferenceScreenMetadata.Companion.EXTRA_SCREEN_ARGS
import com.android.settingslib.metadata.PreferenceScreenMetadata.Companion.EXTRA_SCREEN_KEY
import com.android.settingslib.metadata.PreferenceScreenMetadataFactory
import com.android.settingslib.metadata.PreferenceScreenMetadataParameterizedFactory
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.types.AnyString
import com.android.settingslib.metadata.preferencesapi.types.GeneratedParameterType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import com.android.settingslib.spa.framework.util.KEY_DESTINATION
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import com.android.settingslib.metadata.preferencesapi.safe
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowActivityEmbeddingUtils::class])
class SettingsLaunchpadActivityTest {

    companion object {
        const val TEST_SCREEN_KEY = "test_screen_key"
        const val API_SCREEN_KEY = "api_screen_key"
        const val API_SCREEN_WITH_PERMISSIONS_KEY = "api_screen_with_permissions_key"
        const val DYNAMIC_SPA_SCREEN_KEY = "dynamic_spa_screen_key"
        const val STATIC_SPA_SCREEN_KEY = "static_spa_screen_key"
        const val SPA_ROUTE_PREFIX = "spa_route_prefix"

        const val TEST_PERMISSION = "com.android.settings.TEST_PERMISSION"
        const val OTHER_PACKAGE = "com.other.package"

        var screenEnabled = true
        var preconditionsAreMet = true

        val currentGrantedPermissions = mutableSetOf<String>()
    }

    private lateinit var context: Context
    private lateinit var fakeFactory: FakeParameterizedFactory
    private lateinit var fakeApiFactory: PreferenceScreenMetadataFactory
    private lateinit var fakeApiWithPermissionsFactory: PreferenceScreenMetadataFactory
    private lateinit var fakeDynamicSpaApiFactory: PreferenceScreenMetadataParameterizedFactory
    private lateinit var fakeStaticSpaApiFactory: PreferenceScreenMetadataFactory

    // Dummy class for testing fragment launching
    class TestFragment : Fragment()

    class SpySettingsLaunchpadActivity : SettingsLaunchpadActivity() {
        var launchedFromPackageToReturn: String? = null
        var launchedFromUidToReturn: Int = Process.INVALID_UID

        override fun getLaunchedFromPackage(): String? = launchedFromPackageToReturn

        override fun getLaunchedFromUid(): Int = launchedFromUidToReturn

        override fun validatePermission(permission: String, uid: Int): Int {
            return if (currentGrantedPermissions.contains(permission)) {
                PackageManager.PERMISSION_GRANTED
            } else {
                PackageManager.PERMISSION_DENIED
            }
        }

        override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
            return validatePermission(permission, uid)
        }
    }

    /**
     * A fake [PreferencesApiScreen] for testing the category mapping logic. It contains screen
     * preconditions which by default are met, but they can be set through [preconditionsAreMet]
     * variable in tests.
     */
    class FakeApiScreen :
        PreferencesApiScreen(
            key = API_SCREEN_KEY,
            topLevelSettingsCategory = Category.APPS,
            fragment = TestFragment::class,
            purpose = 0,
        ) {
        init {
            flag { screenEnabled }
            preconditions("Test preconditions") {
                if (preconditionsAreMet) {
                    Allowed
                } else {
                    Custom("Test preconditions not met", stability = PreconditionStability.UNSTABLE)
                }
            }
        }
    }

    class FakeApiScreenWithPermissions :
        PreferencesApiScreen(
            key = API_SCREEN_WITH_PERMISSIONS_KEY,
            topLevelSettingsCategory = Category.APPS,
            fragment = TestFragment::class,
            purpose = 0,
        ) {
        init {
            permissions(TEST_PERMISSION)
        }

        override fun fragmentClass(): Class<out Fragment> = TestFragment::class.java
    }

    class FakeDynamicSpaScreen :
        PreferencesApiScreen(
            key = DYNAMIC_SPA_SCREEN_KEY,
            topLevelSettingsCategory = Category.APPS,
            purpose = 0, // Use constructor for dynamic SPA screens
        ) {
        init {
            parameters {
                parameter(
                    "package",
                    0,
                    true,
                    GeneratedParameterType(0) {
                        listOf(GeneratedValue("value".safe(), "type_description".safe()))
                    },
                )
                prepareSpaRoute { params -> "$SPA_ROUTE_PREFIX/${params["package"]}" }
            }
            preconditions("Test preconditions") {
                if (preconditionsAreMet) {
                    Allowed
                } else {
                    Custom("Test preconditions not met", stability = PreconditionStability.UNSTABLE)
                }
            }
        }
    }

    class FakeStaticSpaScreen :
        PreferencesApiScreen(
            key = STATIC_SPA_SCREEN_KEY,
            topLevelSettingsCategory = Category.APPS,
            spaRoutePrefix = SPA_ROUTE_PREFIX,
            purpose = 0,
        ) {
        init {
            preconditions("Test preconditions") {
                if (preconditionsAreMet) Allowed else Custom("Test preconditions not met", stability = PreconditionStability.UNSTABLE)
            }
        }
    }

    class TestDynamicSpaScreenFactory :
        PreferenceScreenMetadataParameterizedFactory, PreferenceScreenMixin {
        override val key: String
            get() = DYNAMIC_SPA_SCREEN_KEY

        override fun create(context: Context, args: Bundle): PreferenceScreenMetadata {
            val screen = FakeDynamicSpaScreen()
            if (args.containsKey("package")) {
                val keyParameters =
                    screen.parametersSchema!!.prepare("package" to args.getString("package")!!)
                screen.initializeParameters(keyParameters)
            }
            return screen
        }

        override fun createWithKeyParameters(
            context: Context,
            keyParameters: ValidatedKeyParameters,
        ): PreferenceScreenMetadata {
            val screen = FakeDynamicSpaScreen()
            screen.initializeParameters(keyParameters)
            return screen
        }

        // Add stubs for PreferenceScreenMixin
        override val title: Int
            get() = 0

        override val highlightMenuKey: Int
            get() = 0

        override fun getMetricsCategory(): Int = 0

        override fun fragmentClass(): Class<out Fragment>? = null

        override val purpose: Int
            get() = 0

        override fun getPreferenceHierarchy(
            context: Context,
            coroutineScope: kotlinx.coroutines.CoroutineScope,
        ): com.android.settingslib.metadata.PreferenceHierarchy =
            throw NotImplementedError("Factory only")

        override fun parameters(context: Context): Flow<Bundle> =
            throw NotImplementedError("Factory only")

        override fun keyParameters(context: Context): Flow<ValidatedKeyParameters> =
            throw NotImplementedError("Factory only")

        override val parametersSchema: KeyParametersSchema
            get() = FakeDynamicSpaScreen().parametersSchema!!
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
        clearAllStartedActivities()
        currentGrantedPermissions.clear()

        // screen is enabled by default
        screenEnabled = true

        // default value for preconditions variable
        preconditionsAreMet = true

        fakeFactory = FakeParameterizedFactory()
        fakeApiFactory = PreferenceScreenMetadataFactory { FakeApiScreen() }
        fakeApiWithPermissionsFactory = PreferenceScreenMetadataFactory {
            FakeApiScreenWithPermissions()
        }
        fakeDynamicSpaApiFactory = TestDynamicSpaScreenFactory()
        fakeStaticSpaApiFactory = PreferenceScreenMetadataFactory { FakeStaticSpaScreen() }

        PreferenceScreenRegistry.preferenceScreenMetadataFactories =
            FixedArrayMap(5) {
                it.put(API_SCREEN_KEY, fakeApiFactory)
                it.put(API_SCREEN_WITH_PERMISSIONS_KEY, fakeApiWithPermissionsFactory)
                it.put(DYNAMIC_SPA_SCREEN_KEY, fakeDynamicSpaApiFactory)
                it.put(STATIC_SPA_SCREEN_KEY, fakeStaticSpaApiFactory)
                it.put(TEST_SCREEN_KEY, fakeFactory)
            }
    }

    @After
    fun cleanUp() {
        ShadowActivityEmbeddingUtils.reset()
        clearAllStartedActivities()
    }

    private fun clearAllStartedActivities() {
        // Clear from application shadow
        val app = ApplicationProvider.getApplicationContext<Application>()
        while (shadowOf(app).nextStartedActivity != null) {
        }
    }

    private fun setupActivity(intent: Intent): ActivityController<SpySettingsLaunchpadActivity> {
        val controller = buildActivity(intent)
        val activity = controller.get()
        // Default to self-launch for successful tests unless overridden
        activity.launchedFromPackageToReturn = context.packageName
        activity.launchedFromUidToReturn = Process.myUid()
        currentGrantedPermissions.add(Manifest.permission.READ_SYSTEM_PREFERENCES)
        currentGrantedPermissions.add(Manifest.permission.EXECUTE_APP_FUNCTIONS)

        return controller
    }

    private fun buildActivity(intent: Intent): ActivityController<SpySettingsLaunchpadActivity> {
        return Robolectric.buildActivity(SpySettingsLaunchpadActivity::class.java, intent)
    }

    private fun getNextStartedActivity(activity: SpySettingsLaunchpadActivity): Intent? {
        // Activity shadow has priority for started activity
        return shadowOf(activity).nextStartedActivity
            ?: shadowOf(ApplicationProvider.getApplicationContext<Application>())
                .nextStartedActivity
    }

    @Test
    fun onCreate_noIdentitySharing_shouldFinish() {
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
            }

        val controller = setupActivity(intent)
        val activity = controller.get()
        activity.launchedFromPackageToReturn = null
        currentGrantedPermissions.clear()

        controller.create()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNull()
    }

    @Test
    fun onCreate_noPermission_shouldFinish() {
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
            }

        val controller = setupActivity(intent)
        val activity = controller.get()
        activity.launchedFromPackageToReturn = OTHER_PACKAGE
        activity.launchedFromUidToReturn = 12345
        currentGrantedPermissions.clear()

        controller.create()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNull()
    }

    @Test
    fun onCreate_isSameApp_shouldSucceed() {
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
            }

        val controller = setupActivity(intent)
        val activity = controller.create().get()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNotNull()
    }

    @Test
    fun launch_screenSpecificPermission_missingPermission_shouldFinish() {
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, API_SCREEN_WITH_PERMISSIONS_KEY)
            }

        val controller = setupActivity(intent)
        val activity = controller.get()
        activity.launchedFromPackageToReturn = OTHER_PACKAGE
        activity.launchedFromUidToReturn = 20000
        // Grant base permission but NOT screen permission
        currentGrantedPermissions.clear()
        currentGrantedPermissions.add(Manifest.permission.WRITE_SYSTEM_PREFERENCES)

        controller.create()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNull()
    }

    @Test
    fun launch_screenSpecificPermission_hasPermission_shouldSucceed() {
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, API_SCREEN_WITH_PERMISSIONS_KEY)
            }

        val controller = setupActivity(intent)
        val activity = controller.get()
        activity.launchedFromPackageToReturn = OTHER_PACKAGE
        activity.launchedFromUidToReturn = 30000
        currentGrantedPermissions.clear()
        currentGrantedPermissions.add(Manifest.permission.READ_SYSTEM_PREFERENCES)
        currentGrantedPermissions.add(TEST_PERMISSION)

        controller.create()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNotNull()
    }

    @Test
    fun launch_screenSpecificPermission_isTrustedCaller_shouldSucceed() {
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, API_SCREEN_WITH_PERMISSIONS_KEY)
            }

        // Trusted caller (EXECUTE_APP_FUNCTIONS) bypasses screen permission
        val controller = setupActivity(intent)
        val activity = controller.create().get()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNotNull()
    }

    @Test
    fun launch_screenSpecificPermission_notTrustedCaller_shouldFail() {
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, API_SCREEN_WITH_PERMISSIONS_KEY)
            }

        // Caller with READ_SYSTEM_PREFERENCES is no longer trusted for screen bypass
        val controller = buildActivity(intent)
        val activity = controller.get()
        activity.launchedFromPackageToReturn = OTHER_PACKAGE
        activity.launchedFromUidToReturn = 40000
        currentGrantedPermissions.clear()
        currentGrantedPermissions.add(Manifest.permission.READ_SYSTEM_PREFERENCES)

        controller.create()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNull()
    }

    @Test
    fun onCreate_missingScreenKey_shouldFinish() {
        val intent = Intent(context, SettingsLaunchpadActivity::class.java)
        val controller = setupActivity(intent)
        val activity = controller.create().get()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNull()
    }

    @Test
    fun onCreate_screenNotFound_shouldFinish() {
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, "non_existent_key")
            }
        val controller = setupActivity(intent)
        val activity = controller.create().get()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNull()
    }

    @Test
    fun onCreate_screenIsDisabled_shouldFinish() {
        screenEnabled = false
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, API_SCREEN_KEY)
            }
        val controller = setupActivity(intent)
        val activity = controller.create().get()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNull()
    }

    @Test
    fun onCreate_preconditionsNotMet_shouldFinish() {
        preconditionsAreMet = false
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, API_SCREEN_KEY)
            }
        val controller = setupActivity(intent)
        val activity = controller.create().get()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNull()
    }

    @Test
    fun launch_intentFromSearch_shouldLaunchSubSettingsWithCorrectKeyToHighlight() {
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(false)
        val highlightKeyValue = "preference_to_highlight"
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_FRAGMENT_ARG_KEY, "CS:$TEST_SCREEN_KEY/$highlightKeyValue")
            }

        val controller = buildActivity(intent)
        val activity = controller.get()
        activity.launchedFromPackageToReturn = context.packageName
        activity.launchedFromUidToReturn = Process.myUid()
        currentGrantedPermissions.add(Manifest.permission.READ_SYSTEM_PREFERENCES)

        controller.create()

        val nextIntent = getNextStartedActivity(activity)
        assertThat(nextIntent).isNotNull()
        assertThat(nextIntent!!.component?.className).isEqualTo(SubSettings::class.java.name)
        val fragmentArgs = nextIntent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
        assertThat(fragmentArgs?.getString(EXTRA_FRAGMENT_ARG_KEY)).isEqualTo(highlightKeyValue)
    }

    @Test
    fun launch_onePane_shouldLaunchSubSettingsDirectly() {
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(false)
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
            }

        val controller = setupActivity(intent)
        val activity = controller.get()
        controller.create()

        val nextActivity = getNextStartedActivity(activity)
        assertThat(nextActivity).isNotNull()
        assertThat(nextActivity!!.component?.className).isEqualTo(SubSettings::class.java.name)
        assertThat(nextActivity.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
            .isEqualTo(TestFragment::class.java.name)
    }

    @Test
    fun launch_withSpaRoute_shouldLaunchSpaActivityWithDynamicRoute() {
        val packageName = "com.example.app"
        val screenArgs = Bundle().apply { putString("package", packageName) }
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(false)
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, DYNAMIC_SPA_SCREEN_KEY)
                putExtra(EXTRA_SCREEN_ARGS, screenArgs)
            }

        val controller = setupActivity(intent)
        val activity = controller.get()
        controller.create()

        val nextActivity = getNextStartedActivity(activity)
        assertThat(nextActivity).isNotNull()
        assertThat(nextActivity!!.component?.className).isEqualTo(SpaActivity::class.java.name)
        assertThat(nextActivity.getStringExtra(KEY_DESTINATION))
            .isEqualTo("$SPA_ROUTE_PREFIX/$packageName")
    }

    @Test
    fun launch_withSpaRoutePrefix_twoPane_shouldLaunchTrampolineActivity() {
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(true)
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, STATIC_SPA_SCREEN_KEY)
            }

        val controller = setupActivity(intent)
        val activity = controller.get()
        controller.create()

        val nextActivity = getNextStartedActivity(activity)
        assertThat(nextActivity).isNotNull()
        assertThat(nextActivity!!.action)
            .isEqualTo(Settings.ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY)
    }

    @Test
    fun launch_twoPane_shouldLaunchTrampoline() {
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(true)
        ShadowActivityEmbeddingUtils.setIsAlreadyEmbedded(false)
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
            }

        val controller = setupActivity(intent)
        val activity = controller.get()
        controller.create()

        val nextActivity = getNextStartedActivity(activity)
        assertThat(nextActivity).isNotNull()
        assertThat(nextActivity!!.action)
            .isEqualTo(Settings.ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY)
    }

    @Test
    fun launch_withArgs_shouldPassArgsToFragment() {
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(false)
        val screenArgs = Bundle().apply { putString("test_arg_key", "test_arg_value") }
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
                putExtra(EXTRA_SCREEN_ARGS, screenArgs)
            }

        val controller = setupActivity(intent)
        val activity = controller.get()
        controller.create()

        val nextActivity = getNextStartedActivity(activity)
        assertThat(nextActivity).isNotNull()
        assertThat(fakeFactory.getParameter("test_arg_key")).isEqualTo("test_arg_value")
        val fragmentArgs =
            nextActivity!!.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
        assertThat(fragmentArgs?.getString(EXTRA_BINDING_SCREEN_KEY)).isEqualTo(TEST_SCREEN_KEY)
    }

    @Test
    fun launch_withHighlightKey_shouldPassKeyToFragment() {
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(false)
        val highlightKeyValue = "preference_to_highlight"
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
                putExtra(SettingsLaunchpadActivity.EXTRA_HIGHLIGHT_KEY, highlightKeyValue)
            }

        val controller = setupActivity(intent)
        val activity = controller.get()
        controller.create()

        val nextActivity = getNextStartedActivity(activity)
        assertThat(nextActivity).isNotNull()
        val fragmentArgs =
            nextActivity!!.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
        assertThat(fragmentArgs!!.getString(EXTRA_FRAGMENT_ARG_KEY))
            .isEqualTo(highlightKeyValue)
    }

    @Test
    fun onCreate_fragmentClassNameIsNull_shouldFinish() {
        fakeFactory.fragmentClassToReturn = null
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
            }
        val controller = setupActivity(intent)
        val activity = controller.get()
        controller.create()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNull()
    }

    @Test
    fun launch_twoPane_withPreferenceScreenMixin_usesHighlightMenuKey() {
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(true)
        ShadowActivityEmbeddingUtils.setIsAlreadyEmbedded(false)
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
            }

        val controller = setupActivity(intent)
        val activity = controller.get()
        controller.create()

        val nextActivity = getNextStartedActivity(activity)
        assertThat(nextActivity).isNotNull()
        val expectedMenuKey = context.getString(R.string.menu_key_display)
        assertThat(
            nextActivity!!.getStringExtra(
                Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_HIGHLIGHT_MENU_KEY
            )
        )
            .isEqualTo(expectedMenuKey)
    }

    @Test
    fun launch_twoPane_withApiScreen_usesCategoryMap() {
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(true)
        ShadowActivityEmbeddingUtils.setIsAlreadyEmbedded(false)
        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, API_SCREEN_KEY)
            }

        val controller = setupActivity(intent)
        val activity = controller.get()
        controller.create()

        val nextActivity = getNextStartedActivity(activity)
        assertThat(nextActivity).isNotNull()
        val expectedMenuKey = context.getString(R.string.menu_key_apps)
        assertThat(
            nextActivity!!.getStringExtra(
                Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_HIGHLIGHT_MENU_KEY
            )
        )
            .isEqualTo(expectedMenuKey)
    }

    @Test
    fun launch_withLaunchScreenExtra_shouldPassExtrasToFragment() {
        ShadowActivityEmbeddingUtils.setIsEmbeddingActivityEnabled(false)
        val extraKey1 = "package"
        val extraValue1 = "com.google.android.gm"
        val extraKey2 = "id"
        val extraValue2 = 2737
        val launchScreenExtra =
            Bundle().apply {
                putString(extraKey1, extraValue1)
                putInt(extraKey2, extraValue2)
            }

        fakeFactory.launchIntentToReturn =
            Intent(PreferenceScreenMetadata.LAUNCH_SETTINGS_PAGES_ACTION).apply {
                putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
                putExtra(EXTRA_LAUNCH_SCREEN, launchScreenExtra)
            }

        val intent =
            Intent(context, SettingsLaunchpadActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
            }

        val controller = setupActivity(intent)
        val activity = controller.get()
        controller.create()

        val nextActivity = getNextStartedActivity(activity)
        assertThat(nextActivity).isNotNull()
        val fragmentArgs =
            nextActivity!!.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
        assertThat(fragmentArgs!!.getString(extraKey1)).isEqualTo(extraValue1)
        assertThat(fragmentArgs.getInt(extraKey2)).isEqualTo(extraValue2)
    }

    @Test
    fun onCreate_screenIsSensitive_shouldBlockLaunch() {
        fakeFactory.sensitivityLevelToReturn = SensitivityLevel.DO_NOT_EXPOSE

        val intent = Intent(context, SettingsLaunchpadActivity::class.java).apply {
            putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
        }

        val controller = setupActivity(intent)
        val activity = controller.get()
        controller.create()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNull()
    }

    @Test
    fun onCreate_screenIsUiOnly_shouldBlockLaunch() {
        fakeFactory.tagsToReturn = arrayOf(UI_ONLY_PREFERENCE)
        fakeFactory.sensitivityLevelToReturn = SensitivityLevel.NO_SENSITIVITY

        val intent = Intent(context, SettingsLaunchpadActivity::class.java).apply {
            putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
        }

        val controller = setupActivity(intent)
        val activity = controller.get()
        controller.create()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNull()
    }

    @Test
    fun onCreate_isNotAvailable_shouldBlockLaunch() {
        fakeFactory.isAvailableToReturn = false

        val intent = Intent(context, SettingsLaunchpadActivity::class.java).apply {
            putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
        }

        val controller = setupActivity(intent)
        val activity = controller.get()
        controller.create()

        assertThat(activity.isFinishing).isTrue()
        assertThat(getNextStartedActivity(activity)).isNull()
    }

    @Test
    fun onCreate_allChecksPass_shouldAllowLaunch() {
        fakeFactory.sensitivityLevelToReturn = SensitivityLevel.NO_SENSITIVITY
        fakeFactory.tagsToReturn = emptyArray()
        fakeFactory.isAvailableToReturn = true

        val intent = Intent(context, SettingsLaunchpadActivity::class.java).apply {
            putExtra(EXTRA_SCREEN_KEY, TEST_SCREEN_KEY)
        }

        val controller = setupActivity(intent)
        val activity = controller.get()
        controller.create()

        val nextActivity = getNextStartedActivity(activity)
        assertThat(nextActivity).isNotNull()
        assertThat(nextActivity!!.component?.className).isEqualTo(SubSettings::class.java.name)
    }

    class FakeParameterizedFactory :
        PreferenceScreenMetadataParameterizedFactory,
        PreferenceScreenMixin,
        PreferenceAvailabilityProvider {
        var fragmentClassToReturn: Class<out Fragment>? = TestFragment::class.java
        var launchIntentToReturn: Intent? = null
        var sensitivityLevelToReturn: Int = SensitivityLevel.NO_SENSITIVITY
        var tagsToReturn: Array<String> = emptyArray()
        var isAvailableToReturn: Boolean = true

        private var receivedBundle: Bundle? = null
        private var receivedKeyParameters: ValidatedKeyParameters? = null

        override fun create(context: Context, args: Bundle): PreferenceScreenMetadata {
            receivedBundle = args
            return this
        }

        override fun acceptEmptyArguments(): Boolean = true

        override val availabilityDescription: String
            get() = ""

        override fun isAvailable(context: Context): Boolean = isAvailableToReturn

        override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

        override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
            launchIntentToReturn

        override val key: String
            get() = TEST_SCREEN_KEY

        override val title: Int
            get() = 0

        override val highlightMenuKey: Int
            get() = R.string.menu_key_display

        override fun getMetricsCategory(): Int = 0

        override fun fragmentClass(): Class<out Fragment>? = fragmentClassToReturn

        override val sensitivityLevel: Int
            get() = sensitivityLevelToReturn

        override fun tags(context: Context): Array<String> = tagsToReturn

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
            get() = KeyParametersSchema {
                parameter("test_arg_key", "The test argument", type = AnyString)
            }

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
