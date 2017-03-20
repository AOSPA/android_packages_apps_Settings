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

import android.app.AlertDialog;
import android.content.Context;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.android.settings.R;
import com.android.settingslib.ColorPickerDialog;
import com.android.settingslib.ColorPickerDialogAdapter;

public class BatteryLightPreference extends Preference implements DialogInterface.OnDismissListener {

    private static String TAG = "BatteryLightPreference";
    public static final int DEFAULT_COLOR = 0xFFFFFF; //White

    private ImageView mLightColorView;
    private Resources mResources;
    private int mColorValue;
    private Dialog mDialog;

    /**
     * @param context
     * @param attrs
     */
    public BatteryLightPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mColorValue = DEFAULT_COLOR;
        init();
    }

    public BatteryLightPreference(Context context, int color) {
        super(context, null);
        mColorValue = color;
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_battery_light);
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
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mLightColorView = (ImageView) holder.findViewById(R.id.light_color);

        updatePreferenceViews();
    }

    private void updatePreferenceViews() {
        final int size = (int) getContext().getResources().getDimension(R.dimen.oval_notification_size);

        if (mLightColorView != null) {
            mLightColorView.setEnabled(true);
            final int imageColor = ((mColorValue & 0xF0F0F0) == 0xF0F0F0) ?
                    (mColorValue - 0x101010) : mColorValue;
            mLightColorView.setImageDrawable(createOvalShape(size, 0xFF000000 + imageColor));
        }
    }

    @Override
    protected void onClick() {
        if (mDialog != null && mDialog.isShowing()) return;
        mDialog = getDialog();
        mDialog.show();
    }

    public Dialog getDialog() {
        final int[] colors = mResources.getIntArray(
                R.array.led_color_picker_dialog_colors);
        final ColorPickerDialog dialog = new ColorPickerDialog(getContext());
        final ColorPickerDialogAdapter adapter = dialog.getAdapter();

        adapter.setColors(colors);
        adapter.setSelectedImageResourceId(R.drawable.ic_check_green_24dp);
        adapter.setSelectedImageColorFilter(Color.WHITE);
        adapter.setSelectedColor(getColor());

        dialog.setOnCancelListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.setOnOkListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mColorValue =  dialog.getSelectedColor();
                updatePreferenceViews();
                callChangeListener(this);
                dialog.dismiss();
            }
        });

        dialog.setOnColorSelectedListener(new ColorPickerDialog.OnColorSelectedListener() {
            @Override
            public void onColorSelected(DialogInterface d, int color) {
                if (adapter.getSelectedPosition() == 0) {
                    adapter.setSelectedImageColorFilter(Color.DKGRAY);
                } else {
                    adapter.setSelectedImageColorFilter(Color.WHITE);
                }
            }
        });

        return dialog;
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
