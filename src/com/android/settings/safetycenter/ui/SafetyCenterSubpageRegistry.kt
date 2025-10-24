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
import androidx.annotation.XmlRes
import com.android.settings.R
import org.xmlpull.v1.XmlPullParser

/**
 * Registry for Safety Center subpage configurations.
 *
 * This object acts as a single source of truth for:
 * - Mapping subpage keys to their preference XML resources.
 * - Extracting safety source IDs from <SafetySourcePreference> tags within those XMLs.
 * - Defining issue-only safety source IDs associated with each subpage.
 *
 * Note: This object is not designed to be thread-safe. It is intended to be accessed only from the
 * main thread, as is typical for UI-related components and PreferenceFragment lifecycles.
 */
object SafetyCenterSubpageRegistry {
    private const val TAG = "SCSubpageRegistry"
    private const val STRING_RES_PREFIX = "@"
    private const val ATTR_SAFETY_SOURCE = "safetySource"
    private const val TAG_SAFETY_SOURCE_PREFERENCE =
        "com.android.settingslib.safetycenter.SafetySourcePreference"
    private const val RES_AUTO_NAMESPACE = "http://schemas.android.com/apk/res-auto"

    /** Enum representing the unique keys for each subpage. */
    enum class SubpageKey {
        APP_SECURITY,
        DEVICE_UNLOCK,
        ACCOUNT_SECURITY,
        DEVICE_FINDERS,
        SYSTEM_AND_UPDATES,
        CELLULAR_NETWORK_SECURITY,
        PRIVACY_CONTROLS,
    }

    /** Maps Subpage key to its XML resource containing SafetySourcePreference tags. */
    private val subpageXmlResources =
        mapOf(
            SubpageKey.APP_SECURITY to R.xml.safety_center_app_security_subpage,
            SubpageKey.DEVICE_UNLOCK to R.xml.safety_center_device_unlock_subpage,
            SubpageKey.ACCOUNT_SECURITY to R.xml.safety_center_account_security_subpage,
            SubpageKey.DEVICE_FINDERS to R.xml.safety_center_device_finders_subpage,
            SubpageKey.SYSTEM_AND_UPDATES to R.xml.safety_center_system_and_updates_subpage,
            SubpageKey.CELLULAR_NETWORK_SECURITY to
                R.xml.safety_center_cellular_network_security_subpage,
            SubpageKey.PRIVACY_CONTROLS to R.xml.safety_center_privacy_controls_settings,
        )

    /**
     * Maps Subpage key to a List of safety source IDs that ONLY provide issues, not full entries.
     * These are not defined in the XML.
     */
    private val subpageIssueOnlySources =
        mapOf<SubpageKey, List<String>>(
            SubpageKey.APP_SECURITY to emptyList(),
            SubpageKey.DEVICE_UNLOCK to listOf("AndroidIdentityCheck"),
            SubpageKey.ACCOUNT_SECURITY to emptyList(),
            SubpageKey.DEVICE_FINDERS to emptyList(),
            SubpageKey.SYSTEM_AND_UPDATES to emptyList(),
            SubpageKey.CELLULAR_NETWORK_SECURITY to emptyList(),
            SubpageKey.PRIVACY_CONTROLS to
                listOf(
                    "AndroidAccessibility",
                    "AndroidNotificationListener",
                    "AndroidBackgroundLocation",
                    "AndroidPermissionAutoRevoke",
                    "AndroidCertificateTransparency",
                ),
        )

    // Cache to store parsed safety source IDs from XML
    private val parsedSubpageSafetySourcesCache = mutableMapOf<SubpageKey, List<String>>()

    /**
     * Gets all safety source IDs (from XML and issue-only) for a given subpage key, Results from
     * XML parsing are cached.
     *
     * @param context Context to access resources.
     * @param subpageKey The enum key identifying the subpage.
     * @return A distinct List of all safety source IDs.
     */
    fun getAllSafetySourceIds(context: Context, subpageKey: SubpageKey): List<String> {
        val xmlSources = getXmlSafetySourceIds(context, subpageKey)
        val issueOnlySources = getIssueOnlySafetySourceIds(subpageKey)
        return (xmlSources + issueOnlySources).distinct()
    }

    /** Gets safety source IDs defined in the subpage's XML. */
    fun getXmlSafetySourceIds(context: Context, subpageKey: SubpageKey): List<String> {
        return parsedSubpageSafetySourcesCache.getOrPut(subpageKey) {
            val xmlResId = subpageXmlResources[subpageKey]
            if (xmlResId == null) {
                Log.w(TAG, "No XML resource found for subpage key: $subpageKey")
                emptyList()
            } else {
                parseSafetySourcesFromXml(context, xmlResId)
            }
        }
    }

    /** Gets issue-only safety source IDs for the subpage. */
    fun getIssueOnlySafetySourceIds(subpageKey: SubpageKey): List<String> {
        return subpageIssueOnlySources[subpageKey] ?: emptyList()
    }

    private fun parseSafetySourceAttribute(context: Context, sourceIdAttr: String?): String? {
        if (sourceIdAttr.isNullOrBlank()) {
            return null
        }

        return if (sourceIdAttr.startsWith(STRING_RES_PREFIX)) {
            try {
                val resId = sourceIdAttr.substring(1).toInt()
                context.getString(resId)
            } catch (e: Exception) {
                Log.e(TAG, "Could not get string for $sourceIdAttr", e)
                null
            }
        } else {
            sourceIdAttr
        }
    }

    /**
     * Parses an XML preference screen to extract safetySource attributes from
     * SafetySourcePreference tags. It resolves string references.
     */
    private fun parseSafetySourcesFromXml(context: Context, @XmlRes xmlResId: Int): List<String> {
        val safetySourceIds = mutableListOf<String>()
        try {
            context.resources.getXml(xmlResId).use { parser ->
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (
                        eventType == XmlPullParser.START_TAG &&
                            parser.name == TAG_SAFETY_SOURCE_PREFERENCE
                    ) {
                        val sourceIdAttr =
                            parser.getAttributeValue(RES_AUTO_NAMESPACE, ATTR_SAFETY_SOURCE)
                        parseSafetySourceAttribute(context, sourceIdAttr)?.let {
                            if (it.isNotBlank()) {
                                safetySourceIds.add(it)
                            } else {
                                Log.w(
                                    TAG,
                                    "Parsed safety source ID is blank for attribute: $sourceIdAttr",
                                )
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Safety Sources from XML $xmlResId", e)
        }
        return safetySourceIds.distinct()
    }
}
