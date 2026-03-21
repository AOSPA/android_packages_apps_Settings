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

package com.android.settings.applications.specialaccess.notificationaccess

import android.app.NotificationManager
import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS
import android.provider.Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME
import android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_ALERTING
import android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_CONVERSATIONS
import android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_ONGOING
import android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_SILENT
import com.android.settings.R
import com.android.settings.applications.InstalledPackageName
import com.android.settings.applications.getApplicationInfo
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.notification.NotificationBackend
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.types.AnyString
import com.android.settingslib.preference.SwitchPreferenceBinding
import com.android.settingslib.widget.MainSwitchPreferenceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** "Apps" -> "Special app access" -> "Notification read, reply & control" -> {app name} */
@ProvidePreferenceScreen(AppInfoNotificationAccessScreen.KEY, parameterized = true)
open class AppInfoNotificationAccessScreen
private constructor(
    val context: Context,
    @Deprecated(
        "This property will be removed once the catalyst framework stops passing the arguments as a bundle. Use the keyParameters instead."
    )
    final override val arguments: Bundle?,
    final override val keyParameters: ValidatedKeyParameters?,
) :
    PreferenceScreenMixin,
    PreferenceSummaryProvider,
    PreferenceTitleProvider,
    PreferenceAvailabilityProvider {

    private val packageName: String =
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            keyParameters!!.getRequired(KEY_SERVICE)!!.split("/", limit = 2)[0]
        } else {
            arguments!!.getString(KEY_APP_PACKAGE_NAME)!!
        }
    private val serviceName: String =
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            keyParameters!!.getRequired(KEY_SERVICE)!!.split("/", limit = 2)[1]
        } else {
            arguments!!.getString(KEY_SERVICE_NAME)!!
        }

    private val appInfo = context.getApplicationInfo(packageName)

    private val storage: KeyValueStore =
        NotificationAccessStorage(context, packageName, serviceName)

    @Deprecated(
        "This constructor will be removed once the catalyst framework stops passing the arguments as a bundle. Use the other constructor instead."
    )
    constructor(context: Context, args: Bundle) : this(context, args, null)

    constructor(
        context: Context,
        keyParameters: ValidatedKeyParameters,
    ) : this(context, null, keyParameters)

    override val key: String
        get() = KEY

    override val keyParametersSchema: KeyParametersSchema
        get() = parametersSchema

    //TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.device_state_app_info_notification_access_purpose


    override val screenTitle: Int
        get() = R.string.manage_notification_access_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getMetricsCategory() = SettingsEnums.NOTIFICATION_ACCESS_DETAIL

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun getTitle(context: Context): CharSequence? =
        appInfo?.loadLabel(context.packageManager)

    override fun getSummary(context: Context): CharSequence? =
        context.getString(
            when (storage.getBoolean(NotificationAccessApprovalPreference.KEY)) {
                true -> R.string.notification_listener_allowed
                else -> R.string.notification_listener_not_allowed
            }
        )

    override fun isFlagEnabled(context: Context) = false

    override val availabilityDescription = "The app must be enabled."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context) = appInfo != null

    override fun extras(context: Context): Bundle? =
        Bundle(1).apply { putString(KEY_EXTRA_PACKAGE_NAME, packageName) }

    override fun hasCompleteHierarchy() = false

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
            putExtra(EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, packageName)
        }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            // Main switch preference
            +NotificationAccessApprovalPreference(storage)
            // Filter type preferences
            +NotificationAccessOngoingPreference(storage)
            +NotificationAccessConversationsPreference(storage)
            +NotificationAccessAlertingPreference(storage)
            +NotificationAccessSilentPreference(storage)
        }

    class AppInfoNotificationAccessScreenPreference(
        private val screenMetadata : AppInfoNotificationAccessScreen
    ) : PreferenceMetadata, PreferenceSummaryProvider, PreferenceTitleProvider, PreferenceAvailabilityProvider {
        override val key : String
            get() = "device_state_app_info_notification_access_preference"

        override val purpose : Int
            get() = screenMetadata.purpose

        override val indexable = false

        override fun tags(context: Context) = arrayOf(METADATA_IN_UI)

        override fun isEnabled(context: Context) : Boolean = screenMetadata.isEnabled(context)

        override fun getSummary(context: Context) : CharSequence? = screenMetadata.getSummary(context)

        override val availabilityDescription = screenMetadata.availabilityDescription

        override fun getAvailabilityStability() = screenMetadata.getAvailabilityStability()

        override fun isAvailable(context: Context) : Boolean = screenMetadata.isAvailable(context)

        override fun getTitle(context: Context): CharSequence? = screenMetadata.getTitle(context)
    }

    companion object : ParameterizedPreferenceScreenArgumentsFactory {
        const val KEY = "device_state_app_info_notification_access"

        const val KEY_EXTRA_PACKAGE_NAME = "package_name"
        const val KEY_APP_PACKAGE_NAME = "app"
        const val KEY_SERVICE_NAME = "serviceName"
        const val KEY_SERVICE = "service"

        @JvmStatic
        override val parametersSchema = KeyParametersSchema {
            parameter(KEY_SERVICE, "The componentname of the service", required = true, type = NotificationListenerService)
        }

        @JvmStatic
        override fun keyParameters(context: Context): Flow<ValidatedKeyParameters> {
            // TODO (b/457649430): when the catalyst framework stops passing the arguments as a
            // bundle: replace the parameters(context) call to the actual implementation,
            // or make this function the primary implementation and the legacy parameters() should
            // call this one.
            return parameters(context).map { bundle ->
                val service = bundle.getString(KEY_APP_PACKAGE_NAME)!! + "/" + bundle.getString(KEY_SERVICE_NAME)!!
                parametersSchema.prepare(Bundle(1).apply { putString(KEY_SERVICE, service) })
            }
        }

        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> = flow {
            val services = AppsNotificationAccessScreen.loadNotificationListenerServices(context)
            for (service in services) {
                emit(
                    Bundle(1).apply {
                        putString(KEY_APP_PACKAGE_NAME, service.packageName)
                        putString(KEY_SERVICE_NAME, service.name)
                    }
                )
            }
        }
    }
}

