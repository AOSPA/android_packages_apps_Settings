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

package com.android.settings.notification.app

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.ConversationListSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.notification.NotificationBackend
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.utils.StringUtil
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_NOTIFICATIONS

// LINT.IfChange
@ProvidePreferenceScreen(ConversationListScreen.KEY)
open class ConversationListScreen : PreferenceScreenMixin, PreferenceSummaryProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_NOTIFICATIONS)

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.conversations_purpose

    override val screenTitle: Int
        get() = R.string.zen_mode_conversations_title

    override val title: Int
        get() = R.string.conversations_category_title

    private val backend = NotificationBackend()

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +ConversationListScreenPreference(this@ConversationListScreen) }

    override fun fragmentClass(): Class<out Fragment>? = ConversationListSettings::class.java

    override fun hasCompleteHierarchy() = false

    override val highlightMenuKey
        get() = R.string.menu_key_notifications

    override fun getMetricsCategory(): Int = SettingsEnums.NOTIFICATION_CONVERSATION_LIST_SETTINGS

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, ConversationListSettingsActivity::class.java, metadata?.key)

    override fun getSummary(context: Context): CharSequence? {
        val count: Int = backend.getConversations(true).getList().size
        if (count == 0) {
            return context.getText(R.string.priority_conversation_count_zero)
        }
        return StringUtil.getIcuPluralsString(context, count, R.string.priority_conversation_count)
    }

    class ConversationListScreenPreference(
        private val screenMetadata : ConversationListScreen
    ) : PreferenceMetadata, PreferenceSummaryProvider, PersistentPreference<String> {
        override val key : String
            get() = "conversations_preference"

        override val purpose : Int
            get() = screenMetadata.purpose

        override fun tags(context: Context) = arrayOf(METADATA_IN_UI)

        override val indexable = false

        override fun isEnabled(context: Context) : Boolean = screenMetadata.isEnabled(context)

        override fun getSummary(context: Context) : CharSequence? = screenMetadata.getSummary(context)

        override val supportsWrite: Boolean
            get() = false

        override val valueType = String::class.javaObjectType

        override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)
    }

    companion object {
        const val KEY = "conversations"
    }
}
// LINT.ThenChange(ConversationListSettings.java,
//                 ../ConversationListSummaryPreferenceController.java)
