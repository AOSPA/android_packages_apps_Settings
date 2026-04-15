/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.catalyst

import android.Manifest.permission.INTERACT_ACROSS_PROFILES
import android.Manifest.permission.INTERACT_ACROSS_USERS
import android.Manifest.permission.MANAGE_USERS
import android.app.Application
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.fragment.app.Fragment
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.CannotSetException
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settings.testutils2.InvalidValueException
import com.android.settings.testutils2.MissingPermissionException
import com.android.settings.testutils2.Parameters
import com.android.settings.testutils2.RegionalRestrictionException
import com.android.settingslib.datastore.or
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.preconditions.RegionalRestriction
import com.android.settingslib.metadata.preferencesapi.types.AnyString
import com.android.settingslib.metadata.preferencesapi.types.GeneratedParameterType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import com.google.common.truth.Truth
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import com.android.settingslib.metadata.preferencesapi.safe
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

@RunWith(AndroidJUnit4::class)
class ApiTesterTest {

    class TestScreen :
        PreferencesApiScreen(
            key = "A",
            topLevelSettingsCategory = Category.SYSTEM,
            fragment = Fragment::class,
            purpose = 0,
        ) {
        init {
            flag { Flags.catalystMigration26q2() }
            tags("a", "b")

            preference(key = "preference_with_tags", purpose = 0, type = AnyString) {
                tags("a", "b", "c")
                get { execute { "Hello" } }
            }

            preference(
                key =
                    "preference_which_requires_interact_across_users_and_interact_across_profiles",
                purpose = 0,
                type = AnyString,
            ) {
                permissions(INTERACT_ACROSS_PROFILES and INTERACT_ACROSS_USERS)
                get { execute { "Hello" } }
            }

            preference(
                key = "preference_which_requires_interact_across_users_or_interact_across_profiles",
                purpose = 0,
                type = AnyString,
            ) {
                permissions(INTERACT_ACROSS_PROFILES or INTERACT_ACROSS_USERS)
                get { execute { "Hello" } }
            }

            preference(
                key =
                    "preference_which_requires_interact_across_users_and_interact_across_profiles_or_manage_users",
                purpose = 0,
                type = AnyString,
            ) {
                permissions((INTERACT_ACROSS_PROFILES and INTERACT_ACROSS_USERS) or MANAGE_USERS)
                get { execute { "Hello" } }
            }

            preference(
                key = "preference_which_has_value_hello_and_no_setter",
                purpose = 0,
                type = AnyString,
            ) {
                get { execute { "Hello" } }
            }
            preference(
                key = "preference_which_fails_permissions_in_get",
                purpose = 0,
                type = AnyString,
            ) {
                get {
                    permissions(INTERACT_ACROSS_PROFILES)
                    execute { "Hello" }
                }
            }
            preference(
                key = "preference_which_fails_permissions_in_set",
                purpose = 0,
                type = AnyString,
            ) {
                get { execute { "Hey" } }
                set {
                    permissions(INTERACT_ACROSS_PROFILES)
                    execute {}
                }
            }
            preference(
                key = "preference_which_requires_interact_across_profiles",
                purpose = 0,
                type = AnyString,
            ) {
                permissions(INTERACT_ACROSS_PROFILES)
                get { execute { "Hello" } }
                set { execute {} }
            }
            preference(
                key = "preference_with_fails_precondition_with_hardware",
                purpose = 0,
                type = AnyString,
            ) {
                preconditions(0) { HardwareUnsupported(R.string.hardware_unsupported_exception) }
                get { execute { "Hello" } }
                set { execute {} }
            }
            preference(
                key = "preference_with_fails_precondition_with_hardware_in_get",
                purpose = 0,
                type = AnyString,
            ) {
                get {
                    preconditions(0) {
                        HardwareUnsupported(R.string.hardware_unsupported_exception)
                    }
                    execute { "Hello" }
                }
                set { execute {} }
            }
            preference(
                key = "preference_with_fails_precondition_with_hardware_in_set",
                purpose = 0,
                type = AnyString,
            ) {
                get { execute { "Hello" } }
                set {
                    preconditions(0) {
                        HardwareUnsupported(R.string.hardware_unsupported_exception)
                    }
                    execute {}
                }
            }
            preference(
                key = "preference_with_fails_precondition_with_regional_restriction",
                purpose = 0,
                type = AnyString,
            ) {
                preconditions(0) { RegionalRestriction(R.string.hardware_unsupported_exception) }
                get { execute { "Hello" } }
                set { execute {} }
            }
            preference(
                key = "preference_with_setter_and_getter_cant_set_a",
                purpose = 0,
                type = AnyString,
            ) {
                var theValue = "Hello"
                get { execute { theValue } }
                set {
                    valuePreconditions(0) { value ->
                        if (value == "a") Custom(R.string.custom_exception, stability = PreconditionStability.UNSTABLE) else Allowed
                    }
                    execute { value -> theValue = value }
                }
            }
            preference(
                key = "preference_with_generated_type",
                purpose = 0,
                type =
                    GeneratedType<String>(R.string.generated_type_description) {
                        listOf(
                            GeneratedValue<String>("value1".safe(), "first".safe()),
                            GeneratedValue<String>("value2".safe(), "second".safe()),
                        )
                    },
            ) {
                var theValue = "value1"
                get { execute { theValue } }
                set { execute { value -> theValue = value } }
            }

            preference(
                key = "preference_with_generated_type_gets_and_sets_invalid_value",
                purpose = 0,
                type =
                    GeneratedType<String>(R.string.generated_type_description) {
                        listOf(
                            GeneratedValue<String>("value1".safe(), "first".safe()),
                            GeneratedValue<String>("value2".safe(), "second".safe()),
                        )
                    },
            ) {
                var theValue = "Hello"
                get { execute { "value3" } }
                set { execute { value -> theValue = value } }
            }
        }
    }

