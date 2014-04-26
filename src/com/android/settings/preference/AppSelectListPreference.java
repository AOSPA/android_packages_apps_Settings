/*
 * Copyright (C) 2013 The ChameleonOS Project
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
package com.android.settings.preference;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ComponentName;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import com.android.settings.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A preference that lists installed applications, with icons, as a single select list.
 * It will return the launch intent as string if an app is clicked
 *
 * if the positive button is pressed it will return positiveButtonValue or null
 *
 * a neutral button can be added optional by adding the neutralButtonText
 * attribute. Pressing it will return neutralButtonValue or null
 *
 * e.g.
 *         <com.android.settings.preference.AppSelectListPreference
 *               android:key="calendar_shortcut"
 *               android:title="@string/calendar_shortcut_title"
 *               android:positiveButtonText="@string/default_shortcut"
 *               settings:neutralButtonText="@string/default_shortcut2"
 *               settings:neutralButtonValue="DEFAULT"
 *               android:persistent="false"/>
 *
 */
public class AppSelectListPreference extends DialogPreference {
    private final List<MyApplicationInfo> mPackageInfoList = new ArrayList<MyApplicationInfo>();
    private AppListAdapter mAdapter;
    private CharSequence mReturnValue;
    private CharSequence mNeutralButtonText;
    private CharSequence mNeutralButtonValue;
    private CharSequence mPositiveButtonValue;
    private AlertDialog.Builder mBuilder;
    private boolean mNeutralButtonPressed;

    private static final String SETTINGS = "http://schemas.android.com/apk/res/com.android.settings";

    public AppSelectListPreference(Context context) {
        this(context, null);
    }

    public AppSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setValuesFromXml(attrs);

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> installedAppsInfo = getContext().getPackageManager().queryIntentActivities(
                mainIntent, 0);

        for (ResolveInfo info : installedAppsInfo) {
            MyApplicationInfo myInfo = new MyApplicationInfo();
            myInfo.resolveInfo = info;
            myInfo.label = getResolveInfoTitle(info);
            mPackageInfoList.add(myInfo);
        }
        Collections.sort(mPackageInfoList, sDisplayNameComparator);
    }

    private void setValuesFromXml(AttributeSet attrs) {
        final TypedArray typedArray = getContext().obtainStyledAttributes(
                      attrs, R.styleable.AppSelectListPreference);

        mNeutralButtonText = getAttributeStringValue(attrs, SETTINGS, "neutralButtonText", null);
        Integer id = typedArray.getResourceId(R.styleable.AppSelectListPreference_neutralButtonText, 0);
        if (id > 0) {
            mNeutralButtonText = getContext().getResources().getString(id);
        }

        mNeutralButtonValue = getAttributeStringValue(attrs, SETTINGS, "neutralButtonValue", null);
        id = typedArray.getResourceId(R.styleable.AppSelectListPreference_neutralButtonValue, 0);
        if (id > 0) {
            mNeutralButtonValue = getContext().getResources().getString(id);
        }

        mPositiveButtonValue = getAttributeStringValue(attrs, SETTINGS, "positiveButtonValue", null);
        id = typedArray.getResourceId(R.styleable.AppSelectListPreference_positiveButtonValue, 0);
        if (id > 0) {
            mPositiveButtonValue = getContext().getResources().getString(id);
        }
        typedArray.recycle();
    }

    private String getAttributeStringValue(AttributeSet attrs, String namespace, String name, String defaultValue) {
        String value = attrs.getAttributeValue(namespace, name);
        if(value == null) {
            value = defaultValue;
        }
        return value;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        mAdapter = new AppListAdapter(getContext());
        builder.setAdapter(mAdapter, null);
    }

    private String getResolveInfoTitle(ResolveInfo info) {
        CharSequence label = info.loadLabel(getContext().getPackageManager());
        if (label == null) label = info.activityInfo.name;
        return label != null ? label.toString() : null;
    }

    private Intent getIntentForResolveInfo(ResolveInfo info, String action) {
        Intent intent = new Intent(action);
        ActivityInfo ai = info.activityInfo;
        intent.setClassName(ai.packageName, ai.name);
        return intent;
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        final AlertDialog dialog = (AlertDialog) getDialog();
        final ListView listView = dialog.getListView();
        mReturnValue = mPositiveButtonValue;
        listView.setItemsCanFocus(false);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final AppViewHolder holder = (AppViewHolder) view.getTag();

                MyApplicationInfo myInfo = mAdapter.getItem(position);
                ResolveInfo info = myInfo.resolveInfo;
                Intent intent = getIntentForResolveInfo(info, Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);

                mReturnValue = intent.toUri(0).toString();
                onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_NEUTRAL){
            mReturnValue = mNeutralButtonValue;
            mNeutralButtonPressed = true;
        }
        super.onClick(dialog, which);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        // can be null an intent or whatever set as mNeutralButtonValue
        if (positiveResult || mNeutralButtonPressed) {
            callChangeListener(mReturnValue);
        }
    }

    class MyApplicationInfo {
        ApplicationInfo info;
        CharSequence label;
        ResolveInfo resolveInfo;
    }

    public class AppListAdapter extends ArrayAdapter<MyApplicationInfo> {
        private final LayoutInflater mInflater;

        public AppListAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            addAll(mPackageInfoList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid unnecessary calls
            // to findViewById() on each row.
            AppViewHolder holder = AppViewHolder.createOrRecycle(mInflater, convertView);
            convertView = holder.rootView;
            MyApplicationInfo info = getItem(position);
            holder.appName.setText(info.label);
            Drawable icon = info.resolveInfo.loadIcon(getContext().getPackageManager());
            if (icon != null) {
                holder.appIcon.setImageDrawable(icon);
            } else {
                holder.appIcon.setImageDrawable(null);
            }
            return convertView;
        }

        @Override
        public MyApplicationInfo getItem(int position) {
            return mPackageInfoList.get(position);
        }
    }

    public static class AppViewHolder {
        public View rootView;
        public TextView appName;
        public ImageView appIcon;

        public static AppViewHolder createOrRecycle(LayoutInflater inflater, View convertView) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.app_item, null);

                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                AppViewHolder holder = new AppViewHolder();
                holder.rootView = convertView;
                holder.appName = (TextView) convertView.findViewById(R.id.app_name);
                holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
                convertView.setTag(holder);
                return holder;
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                return (AppViewHolder)convertView.getTag();
            }
        }
    }

    private final static Comparator<MyApplicationInfo> sDisplayNameComparator
            = new Comparator<MyApplicationInfo>() {

        private final Collator collator = Collator.getInstance();

        public final int compare(MyApplicationInfo a, MyApplicationInfo b) {
            return collator.compare(a.label, b.label);
        }
    };

    protected Dialog createDialog() {
        super.createDialog();
        Context context = getContext();

        mBuilder = new AlertDialog.Builder(context)
            .setTitle(getDialogTitle())
            .setIcon(getDialogIcon())
            .setPositiveButton(getPositiveButtonText(), this)
            .setNegativeButton(getNegativeButtonText(), this);

        if (mNeutralButtonText != null){
            mBuilder.setNeutralButton(mNeutralButtonText, this);
        }

        View contentView = onCreateDialogView();
        if (contentView != null) {
            onBindDialogView(contentView);
            mBuilder.setView(contentView);
        } else {
            mBuilder.setMessage(getDialogMessage());
        }

        onPrepareDialogBuilder(mBuilder);

        return mBuilder.create();
    }
}
