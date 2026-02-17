/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.applications.intentpicker

import android.view.View
import android.widget.ListAdapter
import androidx.appcompat.app.AlertDialog

object AlertDialogHelper {

    /** Ensure locale text direction is used */
    @JvmStatic
    fun fixTextDirection(dialog: AlertDialog) {
        dialog.listView?.textDirection = View.TEXT_DIRECTION_LOCALE
    }

    /** Prevent "double-tap to activate" TalkBack announcements */
    @JvmStatic
    fun fixAccessibilityAnnouncements(dialog: AlertDialog) {
        dialog.setOnShowListener {
            val listView = dialog.listView
            val adapter = listView?.adapter
            if (adapter != null) {
                listView.adapter = ReadOnlyAdapter(adapter)
            }
        }
    }

    class ReadOnlyAdapter(private val adapter: ListAdapter) : ListAdapter by adapter {
        override fun isEnabled(position: Int): Boolean {
            return false
        }

        override fun getAutofillOptions(): Array<out CharSequence?>? {
            return adapter.autofillOptions
        }
    }
}
