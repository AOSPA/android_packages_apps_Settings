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

package com.android.settings.safetycenter.ui

import android.Manifest.permission_group.CAMERA
import android.Manifest.permission_group.LOCATION
import android.Manifest.permission_group.MICROPHONE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.settings.R
import com.android.settings.safetycenter.ui.model.SafetyCenterQsViewModel
import com.android.settings.safetycenter.ui.model.SafetyCenterQsViewModel.SensorState
import com.android.settings.safetycenter.ui.model.SafetyCenterQsViewModelFactory
import com.android.settingslib.RestrictedLockUtils
import kotlin.math.roundToInt

/** The Quick Settings fragment for the safety center. */
class SafetyCenterQsFragment : Fragment() {

    private lateinit var viewModel: SafetyCenterQsViewModel
    private var areSensorTogglesReady: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = SafetyCenterQsViewModelFactory(requireActivity().application)
        viewModel =
            ViewModelProvider(requireActivity(), factory)[SafetyCenterQsViewModel::class.java]
        viewModel.sensorPrivacyLiveData.observe(this, this::setSensorToggleState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root =
            inflater.inflate(R.layout.safety_center_qs, container, /* attachToRoot */ false)
                as ViewGroup
        root.visibility = View.GONE
        root.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        root.setOnApplyWindowInsetsListener { v, w ->
            val insets = w.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsets.CONSUMED
        }

        val closeButton = root.findViewById<View>(R.id.close_button)
        closeButton.setOnClickListener { requireActivity().finish() }
        SafetyCenterTouchTarget.configureSize(closeButton, R.dimen.min_tap_target_size)

        childFragmentManager
            .beginTransaction()
            .add(R.id.safety_center_prefs, SafetyCenterFragment.newInstance(isQuickSettings = true))
            .commitNow()
        return root
    }

    private fun maybeEnableView(rootView: View?) {
        if (areSensorTogglesReady) {
            rootView?.visibility = View.VISIBLE
        }
    }

    private fun setSensorToggleState(sensorStates: Map<String, SensorState>?) {
        if (!areSensorTogglesReady) {
            areSensorTogglesReady = true
            maybeEnableView(view)
            setupSensorToggles(sensorStates, view)
        }
        updateSensorToggleState(sensorStates, view)
    }

    private fun setupSensorToggles(sensorStates: Map<String, SensorState>?, rootView: View?) {
        rootView ?: return
        val currentSensorStates = sensorStates ?: emptyMap()

        val toggleContainer = rootView.findViewById<LinearLayout>(R.id.toggle_container)
        toggleContainer.setBackgroundResource(R.drawable.sc_qs_toggle_container_background)

        var row = addRow(toggleContainer)

        for (groupName in TOGGLE_BUTTONS) {
            val sensorVisible = currentSensorStates[groupName]?.visible ?: true
            if (!sensorVisible) {
                continue
            }

            addToggle(groupName, row)

            if (row.childCount >= MAX_TOGGLES_PER_ROW) {
                row = addRow(toggleContainer)
            }
        }
        addSettingsToggle(row)
    }

    private fun addRow(parent: ViewGroup): LinearLayout {
        val row = LinearLayout(parent.context, null, 0, R.style.SafetyCenterQsToggleRow)
        parent.addView(row)
        return row
    }

    private fun addToggle(tag: String, parent: ViewGroup): View {
        val toggle = layoutInflater.inflate(R.layout.safety_center_toggle_button, parent, false)
        toggle.tag = tag
        parent.addView(toggle)
        return toggle
    }

    private fun addSettingsToggle(parent: ViewGroup): View {
        val securitySettings = addToggle(SETTINGS_TOGGLE_TAG, parent)
        val context = requireContext()
        return securitySettings.apply {
            setOnClickListener {
                val intent = Intent(Intent.ACTION_SAFETY_CENTER)
                NavigationSource.QUICK_SETTINGS_TILE.addToIntent(intent)
                context.startActivity(intent)
            }

            findViewById<TextView>(R.id.toggle_sensor_name).apply {
                text = getString(R.string.settings_label)
                isSelected = true
            }
            findViewById<View>(R.id.toggle_sensor_status).visibility = View.GONE
            findViewById<ImageView>(R.id.toggle_sensor_icon)
                .setImageDrawable(
                    applyTint(
                        context,
                        context.getDrawable(R.drawable.ic_safety_center_shield)!!,
                        android.R.attr.textColorPrimaryInverse,
                    )
                )
            findViewById<View>(R.id.arrow_icon).apply {
                visibility = View.VISIBLE
                (this as ImageView).setImageDrawable(
                    applyTint(
                        context,
                        context.getDrawable(R.drawable.ic_chevron_right)!!,
                        android.R.attr.textColorSecondaryInverse,
                    )
                )
            }

            ViewCompat.replaceAccessibilityAction(
                this,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                getString(R.string.safety_center_qs_open_action),
                null,
            )
        }
    }

