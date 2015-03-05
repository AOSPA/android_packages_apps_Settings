/**
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings.applications;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.AppOpsState.AppOpEntry;

public class AppOpsCategory extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<AppOpEntry>> {

    private static final int RESULT_APP_DETAILS = 1;

    private AppListAdapter mAdapter;

    private String mCurrentPkgName;

    private AppOpsState mState;

    public AppOpsCategory() {
    }

    /**
     * Helper receiver set up to listen to changes in the application list and forward the change
     * event on to the appropriate sections.
     */
    public static class PackageIntentReceiver extends BroadcastReceiver {
        private final AppListLoader mLoader;

        public PackageIntentReceiver(final AppListLoader loader) {
            mLoader = loader;

            // Events related to appliation installation
            final IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            loader.getContext().registerReceiver(this, filter);

            // Events related to applications on the SD card
            final IntentFilter sdFilter = new IntentFilter();
            sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
            sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
            loader.getContext().registerReceiver(this, sdFilter);
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            mLoader.onContentChanged();
        }
    }

    /**
     * A custom Loader that loads all of the installed applications.
     */
    public static class AppListLoader extends AsyncTaskLoader<List<AppOpEntry>> {
        private final Configuration mLastConfiguration = new Configuration();
        private final AppOpsState mState;

        private List<AppOpEntry> mApps = null;
        private int mLastDensity = 0;
        private PackageIntentReceiver mPackageObserver = null;

        public AppListLoader(final Context context, final AppOpsState state) {
            super(context);

            mState = state;
        }

        @Override
        public List<AppOpEntry> loadInBackground() {
            return mState.buildState();
        }

        /**
         * Delivers data to the client. The super implementation will take care of actually
         * delivering it. This implementation simply adds special handling and caring logic.
         */
        @Override
        public void deliverResult(final List<AppOpEntry> apps) {
            if (isReset()) {
                // This load has been reset so the new List can be released.
                if (apps != null) {
                    onReleaseResources(apps);
                }
            }

            final List<AppOpEntry> oldApps = mApps;
            mApps = apps;

            if (isStarted()) {
                // If this Loader is currently started, we can deliver the List right away.
                super.deliverResult(apps);
            }

            // The old List is no longer in use so it can be released.
            if (oldApps != null) {
                onReleaseResources(oldApps);
            }
        }

        @Override
        protected void onStartLoading() {
            // Force a reload here as changes are not monitored when loading is stopped.
            onContentChanged();

            if (mApps != null) {
                // Deliver results immediately, if they are available.
                deliverResult(mApps);
            }

            // Start watching for changes in the application data.
            if (mPackageObserver == null) {
                mPackageObserver = new PackageIntentReceiver(this);
            }

            // Check for interesting changes in the configuration.
            boolean hasConfigChanged = false;
            final Resources res = getContext().getResources();
            final boolean hasDensityChanged = mLastDensity != res.getDisplayMetrics().densityDpi;
            final int configChanges = mLastConfiguration.updateFrom(res.getConfiguration());
            if (hasDensityChanged || (configChanges & (ActivityInfo.CONFIG_LOCALE |
                    ActivityInfo.CONFIG_UI_MODE | ActivityInfo.CONFIG_SCREEN_LAYOUT)) != 0) {
                mLastDensity = res.getDisplayMetrics().densityDpi;
                hasConfigChanged = true;
            }

            if (takeContentChanged() || hasConfigChanged || mApps == null) {
                // Start a load now, if anything has changed or no data is available.
                forceLoad();
            }
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        public void onCanceled(final List<AppOpEntry> apps) {
            super.onCanceled(apps);
            onReleaseResources(apps);
        }

        @Override
        protected void onReset() {
            super.onReset();

            // Ensure the loader is stopped.
            onStopLoading();

            // Stop watching for changes.
            if (mPackageObserver != null) {
                getContext().unregisterReceiver(mPackageObserver);
                mPackageObserver = null;
            }

            // Release all the resources!
            if (mApps != null) {
                onReleaseResources(mApps);
                mApps = null;
            }
        }

        protected void onReleaseResources(final List<AppOpEntry> apps) {
            // no-op: a List<?> has nothing to close or release or do.
        }
    }

    public static class AppListAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        private final Resources mResources;
        private final AppOpsState mState;

        private List<AppOpEntry> mList;

        public AppListAdapter(final Context context, final AppOpsState state) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mResources = context.getResources();
            mState = state;
        }

        public void setData(final List<AppOpEntry> data) {
            mList = data;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mList == null ? 0 : mList.size();
        }

        @Override
        public AppOpEntry getItem(final int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(final int position) {
            return position;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final AppOpEntry item = getItem(position);
            final View view = convertView == null ?
                    mInflater.inflate(R.layout.app_ops_item, parent, false) : convertView;

            ((ImageView) view.findViewById(R.id.app_icon))
                    .setImageDrawable(item.getAppEntry().getIcon());

            ((TextView) view.findViewById(R.id.app_name))
                    .setText(item.getAppEntry().getLabel());

            ((TextView) view.findViewById(R.id.op_name))
                    .setText(item.getSummaryText(mState));

            ((TextView) view.findViewById(R.id.op_time))
                    .setText(item.getTimeText(mResources, false));

            return view;
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mState = new AppOpsState(getActivity());
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Show a message to the user if there are no appliations to display. (Unlikely, but hey!)
        setEmptyText("No applications"); // TODO Pull this string from a resource instead.

        // Ensure the system knows that we can provide an options menu.
        setHasOptionsMenu(true);

        // Construct an empty adapter to use for displaying the loaded data.
        setListAdapter(mAdapter = new AppListAdapter(getActivity(), mState));

        // Kick the UI off with a progress indicator.
        setListShown(false);

        // Prepare the loader.
        getLoaderManager().initLoader(0, null, this);
    }
    
    @Override
    public void onListItemClick(final ListView l, final View v, final int position, final long id) {
        final AppOpEntry item = mAdapter.getItem(position);
        if (item != null) {
            mCurrentPkgName = item.getAppEntry().getApplicationInfo().packageName;

            final Bundle args = new Bundle();
            args.putString(AppOpsDetails.ARG_PACKAGE_NAME, mCurrentPkgName);
            ((SettingsActivity) getActivity()).startPreferencePanel(AppOpsDetails.class.getName(),
                    args, R.string.app_ops_settings, null, this, RESULT_APP_DETAILS);
        }
    }

    @Override
    public Loader<List<AppOpEntry>> onCreateLoader(final int id, final Bundle args) {
        return new AppListLoader(getActivity(), mState);
    }

    @Override
    public void onLoadFinished(final Loader<List<AppOpEntry>> loader, final List<AppOpEntry> data) {
        mAdapter.setData(data);

        if (isResumed()) {
            // We want to look good when we are visible.
            setListShown(true);
        } else {
            // "No animation" mode on when we ain't visible.
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(final Loader<List<AppOpEntry>> loader) {
        mAdapter.setData(null);
    }
}
