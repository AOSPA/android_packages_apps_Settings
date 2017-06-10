/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.batterylight;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.android.settings.R;

public class BatteryLightPreference extends Preference {

    private static String TAG = "BatteryLightPreference";
    private static final int DEFAULT_COLOR = 0xFFFFFF;

    private int mColorValue;

    private Context mContext;
    private BatteryLightDialog mDialog;
    private ImageView mLightColorView;

    public BatteryLightPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, DEFAULT_COLOR);
    }

    public BatteryLightPreference(Context context, int color) {
        super(context, null);
        init(context, color);
    }

    private void init(Context context, int color) {
        setLayoutResource(R.layout.preference_battery_light);
        mColorValue = color;
        mContext = context;
    }

    public void setColor(int color) {
        mColorValue = color;
        updatePreferenceViews();
    }

    public int getColor() {
        return mColorValue;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mLightColorView = (ImageView) holder.findViewById(R.id.light_color);
        updatePreferenceViews();
    }

    private void updatePreferenceViews() {
        final int size = (int) mContext.getResources().getDimension(R.dimen.oval_notification_size);

        if (mLightColorView != null) {
            mLightColorView.setEnabled(true);
            mLightColorView.setImageDrawable(createOvalShape(size, 0xFF000000 | mColorValue));
        }
    }

    @Override
    protected void onClick() {
        mDialog = getDialog();
        mDialog.setSelectedColor(0xFF000000 | mColorValue);
        mDialog.show();
    }

    private BatteryLightDialog getDialog() {
        if (mDialog == null) {
            mDialog = new BatteryLightDialog(mContext);
            mDialog.setOnCancelListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDialog.dismiss();
                }
            });
            mDialog.setOnOkListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setColor(mDialog.getSelectedColor() & 0x00FFFFFF);
                    callChangeListener(this);
                    mDialog.dismiss();
                }
            });
            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface d) {
                    mDialog = null;
                }
            });
        }    
        return mDialog;
    }

    private static ShapeDrawable createOvalShape(int size, int color) {
        ShapeDrawable shape = new ShapeDrawable(new OvalShape());
        shape.setIntrinsicHeight(size);
        shape.setIntrinsicWidth(size);
        shape.getPaint().setColor(color);
        return shape;
    }
}
