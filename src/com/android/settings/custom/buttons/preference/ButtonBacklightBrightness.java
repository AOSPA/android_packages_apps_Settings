/*
 * Copyright (C) 2013 The CyanogenMod Project
 *               2017 The LineageOS Project
 *               2018 The PixelExperience Project
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

package com.android.settings.custom.buttons.preference;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.android.settings.R;

import static com.android.internal.util.aospa.hwkeys.DeviceKeysConstants.*;

import com.android.settings.custom.preference.CustomDialogPreference;

public class ButtonBacklightBrightness extends CustomDialogPreference<AlertDialog> implements
        SeekBar.OnSeekBarChangeListener {
    private static final int DEFAULT_BUTTON_TIMEOUT = 5;

    public static final String KEY_BUTTON_BACKLIGHT = "pre_navbar_button_backlight";

    private ButtonBrightnessControl mButtonBrightness;
    private BrightnessControl mActiveControl;

    private ViewGroup mTimeoutContainer;
    private SeekBar mTimeoutBar;
    private TextView mTimeoutValue;

    private ContentResolver mResolver;

    private int mOriginalTimeout;

    public ButtonBacklightBrightness(Context context, AttributeSet attrs) {
        super(context, attrs);

        mResolver = context.getContentResolver();

        setDialogLayoutResource(R.layout.button_backlight);

        if (isButtonSupported()) {
            boolean isSingleValue = !context.getResources().getBoolean(
                    com.android.internal.R.bool.config_deviceHasVariableButtonBrightness);

            int defaultBrightness = context.getResources().getInteger(
                    com.android.internal.R.integer.config_buttonBrightnessSettingDefault);

            mButtonBrightness = new ButtonBrightnessControl(
                    Settings.System.BUTTON_BRIGHTNESS,
                    Settings.System.BUTTON_BACKLIGHT_ONLY_WHEN_PRESSED,
                    isSingleValue, defaultBrightness);
            mActiveControl = mButtonBrightness;
        }

        updateSummary();
    }

    @Override
    protected void onClick(AlertDialog d, int which) {
        super.onClick(d, which);

        updateBrightnessPreview();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener listener) {
        super.onPrepareDialogBuilder(builder, listener);
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setNeutralButton(R.string.reset,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    @Override
    protected boolean onDismissDialog(AlertDialog dialog, int which) {
        if (which == DialogInterface.BUTTON_NEUTRAL) {
            mTimeoutBar.setProgress(DEFAULT_BUTTON_TIMEOUT);
            applyTimeout(DEFAULT_BUTTON_TIMEOUT);
            if (mButtonBrightness != null) {
                mButtonBrightness.reset();
            }
            return false;
        }
        return true;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mTimeoutContainer = (ViewGroup) view.findViewById(R.id.timeout_container);
        mTimeoutBar = (SeekBar) view.findViewById(R.id.timeout_seekbar);
        mTimeoutValue = (TextView) view.findViewById(R.id.timeout_value);
        mTimeoutBar.setMax(30);
        mTimeoutBar.setOnSeekBarChangeListener(this);
        mOriginalTimeout = getTimeout();
        mTimeoutBar.setProgress(mOriginalTimeout);
        handleTimeoutUpdate(mTimeoutBar.getProgress());

        ViewGroup buttonContainer = (ViewGroup) view.findViewById(R.id.button_container);
        if (mButtonBrightness != null) {
            mButtonBrightness.init(buttonContainer);
        } else {
            buttonContainer.setVisibility(View.GONE);
            mTimeoutContainer.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (!positiveResult) {
            applyTimeout(mOriginalTimeout);
            return;
        }

        if (mButtonBrightness != null) {
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .edit()
                    .putInt(KEY_BUTTON_BACKLIGHT, mButtonBrightness.getBrightness(false))
                    .apply();
        }

        applyTimeout(mTimeoutBar.getProgress());
        if (mButtonBrightness != null) {
            mButtonBrightness.applyBrightness();
        }

        updateSummary();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) {
            return superState;
        }

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.timeout = mTimeoutBar.getProgress();
        if (mButtonBrightness != null) {
            myState.button = mButtonBrightness.getBrightness(false);
        }

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        mTimeoutBar.setProgress(myState.timeout);
        if (mButtonBrightness != null) {
            mButtonBrightness.setBrightness(myState.button);
        }
    }

    public boolean isButtonSupported() {
        final Resources res = getContext().getResources();
        final int deviceKeys = res.getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        // All hardware keys besides volume and camera can possibly have a backlight
        boolean hasBacklightKey = (deviceKeys & KEY_MASK_HOME) != 0
                || (deviceKeys & KEY_MASK_BACK) != 0
                || (deviceKeys & KEY_MASK_MENU) != 0
                || (deviceKeys & KEY_MASK_ASSIST) != 0
                || (deviceKeys & KEY_MASK_APP_SWITCH) != 0;
        boolean hasBacklight = res.getInteger(
                com.android.internal.R.integer.config_buttonBrightnessSettingDefault) > 0;

        return hasBacklightKey && hasBacklight;
    }

    public void updateSummary() {
        if (mButtonBrightness != null) {
            int buttonBrightness = mButtonBrightness.getBrightness(true);
            int timeout = getTimeout();

            if (buttonBrightness == 0) {
                setSummary(R.string.backlight_summary_disabled);
            } else if (timeout == 0) {
                setSummary(R.string.backlight_timeout_unlimited);
            } else {
                setSummary(getContext().getString(R.string.backlight_summary_enabled_with_timeout,
                        getTimeoutString(timeout)));
            }
        } else {
            setSummary(R.string.backlight_summary_disabled);
        }
    }

    private String getTimeoutString(int timeout) {
        return getContext().getResources().getQuantityString(
                R.plurals.backlight_timeout_time, timeout, timeout);
    }

    private int getTimeout() {
        return Settings.System.getInt(mResolver,
                Settings.System.BUTTON_BACKLIGHT_TIMEOUT, DEFAULT_BUTTON_TIMEOUT * 1000) / 1000;
    }

    private void applyTimeout(int timeout) {
        Settings.System.putInt(mResolver,
                Settings.System.BUTTON_BACKLIGHT_TIMEOUT, timeout * 1000);
    }

    private void updateBrightnessPreview() {
        if (getDialog() == null || getDialog().getWindow() == null) {
            return;
        }
        Window window = getDialog().getWindow();
        LayoutParams params = window.getAttributes();
        if (mActiveControl != null) {
            params.buttonBrightness = (float) mActiveControl.getBrightness(false) / 255.0f;
        } else {
            params.buttonBrightness = -1;
        }
        window.setAttributes(params);
    }

    private void updateTimeoutEnabledState() {
        int buttonBrightness = mButtonBrightness != null
                ? mButtonBrightness.getBrightness(false) : 0;
        int count = mTimeoutContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            mTimeoutContainer.getChildAt(i).setEnabled(buttonBrightness != 0);
        }
    }

    private void handleTimeoutUpdate(int timeout) {
        if (timeout == 0) {
            mTimeoutValue.setText(R.string.backlight_timeout_unlimited);
        } else {
            mTimeoutValue.setText(getTimeoutString(timeout));
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        handleTimeoutUpdate(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Do nothing here
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        applyTimeout(seekBar.getProgress());
    }

    private static class SavedState extends BaseSavedState {
        int timeout;
        int button;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            timeout = source.readInt();
            button = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(timeout);
            dest.writeInt(button);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private class BrightnessControl implements
            SeekBar.OnSeekBarChangeListener, CheckBox.OnCheckedChangeListener {
        private String mSetting;
        private boolean mIsSingleValue;
        private int mDefaultBrightness;
        private CheckBox mCheckBox;
        private SeekBar mSeekBar;
        private TextView mValue;

        public BrightnessControl(String setting, boolean singleValue, int defaultBrightness) {
            mSetting = setting;
            mIsSingleValue = singleValue;
            mDefaultBrightness = defaultBrightness;
        }

        public BrightnessControl(String setting, boolean singleValue) {
            this(setting, singleValue, 255);
        }

        public void init(ViewGroup container) {
            int brightness = getBrightness(true);

            if (mIsSingleValue) {
                container.findViewById(R.id.seekbar_container).setVisibility(View.GONE);
                mCheckBox = (CheckBox) container.findViewById(R.id.backlight_switch);
                mCheckBox.setChecked(brightness != 0);
                mCheckBox.setOnCheckedChangeListener(this);
            } else {
                container.findViewById(R.id.checkbox_container).setVisibility(View.GONE);
                mSeekBar = (SeekBar) container.findViewById(R.id.seekbar);
                mValue = (TextView) container.findViewById(R.id.value);

                mSeekBar.setMax(255);
                mSeekBar.setProgress(brightness);
                mSeekBar.setOnSeekBarChangeListener(this);
            }

            handleBrightnessUpdate(brightness);
        }

        public int getBrightness(boolean persisted) {
            if (mCheckBox != null && !persisted) {
                return mCheckBox.isChecked() ? mDefaultBrightness : 0;
            } else if (mSeekBar != null && !persisted) {
                return mSeekBar.getProgress();
            }
            return Settings.System.getInt(mResolver, mSetting, mDefaultBrightness);
        }

        public void applyBrightness() {
            Settings.System.putInt(mResolver, mSetting, getBrightness(false));
        }

        /* Behaviors when it's a seekbar */
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            handleBrightnessUpdate(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mActiveControl = this;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Do nothing here
        }

        /* Behaviors when it's a plain checkbox */
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mActiveControl = this;
            updateBrightnessPreview();
            updateTimeoutEnabledState();
        }

        public void setBrightness(int value) {
            if (mIsSingleValue) {
                mCheckBox.setChecked(value != 0);
            } else {
                mSeekBar.setProgress(value);
            }
        }

        public void reset() {
            setBrightness(mDefaultBrightness);
        }

        private void handleBrightnessUpdate(int brightness) {
            updateBrightnessPreview();
            if (mValue != null) {
                mValue.setText(String.format("%d%%", (int)((brightness * 100) / 255)));
            }
            updateTimeoutEnabledState();
        }
    }

    private class ButtonBrightnessControl extends BrightnessControl {
        private String mOnlyWhenPressedSetting;
        private CheckBox mOnlyWhenPressedCheckBox;

        public ButtonBrightnessControl(String brightnessSetting, String onlyWhenPressedSetting,
                boolean singleValue, int defaultBrightness) {
            super(brightnessSetting, singleValue, defaultBrightness);
            mOnlyWhenPressedSetting = onlyWhenPressedSetting;
        }

        @Override
        public void init(ViewGroup container) {
            super.init(container);

            mOnlyWhenPressedCheckBox =
                    (CheckBox) container.findViewById(R.id.backlight_only_when_pressed_switch);
            mOnlyWhenPressedCheckBox.setChecked(isOnlyWhenPressedEnabled());
            mOnlyWhenPressedCheckBox.setOnCheckedChangeListener(this);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            super.onCheckedChanged(buttonView, isChecked);
            setOnlyWhenPressedEnabled(mOnlyWhenPressedCheckBox.isChecked());
        }

        public boolean isOnlyWhenPressedEnabled() {
            return Settings.System.getInt(mResolver, mOnlyWhenPressedSetting, 0) == 1;
        }

        public void setOnlyWhenPressedEnabled(boolean enabled) {
            Settings.System.putInt(mResolver, mOnlyWhenPressedSetting, enabled ? 1 : 0);
        }
    }
}
