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

package com.android.settings.accessibility.shared

import android.content.Context
import android.text.TextUtils
import android.view.View
import androidx.annotation.StringRes
import androidx.preference.Preference
import com.android.settings.accessibility.AccessibilityFooterPreference
import com.android.settings.widget.FooterPreferenceBinding
import com.android.settings.widget.FooterPreferenceMetadata
import com.android.settingslib.HelpUtils
import com.android.settingslib.metadata.PreferenceMetadata

/**
 * Interface to provide dynamic accessibility footer preference introduction title.
 *
 * Implement this interface implies that the accessibility footer preference introduction title
 * should not be cached for indexing.
 */
interface AccessibilityFooterPreferenceIntroductionTitleProvider {

    /** Provides accessibility footer preference introduction title. */
    fun getIntroductionTitle(context: Context): CharSequence?
}

/** Metadata of [AccessibilityFooterPreference]. */
interface AccessibilityFooterPreferenceMetadata : FooterPreferenceMetadata {
    val introductionTitle: Int
        @StringRes get() = 0

    val helpResource: Int
        @StringRes get() = 0

    val learnMoreText: Int
        @StringRes get() = 0
}

/** Binding for [AccessibilityFooterPreferenceMetadata]. */
interface AccessibilityFooterPreferenceBinding : FooterPreferenceBinding {

    override fun createWidget(context: Context) = AccessibilityFooterPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        metadata as AccessibilityFooterPreferenceMetadata
        val footerPreference = preference as AccessibilityFooterPreference
        val context = preference.context
        val helpIntent =
            if (metadata.helpResource != 0) {
                HelpUtils.getHelpIntent(
                    context,
                    context.getString(metadata.helpResource),
                    context.javaClass.getName(),
                )
            } else {
                null
            }

        if (helpIntent != null) {
            footerPreference.setLearnMoreAction(
                View.OnClickListener { view: View? -> view!!.startActivityForResult(helpIntent, 0) }
            )
            footerPreference.setLearnMoreText(context.getString(metadata.learnMoreText))
            footerPreference.isLinkEnabled = true
        } else {
            footerPreference.isLinkEnabled = false
        }

        if (preference.isVisible) {
            updateContentDescription(
                footerPreference,
                getPreferenceIntroductionTitle(preference.context, metadata.introductionTitle),
                footerPreference.title,
            )
        }

        footerPreference.isSelectable = false
    }

    private fun updateContentDescription(
        footerPreference: AccessibilityFooterPreference,
        introductionTitle: CharSequence?,
        textToDescribe: CharSequence?,
    ) {
        if (TextUtils.isEmpty(textToDescribe)) {
            return
        }

        val sb = StringBuffer()
        sb.append(introductionTitle).append("\n\n").append(textToDescribe)
        footerPreference.contentDescription = sb.toString()
    }

    private fun getPreferenceIntroductionTitle(
        context: Context,
        introductionTitle: Int,
    ): CharSequence? {
        return if (introductionTitle != 0) {
            context.getString(introductionTitle)
        } else {
            (this as? AccessibilityFooterPreferenceIntroductionTitleProvider)?.getIntroductionTitle(
                context
            )
        }
    }
}
