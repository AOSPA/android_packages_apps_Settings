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

package com.android.settings.accessibility.shared.utils

import android.content.Context
import android.content.DialogInterface
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.widget.FocusIndicatorDrawable
import com.android.settingslib.widget.LottieColorUtils
import com.android.settingslib.widget.SettingsThemeHelper
import com.google.android.setupcompat.util.DelightHelper

const val FOCUS_INDICATOR_HORIZONTAL_PADDING_ADJUSTMENT_DP = -3
const val FOCUS_INDICATOR_TOP_PADDING_ADJUSTMENT_DP = 6
const val FOCUS_INDICATOR_BOTTOM_PADDING_ADJUSTMENT_DP = -3
const val FOCUS_INDICATOR_CORNER_RADIUS_DP = 24

// TODO(b/407080818): Delete this function when we no longer need to adjust the layout
/**
 * Adjusts the layout of the given [LottieAnimationView] for the Setup Wizard.
 *
 * This function is a workaround for an issue where `IllustrationPreference` changes the
 * illustrationFrame's width to the shortest device side, which can cause the image to be cut off
 * when displayed in the Setup Wizard. This function sets the width of the illustrationFrame to
 * `MATCH_PARENT` to resolve this issue.
 */
fun adjustIllustrationLayoutForSetupWizard(view: LottieAnimationView) {
    // IllustrationPreference changes the illustrationFrame's width to the shortest
    // device side.
    // This potentially breaks the image when shown in SetupWizard. Sets the width to
    // MATCH_PARENT solves the image cutoff issue on SUW
    if (SettingsThemeHelper.isExpressiveTheme(view.context) && view.context.isInSetupWizard()) {
        if (view.parent is ViewGroup) {
            val illustrationFrame = view.parent as ViewGroup
            val lp: ViewGroup.LayoutParams = illustrationFrame.layoutParams
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            illustrationFrame.layoutParams = lp
        }
    }
}

/**
 * Handles the animation playback logic, applying a delay if the 'Delight' helper requires it for
 * SUW transitions.
 */
fun handleIllustrationAnimationForSetupWizard(view: LottieAnimationView) {
    val context = view.context
    if (DelightHelper.shouldApplyAnimatedIcon(context) && context.isInSetupWizard()) {
        // Cancel any pending playback to prevent premature triggers during the delay period.
        view.cancelAnimation()

        // Ensure the animation color palette matches the Material Expressive theme once stopped.
        if (SettingsThemeHelper.isExpressiveTheme(context)) {
            LottieColorUtils.applyMaterialColor(context, view)
        }

        val delayMs =
            context.resources
                .getInteger(com.google.android.setupdesign.R.integer.sud_lottie_animation_delay_ms)
                .toLong()
        view.postDelayed({ view.playAnimation() }, delayMs)
    }
}

/**
 * Determines if the focus rings should be shown in the Setup Wizard.
 *
 * @param context the current context.
 * @return `true` if the focus rings should be shown.
 */
fun shouldShowFocusRingsInSuw(context: Context): Boolean {
    val isInsetFocusRingFlagEnabled = Flags.enableInsetFocusRingsInSuwReadOnly()
    val isInsetFocusRingConfigEnabled =
        context.resources.getBoolean(com.android.internal.R.bool.config_enableInsetFocusRingsInSuw)
    val isDeviceNotProvisioned =
        Settings.Global.getInt(context.contentResolver, Settings.Global.DEVICE_PROVISIONED, 0) == 0
    return isInsetFocusRingFlagEnabled && isInsetFocusRingConfigEnabled && isDeviceNotProvisioned
}

/**
 * The style of the button to which the focus ring is applied.
 *
 * This is used to determine the color of the focus ring.
 */
enum class ButtonStyle {
    FILLED,
    BORDERLESS,
}

/**
 * Applies focus rings and other accessibility settings to a dialog when shown in the Setup Wizard.
 *
 * This function handles applying focus rings to standard buttons and specific custom views (like
 * the tutorial ViewPager) and adjusts view focusability for a better accessibility experience
 * during SUW.
 *
 * @param dialog The [AlertDialog] to modify.
 */
fun configureFocusRingsForDialog(dialog: AlertDialog) {
    // Apply focus rings to standard dialog buttons if they exist.
    dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.let {
        applyFocusRingToButton(it, ButtonStyle.FILLED)
    }
    dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.let {
        applyFocusRingToButton(it, ButtonStyle.BORDERLESS)
    }

    // Make the button panel not focusable. This is to avoid having the container of the
    // buttons be focusable, which is not an interactive element.
    dialog.findViewById<View>(androidx.appcompat.R.id.buttonPanel)?.let {
        it.isFocusable = false
        it.isFocusableInTouchMode = false
    }

    // Apply focus rings and settings to custom views from tutorials, if they exist.
    dialog.findViewById<View>(R.id.view_pager)?.let { viewPager ->
        viewPager.foreground = FocusIndicatorDrawable.Builder(dialog.context).build()

        // Make the view group containing the lottie animation not focusable to
        // prevent interaction with non-interactive elements.
        (viewPager.parent as? View)?.let {
            it.isFocusable = false
            it.isFocusableInTouchMode = false
        }
        (viewPager.parent?.parent as? ViewGroup)?.let {
            it.isFocusable = false
            it.isFocusableInTouchMode = false
        }
    }
}

private fun applyFocusRingToButton(button: Button, style: ButtonStyle) {
    button.isSingleLine = false
    val colorRes =
        when (style) {
            ButtonStyle.FILLED -> com.android.internal.R.color.materialColorOnPrimary
            ButtonStyle.BORDERLESS -> com.android.internal.R.color.materialColorPrimary
        }
    button.foreground =
        FocusIndicatorDrawable.Builder(button.context)
            .withHorizontalPaddingAdjustment(FOCUS_INDICATOR_HORIZONTAL_PADDING_ADJUSTMENT_DP)
            .withVerticalPaddingAdjustments(
                FOCUS_INDICATOR_TOP_PADDING_ADJUSTMENT_DP,
                FOCUS_INDICATOR_BOTTOM_PADDING_ADJUSTMENT_DP,
            )
            .withCornerRadius(FOCUS_INDICATOR_CORNER_RADIUS_DP)
            .withColorRes(colorRes)
            .build()
}
