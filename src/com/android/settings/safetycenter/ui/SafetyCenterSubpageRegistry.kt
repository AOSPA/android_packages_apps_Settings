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

package com.android.settings.safetycenter.ui

import android.content.Context
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragmentRegistry
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.safetycenter.SafetySourcePreference
import org.xmlpull.v1.XmlPullParser

/**
 * Registry for Safety Center subpage configurations.
 *
 * This object serves as the central point for:
 * - Defining preference key constants for all subpages.
 * - Mapping preference keys to [SubpageConfig] (XML, title, summary, issue-only sources, subpage
 *   fragment).
 * - Mapping external subpage IDs to preference keys.
 * - Parsing subpage XMLs to extract configurations from [SafetySourcePreference] tags.
 * - Providing utility functions to retrieve lists of source IDs related to a subpage.
 */
object SafetyCenterSubpageRegistry {
    private const val TAG = "SCSubpageRegistry"
    private const val STRING_RES_PREFIX = "@"
    private const val NS_ANDROID = "http://schemas.android.com/apk/res/android"
    private const val NS_SETTINGS = "http://schemas.android.com/apk/res-auto"
    private const val TAG_SAFETY_SOURCE_PREFERENCE =
        "com.android.settingslib.safetycenter.SafetySourcePreference"
    private const val ATTR_KEY = "key"
    private const val ATTR_SAFETY_SOURCE = "safetySource"
    private const val ATTR_PROFILE = "profile"
    private const val ATTR_KEYWORDS = "keywords"

    // Subpage Preference Keys
    const val APP_SECURITY_SUBPAGE_KEY = "app_security_subpage"
    const val DEVICE_UNLOCK_SUBPAGE_KEY = "device_unlock_subpage"
    const val ACCOUNT_SECURITY_SUBPAGE_KEY = "account_security_subpage"
    const val DEVICE_FINDERS_SUBPAGE_KEY = "device_finders_subpage"
    const val SYSTEM_AND_UPDATES_SUBPAGE_KEY = "system_and_updates_subpage"
    const val PRIVACY_CONTROLS_SUBPAGE_KEY = "privacy_controls_page"

    // Illustration Preference Keys
    private const val APP_SECURITY_ILLUSTRATION_KEY = "app_security_illustration"
    private const val DEVICE_UNLOCK_ILLUSTRATION_KEY = "device_unlock_illustration"
    private const val ACCOUNT_SECURITY_ILLUSTRATION_KEY = "account_security_illustration"
    private const val DEVICE_FINDERS_ILLUSTRATION_KEY = "device_finders_illustration"
    private const val SYSTEM_AND_UPDATES_ILLUSTRATION_KEY = "system_and_updates_illustration"

    // Issues Banner Group Preference Keys
    private const val APP_SECURITY_ISSUES_KEY = "app_security_issues_banner_group"
    private const val DEVICE_UNLOCK_ISSUES_KEY = "device_unlock_issues_banner_group"
    private const val ACCOUNT_SECURITY_ISSUES_KEY = "account_security_issues_banner_group"
    private const val DEVICE_FINDERS_ISSUES_KEY = "device_finders_issues_banner_group"
    private const val SYSTEM_AND_UPDATES_ISSUES_KEY = "system_and_updates_issues_banner_group"
    private const val PRIVACY_CONTROLS_ISSUES_KEY = "privacy_controls_issues_banner_group"

