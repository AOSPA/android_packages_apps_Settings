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
import androidx.test.core.app.ApplicationProvider
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.preferencesapi.ApiOperationContext
import com.android.settingslib.metadata.preferencesapi.ApiPreference
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.extractSafety
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.ApiPreconditions
import com.android.settingslib.metadata.preferencesapi.preconditions.EnterpriseRestriction
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.preconditions.InvalidPreference
import com.android.settingslib.metadata.preferencesapi.preconditions.RegionalRestriction
import com.android.settingslib.metadata.preferencesapi.types.FiniteOptionsType
import com.android.settingslib.metadata.preferencesapi.types.clearCache
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

/**
 * Generic exception thrown if the preconditions of a get/set operation made through the ApiTester
 * failed.
 */
open class FailedPreconditionException : Exception()

/**
 * This exception is thrown if the preconditions for a get/set operation made through the ApiTester
 * failed due to a missing permission restriction, or if the context the ApiTester instance is
 * running in lacks the required permissions to execute the get/set operation.
 */
class MissingPermissionException(val reason: String) : FailedPreconditionException()

/**
 * This exception is thrown if the preconditions for a get/set operation made through the ApiTester
 * failed due to an enterprise restriction.
 */
class EnterpriseRestrictionException(val reason: String) : FailedPreconditionException()

/**
 * This exception is thrown if the preconditions for a get/set operation made through the ApiTester
 * failed due to a hardware unsupported restriction.
 */
class HardwareUnsupportedException(val reason: String) : FailedPreconditionException()

/**
 * This exception is thrown if the preconditions for a get/set operation made through the ApiTester
 * failed due to an invalid state of another preference.
 */
class InvalidPreferenceException(val reason: String) : FailedPreconditionException()

/**
 * This exception is thrown if the preconditions for a get/set operation made through the ApiTester
 * failed due to a regional restriction.
 */
class RegionalRestrictionException(val reason: String) : FailedPreconditionException()

/**
 * This exception is thrown if there is a set operation about to be performed on a preference with
 * no setter.
 */
class CannotSetException : Exception()

/**
 * This exception is thrown if there is a value in the get/set operation that does not respect the
 * type of the associated preference.
 */
class InvalidValueException(val reason: String) : Exception()

/**
 * This class contains information regarding a screen, retrieved from the underlying infrastructure.
 */
class ScreenInfo

/**
 * Helper class for testing an api screen, including potential preferences it includes.
 *
 * @param instance - The api screen instance we are performing a test on.
 * @param context - The application context.
 */