    private fun updateSensorToggleState(sensorStates: Map<String, SensorState>?, rootView: View?) {
        rootView ?: return
        val currentSensorStates = sensorStates ?: emptyMap()
        val context = requireContext()

        for (groupName in TOGGLE_BUTTONS) {
            val toggle = rootView.findViewWithTag<View>(groupName) ?: continue
            val sensorState = currentSensorStates[groupName]
            val admin = sensorState?.admin
            val sensorEnabled = sensorState?.enabled ?: true
            val sensorBlockedByAdmin = admin != null

            setupToggleClickListener(toggle, groupName, sensorBlockedByAdmin, admin, context)
            updateToggleText(toggle, groupName, sensorEnabled, context)
            updateToggleVisuals(toggle, sensorEnabled, sensorBlockedByAdmin, groupName, context)
            updateToggleAccessibility(toggle, context)
        }
    }

    private fun setupToggleClickListener(
        toggle: View,
        groupName: String,
        sensorBlockedByAdmin: Boolean,
        admin: RestrictedLockUtils.EnforcedAdmin?,
        context: Context,
    ) {
        if (sensorBlockedByAdmin) {
            toggle.setOnClickListener {
                context.startActivity(
                    RestrictedLockUtils.getShowAdminSupportDetailsIntent(context, admin)
                )
            }
        } else {
            toggle.setOnClickListener { viewModel.toggleSensor(groupName) }
        }
    }

    private fun updateToggleText(
        toggle: View,
        groupName: String,
        sensorEnabled: Boolean,
        context: Context,
    ) {
        val groupLabel = toggle.findViewById<TextView>(R.id.toggle_sensor_name)
        groupLabel.text = getPermGroupLabel(groupName)
        groupLabel.isSelected = true // To enable marquee

        val blockedStatus = toggle.findViewById<TextView>(R.id.toggle_sensor_status)
        blockedStatus.text = context.getString(getSensorStatusTextResId(groupName, sensorEnabled))
        blockedStatus.isSelected = true // To enable marquee
    }

    private fun updateToggleVisuals(
        toggle: View,
        sensorEnabled: Boolean,
        sensorBlockedByAdmin: Boolean,
        groupName: String,
        context: Context,
    ) {
        val useEnabledBackground = sensorEnabled && !sensorBlockedByAdmin

        val colorPrimary =
            getTextColor(
                primary = true,
                inverse = useEnabledBackground,
                useLowerOpacity = sensorBlockedByAdmin,
            )
        val colorSecondary =
            getTextColor(
                primary = false,
                inverse = useEnabledBackground,
                useLowerOpacity = sensorBlockedByAdmin,
            )

        toggle.setBackgroundResource(
            when {
                useEnabledBackground -> R.drawable.safety_center_sensor_toggle_enabled
                else -> R.drawable.safety_center_sensor_toggle_disabled
            }
        )

        val groupLabel = toggle.findViewById<TextView>(R.id.toggle_sensor_name)
        val blockedStatus = toggle.findViewById<TextView>(R.id.toggle_sensor_status)
        val iconView = toggle.findViewById<ImageView>(R.id.toggle_sensor_icon)

        groupLabel.setTextColor(colorPrimary)
        blockedStatus.setTextColor(colorSecondary)

        val icon =
            if (sensorEnabled) {
                getPermGroupIcon(context, groupName, colorPrimary)
            } else {
                context.getDrawable(getBlockedIconResId(groupName))?.apply { setTint(colorPrimary) }
            }
        iconView.setImageDrawable(icon)
    }

    private fun updateToggleAccessibility(toggle: View, context: Context) {
        val groupLabel = toggle.findViewById<TextView>(R.id.toggle_sensor_name)
        val blockedStatus = toggle.findViewById<TextView>(R.id.toggle_sensor_status)

        toggle.contentDescription =
            context.getString(
                R.string.safety_center_qs_privacy_control,
                groupLabel.text,
                blockedStatus.text,
            )

        ViewCompat.replaceAccessibilityAction(
            toggle,
            AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
            context.getString(R.string.safety_center_toggle),
            null,
        )
    }

    @ColorInt
    private fun getTextColor(primary: Boolean, inverse: Boolean, useLowerOpacity: Boolean): Int {
        val attr =
            when {
                primary && inverse -> android.R.attr.textColorPrimaryInverse
                primary && !inverse -> android.R.attr.textColorPrimary
                !primary && inverse -> android.R.attr.textColorSecondaryInverse
                else -> android.R.attr.textColorSecondary
            }
        val value = TypedValue()
        requireContext().theme.resolveAttribute(attr, value, true)
        val colorRes = if (value.resourceId != 0) value.resourceId else value.data
        val color = requireContext().getColor(colorRes)

        return if (useLowerOpacity) {
            colorWithAdjustedAlpha(color, 0.5f)
        } else {
            color
        }
    }