    /**
     * Configuration map for each subpage, keyed by the preference key string. Provides the XML
     * resource, title resource, default summary resource, and any issue-only sources.
     */
    val subpageConfigs =
        mapOf(
            APP_SECURITY_SUBPAGE_KEY to
                SubpageConfig(
                    xmlResId = R.xml.safety_center_app_security_subpage,
                    titleResId = R.string.app_security_subpage_title,
                    defaultSummaryResId = R.string.safety_center_app_security_summary,
                    subpageFragmentClassName = AppSecuritySubpageFragment::class.qualifiedName!!,
                    illustrationPrefKey = APP_SECURITY_ILLUSTRATION_KEY,
                    illustrationResId = R.drawable.safety_center_app_security_subpage_illustration,
                    issuesBannerGroupPrefKey = APP_SECURITY_ISSUES_KEY,
                ),
            DEVICE_UNLOCK_SUBPAGE_KEY to
                SubpageConfig(
                    xmlResId = R.xml.safety_center_device_unlock_subpage,
                    titleResId = R.string.device_unlock_subpage_title,
                    defaultSummaryResId = R.string.safety_center_device_unlock_summary,
                    subpageFragmentClassName = DeviceUnlockSubpageFragment::class.qualifiedName!!,
                    illustrationPrefKey = DEVICE_UNLOCK_ILLUSTRATION_KEY,
                    illustrationResId = R.drawable.safety_center_device_unlock_subpage_illustration,
                    issuesBannerGroupPrefKey = DEVICE_UNLOCK_ISSUES_KEY,
                ),
            ACCOUNT_SECURITY_SUBPAGE_KEY to
                SubpageConfig(
                    xmlResId = R.xml.safety_center_account_security_subpage,
                    titleResId = R.string.account_security_subpage_title,
                    defaultSummaryResId = R.string.safety_center_account_security_summary,
                    subpageFragmentClassName =
                        AccountSecuritySubpageFragment::class.qualifiedName!!,
                    illustrationPrefKey = ACCOUNT_SECURITY_ILLUSTRATION_KEY,
                    illustrationResId =
                        R.drawable.safety_center_account_security_subpage_illustration,
                    issuesBannerGroupPrefKey = ACCOUNT_SECURITY_ISSUES_KEY,
                ),
            DEVICE_FINDERS_SUBPAGE_KEY to
                SubpageConfig(
                    xmlResId = R.xml.safety_center_device_finders_subpage,
                    titleResId = R.string.device_finders_subpage_title,
                    defaultSummaryResId = R.string.safety_center_device_finders_summary,
                    subpageFragmentClassName = DeviceFindersSubpageFragment::class.qualifiedName!!,
                    issueOnlySources = listOf("AndroidIdentityCheck"),
                    illustrationPrefKey = DEVICE_FINDERS_ILLUSTRATION_KEY,
                    illustrationResId =
                        R.drawable.safety_center_device_finders_subpage_illustration,
                    issuesBannerGroupPrefKey = DEVICE_FINDERS_ISSUES_KEY,
                ),
            SYSTEM_AND_UPDATES_SUBPAGE_KEY to
                SubpageConfig(
                    xmlResId = R.xml.safety_center_system_and_updates_subpage,
                    titleResId = R.string.system_and_updates_subpage_title,
                    defaultSummaryResId = R.string.safety_center_system_and_updates_summary,
                    subpageFragmentClassName =
                        SystemAndUpdatesSubpageFragment::class.qualifiedName!!,
                    illustrationPrefKey = SYSTEM_AND_UPDATES_ILLUSTRATION_KEY,
                    illustrationResId =
                        R.drawable.safety_center_system_and_updates_subpage_illustration,
                    issuesBannerGroupPrefKey = SYSTEM_AND_UPDATES_ISSUES_KEY,
                ),
            PRIVACY_CONTROLS_SUBPAGE_KEY to
                SubpageConfig(
                    xmlResId = R.xml.safety_center_privacy_controls_settings,
                    titleResId = R.string.privacy_sources_title,
                    defaultSummaryResId = R.string.privacy_sources_summary,
                    subpageFragmentClassName = PrivacyControlsFragment::class.qualifiedName!!,
                    issueOnlySources =
                        listOf(
                            "AndroidAccessibility",
                            "AndroidNotificationListener",
                            "AndroidBackgroundLocation",
                            "AndroidPermissionAutoRevoke",
                            "AndroidCertificateTransparency",
                        ),
                    issuesBannerGroupPrefKey = PRIVACY_CONTROLS_ISSUES_KEY,
                ),
        )

