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

package com.android.settings.accessibility.a11yservice.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.accessibility.AccessibilitySettings
import com.android.settings.accessibility.FeedbackManager
import com.android.settings.accessibility.a11yservice.A11yServicePreferenceFragment
import com.android.settings.accessibility.a11yservice.data.AccessibilityService
import com.android.settings.accessibility.a11yservice.data.UseServiceDataStore
import com.android.settings.accessibility.a11yservice.ui.A11yServiceFooterPreference.Companion.FOOTER_KEY
import com.android.settings.accessibility.a11yservice.ui.A11yServiceFooterPreference.Companion.HTML_FOOTER_KEY
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.extensions.getComponentName
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settings.accessibility.extensions.putComponentName
import com.android.settings.accessibility.shared.ui.FeedbackButtonPreference
import com.android.settings.accessibility.shared.ui.LaunchAppInfoPreference
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.utils.highlightPreference
import com.android.settingslib.RestrictedPreference
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.TwoTargetPreference.ICON_SIZE_MEDIUM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@ProvidePreferenceScreen(A11yServiceScreen.KEY, parameterized = true)
open class A11yServiceScreen
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
    PreferenceBinding,
    PreferenceLifecycleProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    @Deprecated(
        "This constructor will be removed once the catalyst framework stops passing the arguments as a bundle. Use the other constructor instead."
    )
    constructor(context: Context, args: Bundle) : this(context, args, null)

    constructor(
        context: Context,
        keyParameters: ValidatedKeyParameters,
    ) : this(context, null, keyParameters)

    private val featureComponentName: ComponentName by lazy {
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            val componentNameString =
                requireNotNull(keyParameters!![AccessibilitySettings.EXTRA_COMPONENT_NAME])
            requireNotNull(ComponentName.unflattenFromString(componentNameString))
        } else {
            requireNotNull(arguments!!.getComponentName(AccessibilitySettings.EXTRA_COMPONENT_NAME))
        }
    }

    private val accessibilityServiceInfo by lazy {
        AccessibilityRepositoryProvider.get(context)
            .getAccessibilityServiceInfo(featureComponentName)
    }

    private var serviceEnablementStorage: UseServiceDataStore? = null
    private var serviceEnablementObserver: KeyedObserver<String>? = null

    override val key: String
        get() = KEY

    override val keyParametersSchema: KeyParametersSchema
        get() = parametersSchema

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.a11y_service_detail_screen_purpose

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override fun getMetricsCategory(): Int {
        return featureFactory.accessibilityPageIdFeatureProvider.getCategory(featureComponentName)
    }

    override fun getTitle(context: Context): CharSequence? {
        return accessibilityServiceInfo?.getFeatureName(context)
    }

    override fun getSummary(context: Context): CharSequence? {
        return AccessibilitySettings.getServiceSummary(
            context,
            accessibilityServiceInfo,
            serviceEnablementStorage?.getBoolean(key) ?: false,
        )
    }

    override fun fragmentClass(): Class<out Fragment>? {
        return A11yServicePreferenceFragment::class.java
    }

    override fun createWidget(context: Context): Preference {
        val resolveInfo = accessibilityServiceInfo?.resolveInfo
        val packageName = resolveInfo?.serviceInfo?.packageName
        return RestrictedPreference(
                context,
                packageName,
                resolveInfo?.serviceInfo?.applicationInfo?.uid ?: 0,
            )
            .apply {
                setIconSize(ICON_SIZE_MEDIUM)
                isIconSpaceReserved = true
            }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.icon = getIcon(preference.context)
    }

    private fun getIcon(context: Context): Drawable {
        val icon =
            accessibilityServiceInfo?.resolveInfo?.run {
                if (iconResource != 0) {
                    loadIcon(context.packageManager)
                } else null
            } ?: ContextCompat.getDrawable(context, R.drawable.ic_accessibility_generic)
        return Utils.getAdaptiveIcon(context, icon, Color.WHITE)
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        if (isEntryPoint(context)) {
            // Register an observer for updating summary based on service's on/off state.
            accessibilityServiceInfo?.let { a11yServiceInfo ->
                val dataStore = UseServiceDataStore(context, a11yServiceInfo)
                val observer =
                    KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(bindingKey) }
                dataStore.addObserver(bindingKey, observer, HandlerExecutor.main)

                serviceEnablementStorage = dataStore
                serviceEnablementObserver = observer
            }
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        if (isEntryPoint(context)) {
            serviceEnablementObserver?.let { observer ->
                serviceEnablementStorage?.removeObserver(bindingKey, observer)
            }
        }
    }

    override val bindingKey: String
        get() = featureComponentName.flattenToString()

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            val serviceInfo = accessibilityServiceInfo ?: return@preferenceHierarchy
            +IntroPreference(serviceInfo)
            +A11yServiceIllustrationPreference(serviceInfo)
            +UseServicePreference(context, serviceInfo, metricsCategory)
            +PreferenceCategory(
                key = "general_categories",
                purpose = R.string.general_categories_purpose,
                title = R.string.accessibility_screen_option,
            ) +=
                {
                    +A11yServiceShortcutPreference(context, serviceInfo, metricsCategory)
                    +A11yServiceSettingPreference(serviceInfo)
                    +LaunchAppInfoPreference(
                        key = "accessibility_service_app_info",
                        purpose = R.string.accessibility_service_app_info_purpose,
                        packageName = serviceInfo.componentName.packageName,
                    )
                }
            +A11yServiceFooterPreference(
                HTML_FOOTER_KEY,
                purpose = R.string.a11y_service_detail_screen_html_footer_info_purpose,
                serviceInfo,
                loadHtmlFooter = true,
            )
            +A11yServiceFooterPreference(
                FOOTER_KEY,
                purpose = R.string.a11y_service_detail_screen_footer_info_purpose,
                serviceInfo,
                loadHtmlFooter = false,
            )
            +FeedbackButtonPreference { FeedbackManager(context, metricsCategory) }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_DETAILS_SETTINGS).apply {
            putExtra(Intent.EXTRA_COMPONENT_NAME, featureComponentName.flattenToString())

            if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                highlightPreference(keyParameters!!, metadata?.bindingKey)
            } else {
                highlightPreference(arguments!!, metadata?.bindingKey)
            }
        }
    }

    companion object : ParameterizedPreferenceScreenArgumentsFactory {
        const val KEY = "a11y_service_detail_screen"

        @JvmStatic
        override val parametersSchema = KeyParametersSchema {
            parameter(
                AccessibilitySettings.EXTRA_COMPONENT_NAME,
                "The accessibility component to be configured",
                required = true,
                type = AccessibilityService,
            )
        }

        @JvmStatic
        override fun keyParameters(context: Context): Flow<ValidatedKeyParameters> {
            // TODO (b/457649430): when the catalyst framework stops passing the arguments as a
            // bundle: replace the parameters(context) call to the actual implementation,
            // or make this function the primary implementation and the legacy parameters() should
            // call this one.
            return parameters(context).map { bundle -> parametersSchema.prepare(bundle) }
        }

        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        @OptIn(ExperimentalCoroutinesApi::class)
        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> {
            return flow {
                AccessibilityRepositoryProvider.get(context)
                    .accessibilityServiceInfos
                    .first()
                    .filter { a11yServiceInfo ->
                        a11yServiceInfo.resolveInfo?.serviceInfo?.applicationInfo?.run {
                            isSystemApp || isUpdatedSystemApp
                        } ?: false
                    }
                    .forEach { a11yServiceInfo ->
                        emit(
                            Bundle(1).apply {
                                putComponentName(
                                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                                    a11yServiceInfo.componentName,
                                )
                            }
                        )
                    }
            }
        }
    }
}
