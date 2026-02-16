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

package com.android.settings.accessibility.setupwizard

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.accessibility.AccessibilityManager
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentActivity
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settingslib.utils.ThreadUtils
import com.google.android.setupdesign.items.Item

/**
 * A controller that manages a Setup Wizard item representing a specific Accessibility Service.
 *
 * This controller dynamically updates the [Item]'s title and icon by looking up the service's
 * [AccessibilityServiceInfo] based on the provided [serviceComponentString]. It handles the
 * visibility of the item (hiding it if the service is not installed) and defines the behavior when
 * the item is selected.
 */
class AccessibilityServiceItemController(
    private val context: Context,
    targetItem: Item,
    private val serviceComponentString: String,
) : BaseItemController(targetItem) {

    override fun bindData(item: Item) {
        ThreadUtils.postOnBackgroundThread {
            val info = findAccessibilityServiceInfo(serviceComponentString)
            val isVisible = info != null
            val title = info?.getFeatureName(context)
            val icon = info?.let { getResizedIcon(context, it) }

            ThreadUtils.getUiThreadHandler().post {
                item.isVisible = isVisible
                if (isVisible) {
                    item.title = title
                    item.icon = icon
                }
                item.notifyItemChanged()
            }
        }
    }

    override fun onItemSelected(activity: FragmentActivity) {
        AccessibilityServiceSetupWizardFragment.show(
            fragmentManager = activity.supportFragmentManager,
            containerId = R.id.fragment_container,
            componentName = ComponentName.unflattenFromString(serviceComponentString)!!,
            description = targetItem.summary.toString(),
        )
    }

    private fun findAccessibilityServiceInfo(serviceString: String?): AccessibilityServiceInfo? {
        if (serviceString.isNullOrEmpty()) return null
        val componentName = ComponentName.unflattenFromString(serviceString) ?: return null
        return findService(componentName)
    }

    private fun findService(componentName: ComponentName): AccessibilityServiceInfo? {
        val manager = context.getSystemService(AccessibilityManager::class.java) ?: return null
        return manager.installedAccessibilityServiceList.firstOrNull { info ->
            componentName == info.componentName
        }
    }

    private fun getResizedIcon(context: Context, info: AccessibilityServiceInfo): Drawable {
        val pm = context.packageManager
        val icon = info.resolveInfo.loadIcon(pm)
        val adaptiveIcon = Utils.getAdaptiveIcon(context, icon, Color.WHITE)

        // TODO (b/467926454): Applied theme to customize the icon size of item to avoid resize
        // function entirely.
        val sizePx = context.resources.getDimensionPixelSize(R.dimen.accessibility_icon_size)
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        adaptiveIcon.setBounds(0, 0, canvas.width, canvas.height)
        adaptiveIcon.draw(canvas)

        return bitmap.toDrawable(context.resources)
    }
}