    fun getSubpageFragmentClassNameFor(context: Context, subpageId: String): String? {
        val subpageIdToPreferenceKey =
            mapOf(
                context.getString(R.string.config_safety_center_app_security_subpage_id) to
                    APP_SECURITY_SUBPAGE_KEY,
                context.getString(R.string.config_safety_center_accounts_subpage_id) to
                    ACCOUNT_SECURITY_SUBPAGE_KEY,
                context.getString(R.string.config_safety_center_device_finders_subpage_id) to
                    DEVICE_FINDERS_SUBPAGE_KEY,
                context.getString(R.string.config_safety_center_updates_subpage_id) to
                    SYSTEM_AND_UPDATES_SUBPAGE_KEY,
                context.getString(R.string.config_safety_center_lock_screen_subpage_id) to
                    DEVICE_UNLOCK_SUBPAGE_KEY,
                context.getString(R.string.config_safety_center_privacy_controls_subpage_id) to
                    PRIVACY_CONTROLS_SUBPAGE_KEY,
            )

        return subpageConfigs[subpageIdToPreferenceKey[subpageId]]?.subpageFragmentClassName
    }

    /**
     * Checks if a given subpage has any injected tiles.
     *
     * @param preferenceKey The string key for the subpage.
     * @return `true` if the subpage has injected tiles, `false` otherwise.
     */
    fun hasInjectedTiles(preferenceKey: String): Boolean {
        val subpageClassName =
            subpageConfigs[preferenceKey]?.subpageFragmentClassName ?: return false
        val categoryKey =
            DashboardFragmentRegistry.PARENT_TO_CATEGORY_KEY_MAP[subpageClassName] ?: return false
        val category = featureFactory.dashboardFeatureProvider.getTilesForCategory(categoryKey)
        return category?.tiles?.isNotEmpty() ?: false
    }

    /**
     * Gets the XML resource ID for a given [preferenceKey].
     *
     * @param preferenceKey The string key for the subpage.
     * @return The XML resource ID, or null if not found.
     */
    @XmlRes fun getXmlResId(preferenceKey: String): Int? = subpageConfigs[preferenceKey]?.xmlResId

    /**
     * Gets the title string resource ID for a given [preferenceKey].
     *
     * @param preferenceKey The string key for the subpage.
     * @return The title string resource ID, or null if not found.
     */
    @StringRes
    fun getTitleResId(preferenceKey: String): Int? = subpageConfigs[preferenceKey]?.titleResId

    /**
     * Gets the default summary string resource ID for a given [preferenceKey].
     *
     * @param preferenceKey The string key for the subpage.
     * @return The default summary string resource ID, or null if not found.
     */
    @StringRes
    fun getDefaultSummaryResId(preferenceKey: String): Int? =
        subpageConfigs[preferenceKey]?.defaultSummaryResId

    /**
     * Gets the illustration preference key for a given [preferenceKey].
     *
     * @param preferenceKey The string key for the subpage.
     * @return The illustration preference key, or null if not found.
     */
    fun getIllustrationPrefKey(preferenceKey: String): String? =
        subpageConfigs[preferenceKey]?.illustrationPrefKey

    /**
     * Gets the illustration drawable resource ID for a given [preferenceKey].
     *
     * @param preferenceKey The string key for the subpage.
     * @return The illustration drawable resource ID, or null if not found.
     */
    @DrawableRes
    fun getIllustrationResId(preferenceKey: String): Int? =
        subpageConfigs[preferenceKey]?.illustrationResId

    /**
     * Gets the issues banner group preference key for a given [preferenceKey].
     *
     * @param preferenceKey The string key for the subpage.
     * @return The issues banner group preference key, or null if not found.
     */
    fun getIssuesBannerGroupPrefKey(preferenceKey: String): String? =
        subpageConfigs[preferenceKey]?.issuesBannerGroupPrefKey

