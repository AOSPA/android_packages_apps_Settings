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
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

public class NotificationLightPreference extends Preference implements DialogInterface.OnDismissListener,
            View.OnLongClickListener {

    private static String TAG = "NotificationLightPreference";
    public static final int DEFAULT_TIME = 1000;
    public static final int DEFAULT_COLOR = 0xFFFFFF; //White

    private ImageView mLightColorView;
    private TextView mOnValueView;
    private TextView mOffValueView;
    private Resources mResources;
    private int mColorValue;
    private Dialog mDialog;

    private int mOnValue;
    private int mOffValue;
    private boolean mOnOffChangeable;

    public interface ItemLongClickListener {
        public boolean onItemLongClick(String key);
    }

    private ItemLongClickListener mLongClickListener;

    /**
     * @param context
     * @param attrs
     */
    public NotificationLightPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mColorValue = DEFAULT_COLOR;
        mOnValue = DEFAULT_TIME;
        mOffValue = DEFAULT_TIME;
        mOnOffChangeable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_ledCanPulse);
        init();
    }

    /**
     * @param context
     * @param color
     * @param onValue
     * @param offValue
     */
    public NotificationLightPreference(Context context, int color, int onValue, int offValue) {
        super(context, null);
        mColorValue = color;
        mOnValue = onValue;
        mOffValue = offValue;
        mOnOffChangeable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_ledCanPulse);
        init();
    }

    /**
     * @param context
     * @param color
     * @param onValue
     * @param offValue
     */
    public NotificationLightPreference(Context context, int color, int onValue, int offValue, boolean onOffChangeable) {
        super(context, null);
        mColorValue = color;
        mOnValue = onValue;
        mOffValue = offValue;
        mOnOffChangeable = onOffChangeable;
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_notification_light);
        mResources = getContext().getResources();
    }

    public void setColor(int color) {
        mColorValue = color;
        updatePreferenceViews();
    }

    public int getColor() {
        return mColorValue;
    }

    public void onStart() {
        NotificationLightDialog d = (NotificationLightDialog) getDialog();
        if (d != null) {
            d.onStart();
        }
    }

    public void onStop() {
        NotificationLightDialog d = (NotificationLightDialog) getDialog();
        if (d != null) {
            d.onStop();
        }
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

        if (!getContext().getResources().getBoolean(com.android.internal.R.bool.config_multiColorNotificationLed)) {
            mLightColorView.setVisibility(View.GONE);
        }

        updatePreferenceViews();
        holder.itemView.setOnLongClickListener(this);
    }

    private void updatePreferenceViews() {
        final int size = (int) getContext().getResources().getDimension(R.dimen.oval_notification_size);

        if (mLightColorView != null) {
            mLightColorView.setEnabled(true);
            final int imageColor = ((mColorValue & 0xF0F0F0) == 0xF0F0F0) ?
                    (mColorValue - 0x101010) : mColorValue;
            mLightColorView.setImageDrawable(createOvalShape(size, 0xFF000000 + imageColor));
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
        if (mDialog != null && mDialog.isShowing()) return;
        mDialog = getDialog();
        mDialog.setOnDismissListener(this);
        mDialog.show();
    }

    public Dialog getDialog() {
        final NotificationLightDialog d = new NotificationLightDialog(getContext(),
                0xFF000000 + mColorValue, mOnValue, mOffValue, mOnOffChangeable); 

        d.setButton(AlertDialog.BUTTON_POSITIVE, mResources.getString(R.string.ok),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mColorValue =  d.getColor() & 0x00FFFFFF; // strip alpha, led does not support it
                mOnValue = d.getPulseSpeedOn();
                mOffValue = d.getPulseSpeedOff();
                updatePreferenceViews();
                callChangeListener(this);
            }
        });
        d.setButton(AlertDialog.BUTTON_NEGATIVE, mResources.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

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
            return getContext().getResources().getString(R.string.pulse_length_always_on);
        }
        if (time == DEFAULT_TIME) {
            return getContext().getResources().getString(R.string.default_time);
        }

        String[] timeNames = getContext().getResources().getStringArray(R.array.notification_pulse_length_entries);
        String[] timeValues = getContext().getResources().getStringArray(R.array.notification_pulse_length_values);

        for (int i = 0; i < timeValues.length; i++) {
            if (Integer.decode(timeValues[i]).equals(time)) {
                return timeNames[i];
            }
        }

        return getContext().getResources().getString(R.string.custom_time);
    }

    private String mapSpeedValue(Integer time) {
        if (time == DEFAULT_TIME) {
            return getContext().getResources().getString(R.string.default_time);
        }

        String[] timeNames = getContext().getResources().getStringArray(R.array.notification_pulse_speed_entries);
        String[] timeValues = getContext().getResources().getStringArray(R.array.notification_pulse_speed_values);

        for (int i = 0; i < timeValues.length; i++) {
            if (Integer.decode(timeValues[i]).equals(time)) {
                return timeNames[i];
            }
        }

        return getContext().getResources().getString(R.string.custom_time);
    }

}
