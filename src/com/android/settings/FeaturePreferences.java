/*
 * Copyright (C) 2016 The Paranoid Android Project
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

package com.android.settings;

import android.annotation.LayoutRes;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.android.internal.logging.MetricsLogger;

import java.util.ArrayList;

public class FeaturePreferences extends InstrumentedFragment {

    private static final String TAG = "FeaturePreferences";
    private static final boolean DEBUG = false;

    private View mMainView;
    private ListView mSettingsList;
    private SettingsListAdapter mSettingsAdapter;
    private Button mResetAllButton;
    private Button mResetSelectedButton;

    private ContentResolver mCr;

    private ArrayList<Preference> customPrefs = new ArrayList<>();

    private final Button.OnClickListener mButtonListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mResetAllButton) {
                for (String setting : Settings.Secure.SETTINGS_TO_RESET) {
                    Settings.Secure.putInt(getContext().getContentResolver(), setting, 0);
                }
                updateActiveCustomPreferences();
                mResetSelectedButton.setEnabled(false);
            } else if (v == mResetSelectedButton) {
                SparseBooleanArray checked = mSettingsList.getCheckedItemPositions();
                if (checked != null) {
                    for (int i = 0; i < checked.size(); i++) {
                        if (checked.valueAt(i)) {
                            Settings.Secure.putInt(mCr, customPrefs.get(i).key, 0);
                        }
                    }
                }
                updateActiveCustomPreferences();
                mResetSelectedButton.setEnabled(false);
            }
        }
    };

    private void updateActiveCustomPreferences() {
        customPrefs.clear();
        try {
            Context con = getActivity().getApplicationContext()
                    .createPackageContext("com.android.systemui", 0);
            Resources r = con.getResources();
            for (String setting : Settings.Secure.SETTINGS_TO_RESET) {
                if (!(Settings.Secure.getInt(con.getContentResolver(), setting, 0) == 0)) {
                    String key = setting.toLowerCase();
                    int nameResId = r.getIdentifier(setting + "_name", "string", "com.android.systemui");
                    int descResId = r.getIdentifier(setting + "_summary", "string", "com.android.systemui");
                    if (nameResId != 0 && descResId != 0) {
                        try {
                            String name = (String) r.getText(nameResId);
                            String desc = (String) r.getText(descResId);
                            customPrefs.add(new Preference(key, name, desc));
                        } catch (Resources.NotFoundException e) {
                            Log.e(TAG, "Resource not found for: " + setting, e);
                        }
                    } else {
                        Log.v(TAG, "Missing strings for: " + setting);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "NameNotFoundException", e);
        }
        mSettingsAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mCr = getContext().getContentResolver();

        mMainView = inflater.inflate(R.layout.feature_preferences, null);
        mSettingsList = (ListView) mMainView.findViewById(R.id.setting_list);

        mSettingsAdapter = new SettingsListAdapter(getContext(),
                R.layout.feature_preferences_list_item, inflater, customPrefs);

        mSettingsList.setAdapter(mSettingsAdapter);

        updateActiveCustomPreferences();

        mResetAllButton = (Button) mMainView.findViewById(R.id.reset_all_prefs);
        mResetSelectedButton = (Button) mMainView.findViewById(R.id.reset_selected_prefs);
        mResetAllButton.setOnClickListener(mButtonListener);
        mResetSelectedButton.setOnClickListener(mButtonListener);
        mResetSelectedButton.setEnabled(false);
        mSettingsList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mSettingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ViewHolder holder = (ViewHolder) view.getTag();
                CheckBox checkBoxView = holder.getCheckBox();
                checkBoxView.setChecked(!checkBoxView.isChecked());
                holder.setChecked(checkBoxView.isChecked());
                mSettingsList.setItemChecked(position, holder.isChecked());
                updateResetSelectedButton();
            }
        });

        if (DEBUG) Log.d(TAG, "created view. items: " + customPrefs.size());

        return mMainView;
    }

    private void updateResetSelectedButton() {
        if (mSettingsList.getCheckedItemCount() > 0) {
            mResetSelectedButton.setEnabled(true);
        } else {
            mResetSelectedButton.setEnabled(false);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.FEATURE_PREFERENCES;
    }

    private class Preference {
        protected String key;
        protected String name;
        protected String desc;

        public Preference(String key, String name, String desc) {
            this.key = key;
            this.name = name;
            this.desc = desc;
        }
    }

    private class ViewHolder {
        private CheckBox checkBox;
        private TextView settingName;
        private TextView settingDescription;

        private boolean checked;


        public ViewHolder(CheckBox checkBox, TextView settingName, TextView settingDescription) {
            this.checkBox = checkBox;
            this.settingName = settingName;
            this.settingDescription = settingDescription;
        }

        public CheckBox getCheckBox() {
            return checkBox;
        }

        public boolean isChecked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }
    }

    private class SettingsListAdapter extends ArrayAdapter<Preference> {

        private LayoutInflater mInflater;

        public SettingsListAdapter(Context context, @LayoutRes int resource,
                                   LayoutInflater inflater, ArrayList<Preference> items) {
            super(context, resource, items);
            mInflater = inflater;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            CheckBox checkBox;
            TextView name;
            TextView desc;

            Preference item = getItem(position);
            if (DEBUG) Log.d(TAG, "item: " + item.key + " value: " + item.desc);

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.feature_preferences_list_item, null);

                name = (TextView) convertView.findViewById(R.id.setting_title);
                desc = (TextView) convertView.findViewById(R.id.setting_description);
                checkBox = (CheckBox) convertView.findViewById(R.id.setting_checkbox);

                name.setText(item.name);
                desc.setText(item.desc);

                convertView.setTag(new ViewHolder(checkBox, name, desc));

                checkBox.setTag(convertView);
                checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CheckBox cb = (CheckBox) v;
                        View listItem = (View) v.getTag();
                        ViewHolder holder = (ViewHolder) listItem.getTag();
                        holder.setChecked(cb.isChecked());
                        mSettingsList.setItemChecked(position, holder.isChecked());
                        updateResetSelectedButton();
                    }
                });
            }
            return convertView;
        }
    }
}
