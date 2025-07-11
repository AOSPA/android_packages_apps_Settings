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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import com.android.settings.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Fragment that shows Identity Check promo card.
 */
class IdentityCheckPromoCardFragment : BottomSheetDialogFragment() {

    private var onDismissListener : OnDismissListener? = null

    override fun getTheme(): Int = R.style.IdentityCheckPromoCardDialogStyle

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        //This applies bottom sheet dialog background to navigation bar
        val window: Window? = dialog.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        dialog.setOnShowListener { _ ->
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior = BottomSheetBehavior.from(bottomSheet)

                //Set height to 3/4th the screen size so that users can cancel by clicking outside
                //the dialog
                val windowMetrics = requireContext()
                    .getSystemService(WindowManager::class.java).currentWindowMetrics
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
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.identity_check_promo_card, container, false)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.onDismiss(dialog)
    }

    fun setOnDismissListener(onDismissListener: OnDismissListener) {
        this.onDismissListener = onDismissListener
    }
}