    class FailingPermissionScreen :
        PreferencesApiScreen(
            key = "B",
            topLevelSettingsCategory = Category.SYSTEM,
            fragment = Fragment::class,
            purpose = 0,
        ) {
        init {
            flag { Flags.catalystMigration26q2() }
            permissions(INTERACT_ACROSS_PROFILES)
            preference(key = "main_pref", purpose = 0, type = AnyString) {
                get { execute { "Hello" } }
                set { execute {} }
            }
        }
    }

    class FailingPreconditionScreen :
        PreferencesApiScreen(
            key = "C",
            topLevelSettingsCategory = Category.SYSTEM,
            fragment = Fragment::class,
            purpose = 0,
        ) {
        init {
            flag { Flags.catalystMigration26q2() }
            preconditions(0) { HardwareUnsupported(R.string.hardware_unsupported_exception) }
            preference(key = "main_pref", purpose = 0, type = AnyString) {
                get { execute { "Hello" } }
                set { execute {} }
            }
        }
    }

    class ParameterizedScreen :
        PreferencesApiScreen(
            key = "D",
            topLevelSettingsCategory = Category.SYSTEM,
            fragment = Fragment::class,
            purpose = 0,
        ) {
        init {
            flag { Flags.catalystMigration26q2() }
            parameters {
                parameter(
                    "package",
                    R.string.parameter_purpose,
                    type =
                        GeneratedParameterType(R.string.parameter_type_description) {
                            listOf(
                                GeneratedValue("parameter1".safe(), "first parameter description".safe()),
                                GeneratedValue("parameter2".safe(), "second parameter description".safe()),
                            )
                        },
                )
                prepareScreenExtras { parameters, extras ->
                    extras.putString("pkg", parameters["package"])
                }
            }
            preference(
                key = "preference_with_parameter_precondition",
                purpose = 0,
                type = AnyString,
            ) {
                preconditions(R.string.parameterized_precondition) {
                    if (parameters["package"] == "parameter1") {
                        Allowed
                    } else Custom(R.string.parameter_must_be_parameter1, stability = PreconditionStability.UNSTABLE)
                }
                get { execute { "hello" } }
            }
            preference(
                key = "preference_of_parameterized_screen",
                purpose = 0,
                type = AnyString,
            ) {
                var storageForParameter1 = ""
                var storageForParameter2 = ""
                get {
                    execute {
                        if (parameters["package"] == "parameter1") storageForParameter1
                        else if (parameters["package"] == "parameter2") storageForParameter2
                        else "Invalid"
                    }
                }
                set {
                    execute { value ->
                        if (parameters["package"] == "parameter1") storageForParameter1 = value
                        else if (parameters["package"] == "parameter2") storageForParameter2 = value
                    }
                }
            }

            preference(
                key = "preference_of_parameterized_screen_with_missing_permission",
                purpose = 0,
                type = AnyString,
            ) {
                get {
                    permissions(INTERACT_ACROSS_PROFILES)
                    execute { parameters["package"]!! }
                }
                set {
                    permissions(INTERACT_ACROSS_PROFILES)
                    execute { value -> }
                }
            }

            preference(
                key = "get_preference_of_parameterized_screen",
                purpose = 0,
                type = AnyString,
            ) {
                get {
                    execute {
                        if (parameters["package"] == "parameter1") "parameter1_value"
                        else "parameter2_value"
                    }
                }
            }
        }
    }

