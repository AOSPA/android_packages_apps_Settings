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
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.fragment.app.Fragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R;
import com.android.settings.flags.Flags
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.CannotSetException
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settings.testutils2.InvalidValueException
import com.android.settings.testutils2.MissingPermissionException
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.AnyString
import com.android.settingslib.metadata.preferencesapi.types.GeneratedType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import com.google.common.truth.Truth
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApiTesterTest {

    class TestScreen : PreferencesApiScreen(
        key = "A",
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = Fragment::class,
        purpose = 0
    ) {
        init {
            flag {
                Flags.catalystMigration26q2()
            }
            preference(
                key = "preference_which_has_value_hello_and_no_setter",
                purpose = 0,
                type = AnyString
            ) {
                get {
                    execute {
                        "Hello"
                    }
                }
            }
            preference(
                key = "preference_which_fails_permissions_in_get",
                purpose = 0,
                type = AnyString
            ) {
                get {
                    permissions(listOf(INTERACT_ACROSS_PROFILES))
                    execute {
                        "Hello"
                    }
                }
            }
            preference(
                key = "preference_which_fails_permissions_in_set",
                purpose = 0,
                type = AnyString
            ) {
                get {
                    execute {
                        "Hey"
                    }
                }
                set {
                    permissions(listOf(INTERACT_ACROSS_PROFILES))
                    execute { }
                }
            }
            preference(
                key = "preference_which_requires_interact_across_profiles",
                purpose = 0,
                type = AnyString
            ) {
                permissions(listOf(INTERACT_ACROSS_PROFILES))
                get {
                    execute {
                        "Hello"
                    }
                }
                set {
                    execute { }
                }
            }
            preference(
                key = "preference_with_fails_precondition_with_hardware",
                purpose = 0,
                type = AnyString
            ) {
                preconditions(0) {
                    HardwareUnsupported(R.string.hardware_unsupported_exception)
                }
                get {
                    execute {
                        "Hello"
                    }
                }
                set {
                    execute {}
                }
            }
            preference(
                key = "preference_with_fails_precondition_with_hardware_in_get",
                purpose = 0,
                type = AnyString
            ) {
                get {
                    preconditions(0) {
                        HardwareUnsupported(R.string.hardware_unsupported_exception)
                    }
                    execute {
                        "Hello"
                    }
                }
                set {
                    execute { }
                }
            }
            preference(
                key = "preference_with_fails_precondition_with_hardware_in_set",
                purpose = 0,
                type = AnyString
            ) {
                get {
                    execute {
                        "Hello"
                    }
                }
                set {
                    preconditions(0) {
                        HardwareUnsupported(R.string.hardware_unsupported_exception)
                    }
                    execute { }
                }
            }
            preference(
                key = "preference_with_setter_and_getter_cant_set_a",
                purpose = 0,
                type = AnyString
            ) {
                var theValue = "Hello"
                get {
                    execute {
                        theValue
                    }
                }
                set {
                    valuePreconditions(0) { value ->
                        if (value == "a") Custom(R.string.custom_exception)
                        else Allowed
                    }
                    execute { value -> theValue = value }
                }
            }
            preference(
                key = "preference_with_generated_type",
                purpose = 0,
                type = GeneratedType<String>(R.string.generated_type_description) {
                    listOf(
                        GeneratedValue<String>("value1", "first"),
                        GeneratedValue<String>("value2", "second")
                    )
                }
            ) {
                var theValue = "value1"
                get {
                    execute {
                        theValue
                    }
                }
                set {
                    execute { value->
                        theValue = value
                    }
                }

            }

            preference(
                key = "preference_with_generated_type_gets_and_sets_invalid_value",
                purpose = 0,
                type = GeneratedType<String>(R.string.generated_type_description) {
                    listOf(
                        GeneratedValue<String>("value1", "first"),
                        GeneratedValue<String>("value2", "second")
                    )
                }
            ) {
                var theValue = "Hello"
                get {
                    execute {
                        "value3"
                    }
                }
                set {
                    execute { value ->
                        theValue = value
                    }
                }
            }
        }
    }

    class FailingPermissionScreen : PreferencesApiScreen(
        key = "B",
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = Fragment::class,
        purpose = 0
    ) {
        init {
            flag {
                Flags.catalystMigration26q2()
            }
            permissions(listOf(INTERACT_ACROSS_PROFILES))
            preference(
                key = "main_pref",
                purpose = 0,
                type = AnyString
            ) {
            get {
                execute {
                    "Hello"
                }
            }
            set {
                execute {}
            }
        }
        }
    }

    class FailingPreconditionScreen : PreferencesApiScreen(
        key = "C",
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = Fragment::class,
        purpose = 0
    ) {
        init {
            flag {
                Flags.catalystMigration26q2()
            }
            preconditions(0){
                HardwareUnsupported(R.string.hardware_unsupported_exception)
            }
            preference(
                key = "main_pref",
                purpose = 0,
                type = AnyString
            ) {
                get {
                    execute {
                        "Hello"
                    }
                }
                set {
                    execute {}
                }
            }
        }
    }

    val tester = ApiTester(TestScreen())
    val testerFailingPermissions = ApiTester(FailingPermissionScreen())
    val testerFailingPreconditions = ApiTester(FailingPreconditionScreen())

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
        assertFailsWith<MissingPermissionException> { testerFailingPermissions.get<String>("main_pref") }
    }

    @Test
    fun set_failingScreenPermissions_throwsError() {
        assertFailsWith<MissingPermissionException> { testerFailingPermissions.set<String>("main_pref", "") }
    }

    @Test
    fun get_failingScreenPreconditions_throwsError() {
        assertFailsWith<HardwareUnsupportedException> { testerFailingPreconditions.get<String>("main_pref") }
    }

    @Test
    fun set_failingScreenPreconditions_throwsError() {
        assertFailsWith<HardwareUnsupportedException> { testerFailingPreconditions.set<String>("main_pref", "") }
    }

    @Test
    fun get_failingGetPermissions_throwsError() {
        assertFailsWith<MissingPermissionException> { tester.get<String>("preference_which_fails_permissions_in_get") }
    }

    @Test
    fun get_failingSetPermissions_throwsError() {
        assertFailsWith<MissingPermissionException> { tester.set<String>("preference_which_fails_permissions_in_set", "") }
    }

    @Test
    fun get_returnsCorrectValue() {
        Truth.assertThat(tester.get<String>("preference_which_has_value_hello_and_no_setter")).isEqualTo("Hello")
    }
    @Test
    fun set_noSetter_throwsException() {
        assertFailsWith<CannotSetException> { tester.set("preference_which_has_value_hello_and_no_setter", "v") }
    }

    @Test
    fun get_missingPermission_throwsException() {
        assertFailsWith<MissingPermissionException> { tester.get<String>("preference_which_requires_interact_across_profiles") }
    }

    @Test
    fun set_missingPermission_throwsException() {
        assertFailsWith<MissingPermissionException> { tester.set("preference_which_requires_interact_across_profiles", "v") }
    }

    @Test
    fun set_missingHardware_throwsException() {
        assertFailsWith<HardwareUnsupportedException> { tester.set("preference_with_fails_precondition_with_hardware", "v") }
    }

    @Test
    fun set_setsValue() {
        tester.set("preference_with_setter_and_getter_cant_set_a", "abc123")
        Truth.assertThat(tester.get<String>("preference_with_setter_and_getter_cant_set_a")).isEqualTo("abc123")
    }

    @Test
    fun get_missingHardwareFromSetPrecondition_throwsException() {
        assertFailsWith<HardwareUnsupportedException> { tester.get<String>("preference_with_fails_precondition_with_hardware_in_get") }
    }

    @Test
    fun set_missingHardwareFromSetPrecondition_throwsException() {
        assertFailsWith<HardwareUnsupportedException> { tester.set("preference_with_fails_precondition_with_hardware_in_set", "v") }
    }

    @Test
    fun set_failsValuePreconditions_throwsException() {
        assertFailsWith<FailedPreconditionException> { tester.set("preference_with_setter_and_getter_cant_set_a", "a") }
    }

    @Test
    fun launchIntent_onScreenPermissionFailure_fails() {
        assertFailsWith<MissingPermissionException> {
            testerFailingPermissions.getLaunchIntent()
        }
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
    fun getPreferenceOptions_generatedType_areCorrect() {
        Truth.assertThat(
            tester.getPreferenceOptions<String>("preference_with_generated_type")
        ).containsExactly(
            ("value1" to "first"),
            ("value2" to "second")
        )
    }

    @Test
    fun set_onGeneratedType_isCorrect() {
        tester.set("preference_with_generated_type", "value1")
        Truth.assertThat(
            tester.get<String>("preference_with_generated_type")
        ).isEqualTo("value1")
    }

    @Test
    fun getPreferenceOptions_onInfiniteType_throwsException(){
        assertFailsWith<Exception> {
            tester.getPreferenceOptions<String>("preference_which_has_value_hello_and_no_setter")
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
            tester.set<String>("preference_with_generated_type_gets_and_sets_invalid_value", "value4")
        }
    }

}