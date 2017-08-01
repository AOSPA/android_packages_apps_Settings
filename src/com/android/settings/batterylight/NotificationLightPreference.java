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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.util.pa.ColorUtils;
import com.android.settings.R;

public class NotificationLightPreference extends Preference
        implements View.OnLongClickListener {

    private static String TAG = "NotificationLightPreference";
    public static final int DEFAULT_COLOR = 0xFFFFFF;
    public static final int DEFAULT_TIME = 1000;

    private boolean mOnOffChangeable;
    private int mColorValue;
    private int mOffValue;
    private int mOnValue;

    private Context mContext;
    private NotificationLightDialog mDialog;
    private Resources mResources;

    private ImageView mLightColorView;
    private TextView mOnValueView;
    private TextView mOffValueView;

    public interface ItemLongClickListener {
        public boolean onItemLongClick(String key);
    }

    private ItemLongClickListener mLongClickListener;

    public NotificationLightPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, DEFAULT_COLOR, DEFAULT_TIME, DEFAULT_TIME, context.getResources().getBoolean(
                com.android.internal.R.bool.config_ledCanPulse));
    }

    public NotificationLightPreference(Context context, int color, int onValue, int offValue) {
        super(context, null);
        init(context, color, onValue, offValue, context.getResources().getBoolean(
                com.android.internal.R.bool.config_ledCanPulse));
    }

    public NotificationLightPreference(Context context, int color, int onValue, int offValue, boolean onOffChangeable) {
        super(context, null);
        init(context, color, onValue, offValue, onOffChangeable);
    }

    private void init(Context context, int color, int onValue, int offValue, boolean onOffChangeable) {
        setLayoutResource(R.layout.preference_notification_light);
        mColorValue = color;
        mOnValue = onValue;
        mOffValue = offValue;
        mOnOffChangeable = onOffChangeable;

        mContext = context;
        mResources = context.getResources();
    }

    public void setColor(int color) {
        mColorValue = color;
        updatePreferenceViews();
    }

    public int getColor() {
        return mColorValue;
    }

    @Override
    public boolean onLongClick(View view) {
        if (mLongClickListener != null) {
            return mLongClickListener.onItemLongClick(getKey());
        }
        return false;
    }

    public void setOnLongClickListener(ItemLongClickListener l) {
        mLongClickListener = l;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mLightColorView = (ImageView) holder.findViewById(R.id.light_color);
        mOnValueView = (TextView) holder.findViewById(R.id.textViewTimeOnValue);
        mOffValueView = (TextView) holder.findViewById(R.id.textViewTimeOffValue);

        // Hide the summary text - it takes up too much space on a low res device
        // We use it for storing the package name for the longClickListener
        TextView tView = (TextView) holder.findViewById(android.R.id.summary);
        tView.setVisibility(View.GONE);

        if (!mResources.getBoolean(com.android.internal.R.bool.config_multiColorNotificationLed)) {
            mLightColorView.setVisibility(View.GONE);
        }

        if (!mResources.getBoolean(com.android.internal.R.bool.config_ledCanPulse)) {
            mOnValueView.setVisibility(View.GONE);
            mOffValueView.setVisibility(View.GONE);
        }

        updatePreferenceViews();
        holder.itemView.setOnLongClickListener(this);
    }

    private void updatePreferenceViews() {
        final int size = (int) mResources.getDimension(R.dimen.oval_notification_size);

        if (mLightColorView != null) {
            mLightColorView.setEnabled(true);
            mLightColorView.setImageDrawable(createOvalShape(size, 0xFF000000 | mColorValue));
        }

        if (mOnValueView != null) {
            mOnValueView.setText(mapLengthValue(mOnValue));
        }

        if (mOffValueView != null) {
            if (mOnValue == 1 || !mOnOffChangeable) {
                mOffValueView.setEnabled(false);
            } else {
                mOffValueView.setEnabled(true);
            }
            mOffValueView.setText(mapSpeedValue(mOffValue));
        }
    }

    @Override
    protected void onClick() {
        mDialog = getDialog();

        int[] colors = mDialog.getAdapter().getColors();
        final int initialPackageColor = getInitialColor();

        // Check if the set package color is still allowed. A color
        // is allowed if is selectable in the color picker dialog. Those
        // includes all the RGB colors, and the latest color extracted from
        // the application icon. Now it may happen that the icon and hereby the
        // color changes, in that case the set color is not a color that
        // can be selected in the dialog, for these we override with the
        // new application icon color.
        boolean setPackageColorAllowed = mColorValue == initialPackageColor;
        for(int i = 0; !setPackageColorAllowed && i < colors.length - 1; i++) {
            setPackageColorAllowed = (colors[i] == (0xFF000000 | mColorValue));
        }

        if (!setPackageColorAllowed) { // Happens when the application icon changes
            mColorValue = initialPackageColor;
            updatePreferenceViews();
        }

        // Inject the initial package color
        colors[colors.length - 1] = initialPackageColor;
        mDialog.setColors(colors);
        mDialog.setSelectedColor(0xFF000000 | mColorValue);
        mDialog.show();
    }

    public NotificationLightDialog getDialog() {
        if (mDialog == null) {
            mDialog = new NotificationLightDialog(mContext,
                    mOnValue, mOffValue, mOnOffChangeable);
            mDialog.setOnCancelListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDialog.dismiss();
                }
            });
            mDialog.setOnOkListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mColorValue = mDialog.getSelectedColor() & 0x00FFFFFF;
                    mOnValue = mDialog.getPulseSpeedOn();
                    mOffValue = mDialog.getPulseSpeedOff();
                    updatePreferenceViews();
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

    private int getInitialColor() {
        int color = DEFAULT_COLOR;
        String packageName = getKey();

        try {
            Drawable icon = mContext.getPackageManager()
                    .getApplicationIcon(packageName);

            color = ColorUtils.getIconColorFromDrawable(icon);
        } catch (NameNotFoundException e) {
            // Shouldn't happen, but just return default
        }

        return color;
    }

    private static ShapeDrawable createOvalShape(int size, int color) {
        ShapeDrawable shape = new ShapeDrawable(new OvalShape());
        shape.setIntrinsicHeight(size);
        shape.setIntrinsicWidth(size);
        shape.getPaint().setColor(color);
        return shape;
    }

    public void setAllValues(int color, int onValue, int offValue) {
        mColorValue = color;
        mOnValue = onValue;
        mOffValue = offValue;
        updatePreferenceViews();
    }

    public void setOnValue(int value) {
        mOnValue = value;
        updatePreferenceViews();
    }

    public int getOnValue() {
        return mOnValue;
    }

    public void setOffValue(int value) {
        mOffValue = value;
        updatePreferenceViews();
    }

    public int getOffValue() {
        return mOffValue;
    }

    private String mapLengthValue(Integer time) {
        if (!mOnOffChangeable) {
            return mResources.getString(R.string.pulse_length_always_on);
        }
        if (time == DEFAULT_TIME) {
            return mResources.getString(R.string.default_time);
        }

        String[] timeNames = mResources.getStringArray(R.array.notification_pulse_length_entries);
        String[] timeValues = mResources.getStringArray(R.array.notification_pulse_length_values);

        for (int i = 0; i < timeValues.length; i++) {
            if (Integer.decode(timeValues[i]).equals(time)) {
                return timeNames[i];
            }
        }

        return mResources.getString(R.string.custom_time);
    }

    private String mapSpeedValue(Integer time) {
        if (time == DEFAULT_TIME) {
            return mResources.getString(R.string.default_time);
        }

        String[] timeNames = mResources.getStringArray(R.array.notification_pulse_speed_entries);
        String[] timeValues = mResources.getStringArray(R.array.notification_pulse_speed_values);

        for (int i = 0; i < timeValues.length; i++) {
            if (Integer.decode(timeValues[i]).equals(time)) {
                return timeNames[i];
            }
        }

        return mResources.getString(R.string.custom_time);
    }

}
