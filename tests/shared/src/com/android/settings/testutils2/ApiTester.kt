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

package com.android.settings.testutils2

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.preferencesapi.ApiOperationContext
import com.android.settingslib.metadata.preferencesapi.ApiPreference
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.ApiPreconditions
import com.android.settingslib.metadata.preferencesapi.preconditions.EnterpriseRestriction
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.preconditions.InvalidPreference
import com.android.settingslib.metadata.preferencesapi.preconditions.MissingPermission
import kotlinx.coroutines.runBlocking

/**
 * Generic exception thrown if the preconditions of a get/set operation made through the ApiTester
 * failed.
 */
open class FailedPreconditionException : Exception()

/**
 * This exception is thrown if the preconditions for a get/set operation made through the ApiTester
 * failed due to a missing permission restriction, or if the context the ApiTester instance
 * is running in lacks the required permissions to execute the get/set operation.
 */
class MissingPermissionException(val reason: String) : FailedPreconditionException()

/**
 * This exception is thrown if the preconditions for a get/set operation made through the ApiTester
 * failed due to an enterprise restriction.
 */
class EnterpriseRestrictionException(val reason: String): FailedPreconditionException()

/**
 * This exception is thrown if the preconditions for a get/set operation made through the ApiTester
 * failed due to a hardware unsupported restriction.
 */
class HardwareUnsupportedException(val reason: String): FailedPreconditionException()

/**
 * This exception is thrown if the preconditions for a get/set operation made through the ApiTester
 * failed due to an invalid state of another preference.
 */
class InvalidPreferenceException(val reason: String) : FailedPreconditionException()

/**
 * This exception is thrown if there is a set operation about to be performed on a preference
 * with no setter.
 */
class CannotSetException : Exception()

/**
 * This class contains information regarding a screen, retrieved from the underlying infrastructure.
 */
class ScreenInfo

/**
 * Helper class for testing an api screen, including potential preferences it includes.
 *
 * @param instance - an api screen instance we are performing a test on.
 */
class ApiTester(private val instance: PreferencesApiScreen) {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun <V> getPreference(key: String) = instance.preferences.first { it.key == key } as ApiPreference<V>

    private fun checkGetPermissions(preference: ApiPreference<*>) {
        val prefPermissions = preference.permissions?.permissions ?: listOf()
        val prefGetPermissions = preference.get.permissions?.permissions ?: listOf()
        val screenPermissions = instance.screenPermissions?.permissions ?: listOf()

        val allPermissions = prefPermissions + prefGetPermissions + screenPermissions

        for (permission in allPermissions) {
            if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                throw MissingPermissionException(permission)
            }
        }
    }

    private fun checkSetPermissions(preference: ApiPreference<*>) {
        val screenPermissions = instance.screenPermissions?.permissions ?: listOf()
        val prefPermissions = preference.permissions?.permissions ?: listOf()
        val prefSetPermissions = preference.set?.permissions?.permissions ?: listOf()

        val allPermissions = prefPermissions + prefSetPermissions + screenPermissions

        for (permission in allPermissions) {
            if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                throw MissingPermissionException(permission)
            }
        }
    }

    private fun checkGetPreconditions(preference: ApiPreference<*>, operationContext: ApiOperationContext) {
        val screenPrecondition = runBlocking {
            preference.screenPreconditions?.check(operationContext) ?: Allowed
        }
        dealWithPreconditionResult(screenPrecondition)
        val commonPrecondition = runBlocking {
            preference.preconditions?.check(operationContext) ?: Allowed
        }
        dealWithPreconditionResult(commonPrecondition)
        val prefPrecondition = runBlocking {
            preference.get.preconditions?.check(operationContext) ?: Allowed
        }
        dealWithPreconditionResult(prefPrecondition)
    }

    private fun <V: Any> checkSetPreconditions(preference: ApiPreference<V>, value: V, operationContext: ApiOperationContext) {
        val screenPrecondition = runBlocking {
            preference.screenPreconditions?.check(operationContext) ?: Allowed
        }
        dealWithPreconditionResult(screenPrecondition)
        val commonPrecondition = runBlocking {
            preference.preconditions?.check(operationContext) ?: Allowed
        }
        dealWithPreconditionResult(commonPrecondition)
        val prefPrecondition = runBlocking {
            preference.set?.preconditions?.check(operationContext) ?: Allowed
        }
        dealWithPreconditionResult(prefPrecondition)
        val valuePrecondition = runBlocking {
            preference.set?.valuePreconditions?.check(operationContext, value) ?: Allowed
        }
        dealWithPreconditionResult(valuePrecondition)
    }

    private fun dealWithPreconditionResult(result: ApiPreconditions) {
        if (result is Allowed) {
            return
        } else if (result is EnterpriseRestriction) {
            throw EnterpriseRestrictionException(context.getString(result.reason))
        } else if (result is HardwareUnsupported) {
            throw HardwareUnsupportedException(context.getString(result.reason))
        } else if (result is InvalidPreference) {
            throw InvalidPreferenceException(context.getString(result.reason))
        } else if (result is MissingPermission) {
            throw MissingPermissionException(context.getString(result.reason))
        }
        throw FailedPreconditionException()
    }


    /**
     * Helper method that returns the screen information extracted from the underlying
     * infrastructure.
     */
    fun getScreen() : ScreenInfo? = if (instance.isFlagEnabled(context)) ScreenInfo() else null

    /**
     * Helper method that executes a get operation over a specific api preference inside the current
     * instance screen.
     *
     * @param key The key of the preference the tester is executing the get operation on.
     */
    fun <V : Any> get(key: String): V {
        val preference = getPreference<V>(key)
        val keyParameters = preference.screenParameters ?: ValidatedKeyParameters.EMPTY
        val operationContext = ApiOperationContext(context, keyParameters)

        checkGetPermissions(preference)
        checkGetPreconditions(preference, operationContext)

        return runBlocking {
            preference.get.execute(operationContext)
        }
    }

    /**
     * Helper method that executes a set operation over a specific api preference inside the current
     * instance screen.
     *
     * @param key The key of the preference the tester is executing the set operation on
     * @param value The new value to be assigned to the preference
     */
    fun <V : Any> set(key: String, value: V) {
        val preference = getPreference<V>(key)
        val setConfig = preference.set ?: throw CannotSetException()
        val keyParameters = preference.screenParameters ?: ValidatedKeyParameters.EMPTY
        val operationContext = ApiOperationContext(context, keyParameters)

        checkSetPermissions(preference)
        checkSetPreconditions(preference, value, operationContext)

        runBlocking {
            setConfig.execute.invoke(operationContext, value)
        }
    }

    /**
     * Helper method that returns the launch intent if the screen permissions and preconditions
     * pass.
     */
    fun getLaunchIntent() : Intent {
        val operationContext = ApiOperationContext(
            context,
            instance.keyParameters ?: ValidatedKeyParameters.EMPTY
        )
        val screenPermissions = runBlocking {
            instance.screenPermissions?.permissions?: listOf()
        }
        for (permission in screenPermissions) {
            if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                throw MissingPermissionException(permission)
            }
        }
        val screenPrecondition = runBlocking {
            instance.screenPreconditions?.check(operationContext) ?: Allowed
        }
        dealWithPreconditionResult(screenPrecondition)
        return instance.getLaunchIntent(context, null)
            ?: throw Exception("Intent should not be null.")
    }
}