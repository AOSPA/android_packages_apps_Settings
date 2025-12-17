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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.fragment.app.Fragment
import com.android.settings.R

/** The Quick Settings fragment for the safety center. */
class SafetyCenterQsFragment : Fragment() {

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

        childFragmentManager
            .beginTransaction()
            .add(R.id.safety_center_prefs, SafetyCenterFragment.newInstance(isQuickSettings = true))
            .commitNow()
        return root
    }
}