class ApiTester(
    private val instance: PreferencesApiScreen,
    private val context: Context = ApplicationProvider.getApplicationContext(),
) {

    init {
        clearCache()
    }

    private val possibleParameters by lazy {
        runBlocking { instance.getAllPossibleParameters(context).toList() }
    }

    private fun <V> getPreference(key: String) =
        instance.preferences.first { it.key == key } as ApiPreference<*, V>

    private fun checkGetPermissions(preference: ApiPreference<*, *>) {
        val pid = android.os.Process.myPid()
        val uid = android.os.Process.myUid()

        val screenPermissions = instance.screenPermissions
        if (screenPermissions != null && !screenPermissions.check(context, pid, uid)) {
            throw MissingPermissionException(screenPermissions.toString())
        }

        val prefPermissions = preference.permissions
        if (prefPermissions != null && !prefPermissions.check(context, pid, uid)) {
            throw MissingPermissionException(prefPermissions.toString())
        }

        val prefGetPermissions = preference.get.permissions
        if (prefGetPermissions != null && !prefGetPermissions.check(context, pid, uid)) {
            throw MissingPermissionException(prefGetPermissions.toString())
        }
    }

    private fun checkSetPermissions(preference: ApiPreference<*, *>) {
        val pid = android.os.Process.myPid()
        val uid = android.os.Process.myUid()

        val screenPermissions = instance.screenPermissions
        if (screenPermissions != null && !screenPermissions.check(context, pid, uid)) {
            throw MissingPermissionException(screenPermissions.toString())
        }

        val prefPermissions = preference.permissions
        if (prefPermissions != null && !prefPermissions.check(context, pid, uid)) {
            throw MissingPermissionException(prefPermissions.toString())
        }

        val prefSetPermissions = preference.set?.permissions
        if (prefSetPermissions != null && !prefSetPermissions.check(context, pid, uid)) {
            throw MissingPermissionException(prefSetPermissions.toString())
        }
    }

    private fun checkGetPreconditions(
        preference: ApiPreference<*, *>,
        operationContext: ApiOperationContext,
    ) {
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

    private fun <V : Any> checkSetPreconditions(
        preference: ApiPreference<*, V>,
        value: V,
        operationContext: ApiOperationContext,
    ) {
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
            throw EnterpriseRestrictionException(result.getReason(context))
        } else if (result is HardwareUnsupported) {
            throw HardwareUnsupportedException(result.getReason(context))
        } else if (result is InvalidPreference) {
            throw InvalidPreferenceException(result.getReason(context))
        } else if (result is RegionalRestriction) {
            throw RegionalRestrictionException(result.getReason(context))
        }
        throw FailedPreconditionException()
    }

    private suspend fun <V : Any> checkPotentialFiniteValue(
        preference: ApiPreference<*, V>,
        value: V,
    ) {
        if (preference.type is FiniteOptionsType<*, *>) {
            if (!getPreferenceOptions<V>(preference.key).map { it.first }.contains(value))
                throw InvalidValueException(
                    "Value $value should be among the allowed " + "finite values for this type."
                )
        }
    }

    /**
     * Initializes the screen with the given [parameters].
     *
     * @param parameters The parameters to initialize the screen with.
     * @throws IllegalStateException if the screen does not have a parameters schema.
     * @throws IllegalArgumentException if the provided parameters are not among the possible ones
     *   for this screen.
     */
    fun initializeScreenParameters(parameters: Parameters) {
        val schema =
            instance.parametersSchema
                ?: throw IllegalStateException(
                    "Attempting to initialize parameters on screen without a parameters schema"
                )
        val validatedKeyParameter = schema.prepare(parameters.values)
        if (!possibleParameters.contains(validatedKeyParameter))
            throw IllegalArgumentException(
                "Received parameters are not among the possible ones for this screen"
            )
        instance.initializeParameters(validatedKeyParameter)
    }

    /**
     * Helper method that returns the screen information extracted from the underlying
     * infrastructure.
     */
    fun getScreen(): ScreenInfo? = if (instance.isFlagEnabled(context)) ScreenInfo() else null

    /**
     * Helper method that executes a get operation over a specific api preference inside the current
     * instance screen.
     *
     * @param key The key of the preference the tester is executing the get operation on.
     * @param parameters The optional new parameters to be assigned to the api screen this
     *   preference is part of, if the screen is parameterized
     */
    fun <V : Any> get(key: String, parameters: Parameters? = null): V {
        val preference = getPreference<V>(key)
        if (parameters != null) initializeScreenParameters(parameters)
        val keyParameters = preference.getScreenParameters.invoke() ?: ValidatedKeyParameters.EMPTY
        val operationContext = ApiOperationContext(context = context, parameters = keyParameters)

        checkGetPermissions(preference)
        checkGetPreconditions(preference, operationContext)

        return runBlocking {
            val result = preference.get.execute(operationContext)
            // TODO(b/470284879) add this functionality in the getter
            checkPotentialFiniteValue(preference, result)
            return@runBlocking result
        }
    }

    /**
     * Helper method that executes a set operation over a specific api preference inside the current
     * instance screen.
     *
     * @param key The key of the preference the tester is executing the set operation on
     * @param value The new value to be assigned to the preference
     * @param parameters The optional new parameters to be assigned to the api screen this
     *   preference is part of, if the screen is parameterized
     */
    fun <V : Any> set(key: String, value: V, parameters: Parameters? = null) {
        val preference = getPreference<V>(key)
        val setConfig = preference.set ?: throw CannotSetException()
        if (parameters != null) initializeScreenParameters(parameters)
        val keyParameters = preference.getScreenParameters.invoke() ?: ValidatedKeyParameters.EMPTY
        val operationContext = ApiOperationContext(context = context, parameters = keyParameters)

        checkSetPermissions(preference)
        checkSetPreconditions(preference, value, operationContext)
        runBlocking {
            // TODO(b/470285824) add this functionality in the setter
            checkPotentialFiniteValue(preference, value)
            setConfig.execute.invoke(operationContext, value)
        }
    }

    /**
     * Helper method that returns the launch intent if the screen permissions and preconditions
     * pass.
     */
    fun getLaunchIntent(): Intent {
        val operationContext =
            ApiOperationContext(
                context = context,
                parameters = instance.keyParameters ?: ValidatedKeyParameters.EMPTY,
            )
        val screenPermissions = runBlocking { instance.screenPermissions }
        if (screenPermissions != null) {
            val pid = android.os.Process.myPid()
            val uid = android.os.Process.myUid()
            if (!screenPermissions.check(context, pid, uid)) {
                throw MissingPermissionException(screenPermissions.toString())
            }
        }
        val screenPrecondition = runBlocking {
            instance.screenPreconditions?.check(operationContext) ?: Allowed
        }
        dealWithPreconditionResult(screenPrecondition)
        return instance.getLaunchIntent(context, null)
            ?: throw Exception("Intent should not be null.")
    }

    /**
     * Helper method to get all the options of a preference which has the FiniteOptionsType type.
     */
    suspend fun <V : Any> getPreferenceOptions(key: String): List<Pair<V, String>> {
        val preference = getPreference<FiniteOptionsType<*, V>>(key)
        val type = preference.type
        if (type is FiniteOptionsType<*, *>) {
            val enforcedType = type as FiniteOptionsType<*, V>
            return runBlocking {
                enforcedType.getOptions(context).map {
                    extractSafety(it.first) as V to extractSafety(it.second) as String
                }
            }
        } else
            throw Exception(
                "Attempting to get all preference options on a " +
                    "preference with infinite options"
            )
    }

    /** Helper method that returns the tags associated with the screen. */
    fun getScreenTags(): Set<String> = instance.tags(context).toSet()

    /** Helper method that returns the tags associated with a preference. */
    fun getPreferenceTags(key: String): Set<String> = getPreference<Any>(key).tags(context).toSet()

    /** Get the screen extras associated with this parameterized screen. */
    fun getLaunchScreenExtras() = instance.launchScreenExtra

    /** Get all the parameter options for a specific parameter name. */
    fun getParameterOptions(parameterName: String): List<String> =
        possibleParameters.flatMap { validatedKeyParameters ->
            validatedKeyParameters.values.filter { it.key == parameterName }.values
        }
}

/** Helper class to wrap parameters of an api screen. */
class Parameters(vararg pairs: Pair<String, String>) {
    val values: Map<String, String> = pairs.toMap()
}