    /**
     * Retrieves the list of safety source IDs that only provide issues for the given
     * [preferenceKey].
     *
     * @param preferenceKey The string key for the subpage.
     * @return A list of issue-only safety source IDs.
     */
    fun getIssueOnlySafetySourceIds(preferenceKey: String): List<String> =
        subpageConfigs[preferenceKey]?.issueOnlySources ?: emptyList()

    /**
     * Retrieves all safety source IDs associated with a given [preferenceKey], including those
     * defined in the XML and those providing only issues.
     *
     * @param context Context to access resources for XML parsing.
     * @param preferenceKey The string key identifying the subpage.
     * @return A distinct list of all safety source IDs.
     */
    fun getAllSafetySourceIds(context: Context, preferenceKey: String): List<String> {
        val xmlSources = getXmlSafetySourceIds(context, preferenceKey)
        val issueOnlySources = getIssueOnlySafetySourceIds(preferenceKey)
        return (xmlSources + issueOnlySources).distinct()
    }

    /**
     * Retrieves safety source IDs defined within [SafetySourcePreference] tags in the subpage's
     * XML.
     *
     * @param context Context to access resources for XML parsing.
     * @param preferenceKey The string key identifying the subpage.
     * @return A distinct list of safety source IDs from the XML.
     */
    fun getXmlSafetySourceIds(context: Context, preferenceKey: String): List<String> {
        return getXmlSafetySourcePrefConfigs(context, preferenceKey).map { it.sourceId }.distinct()
    }

    /**
     * Parses the XML associated with the [preferenceKey] and returns a list of
     * [SafetySourcePrefConfig] objects.
     *
     * @param context Context to access resources.
     * @param preferenceKey The string key identifying the subpage.
     * @return A list of [SafetySourcePrefConfig] extracted from the XML.
     */
    fun getXmlSafetySourcePrefConfigs(
        context: Context,
        preferenceKey: String,
    ): List<SafetySourcePrefConfig> {
        val xmlResId = getXmlResId(preferenceKey)
        return if (xmlResId == null) {
            Log.w(TAG, "No XML resource found for subpage key: $preferenceKey")
            emptyList()
        } else {
            parseSafetySourcePreferencesFromXml(context, xmlResId)
        }
    }

    private fun resolveStringAttribute(context: Context, attrValue: String?): String? {
        if (attrValue.isNullOrBlank()) return null
        return if (attrValue.startsWith(STRING_RES_PREFIX)) {
            try {
                val resId = attrValue.substring(1).toInt()
                context.getString(resId)
            } catch (e: Exception) {
                Log.e(TAG, "Could not resolve string resource: $attrValue", e)
                null
            }
        } else {
            attrValue
        }
    }

    private fun resolveProfileAttribute(attrValue: String?): Int {
        return attrValue?.toIntOrNull() ?: SafetySourcePreference.Profile.PERSONAL.intValue
    }