/**
 * Notification access main switch.
 *
 * Current implementation see:
 * https://source.corp.google.com/h/googleplex-android/platform/superproject/main/+/main:packages/apps/Settings/src/com/android/settings/applications/specialaccess/notificationaccess/ApprovalPreferenceController.java
 */
class NotificationAccessApprovalPreference(private val storage: KeyValueStore) :
    BooleanValuePreference, MainSwitchPreferenceBinding {

    override val key
        get() = KEY

    override val purpose: Int
        get() = R.string.device_state_notification_access_approval_preference_purpose

    override val title
        get() = R.string.notification_access_detail_switch

    override fun storage(context: Context) = storage

    override val supportsWrite = false // The passed in storage doesn't support write.

    companion object {
        const val KEY = "device_state_notification_access_approval_preference"
    }
}

/**
 * Notification access "Real-time" switch.
 *
 * Current implementation see:
 * https://source.corp.google.com/h/googleplex-android/platform/superproject/main/+/main:packages/apps/Settings/src/com/android/settings/applications/specialaccess/notificationaccess/OngoingTypeFilterPreferenceController.java
 */
class NotificationAccessOngoingPreference(private val storage: KeyValueStore) :
    BooleanValuePreference, SwitchPreferenceBinding {

    override val key
        get() = KEY

    override val purpose: Int
        get() = R.string.device_state_notification_access_ongoing_preference_purpose

    override val title
        get() = R.string.notif_type_ongoing

    override fun storage(context: Context) = storage

    override val supportsWrite = false // The passed in storage doesn't support write.

    companion object {
        const val KEY = "device_state_notification_access_ongoing_preference"
    }
}

/**
 * Notification access "Conversations" switch.
 *
 * Current implementation see:
 * https://source.corp.google.com/h/googleplex-android/platform/superproject/main/+/main:packages/apps/Settings/src/com/android/settings/applications/specialaccess/notificationaccess/ConversationTypeFilterPreferenceController.java
 */
class NotificationAccessConversationsPreference(private val storage: KeyValueStore) :
    BooleanValuePreference, SwitchPreferenceBinding {

    override val key
        get() = KEY

    override val purpose: Int
        get() = R.string.device_state_notification_access_conversations_preference_purpose

    override val title
        get() = R.string.notif_type_conversation

    override fun storage(context: Context) = storage

    override val supportsWrite = false // The passed in storage doesn't support write.

    companion object {
        const val KEY = "device_state_notification_access_conversations_preference"
    }
}

