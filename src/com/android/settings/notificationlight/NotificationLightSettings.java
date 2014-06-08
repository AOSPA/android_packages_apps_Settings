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

package com.android.settings.notificationlight;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class NotificationLightSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, AdapterView.OnItemLongClickListener {
    private static final String TAG = "NotificationLightSettings";

    private static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR
        = "notification_light_pulse_default_color";
    private static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON
        = "notification_light_pulse_default_led_on";
    private static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF
        = "notification_light_pulse_default_led_off";
    private static final String NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE
        = "notification_light_pulse_custom_enable";
    private static final String NOTIFICATION_LIGHT_PULSE
        = "notification_light_pulse";
    private static final String NOTIFICATION_LIGHT_PULSE_CALL_COLOR
        = "notification_light_pulse_call_color";
    private static final String NOTIFICATION_LIGHT_PULSE_CALL_LED_ON
        = "notification_light_pulse_call_led_on";
    private static final String NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF
        = "notification_light_pulse_call_led_off";
    private static final String NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR
        = "notification_light_pulse_vmail_color";
    private static final String NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON
        = "notification_light_pulse_vmail_led_on";
    private static final String NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF
        = "notification_light_pulse_vmail_led_off";

    private static final String PULSE_PREF = "pulse_enabled";
    private static final String DEFAULT_PREF = "default";
    private static final String CUSTOM_PREF = "custom_enabled";
    private static final String MISSED_CALL_PREF = "missed_call";
    private static final String VOICEMAIL_PREF = "voicemail";
    public static final int ACTION_TEST = 0;
    public static final int ACTION_DELETE = 1;
    private static final int MENU_ADD = 0;
    private static final int DIALOG_APPS = 0;
    private int mDefaultColor;
    private int mDefaultLedOn;
    private int mDefaultLedOff;
    private PackageManager mPackageManager;
    private boolean mCustomEnabled;
    private boolean mLightEnabled;
    private boolean mVoiceCapable;
    private PreferenceGroup mApplicationPrefList;
    private ApplicationLightPreference mDefaultPref;
    private ApplicationLightPreference mCallPref;
    private ApplicationLightPreference mVoicemailPref;
    private CheckBoxPreference mCustomEnabledPref;
    private Menu mMenu;
    private PackageAdapter mPackageAdapter;
    private String mPackageList;
    private Map<String, Package> mPackages;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notification_light_settings);

        Resources resources = getResources();
        mDefaultColor = resources.getColor(
                com.android.internal.R.color.config_defaultNotificationColor);
        mDefaultLedOn = resources.getInteger(
                com.android.internal.R.integer.config_defaultNotificationLedOn);
        mDefaultLedOff = resources.getInteger(
                com.android.internal.R.integer.config_defaultNotificationLedOff);

        // Get launch-able applications
        mPackageManager = getPackageManager();
        mPackageAdapter = new PackageAdapter();

        mPackages = new HashMap<String, Package>();

        // Determine if the device has voice capabilities
        TelephonyManager tm =
            (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        mVoiceCapable = tm.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDefault();
        refreshCustomApplicationPrefs();
        setCustomEnabled();
        getListView().setOnItemLongClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        setChildrenStarted(getPreferenceScreen(), true);
    }

    @Override
    public void onStop() {
        super.onStop();
        setChildrenStarted(getPreferenceScreen(), false);
    }

    private void setChildrenStarted(PreferenceGroup group, boolean started) {
        final int count = group.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof ApplicationLightPreference) {
                ApplicationLightPreference ap = (ApplicationLightPreference) pref;
                if (started) {
                    ap.onStart();
                } else {
                    ap.onStop();
                }
            } else if (pref instanceof PreferenceGroup) {
                setChildrenStarted((PreferenceGroup) pref, started);
            }
        }
    }

    private void refreshDefault() {
        ContentResolver resolver = getContentResolver();
        int color = Settings.System.getInt(
            resolver, NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR, mDefaultColor);
        int timeOn = Settings.System.getInt(
            resolver, NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON, mDefaultLedOn);
        int timeOff = Settings.System.getInt(
            resolver, NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF, mDefaultLedOff);
        mLightEnabled = Settings.System.getInt(
            resolver, NOTIFICATION_LIGHT_PULSE, 0) == 1;
        mCustomEnabled = Settings.System.getInt(
            resolver, NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE, 0) == 1;

        // Get Missed call and Voicemail values
        int callColor = Settings.System.getInt(
            resolver, NOTIFICATION_LIGHT_PULSE_CALL_COLOR, mDefaultColor);
        int callTimeOn = Settings.System.getInt(
            resolver, NOTIFICATION_LIGHT_PULSE_CALL_LED_ON, mDefaultLedOn);
        int callTimeOff = Settings.System.getInt(
            resolver, NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF, mDefaultLedOff);
        int vmailColor = Settings.System.getInt(
            resolver, NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR, mDefaultColor);
        int vmailTimeOn = Settings.System.getInt(
            resolver, NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON, mDefaultLedOn);
        int vmailTimeOff = Settings.System.getInt(
            resolver, NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF, mDefaultLedOff);

        PreferenceScreen prefSet = getPreferenceScreen();
        PreferenceGroup generalPrefs = (PreferenceGroup) prefSet.findPreference("general_section");
        if (generalPrefs != null) {

            // Pulse preference
            CheckBoxPreference cPref = (CheckBoxPreference) prefSet.findPreference(PULSE_PREF);
            cPref.setChecked(mLightEnabled);
            cPref.setOnPreferenceChangeListener(this);

            // Default preference
            mDefaultPref = (ApplicationLightPreference) prefSet.findPreference(DEFAULT_PREF);
            mDefaultPref.setAllValues(color, timeOn, timeOff);
            mDefaultPref.setEnabled(mLightEnabled);
            mDefaultPref.setOnPreferenceChangeListener(this);

            // Custom enabled preference
            mCustomEnabledPref = (CheckBoxPreference) prefSet.findPreference(CUSTOM_PREF);
            mCustomEnabledPref.setChecked(mCustomEnabled);
            mCustomEnabledPref.setEnabled(mLightEnabled);
            mCustomEnabledPref.setOnPreferenceChangeListener(this);
        }

        PreferenceGroup phonePrefs = (PreferenceGroup) prefSet.findPreference("phone_list");
        if (phonePrefs != null) {

            // Missed call and Voicemail preferences
            // Should only show on devices with a voice capabilities
            if (!mVoiceCapable) {
                prefSet.removePreference(phonePrefs);
            } else {
                mCallPref =
                    (ApplicationLightPreference) prefSet.findPreference(MISSED_CALL_PREF);
                mCallPref.setAllValues(callColor, callTimeOn, callTimeOff);
                mCallPref.setEnabled(mCustomEnabled);
                mCallPref.setOnPreferenceChangeListener(this);

                mVoicemailPref =
                    (ApplicationLightPreference) prefSet.findPreference(VOICEMAIL_PREF);
                mVoicemailPref.setAllValues(vmailColor, vmailTimeOn, vmailTimeOff);
                mVoicemailPref.setEnabled(mCustomEnabled);
                mVoicemailPref.setOnPreferenceChangeListener(this);
            }
        }

        mApplicationPrefList = (PreferenceGroup) prefSet.findPreference("applications_list");
        mApplicationPrefList.setOrderingAsAdded(false);
    }

    private void refreshCustomApplicationPrefs() {
        Context context = getActivity();

        if (!parsePackageList()) {
            return;
        }

        // Add the Application Preferences
        if (mApplicationPrefList != null) {
            mApplicationPrefList.removeAll();

            for (Package pkg : mPackages.values()) {
                try {
                    PackageInfo info = mPackageManager.getPackageInfo(pkg.name,
                            PackageManager.GET_META_DATA);
                    ApplicationLightPreference pref =
                            new ApplicationLightPreference(
                            context, pkg.color, pkg.timeon, pkg.timeoff);

                    pref.setKey(pkg.name);
                    pref.setTitle(info.applicationInfo.loadLabel(mPackageManager));
                    pref.setIcon(info.applicationInfo.loadIcon(mPackageManager));
                    pref.setPersistent(false);
                    pref.setOnPreferenceChangeListener(this);

                    mApplicationPrefList.addPreference(pref);
                } catch (NameNotFoundException e) {
                    // Do nothing
                }
            }
        }

        mPackageAdapter.reloadList();
    }

    private void setCustomEnabled() {
        boolean enabled = mCustomEnabled && mLightEnabled;

        // Phone related preferences
        if (mVoiceCapable) {
            mCallPref.setEnabled(enabled);
            mVoicemailPref.setEnabled(enabled);
        }

        // Custom applications
        if (mApplicationPrefList != null) {
            mApplicationPrefList.setEnabled(enabled);
            setHasOptionsMenu(enabled);
        }
    }

    private void addCustomApplicationPref(String packageName) {
        Package pkg = mPackages.get(packageName);
        if (pkg == null) {
            pkg = new Package(packageName, mDefaultColor, mDefaultLedOn, mDefaultLedOff);
            mPackages.put(packageName, pkg);
            savePackageList(false);
            refreshCustomApplicationPrefs();
        }
    }

    private void removeCustomApplicationPref(String packageName) {
        if (mPackages.remove(packageName) != null) {
            savePackageList(false);
            refreshCustomApplicationPrefs();
        }
    }

    private boolean parsePackageList() {
        final String baseString = Settings.System.getString(getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES);

        if (TextUtils.equals(mPackageList, baseString)) {
            return false;
        }

        mPackageList = baseString;
        mPackages.clear();

        if (baseString != null) {
            final String[] array = TextUtils.split(baseString, "\\|");
            for (String item : array) {
                if (TextUtils.isEmpty(item)) {
                    continue;
                }
                Package pkg = Package.fromString(item);
                if (pkg != null) {
                    mPackages.put(pkg.name, pkg);
                }
            }
        }

        return true;
    }

    private void savePackageList(boolean preferencesUpdated) {
        List<String> settings = new ArrayList<String>();
        for (Package app : mPackages.values()) {
            settings.add(app.toString());
        }
        final String value = TextUtils.join("|", settings);
        if (preferencesUpdated) {
            mPackageList = value;
        }
        Settings.System.putString(getContentResolver(),
                                  Settings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES, value);
    }

    /**
     * Updates the default or package specific notification settings.
     *
     * @param packageName Package name of application specific settings to update
     * @param color
     * @param timeon
     * @param timeoff
     */
    protected void updateValues(String packageName, Integer color,
        Integer timeon, Integer timeoff) {
        ContentResolver resolver = getContentResolver();

        if (packageName.equals(DEFAULT_PREF)) {
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR, color);
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON, timeon);
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF, timeoff);
            refreshDefault();
            return;
        } else if (packageName.equals(MISSED_CALL_PREF)) {
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_CALL_COLOR, color);
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_CALL_LED_ON, timeon);
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF, timeoff);
            refreshDefault();
            return;
        } else if (packageName.equals(VOICEMAIL_PREF)) {
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR, color);
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON, timeon);
            Settings.System.putInt(resolver, NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF, timeoff);
            refreshDefault();
            return;
        }

        // Find the custom package and sets its new values
        Package app = mPackages.get(packageName);
        if (app != null) {
            app.color = color;
            app.timeon = timeon;
            app.timeoff = timeoff;
            savePackageList(true);
        }
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final Preference pref =
            (Preference) getPreferenceScreen().getRootAdapter().getItem(position);

        if (mApplicationPrefList.findPreference(pref.getKey()) != pref) {
            return false;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeCustomApplicationPref(pref.getKey());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        builder.show();
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        String key = preference.getKey();

        if (PULSE_PREF.equals(key)) {
            mLightEnabled = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATION_LIGHT_PULSE,
                    mLightEnabled ? 1 : 0);
            mDefaultPref.setEnabled(mLightEnabled);
            mCustomEnabledPref.setEnabled(mLightEnabled);
            setCustomEnabled();
        } else if (CUSTOM_PREF.equals(key)) {
            mCustomEnabled = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE,
                    mCustomEnabled ? 1 : 0);
            setCustomEnabled();
        } else {
            ApplicationLightPreference tPref = (ApplicationLightPreference) preference;
            updateValues(key, tPref.getColor(), tPref.getOnValue(), tPref.getOffValue());
        }

        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenu = menu;
        mMenu.add(0, MENU_ADD, 0, R.string.notification_light_add)
                .setIcon(R.drawable.ic_menu_add_dark)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD:
                showDialog(DIALOG_APPS);
                return true;
        }
        return false;
    }

    /**
     * Utility classes and supporting methods
     */
    @Override
    public Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Dialog dialog;
        switch (id) {
            case DIALOG_APPS:
                final ListView list = new ListView(getActivity());
                list.setAdapter(mPackageAdapter);

                builder.setTitle(R.string.notification_light_choose_app);
                builder.setView(list);
                dialog = builder.create();

                list.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent,
                        View view, int position, long id) {
                        // Add empty application definition, the user will be able to edit it later
                        PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                        addCustomApplicationPref(info.packageName);
                        dialog.cancel();
                    }
                });
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    /**
     * Application class
     */
    private static class Package {
        public String name;
        public Integer color;
        public Integer timeon;
        public Integer timeoff;

        /**
         * Stores all the application values in one call
         * @param name
         * @param color
         * @param timeon
         * @param timeoff
         */
        public Package(String name, Integer color, Integer timeon, Integer timeoff) {
            this.name = name;
            this.color = color;
            this.timeon = timeon;
            this.timeoff = timeoff;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(name);
            builder.append("=");
            builder.append(color);
            builder.append(";");
            builder.append(timeon);
            builder.append(";");
            builder.append(timeoff);
            return builder.toString();
        }

        public static Package fromString(String value) {
            if (TextUtils.isEmpty(value)) {
                return null;
            }
            String[] app = value.split("=", -1);
            if (app.length != 2)
                return null;

            String[] values = app[1].split(";", -1);
            if (values.length != 3)
                return null;

            try {
                Package item = new Package(app[0], Integer.parseInt(values[0]), Integer
                        .parseInt(values[1]), Integer.parseInt(values[2]));
                return item;
            } catch (NumberFormatException e) {
                return null;
            }
        }

    };

    /**
     * AppItem class
     */
    private static class PackageItem implements Comparable<PackageItem> {
        CharSequence title;
        TreeSet<CharSequence> activityTitles = new TreeSet<CharSequence>();
        String packageName;
        Drawable icon;

        @Override
        public int compareTo(PackageItem another) {
            int result = title.toString().compareToIgnoreCase(another.title.toString());
            return result != 0 ? result : packageName.compareTo(another.packageName);
        }
    }

    /**
     * AppAdapter class
     */
    private class PackageAdapter extends BaseAdapter {
        private List<PackageItem> mInstalledPackages = new LinkedList<PackageItem>();

        public void reloadList() {
            final Handler handler = new Handler();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (mInstalledPackages) {
                        mInstalledPackages.clear();
                    }

                    final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    List<ResolveInfo> installedAppsInfo =
                            mPackageManager.queryIntentActivities(mainIntent, 0);

                    for (ResolveInfo info : installedAppsInfo) {
                        ApplicationInfo appInfo = info.activityInfo.applicationInfo;

                        final PackageItem item = new PackageItem();
                        item.title = appInfo.loadLabel(mPackageManager);
                        item.activityTitles.add(info.loadLabel(mPackageManager));
                        item.icon = appInfo.loadIcon(mPackageManager);
                        item.packageName = appInfo.packageName;

                        if (mPackages.get(item.packageName) != null) {
                            // Skip this package - it's already in the application's custom packages
                            continue;
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                // NO synchronize here: We know that mInstalledApps.clear()
                                // was called and will never be called again.
                                // At this point the only thread modifying mInstalledApp is main
                                int index = Collections.binarySearch(mInstalledPackages, item);
                                if (index < 0) {
                                    mInstalledPackages.add(-index - 1, item);
                                } else {
                                    mInstalledPackages
                                        .get(index).activityTitles.addAll(item.activityTitles);
                                }
                                notifyDataSetChanged();
                            }
                        });
                    }
                }
            }).start();
        }

        public PackageAdapter() {
            reloadList();
        }

        @Override
        public int getCount() {
            synchronized (mInstalledPackages) {
                return mInstalledPackages.size();
            }
        }

        @Override
        public PackageItem getItem(int position) {
            synchronized (mInstalledPackages) {
                return mInstalledPackages.get(position);
            }
        }

        @Override
        public long getItemId(int position) {
            synchronized (mInstalledPackages) {
                // packageName is guaranteed to be unique in mInstalledPackages
                return mInstalledPackages.get(position).packageName.hashCode();
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView != null) {
                holder = (ViewHolder) convertView.getTag();
            } else {
                final LayoutInflater layoutInflater =
                    (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.notification_app_item, null, false);
                holder = new ViewHolder();
                convertView.setTag(holder);
                holder.title =
                    (TextView) convertView.findViewById(com.android.internal.R.id.title);
                holder.summary =
                    (TextView) convertView.findViewById(com.android.internal.R.id.summary);
                holder.icon =
                    (ImageView) convertView.findViewById(R.id.icon);
            }
            PackageItem applicationInfo = getItem(position);

            holder.title.setText(applicationInfo.title);
            holder.icon.setImageDrawable(applicationInfo.icon);

            boolean needSummary = applicationInfo.activityTitles.size() > 0;
            if (applicationInfo.activityTitles.size() == 1) {
                if (TextUtils.equals(
                    applicationInfo.title, applicationInfo.activityTitles.first())) {
                    needSummary = false;
                }
            }

            if (needSummary) {
                holder.summary.setText(TextUtils.join(", ", applicationInfo.activityTitles));
                holder.summary.setVisibility(View.VISIBLE);
            } else {
                holder.summary.setVisibility(View.GONE);
            }

            return convertView;
        }
    }

    static class ViewHolder {
        TextView title;
        TextView summary;
        ImageView icon;
    }
}