    @ColorInt
    private fun colorWithAdjustedAlpha(@ColorInt color: Int, factor: Float): Int =
        Color.argb(
            (Color.alpha(color) * factor).roundToInt(),
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )

    private fun getPermGroupLabel(permissionGroup: String): CharSequence =
        when (permissionGroup) {
            MICROPHONE -> requireContext().getString(R.string.microphone_toggle_label_qs)
            CAMERA -> requireContext().getString(R.string.camera_toggle_label_qs)
            LOCATION -> requireContext().getString(R.string.location_toggle_label_qs)
            else -> getPermGroupLabel(requireContext(), permissionGroup)
        }

    companion object {
        private val TOGGLE_BUTTONS = listOf(CAMERA, MICROPHONE, LOCATION)
        private const val SETTINGS_TOGGLE_TAG = "settings_toggle"
        private const val MAX_TOGGLES_PER_ROW = 2

        private fun getBlockedIconResId(permissionGroup: String): Int {
            return when (permissionGroup) {
                MICROPHONE -> R.drawable.ic_mic_blocked
                CAMERA -> R.drawable.ic_camera_blocked
                LOCATION -> R.drawable.ic_location_blocked
                else -> -1
            }
        }

        private fun getSensorStatusTextResId(permissionGroup: String, enabled: Boolean): Int {
            return when (permissionGroup) {
                LOCATION ->
                    if (enabled) R.string.accessibility_feature_state_on
                    else R.string.location_settings_summary_location_off
                else ->
                    if (enabled) R.string.subscription_available
                    else R.string.safety_center_blocked_label
            }
        }

        private fun applyTint(context: Context, icon: Drawable, attr: Int): Drawable {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(attr, typedValue, true)
            return icon.mutate().apply { setTint(context.getColor(typedValue.resourceId)) }
        }

        /**
         * Gets the icon for a permission group and applies an optional tint.
         *
         * @param context The application context.
         * @param groupName The name of the permission group.
         * @param tint An optional color integer to tint the icon. If null, a default tint is
         *   applied.
         * @return A tinted Drawable for the permission group.
         */
        private fun getPermGroupIcon(
            context: Context,
            groupName: String,
            @ColorInt tint: Int?,
        ): Drawable? {
            val groupInfo = getGroupInfo(groupName, context)
            var icon: Drawable? = null

            if (groupInfo?.icon != 0 && groupInfo != null) {
                icon = loadDrawable(context.packageManager, groupInfo.packageName, groupInfo.icon)
            }

            if (icon == null) {
                icon = ContextCompat.getDrawable(context, R.drawable.ic_perm_device_info)
            }

            icon ?: return null

            return if (tint == null) {
                applyTint(context, icon, android.R.attr.colorControlNormal)
            } else {
                icon.mutate().apply { setTint(tint) }
            }
        }

        /**
         * Gets a permission group's label from the system.
         *
         * @param context The context from which to get the label.
         * @param groupName The name of the permission group whose label we want.
         * @return The permission group's label, or the group name if the group is invalid.
         */
        fun getPermGroupLabel(context: Context, groupName: String): CharSequence {
            val groupInfo = getGroupInfo(groupName, context)
            return groupInfo?.loadSafeLabel(
                context.packageManager,
                0f, /* ellipsizeDip */
                TextUtils.SAFE_STRING_FLAG_FIRST_LINE or TextUtils.SAFE_STRING_FLAG_TRIM,
            ) ?: groupName
        }

        private fun loadDrawable(pm: PackageManager, pkg: String, resId: Int): Drawable? {
            return try {
                pm.getResourcesForApplication(pkg).getDrawable(resId, null)
            } catch (e: Resources.NotFoundException) {
                null
            } catch (e: NameNotFoundException) {
                null
            }
        }

        /**
         * Get the [PackageItemInfo] for the given permission group.
         *
         * @param groupName the group
         * @param context the [Context] to retrieve [PackageManager]
         * @return The info of permission group or null if the group does not have runtime
         *   permissions.
         */
        private fun getGroupInfo(groupName: String, context: Context): PackageItemInfo? {
            try {
                return context.packageManager.getPermissionGroupInfo(groupName, 0)
            } catch (e: NameNotFoundException) {
                /* ignore */
            }
            try {
                return context.packageManager.getPermissionInfo(groupName, 0)
            } catch (e: NameNotFoundException) {
                /* ignore */
            }
            return null
        }
    }
}