/**
 * Notification access "Notifications" switch.
 *
 * Current implementation see:
 * https://source.corp.google.com/h/googleplex-android/platform/superproject/main/+/main:packages/apps/Settings/src/com/android/settings/applications/specialaccess/notificationaccess/AlertingTypeFilterPreferenceController.java
 */
class NotificationAccessAlertingPreference(private val storage: KeyValueStore) :
    BooleanValuePreference, SwitchPreferenceBinding {

    override val key
        get() = KEY

    override val purpose: Int
        get() = R.string.device_state_notification_access_alerting_preference_purpose

    override val title
        get() = R.string.notif_type_alerting

    override fun storage(context: Context) = storage

    override val supportsWrite = false // The passed in storage doesn't support write.

    companion object {
        const val KEY = "device_state_notification_access_alerting_preference"
    }
}

/**
 * Notification access "Silent" switch.
 *
 * Current implementation see:
 * https://source.corp.google.com/h/googleplex-android/platform/superproject/main/+/main:packages/apps/Settings/src/com/android/settings/applications/specialaccess/notificationaccess/SilentTypeFilterPreferenceController.java
 */
class NotificationAccessSilentPreference(private val storage: KeyValueStore) :
    BooleanValuePreference, SwitchPreferenceBinding {

    override val key
        get() = KEY

    override val purpose: Int
        get() = R.string.device_state_notification_access_silent_preference_purpose

    override val title
        get() = R.string.notif_type_silent

    override fun storage(context: Context) = storage

    override val supportsWrite = false // The passed in storage doesn't support write.

    companion object {
        const val KEY = "device_state_notification_access_silent_preference"
    }
}

private class NotificationAccessStorage(
    private val context: Context,
    private val packageName: String,
    private val serviceName: String,
) : NoOpKeyedObservable<String>(), KeyValueStore {

    override fun contains(key: String): Boolean =
        when (key) {
            NotificationAccessApprovalPreference.KEY,
            NotificationAccessOngoingPreference.KEY,
            NotificationAccessConversationsPreference.KEY,
            NotificationAccessAlertingPreference.KEY,
            NotificationAccessSilentPreference.KEY -> true
            else -> false
        }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T =
        when (key) {
            NotificationAccessApprovalPreference.KEY ->
                notificationAccessApproval(context, packageName, serviceName)
            NotificationAccessOngoingPreference.KEY ->
                notificationAccessTypeFilter(
                    FLAG_FILTER_TYPE_ONGOING,
                    context,
                    packageName,
                    serviceName,
                )
            NotificationAccessConversationsPreference.KEY ->
                notificationAccessTypeFilter(
                    FLAG_FILTER_TYPE_CONVERSATIONS,
                    context,
                    packageName,
                    serviceName,
                )
            NotificationAccessAlertingPreference.KEY ->
                notificationAccessTypeFilter(
                    FLAG_FILTER_TYPE_ALERTING,
                    context,
                    packageName,
                    serviceName,
                )
            NotificationAccessSilentPreference.KEY ->
                notificationAccessTypeFilter(
                    FLAG_FILTER_TYPE_SILENT,
                    context,
                    packageName,
                    serviceName,
                )
            else -> throw IllegalArgumentException("Unknown key: $key")
        }
            as T

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {}

    companion object {
        fun notificationAccessApproval(
            context: Context,
            packageName: String,
            serviceName: String,
        ): Boolean {
            val componentName = ComponentName(packageName, serviceName)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            return notificationManager.isNotificationListenerAccessGranted(componentName)
        }

        fun notificationAccessTypeFilter(
            type: Int,
            context: Context,
            packageName: String,
            serviceName: String,
        ): Boolean {
            val componentName = ComponentName(packageName, serviceName)
            val notificationBackend = NotificationBackend()
            val listenerFilter =
                notificationBackend.getListenerFilter(componentName, context.userId)
            return isFlagSet(listenerFilter.types, type)
        }

        private fun isFlagSet(flagData: Int, flag: Int) = (flagData and flag) != 0
    }
}
