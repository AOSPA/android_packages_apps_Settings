/**
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings.applications;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;

import java.util.List;

public class AppOpsDetails extends Fragment {
    public static final String ARG_PACKAGE_NAME = "package";

    private static final String TAG = "AppOpsDetails";

    private AppOpsManager mAppOps;
    private LayoutInflater mInflater;
    private LinearLayout mOperationsSection;
    private PackageInfo mPackageInfo;
    private PackageManager mPm;
    private View mRootView;
    private AppOpsState mState;

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);

        mAppOps = (AppOpsManager) getActivity()
                .getSystemService(Context.APP_OPS_SERVICE);
        mInflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mPm = getActivity().getPackageManager();
        mState = new AppOpsState(getActivity());

        final Bundle args = getArguments();

        String packageName = args == null ? null : args.getString(ARG_PACKAGE_NAME);
        if (packageName == null) {
            Intent intent = args == null ? null : (Intent) args.getParcelable("intent");
            if (intent == null) {
                intent = getActivity().getIntent();
            }
            if (intent != null) {
                packageName = intent.getData().getSchemeSpecificPart();
            }
        }

        try {
            mPackageInfo = mPm.getPackageInfo(packageName,
                    PackageManager.GET_DISABLED_COMPONENTS |
                    PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Unable to get package info for " + packageName, e);
            mPackageInfo = null;
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.app_ops_details, container, false);
        Utils.prepareCustomPreferencesList(container, view, view, false);

        mOperationsSection = (LinearLayout) view.findViewById(R.id.operations_section);
        return mRootView = view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPackageInfo == null) {
            ((SettingsActivity) getActivity()).finishPreferencePanel(this, Activity.RESULT_OK,
                    new Intent().putExtra(ManageApplications.APP_CHG, true));
            return;
        }

        final View appSnippet = mRootView.findViewById(R.id.app_snippet);
        appSnippet.setPaddingRelative(0, appSnippet.getPaddingTop(),
                0, appSnippet.getPaddingBottom());

        final ImageView icon = (ImageView) appSnippet.findViewById(R.id.app_icon);
        icon.setImageDrawable(mPm.getApplicationIcon(mPackageInfo.applicationInfo));
        final TextView label = (TextView) appSnippet.findViewById(R.id.app_name);
        label.setText(mPm.getApplicationLabel(mPackageInfo.applicationInfo));

        final TextView appVersion = (TextView) appSnippet.findViewById(R.id.app_size);
        appVersion.setVisibility(mPackageInfo.versionName == null ? View.INVISIBLE : View.VISIBLE);
        if (mPackageInfo.versionName != null) {
            appVersion.setText(getActivity().getString(R.string.version_text,
                    String.valueOf(mPackageInfo.versionName)));
        }

        mOperationsSection.removeAllViews();

        final Resources res = getActivity().getResources();

        String lastPermGroup = "";
        for (final AppOpsState.OpsTemplate tpl : AppOpsState.ALL_TEMPLATES) {
            for (final AppOpsState.AppOpEntry entry : mState.buildState(tpl,
                    mPackageInfo.applicationInfo.uid, mPackageInfo.packageName)) {
                final int firstOp = entry.getOpEntry(0).getOp();
                final String opPerm = AppOpsManager.opToPermission(firstOp);
                final int opSwitch = AppOpsManager.opToSwitch(firstOp);

                final View view = mInflater.inflate(R.layout.app_ops_details_item,
                        mOperationsSection, false);

                if (opPerm != null) {
                    try {
                        final PermissionInfo pi = mPm.getPermissionInfo(opPerm, 0);
                        if (pi.group != null && !lastPermGroup.equals(pi.group)) {
                            lastPermGroup = pi.group;

                            final PermissionGroupInfo pgi = mPm.getPermissionGroupInfo(pi.group, 0);
                            if (pgi.icon != 0) {
                                ((ImageView) view.findViewById(R.id.op_icon))
                                        .setImageDrawable(pgi.loadIcon(mPm));
                            }
                        }
                    } catch (NameNotFoundException e) {
                        Log.w(TAG, "Unable to get permission info for " + opPerm, e);
                    }
                }

                ((TextView) view.findViewById(R.id.op_name))
                        .setText(entry.getSwitchText(mState));
                ((TextView) view.findViewById(R.id.op_time))
                        .setText(entry.getTimeText(res, true));

                final Switch sw = (Switch) view.findViewById(R.id.switchWidget);
                sw.setChecked(mAppOps.checkOpNoThrow(opSwitch, entry.getPackageOps().getUid(),
                        entry.getPackageOps().getPackageName()) == AppOpsManager.MODE_ALLOWED);
                sw.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mAppOps.setMode(opSwitch,
                                entry.getPackageOps().getUid(),
                                entry.getPackageOps().getPackageName(),
                                isChecked ? AppOpsManager.MODE_ALLOWED :
                                        AppOpsManager.MODE_ERRORED);
                    }

                });

                mOperationsSection.addView(view);
            }
        }
    }
}
