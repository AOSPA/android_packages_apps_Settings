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

package com.android.settings.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.preferences.ColorPickerDialog;

public class ColorPickerPreference extends Preference implements DialogInterface.OnDismissListener {

    private static String TAG = "ColorPickerPreference";
    public static final int DEFAULT_COLOR = 0xFFFFFFFF; //White

    private ImageView mLightColorView;
    private Resources mResources;
    private int mColorValue;
    private Dialog mDialog;

    /**
     * @param context
     * @param attrs
     */
    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mColorValue = DEFAULT_COLOR;
        init();
    }

    public ColorPickerPreference(Context context, int color) {
        super(context, null);
        mColorValue = color;
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_color_picker);
        mResources = getContext().getResources();
    }

    public void setColor(int color) {
        mColorValue = color;
        updatePreferenceViews();
    }

    public int getColor() {
        return mColorValue;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mLightColorView = (ImageView) view.findViewById(R.id.light_color);

        updatePreferenceViews();
    }

    private void updatePreferenceViews() {
        final int size = (int) getContext().getResources().getDimension(R.dimen.oval_notification_size);

        if (mLightColorView != null) {
            mLightColorView.setEnabled(true);
            mLightColorView.setImageDrawable(createOvalShape(size, 0xFF000000 | mColorValue));
        }
    }

    @Override
    protected void onClick() {
        if (mDialog != null && mDialog.isShowing()) return;
        mDialog = getDialog();
        mDialog.setOnDismissListener(this);
        mDialog.show();
    }

    public Dialog getDialog() {
        final ColorPickerDialog d = new ColorPickerDialog(getContext(), mColorValue, true);

        d.setButton(AlertDialog.BUTTON_POSITIVE, mResources.getString(R.string.ok),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mColorValue =  d.getColor();
                updatePreferenceViews();
                callChangeListener(this);
            }
        });
        d.setButton(AlertDialog.BUTTON_NEGATIVE, mResources.getString(R.string.cancel),
                (DialogInterface.OnClickListener) null);

        return d;
    }

    private static ShapeDrawable createOvalShape(int size, int color) {
        ShapeDrawable shape = new ShapeDrawable(new OvalShape());
        shape.setIntrinsicHeight(size);
        shape.setIntrinsicWidth(size);
        shape.getPaint().setColor(color);
        return shape;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mDialog = null;
    }
}
