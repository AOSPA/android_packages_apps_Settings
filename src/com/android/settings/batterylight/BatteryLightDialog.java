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

import com.android.settings.R;
import com.android.settingslib.ColorPickerDialog;
import com.android.settingslib.ColorPickerDialogAdapter;

public class BatteryLightDialog extends ColorPickerDialog
        implements ColorPickerDialog.OnColorSelectedListener {

    private final static String TAG = "BatteryLightDialog";

    private ColorPickerDialogAdapter mAdapter;
    private Context mContext;
    private NotificationManager mNotificationManager;
    private Resources mResources;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BatteryLightDialog.this.dismiss();
        }
    };

    public BatteryLightDialog(Context context) {
        super(context);

        mContext = context;
        mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mResources = context.getResources();

        final int[] colors = mResources.getIntArray(
                R.array.led_color_picker_dialog_colors);

        mAdapter = getAdapter();
        mAdapter.setColors(colors);
        mAdapter.setSelectedImageResourceId(R.drawable.ic_check_green_24dp);
        mAdapter.setSelectedImageColorFilter(Color.WHITE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setOnColorSelectedListener(this);
    }

    public void onColorSelected(DialogInterface dialog, int color) {
        mAdapter.setSelectedImageColorFilter(
                mAdapter.getSelectedPosition() == 0 ? Color.DKGRAY : Color.WHITE);

        onStart();
    }

    @Override
    public void onStart() {
        final Bundle b = new Bundle();
        b.putBoolean(Notification.EXTRA_FORCE_SHOW_LIGHTS, true);

        final Notification.Builder builder = new Notification.Builder(mContext);
        builder.setLights(getSelectedColor(), 1, 0);
        builder.setExtras(b);

        builder.setSmallIcon(R.drawable.ic_settings_leds);
        builder.setContentTitle(mResources.getString(R.string.battery_light_settings));
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

        mContext.registerReceiver(broadcastReceiver, new IntentFilter(TAG));
        mNotificationManager.notify(1, builder.build());
    }

    @Override
    public void onStop() {
        mNotificationManager.cancel(1);
        mContext.unregisterReceiver(broadcastReceiver);
    }
}
