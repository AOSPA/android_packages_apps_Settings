/**
 * Copyright (C) 2015-2017 Paranoid Android
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.ColorPickerDialog;
import com.android.settingslib.ColorPickerDialogAdapter;

import java.util.ArrayList;

public class NotificationLightDialog extends ColorPickerDialog
        implements ColorPickerDialog.OnColorSelectedListener {

    private final static boolean DEBUG = false;
    private final static int LAYOUT_INDEX = 1;
    private final static int LED_UPDATE_DELAY_MS = 250;
    private final static String STATE_KEY_COLOR = "NotificationLightDialog:color";
    private final static String TAG = "NotificationLightDialog";

    private boolean mOnOffChangeable;
    private int mSpeedOff;
    private int mSpeedOn;

    private ColorPickerDialogAdapter mAdapter;
    private Context mContext;
    private Handler mHandler;
    private NotificationManager mNotificationManager;
    private Resources mResources;

    private LayoutInflater mInflater;
    private Spinner mPulseSpeedOff;
    private Spinner mPulseSpeedOn;
    private boolean mLedCanPulse;
    private boolean mMultiColorLed;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationLightDialog.this.dismiss();
        }
    };

    public NotificationLightDialog(Context context, int initialSpeedOn,
            int initialSpeedOff) {
        super(context);
        init(context, initialSpeedOn, initialSpeedOff, true);
    }

    public NotificationLightDialog(Context context, int initialSpeedOn,
            int initialSpeedOff, boolean onOffChangeable) {
        super(context);
        init(context, initialSpeedOn, initialSpeedOff, onOffChangeable);
    }

    private void init(Context context, int speedOn, int speedOff,
            boolean onOffChangeable) {
        mSpeedOn = speedOn;
        mSpeedOff = speedOff;
        mOnOffChangeable = onOffChangeable;

        mContext = context;
        mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mResources = context.getResources();

        final int[] colors = mResources.getIntArray(
                R.array.led_color_picker_dialog_colors);

        mLedCanPulse = mResources.getBoolean(
                com.android.internal.R.bool.config_ledCanPulse);
        mMultiColorLed = mResources.getBoolean(
                com.android.internal.R.bool.config_multiColorNotificationLed);

        mAdapter = getAdapter();
        if (mMultiColorLed) {
            mAdapter.setColors(colors);
            mAdapter.setSelectedImageResourceId(R.drawable.ic_check_green_24dp);
            mAdapter.setSelectedImageColorFilter(Color.WHITE);
        } else {
            final ColorPickerDialog dialog = new ColorPickerDialog(context);
            dialog.setColumns(0);
            mAdapter.setRowHeight(0);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setOnColorSelectedListener(this);

        mInflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        TextView textView = (TextView) findViewById(R.id.text_primary);
        if (mLedCanPulse && mMultiColorLed) {
            textView.setText(mResources.getString(R.string.notification_light_dialog_text));
        } else if (!mLedCanPulse && mMultiColorLed) {
            textView.setText(mResources.getString(R.string.notification_light_dialog_text_multicolor));
        } else if (mLedCanPulse && !mMultiColorLed) {
            textView.setText(mResources.getString(R.string.notification_light_dialog_text_pulse));
        }

        LinearLayout layout = (LinearLayout) findViewById(R.id.layout_root);
        LinearLayout notificationSettingsLayout = (LinearLayout) mInflater
                .inflate(R.layout.dialog_notification_settings, layout, false);
        layout.addView(notificationSettingsLayout, LAYOUT_INDEX);

        mPulseSpeedOn = (Spinner) layout.findViewById(R.id.on_spinner);
        mPulseSpeedOff = (Spinner) layout.findViewById(R.id.off_spinner);

        if (mOnOffChangeable) {
            PulseSpeedAdapter pulseSpeedAdapter = new PulseSpeedAdapter(
                    R.array.notification_pulse_length_entries,
                    R.array.notification_pulse_length_values,
                    mSpeedOn);
            mPulseSpeedOn.setAdapter(pulseSpeedAdapter);
            mPulseSpeedOn.setSelection(pulseSpeedAdapter.getTimePosition(mSpeedOn));
            mPulseSpeedOn.setOnItemSelectedListener(mPulseSelectionListener);

            pulseSpeedAdapter = new PulseSpeedAdapter(R.array.notification_pulse_speed_entries,
                    R.array.notification_pulse_speed_values,
                    mSpeedOff);
            mPulseSpeedOff.setAdapter(pulseSpeedAdapter);
            mPulseSpeedOff.setSelection(pulseSpeedAdapter.getTimePosition(mSpeedOff));
            mPulseSpeedOff.setOnItemSelectedListener(mPulseSelectionListener);
        } else {
            notificationSettingsLayout.setVisibility(View.GONE);
        }

        mPulseSpeedOn.setEnabled(mOnOffChangeable);
        mPulseSpeedOff.setEnabled(mSpeedOn != 1 && mOnOffChangeable);
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt(STATE_KEY_COLOR, getSelectedColor());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        setSelectedColor(state.getInt(STATE_KEY_COLOR));
    }

    @Override
    public void onColorSelected(DialogInterface dialog, int color) {
        mAdapter.setSelectedImageColorFilter(
                mAdapter.getSelectedPosition() == 0 ? Color.DKGRAY : Color.WHITE);
        updateLed(LED_UPDATE_DELAY_MS);
    }

    @Override
    public void onStart() {
        mHandler = new Handler();
        mContext.registerReceiver(broadcastReceiver, new IntentFilter(TAG));
        updateLed(0);
    }

    @Override
    public void onStop() {
        mHandler.removeMessages(0);
        mNotificationManager.cancel(1);
        mContext.unregisterReceiver(broadcastReceiver);
    }

    private void updateLed(int delay) {
        if (mHandler.hasMessages(0)) {
            return;
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run () {
                final boolean pulseEnabled = mPulseSpeedOn.isEnabled();

                final Bundle b = new Bundle();
                b.putBoolean(Notification.EXTRA_FORCE_SHOW_LIGHTS, true);

                final Notification.Builder builder = new Notification.Builder(mContext);
                builder.setLights(getSelectedColor(),
                        pulseEnabled ? getPulseSpeedOn() : 1,
                        pulseEnabled ? getPulseSpeedOff() : 0);
                builder.setExtras(b);

                builder.setSmallIcon(R.drawable.ic_settings_leds);
                builder.setContentTitle(mResources.getString(R.string.notification_light_settings));
                builder.setContentText(mResources.getString(R.string.led_notification_text));
                builder.setOngoing(false);

                final PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0,
                        new Intent(TAG), 0);

                builder.addAction(new Notification.Action.Builder(
                        Icon.createWithResource(mContext, R.drawable.ic_cancel),
                        mResources.getString(R.string.cancel),
                        pendingIntent).build());
                builder.setDeleteIntent(pendingIntent);
                builder.setPriority(Notification.PRIORITY_MAX);
                builder.setWhen(0);

                if (DEBUG) {
                    Log.i(TAG, "onShow(): " + Integer.toHexString(getSelectedColor()) +
                            " " + (pulseEnabled ? getPulseSpeedOn() : 1) +
                            " " + (pulseEnabled ? getPulseSpeedOff() : 0));
                }

                mNotificationManager.notify(1, builder.build());
            }
        }, delay);
    }

    private AdapterView.OnItemSelectedListener mPulseSelectionListener =
            new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (parent == mPulseSpeedOn) {
                mPulseSpeedOff.setEnabled(mPulseSpeedOn.isEnabled() && getPulseSpeedOn() != 1);
            }

            updateLed(LED_UPDATE_DELAY_MS);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    @SuppressWarnings("unchecked")
    public int getPulseSpeedOn() {
        if (mPulseSpeedOn.isEnabled()) {
            return ((Pair<String, Integer>) mPulseSpeedOn.getSelectedItem()).second;
        } else {
            return 1;
        }
    }

    @SuppressWarnings("unchecked")
    public int getPulseSpeedOff() {
        // return 0 if 'Always on' is selected
        return getPulseSpeedOn() == 1 ? 0 : ((Pair<String, Integer>) mPulseSpeedOff.getSelectedItem()).second;
    }

    private class PulseSpeedAdapter extends BaseAdapter implements SpinnerAdapter {
        private ArrayList<Pair<String, Integer>> times;

        public PulseSpeedAdapter(int timeNamesResource, int timeValuesResource) {
            times = new ArrayList<Pair<String, Integer>>();

            String[] time_names = mResources.getStringArray(timeNamesResource);
            String[] time_values = mResources.getStringArray(timeValuesResource);

            for(int i = 0; i < time_values.length; ++i) {
                times.add(new Pair<String, Integer>(time_names[i], Integer.decode(time_values[i])));
            }

        }

        /**
         * This constructor apart from taking a usual time entry array takes the
         * currently configured time value which might cause the addition of a
         * "Custom" time entry in the spinner in case this time value does not
         * match any of the predefined ones in the array.
         *
         * @param timeNamesResource The time entry names array
         * @param timeValuesResource The time entry values array
         * @param customTime Current time value that might be one of the
         *            predefined values or a totally custom value
         */
        public PulseSpeedAdapter(int timeNamesResource, int timeValuesResource, Integer customTime) {
            this(timeNamesResource, timeValuesResource);

            // Check if we also need to add the custom value entry
            if (getTimePosition(customTime) == -1) {
                times.add(new Pair<String, Integer>(mResources
                        .getString(R.string.custom_time), customTime));
            }
        }

        /**
         * Will return the position of the spinner entry with the specified
         * time. Returns -1 if there is no such entry.
         *
         * @param time Time in ms
         * @return Position of entry with given time or -1 if not found.
         */
        public int getTimePosition(Integer time) {
            for (int position = 0; position < getCount(); ++position) {
                if (getItem(position).second.equals(time)) {
                    return position;
                }
            }

            return -1;
        }

        @Override
        public int getCount() {
            return times.size();
        }

        @Override
        public Pair<String, Integer> getItem(int position) {
            return times.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = mInflater.inflate(R.layout.pulse_time_item, parent, false);
            }

            Pair<String, Integer> entry = getItem(position);
            ((TextView) view.findViewById(R.id.textViewName)).setText(entry.first);

            return view;
        }
    }
}