    fun parseSafetySourcePreferencesFromXml(
        context: Context,
        @XmlRes xmlResId: Int,
    ): List<SafetySourcePrefConfig> {
        val safetySourcePrefConfigs = mutableListOf<SafetySourcePrefConfig>()
        try {
            context.resources.getXml(xmlResId).use { parser ->
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (
                        eventType == XmlPullParser.START_TAG &&
                            parser.name == TAG_SAFETY_SOURCE_PREFERENCE
                    ) {
                        val keyAttr = parser.getAttributeValue(NS_ANDROID, ATTR_KEY)
                        val sourceIdAttr = parser.getAttributeValue(NS_SETTINGS, ATTR_SAFETY_SOURCE)
                        val profileAttr = parser.getAttributeValue(NS_SETTINGS, ATTR_PROFILE)
                        val keywordsAttr = parser.getAttributeValue(NS_SETTINGS, ATTR_KEYWORDS)

                        val key = resolveStringAttribute(context, keyAttr)
                        val sourceId = resolveStringAttribute(context, sourceIdAttr)
                        val profile =
                            SafetySourcePreference.Profile.fromIntValue(
                                resolveProfileAttribute(profileAttr)
                            )
                        val keywords = resolveStringAttribute(context, keywordsAttr)

                        if (key != null && sourceId != null) {
                            safetySourcePrefConfigs.add(
                                SafetySourcePrefConfig(key, sourceId, profile, keywords)
                            )
                        } else {
                            Log.w(
                                TAG,
                                "Skipping SafetySourcePreference: key=$key, sourceId=$sourceId in $xmlResId",
                            )
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SafetySourcePreferences from XML $xmlResId", e)
        }
        return safetySourcePrefConfigs
    }

    /**
     * Checks if a Safety Center subpage has any data to display.
     *
     * This is true if the subpage has:
     * - Any dynamic entries from its related safety sources.
     * - OR Any injected tiles.
     *
     * @param context Context to access resources and system services.
     * @param preferenceKey The string key identifying the subpage.
     * @return `true` if the subpage has content to display, `false` otherwise.
     */
    fun isSubpageAvailable(context: Context, preferenceKey: String): Boolean {
        if (hasInjectedTiles(preferenceKey)) {
            Log.d(TAG, "[$preferenceKey] Has injected tiles, subpage is available")
            return true
        }

        val uiData = SafetyCenterSearchIndexUtils.getCurrentSafetyCenterData(context)
        if (uiData == null) {
            Log.d(TAG, "[$preferenceKey] No SafetyCenterUiData, subpage not available")
            return false
        }

        val relatedSafetySources = getXmlSafetySourceIds(context, preferenceKey)
        if (relatedSafetySources.isEmpty()) {
            Log.d(TAG, "[$preferenceKey] No related safety sources, subpage not available")
            return false
        }

        val entries = uiData.getDynamicEntriesForSources(relatedSafetySources)
        val isAvailable = entries.isNotEmpty()
        Log.d(
            TAG,
            "[$preferenceKey] Found ${entries.size} entries, subpage available: $isAvailable",
        )
        return isAvailable
    }
}

/**
 * Data class holding the parsed configuration from a [SafetySourcePreference] tag in XML.
 *
 * @property key The unique preference key (from `android:key`).
 * @property sourceId The Safety Center source ID (from `settings:safetySource`).
 * @property profile The user profile type associated with this preference (from
 *   `settings:profile`).
 * @property keywords The search keywords for the preference (from `settings:keywords`).
 */
data class SafetySourcePrefConfig(
    val key: String,
    val sourceId: String,
    val profile: SafetySourcePreference.Profile,
    val keywords: String? = null,
)

/**
 * Configuration for a single Safety Center subpage.
 *
 * @property xmlResId The resource ID of the XML layout for this subpage.
 * @property titleResId The string resource ID for the title of this subpage.
 * @property defaultSummaryResId The string resource ID for the default summary of this subpage.
 * @property subpageFragmentClassName The class name of the fragment that implements this subpage.
 * @property issueOnlySources A list of safety source IDs that only contribute issues to this
 *   subpage.
 * @property illustrationPrefKey The preference key for the illustration on this subpage.
 * @property illustrationResId The drawable resource ID for the illustration on this subpage.
 * @property issuesBannerGroupPrefKey The preference key for the issues banner group on this
 *   subpage.
 */
data class SubpageConfig(
    @XmlRes val xmlResId: Int,
    @StringRes val titleResId: Int,
    @StringRes val defaultSummaryResId: Int,
    val subpageFragmentClassName: String,
    val issueOnlySources: List<String> = emptyList(),
    val illustrationPrefKey: String? = null,
    @DrawableRes val illustrationResId: Int? = null,
    val issuesBannerGroupPrefKey: String,
)
