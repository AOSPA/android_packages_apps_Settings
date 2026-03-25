/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.biometrics

import android.content.DialogInterface
import android.content.DialogInterface.OnDismissListener
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.BulletSpan
import android.text.style.LeadingMarginSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import com.android.settings.R
import com.android.settings.safetycenter.IdentityCheckSafetySource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Fragment that shows Identity Check promo card. */
class IdentityCheckPromoCardFragment : BottomSheetDialogFragment() {

    private var onDismissListener: OnDismissListener? = null

    override fun getTheme(): Int = R.style.IdentityCheckPromoCardDialogStyle

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // This applies bottom sheet dialog background to navigation bar
        val window: Window? = dialog.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        dialog.setOnShowListener { _ ->
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior = BottomSheetBehavior.from(bottomSheet)

                // Set height to 3/4th the screen size so that users can cancel by clicking outside
                // the dialog
                val windowMetrics =
                    requireContext()
                        .getSystemService(WindowManager::class.java)
                        .currentWindowMetrics
                val screenHeight = windowMetrics.bounds.height()
                val desiredHeight = (screenHeight * 0.75).toInt()

                behavior.peekHeight = desiredHeight
                behavior.maxHeight = desiredHeight
                behavior.skipCollapsed = true
                behavior.isDraggable = false
                behavior.isFitToContents = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.identity_check_promo_card, container, false)
        val summaryView = view.findViewById<TextView>(R.id.summary)
        summaryView.movementMethod = LinkMovementMethod.getInstance()

        val closeButton = view.findViewById<View>(R.id.close_button)
        closeButton.setOnClickListener { dismiss() }

        val action = arguments?.getString(KEY_INTENT_ACTION)
        if (
            action.equals(IdentityCheckSafetySource.ACTION_ISSUE_CARD_WATCH_SHOW_DETAILS) &&
                context!!.resources.getBoolean(R.bool.config_show_identity_check_watch_promo)
        ) {
            val titleView = view.findViewById<TextView>(R.id.title)
            val promoCardTitle = context!!.getString(R.string.identity_check_watch_promo_card_title)
            titleView.text = promoCardTitle

            val string = context!!.getString(R.string.identity_check_promo_card_watch_summary)
            setSummary(string)
        } else {
            val string = context!!.getString(R.string.identity_check_promo_card_summary)
            setSummary(string)
        }
        return view
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.onDismiss(dialog)
    }

    fun setOnDismissListener(onDismissListener: OnDismissListener) {
        this.onDismissListener = onDismissListener
    }

    private fun setSummary(string: String) {
        val summaryView = view?.findViewById<TextView>(R.id.summary)
        val spanned = string.parseAsHtml(HtmlCompat.FROM_HTML_MODE_COMPACT)
        val indentInDp = 14
        val indentInPx = (indentInDp * (context?.resources?.displayMetrics?.density ?: 1f)).toInt()
        val promoCardSummary = spanned.withBulletIndent(indentInPx)
        summaryView?.text = promoCardSummary
    }

    private fun Spanned.withBulletIndent(indentPx: Int): Spannable {
        val spannable = SpannableString(this)
        val bulletSpans = spannable.getSpans(0, spannable.length, BulletSpan::class.java)

        for (span in bulletSpans) {
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)

            spannable.setSpan(
                LeadingMarginSpan.Standard(indentPx),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        return spannable
    }

    companion object {
        const val KEY_INTENT_ACTION = "KEY_INTENT_ACTION"
    }
}
