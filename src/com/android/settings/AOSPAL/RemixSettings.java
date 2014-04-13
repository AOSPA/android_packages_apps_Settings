package com.android.settings.AOSPAL;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.os.Bundle;
import android.provider.Settings;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.util.paranoid.DeviceUtils;

import com.android.settings.AOSPAL.AnimationSettings;
import com.android.settings.AOSPAL.NavBarSettings;
import com.android.settings.AOSPAL.NotificationDrawerQsSettings;
import com.android.settings.AOSPAL.StatusBarSettings;
import com.android.settings.AOSPAL.SystemSettings;
import com.android.settings.AOSPAL.LockscreenSettings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;


import java.util.ArrayList;
import java.util.List;

public class RemixSettings extends SettingsPreferenceFragment {

    private static final String BUGREPORT_URL = "https://sites.google.com/site/aospalrom/bug-report";

    PagerTabStrip mPagerTabStrip;
    ViewPager mViewPager;

    String titleString[];

    ViewGroup mContainer;

    static Bundle mSavedState;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContainer = container;
        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setIcon(R.drawable.ic_settings_system);

        View view = inflater.inflate(R.layout.remix_settings, container, false);
        mViewPager = (ViewPager) view.findViewById(R.id.viewPager);
        mPagerTabStrip = (PagerTabStrip) view.findViewById(R.id.pagerTabStrip);
        mPagerTabStrip.setDrawFullUnderline(true);
        mPagerTabStrip.setTabIndicatorColorResource(android.R.color.holo_blue_light);

        StatusBarAdapter StatusBarAdapter = new StatusBarAdapter(getFragmentManager());
        mViewPager.setAdapter(StatusBarAdapter);

        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // After confirming PreferenceScreen is available, we call super.
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle saveState) {
        super.onSaveInstanceState(saveState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!DeviceUtils.isTablet(getActivity())) {
            mContainer.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.remix_settings_item, menu);
    }

    /**
     * On selecting action bar icons
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Take appropriate action for each action item click
        switch (item.getItemId()) {
        case R.id.action_bugreport:
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BUGREPORT_URL));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(intent);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    class StatusBarAdapter extends FragmentStatePagerAdapter {
        String titles[] = getTitles();
        private Fragment frags[] = new Fragment[titles.length];

        public StatusBarAdapter(FragmentManager fm) {
            super(fm);
            frags[0] = new SystemSettings();
            frags[1] = new StatusBarSettings();
            frags[2] = new NavBarSettings();
            frags[3] = new NotificationDrawerQsSettings();
            frags[4] = new LockscreenSettings();
            frags[5] = new AnimationSettings();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        @Override
        public Fragment getItem(int position) {
            return frags[position];
        }

        @Override
        public int getCount() {
            return frags.length;
        }
    }

    private String[] getTitles() {
        String titleString[];
        if (!DeviceUtils.isPhone(getActivity())) {
        titleString = new String[]{
                    getString(R.string.remix_settings_system_title),
                    getString(R.string.remix_settings_statusbar_title),
                    getString(R.string.navigation_bar),
                    getString(R.string.remix_settings_notification_drawer),
                    getString(R.string.remix_settings_lockscreen_title),
                    getString(R.string.remix_settings_animations_title)};
        } else {
        titleString = new String[]{
                    getString(R.string.remix_settings_system_title),
                    getString(R.string.remix_settings_statusbar_title),
                    getString(R.string.navigation_bar),
                    getString(R.string.remix_settings_notification_drawer_qs),
                    getString(R.string.remix_settings_lockscreen_title),
                    getString(R.string.remix_settings_animations_title)};
        }
        return titleString;
    }
}