    class FailingPreconditionParameterizedScreen :
        PreferencesApiScreen(
            key = "E",
            topLevelSettingsCategory = Category.SYSTEM,
            fragment = Fragment::class,
            purpose = 0,
        ) {
        var parameterizedScreenPreconditionSucceeds = true

        init {
            flag { Flags.catalystMigration26q2() }
            parameters {
                parameter(
                    "package",
                    R.string.parameter_purpose,
                    type =
                        GeneratedParameterType(R.string.parameter_type_description) {
                            listOf(
                                GeneratedValue("parameter1".safe(), "first parameter description".safe()),
                                GeneratedValue("parameter2".safe(), "second parameter description".safe()),
                            )
                        },
                )
            }
            preconditions(R.string.parameterized_precondition) {
                if (parameters["package"] == "parameter1") Allowed
                else HardwareUnsupported(R.string.hardware_unsupported_exception)
            }
            preference(key = "main_preference", purpose = 0, type = AnyString) {
                get { execute { "Hello" } }
            }
        }
    }

    class ScreenWithDeviceStateTag :
        PreferencesApiScreen(
            key = "F",
            topLevelSettingsCategory = Category.SYSTEM,
            fragment = Fragment::class,
            purpose = 0,
        ) {
        init {
            flag { Flags.catalystMigration26q2() }
            tags(PreferencesApiScreen.APP_FUNCTION_STORAGE)
            preference(key = "preference_with_device_state_tag", purpose = 0, type = AnyString) {
                tags(PreferencesApiScreen.APP_FUNCTION_BATTERY)
                get { execute { "Hello" } }
            }
        }
    }

