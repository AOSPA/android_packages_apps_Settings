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

package com.android.settings.supervision

import android.app.Activity
import android.app.supervision.SupervisionManager
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.setFragmentResult
import com.android.settings.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ApprovalMethodChooserDialogFragment : BottomSheetDialogFragment() {

    private var approvalMethods: ArrayList<ResolveInfo>? = null
    private var extras: Bundle? = null

    override fun getTheme(): Int = R.style.SupervisionChooserDialogStyle

    private val approvalMethodLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val resultBundle =
                Bundle().apply {
                    putInt(BUNDLE_KEY_RESULT_CODE, result.resultCode)
                    result.data?.let { putParcelable(BUNDLE_KEY_RESULT_DATA, it) }
                }
            setFragmentResult(REQUEST_KEY_APPROVAL_RESULT, resultBundle)
            dismiss()
        }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        setFragmentResult(
            REQUEST_KEY_APPROVAL_RESULT,
            Bundle().apply { putInt(BUNDLE_KEY_RESULT_CODE, Activity.RESULT_CANCELED) },
        )
        dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            approvalMethods =
                ArrayList(it.getParcelableArrayList(ARG_APPROVAL_METHODS) ?: emptyList())
            extras = it.getBundle(ARG_EXTRAS)
        }
        if (approvalMethods.isNullOrEmpty()) {
            setFragmentResult(
                REQUEST_KEY_APPROVAL_RESULT,
                Bundle().apply { putInt(BUNDLE_KEY_RESULT_CODE, Activity.RESULT_CANCELED) },
            )
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view =
            inflater.inflate(R.layout.supervision_approval_method_chooser_dialog, container, false)
        val container = view.findViewById<LinearLayout>(R.id.supervision_approval_methods_container)
        val packageManager = requireContext().packageManager

        approvalMethods?.forEachIndexed { index, resolveInfo ->
            val itemView =
                layoutInflater.inflate(R.layout.supervision_approval_method_item, container, false)
            val icon = itemView.findViewById<ImageView>(R.id.supervision_approval_method_icon)
            val label = itemView.findViewById<TextView>(R.id.supervision_approval_method_label)

            icon.setImageDrawable(resolveInfo.loadIcon(packageManager))
            label.text = resolveInfo.loadLabel(packageManager)

            val backgroundRes =
                when {
                    index == 0 -> R.drawable.supervision_approval_method_item_background_top
                    index == approvalMethods!!.size - 1 ->
                        R.drawable.supervision_approval_method_item_background_bottom
                    else -> R.drawable.supervision_approval_method_item_background_middle
                }
            itemView.setBackgroundResource(backgroundRes)

            itemView.setOnClickListener {
                val activityInfo = resolveInfo.activityInfo
                val intent =
                    Intent(SupervisionManager.ACTION_CONFIRM_SUPERVISION_APPROVAL).apply {
                        component = ComponentName(activityInfo.packageName, activityInfo.name)
                        extras?.let { putExtras(it) }
                    }
                approvalMethodLauncher.launch(intent)
            }
            container.addView(itemView)
        }
        return view
    }

    companion object {
        private const val ARG_APPROVAL_METHODS = "approval_methods"
        private const val ARG_EXTRAS = "extras"

        // Keys for the Fragment Result API
        const val REQUEST_KEY_APPROVAL_RESULT = "auth_method_chooser_result"
        const val BUNDLE_KEY_RESULT_CODE = "result_code"
        const val BUNDLE_KEY_RESULT_DATA = "result_data"

        fun newInstance(
            authMethods: List<ResolveInfo>,
            extras: Bundle? = null,
        ): ApprovalMethodChooserDialogFragment {
            val fragment = ApprovalMethodChooserDialogFragment()
            val args = Bundle()
            args.putParcelableArrayList(ARG_APPROVAL_METHODS, ArrayList(authMethods))
            args.putBundle(ARG_EXTRAS, extras)
            fragment.arguments = args
            return fragment
        }
    }
}
