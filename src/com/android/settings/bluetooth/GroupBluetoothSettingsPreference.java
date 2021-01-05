/******************************************************************************
 *  Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *      * Neither the name of The Linux Foundation nor the names of its
 *        contributors may be used to endorse or promote products derived
 *        from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/

package com.android.settings.bluetooth;

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import com.android.settings.bluetooth.BluetoothDevicePreference.SortType;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.GearPreference;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import com.android.settings.R;


public class GroupBluetoothSettingsPreference extends GearPreference {

    private static final String TAG = "GroupBluetoothSettingsPreference";

    private static int sDimAlpha = Integer.MIN_VALUE;

    private int mGroupId = -1;

    private final UserManager mUserManager;

    private String contentDescription = null;
    private boolean mHideSecondTarget = false;
    private Resources mResources;
    private int mVisibleCount  = 0;

    public GroupBluetoothSettingsPreference(Context context, int groupId) {
        super(context, null);
        mGroupId = groupId;
        mResources = getContext().getResources();
        mUserManager = context.getSystemService(UserManager.class);
        if (sDimAlpha == Integer.MIN_VALUE) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, outValue, true);
            sDimAlpha = (int) (outValue.getFloat() * 255);
        }
        mVisibleCount = 0;
        onDeviceAttributesChanged();
    }

    @Override
    protected boolean shouldHideSecondTarget() {
        return mUserManager.hasUserRestriction(DISALLOW_CONFIG_BLUETOOTH)
                || mHideSecondTarget;
    }

    public void hideSecondTarget(boolean hideSecondTarget) {
        mHideSecondTarget = hideSecondTarget;
    }

    public void onDeviceAttributesChanged() {
        /*
         * The preference framework takes care of making sure the value has
         * changed before proceeding. It will also call notifyChanged() if
         * any preference info has changed from the previous value.
         */
        Context context = getContext();
        String title  = context.getString(R.string.group_settings);
        setTitle(title + " " + (mGroupId + GroupUtils.GROUP_START_VAL));

        // Used to gray out the item
        setEnabled(true); // Change dynamically if required

        setVisible(true); // Change to dynamic if required
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        // Disable this view if the bluetooth enable/disable preference view is off
        if (null != findPreferenceInHierarchy("bt_checkbox")) {
            setDependency("bt_checkbox");
        }

            ImageView deviceDetails = (ImageView) view.findViewById(R.id.settings_button);

            if (deviceDetails != null) {
                deviceDetails.setOnClickListener(this);
            }
        final ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        if (imageView != null) {
            imageView.setContentDescription(contentDescription);
            // Set property to prevent Talkback from reading out.
            imageView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            imageView.setVisibility(ImageView.VISIBLE);
        }
        super.onBindViewHolder(view);
    }


    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_gear;
    }

    public int getGroupId() {
        return mGroupId;
    }

    void onClicked() {
    }

    public int getVisibleCount() {
        return mVisibleCount;
    }

    public int incrementChildCound() {
        return ++mVisibleCount;
    }

    public int decrementChildCount() {
        return --mVisibleCount;
    }

    public boolean isRemovePref() {
        if (mVisibleCount == 0) {
            return true;
        } else {
            return  false;
        }
    }
}