    val context: Application = ApplicationProvider.getApplicationContext()
    val tester = ApiTester(TestScreen())
    val testerFailingPermissions = ApiTester(FailingPermissionScreen())
    val testerFailingPreconditions = ApiTester(FailingPreconditionScreen())
    val testerParameterized = ApiTester(ParameterizedScreen())
    val testerFailingPreconditionsParameterized =
        ApiTester(FailingPreconditionParameterizedScreen())
    val testerScreenWithDeviceStateTag = ApiTester(ScreenWithDeviceStateTag())

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        Truth.assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        Truth.assertThat(tester.getScreen()).isNull()
    }

    @Test
    fun get_failingScreenPermissions_throwsError() {
        assertFailsWith<MissingPermissionException> {
            testerFailingPermissions.get<String>("main_pref")
        }
    }

    @Test
    fun set_failingScreenPermissions_throwsError() {
        assertFailsWith<MissingPermissionException> {
            testerFailingPermissions.set<String>("main_pref", "")
        }
    }

    @Test
    fun get_failingScreenPreconditions_throwsError() {
        assertFailsWith<HardwareUnsupportedException> {
            testerFailingPreconditions.get<String>("main_pref")
        }
    }

    @Test
    fun set_failingScreenPreconditions_throwsError() {
        assertFailsWith<HardwareUnsupportedException> {
            testerFailingPreconditions.set<String>("main_pref", "")
        }
    }

    @Test
    fun get_failingGetPermissions_throwsError() {
        assertFailsWith<MissingPermissionException> {
            tester.get<String>("preference_which_fails_permissions_in_get")
        }
    }

    @Test
    fun get_failingSetPermissions_throwsError() {
        assertFailsWith<MissingPermissionException> {
            tester.set<String>("preference_which_fails_permissions_in_set", "")
        }
    }

    @Test
    fun get_returnsCorrectValue() {
        Truth.assertThat(tester.get<String>("preference_which_has_value_hello_and_no_setter"))
            .isEqualTo("Hello")
    }

    @Test
    fun set_noSetter_throwsException() {
        assertFailsWith<CannotSetException> {
            tester.set("preference_which_has_value_hello_and_no_setter", "v")
        }
    }

    @Test
    fun get_missingPermission_throwsException() {
        assertFailsWith<MissingPermissionException> {
            tester.get<String>("preference_which_requires_interact_across_profiles")
        }
    }

    @Test
    fun set_missingPermission_throwsException() {
        assertFailsWith<MissingPermissionException> {
            tester.set("preference_which_requires_interact_across_profiles", "v")
        }
    }

    @Test
    fun set_missingHardware_throwsException() {
        assertFailsWith<HardwareUnsupportedException> {
            tester.set("preference_with_fails_precondition_with_hardware", "v")
        }
    }

    @Test
    fun set_setsValue() {
        tester.set("preference_with_setter_and_getter_cant_set_a", "abc123")
        Truth.assertThat(tester.get<String>("preference_with_setter_and_getter_cant_set_a"))
            .isEqualTo("abc123")
    }

    @Test
    fun get_missingHardwareFromSetPrecondition_throwsException() {
        assertFailsWith<HardwareUnsupportedException> {
            tester.get<String>("preference_with_fails_precondition_with_hardware_in_get")
        }
    }

    @Test
    fun set_missingHardwareFromSetPrecondition_throwsException() {
        assertFailsWith<HardwareUnsupportedException> {
            tester.set("preference_with_fails_precondition_with_hardware_in_set", "v")
        }
    }

    @Test
    fun get_regionalRestriction_throwsException() {
        assertFailsWith<RegionalRestrictionException> {
            tester.get<String>("preference_with_fails_precondition_with_regional_restriction")
        }
    }

    @Test
    fun set_regionalRestriction_throwsException() {
        assertFailsWith<RegionalRestrictionException> {
            tester.set("preference_with_fails_precondition_with_regional_restriction", "v")
        }
    }

    @Test
    fun set_failsValuePreconditions_throwsException() {
        assertFailsWith<FailedPreconditionException> {
            tester.set("preference_with_setter_and_getter_cant_set_a", "a")
        }
    }

    @Test
    fun launchIntent_onScreenPermissionFailure_fails() {
        assertFailsWith<MissingPermissionException> { testerFailingPermissions.getLaunchIntent() }
    }

    @Test
    fun launchIntent_onScreenPreconditionsFailure_fails() {
        assertFailsWith<HardwareUnsupportedException> {
            testerFailingPreconditions.getLaunchIntent()
        }
    }

    @Test
    fun launchIntent_correctScreen_hasIntent() {
        Truth.assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun launchIntent_onFailingPreconditionParameterizedScreen_throwsException() {
        // Initialize screen with parameters that cause the screen precondition to fail.
        testerFailingPreconditionsParameterized.initializeScreenParameters(
            Parameters("package" to "parameter2")
        )

        // Verify that getLaunchIntent throws an exception due to the failed precondition.
        assertFailsWith<HardwareUnsupportedException> {
            testerFailingPreconditionsParameterized.getLaunchIntent()
        }
    }

    @Test
    fun launchIntent_onPassedPreconditionParameterizedScreen_isCorrect() {
        // Initialize screen with parameters that allow the screen precondition to pass.
        testerFailingPreconditionsParameterized.initializeScreenParameters(
            Parameters("package" to "parameter1")
        )

        // Verify that getLaunchIntent returns a non-null intent.
        Truth.assertThat(testerFailingPreconditionsParameterized.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getPreferenceOptions_generatedType_areCorrect() {
        runBlocking {
            Truth.assertThat(tester.getPreferenceOptions<String>("preference_with_generated_type"))
                .containsExactly(("value1" to "first"), ("value2" to "second"))
        }
    }

    @Test
    fun set_onGeneratedType_isCorrect() {
        tester.set("preference_with_generated_type", "value1")
        Truth.assertThat(tester.get<String>("preference_with_generated_type")).isEqualTo("value1")
    }

    @Test
    fun getPreferenceOptions_onInfiniteType_throwsException() {
        runBlocking {
            assertFailsWith<Exception> {
                tester.getPreferenceOptions<String>(
                    "preference_which_has_value_hello_and_no_setter"
                )
            }
        }
    }

    @Test
    fun get_onGeneratedValueWithInvalidValue_throwsException() {
        assertFailsWith<InvalidValueException> {
            tester.get<String>("preference_with_generated_type_gets_and_sets_invalid_value")
        }
    }

    @Test
    fun set_onGeneratedValueWithInvalidValue_throwsException() {
        assertFailsWith<InvalidValueException> {
            tester.set<String>(
                "preference_with_generated_type_gets_and_sets_invalid_value",
                "value4",
            )
        }
    }

    @Test
    fun get_onParameterizedPreferenceWithInvalidParameter_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            testerParameterized.get<String>(
                "preference_with_parameter_precondition",
                Parameters("package" to "parameterInvalid"),
            )
        }
    }

    @Test
    fun get_onParameterizedPreferenceWithWrongParameterPrecondition_throwsException() {
        assertFailsWith<FailedPreconditionException> {
            testerParameterized.get<String>(
                "preference_with_parameter_precondition",
                Parameters("package" to "parameter2"),
            )
        }
    }

    @Test
    fun get_onParameterizedPreferenceWithCorrectParameterPrecondition_isCorrect() {
        Truth.assertThat(
                testerParameterized.get<String>(
                    "preference_with_parameter_precondition",
                    Parameters("package" to "parameter1"),
                )
            )
            .isEqualTo("hello")
    }

    @Test
    fun get_onParameterizedPreferenceWithParameter1_isCorrect() {
        Truth.assertThat(
                testerParameterized.get<String>(
                    "get_preference_of_parameterized_screen",
                    Parameters("package" to "parameter1"),
                )
            )
            .isEqualTo("parameter1_value")
    }

    @Test
    fun get_onParameterizedPreferenceWithParameter2_isCorrect() {
        Truth.assertThat(
                testerParameterized.get<String>(
                    "get_preference_of_parameterized_screen",
                    Parameters("package" to "parameter2"),
                )
            )
            .isEqualTo("parameter2_value")
    }

    @Test
    fun set_onParameterizedPreferenceWithParameter1_isCorrect() {
        testerParameterized.set<String>(
            "preference_of_parameterized_screen",
            "parameter1_storage",
            Parameters("package" to "parameter1"),
        )
        Truth.assertThat(
                testerParameterized.get<String>(
                    "preference_of_parameterized_screen",
                    Parameters("package" to "parameter1"),
                )
            )
            .isEqualTo("parameter1_storage")
    }

    @Test
    fun set_onParameterizedPreferenceWithParameter2_isCorrect() {
        testerParameterized.set<String>(
            "preference_of_parameterized_screen",
            "parameter2_storage",
            Parameters("package" to "parameter2"),
        )
        Truth.assertThat(
                testerParameterized.get<String>(
                    "preference_of_parameterized_screen",
                    Parameters("package" to "parameter2"),
                )
            )
            .isEqualTo("parameter2_storage")
    }

    @Test
    fun get_onParameterizedPreferenceWithMissingPermission_throwsException() {
        assertFailsWith<MissingPermissionException> {
            testerParameterized.get<String>(
                "preference_of_parameterized_screen_with_missing_permission",
                Parameters("package" to "parameter2"),
            )
        }
    }

    @Test
    fun set_onParameterizedPreferenceWithMissingPermission_throwsException() {
        assertFailsWith<MissingPermissionException> {
            testerParameterized.set<String>(
                "preference_of_parameterized_screen_with_missing_permission",
                "hey",
                Parameters("package" to "parameter2"),
            )
        }
    }

    @Test
    fun get_withParametersOnNonParameterizedScreen_throwsException() {
        assertFailsWith<IllegalStateException> {
            tester.get<String>(
                "preference_which_has_value_hello_and_no_setter",
                Parameters("package" to "parameter2"),
            )
        }
    }

    @Test
    fun set_withParametersOnNonParameterizedScreen_throwsException() {
        assertFailsWith<IllegalStateException> {
            tester.set<String>(
                "preference_with_setter_and_getter_cant_set_a",
                "value",
                Parameters("package" to "parameter2"),
            )
        }
    }

    @Test
    fun get_onFailingPreconditionParameterizedScreen_throwsException() {
        assertFailsWith<HardwareUnsupportedException> {
            testerFailingPreconditionsParameterized.get<String>(
                "main_preference",
                Parameters("package" to "parameter2"),
            )
        }
    }

    @Test
    fun get_onPassedPreconditionParameterizedScreen_isCorrect() {
        Truth.assertThat(
                testerFailingPreconditionsParameterized.get<String>(
                    "main_preference",
                    Parameters("package" to "parameter1"),
                )
            )
            .isEqualTo("Hello")
    }

    @Test
    fun getLaunchScreenBundle_onParameterizedScreen_isCorrect() {
        testerParameterized.initializeScreenParameters(Parameters("package" to "parameter1"))
        val extras = testerParameterized.getLaunchScreenExtras()
        Truth.assertThat(extras.getString("pkg")).isEqualTo("parameter1")
        Truth.assertThat(extras.keySet().size).isEqualTo(1)
    }

    @Test
    fun getLaunchScreenBundle_onNonParameterizedScreen_isEmpty() {
        val extras = tester.getLaunchScreenExtras()
        Truth.assertThat(extras.keySet().size).isEqualTo(0)
    }

    @Test
    fun get_preferenceRequiresInteractAcrossUsersAndInteractAcrossProfiles_missingPermissions_throwsException() {
        assertFailsWith<MissingPermissionException> {
            tester.get<String>(
                "preference_which_requires_interact_across_users_and_interact_across_profiles"
            )
        }
    }

    @Test
    fun get_preferenceRequiresInteractAcrossUsersAndInteractAcrossProfiles_hasInteractAcrossUsers_throwsException() {
        shadowOf(context).grantPermissions(INTERACT_ACROSS_USERS)
        assertFailsWith<MissingPermissionException> {
            tester.get<String>(
                "preference_which_requires_interact_across_users_and_interact_across_profiles"
            )
        }
    }

    @Test
    fun get_preferenceRequiresInteractAcrossUsersAndInteractAcrossProfiles_hasInteractAcrossProfiles_throwsException() {
        shadowOf(context).grantPermissions(INTERACT_ACROSS_PROFILES)
        assertFailsWith<MissingPermissionException> {
            tester.get<String>(
                "preference_which_requires_interact_across_users_and_interact_across_profiles"
            )
        }
    }

    @Test
    fun get_preferenceRequiresInteractAcrossUsersAndInteractAcrossProfiles_hasBothPermissions_returnsValue() {
        shadowOf(context).grantPermissions(INTERACT_ACROSS_USERS, INTERACT_ACROSS_PROFILES)
        Truth.assertThat(
                tester.get<String>(
                    "preference_which_requires_interact_across_users_and_interact_across_profiles"
                )
            )
            .isEqualTo("Hello")
    }

    @Test
    fun get_preferenceRequiresInteractAcrossUsersOrInteractAcrossProfiles_missingPermissions_throwsException() {
        assertFailsWith<MissingPermissionException> {
            tester.get<String>(
                "preference_which_requires_interact_across_users_or_interact_across_profiles"
            )
        }
    }

    @Test
    fun get_preferenceRequiresInteractAcrossUsersOrInteractAcrossProfiles_hasInteractAcrossUsers_returnsValue() {
        shadowOf(context).grantPermissions(INTERACT_ACROSS_USERS)
        Truth.assertThat(
                tester.get<String>(
                    "preference_which_requires_interact_across_users_or_interact_across_profiles"
                )
            )
            .isEqualTo("Hello")
    }

    @Test
    fun get_preferenceRequiresInteractAcrossUsersOrInteractAcrossProfiles_hasInteractAcrossProfiles_returnsValue() {
        shadowOf(context).grantPermissions(INTERACT_ACROSS_PROFILES)
        Truth.assertThat(
                tester.get<String>(
                    "preference_which_requires_interact_across_users_or_interact_across_profiles"
                )
            )
            .isEqualTo("Hello")
    }

    @Test
    fun get_preferenceRequiresInteractAcrossUsersAndInteractAcrossProfilesOrManageUsers_missingPermissions_throwsException() {
        assertFailsWith<MissingPermissionException> {
            tester.get<String>(
                "preference_which_requires_interact_across_users_and_interact_across_profiles_or_manage_users"
            )
        }
    }

    @Test
    fun get_preferenceRequiresInteractAcrossUsersAndInteractAcrossProfilesOrManageUsers_hasInteractAcrossUsersAndInteractAcrossProfiles_returnsValue() {
        shadowOf(context).grantPermissions(INTERACT_ACROSS_USERS, INTERACT_ACROSS_PROFILES)
        Truth.assertThat(
                tester.get<String>(
                    "preference_which_requires_interact_across_users_and_interact_across_profiles_or_manage_users"
                )
            )
            .isEqualTo("Hello")
    }

    @Test
    fun get_preferenceRequiresInteractAcrossUsersAndInteractAcrossProfilesOrManageUsers_hasManageUsers_returnsValue() {
        shadowOf(context).grantPermissions(MANAGE_USERS)

        Truth.assertThat(
                tester.get<String>(
                    "preference_which_requires_interact_across_users_and_interact_across_profiles_or_manage_users"
                )
            )
            .isEqualTo("Hello")
    }

    @Test
    fun getParameterOptions_onParameterizedScreen_returnsCorrectValues() {
        Truth.assertThat(testerParameterized.getParameterOptions("package"))
            .containsExactly("parameter1", "parameter2")
    }

    @Test
    fun getScreenTags_returnsScreenTags() {
        Truth.assertThat(tester.getScreenTags())
            .containsExactly("a", "b", "api-first", PreferencesApiScreen.APP_FUNCTION_UNCATEGORIZED)
    }

    @Test
    fun getPreferenceTags_forPreferenceWithTags_returnsPreferenceTags() {
        Truth.assertThat(tester.getPreferenceTags("preference_with_tags"))
            .containsExactly("a", "b", "c", "api-first")
    }

    @Test
    fun getPreferenceTags_forPreferenceWithoutTags_returnsApiFirst() {
        Truth.assertThat(tester.getPreferenceTags("preference_which_has_value_hello_and_no_setter"))
            .containsExactly("api-first")
    }

    @Test
    fun getScreenTags_withDeviceStateTag_doesNotAddUncategorizedTag() {
        Truth.assertThat(testerScreenWithDeviceStateTag.getScreenTags())
            .containsExactly(PreferencesApiScreen.APP_FUNCTION_STORAGE, "api-first")
    }

    @Test
    fun getPreferenceTags_withDeviceStateTag_doesNotAddUncategorizedTag() {
        Truth.assertThat(
                testerScreenWithDeviceStateTag.getPreferenceTags("preference_with_device_state_tag")
            )
            .containsExactly(PreferencesApiScreen.APP_FUNCTION_BATTERY, "api-first")
    }
}
