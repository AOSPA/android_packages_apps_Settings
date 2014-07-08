package com.android.settings.PALP;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.util.paranoid.DeviceUtils;

// Pager Interface class (Tab Style)
import com.android.settings.PALP.extensions.PagerSlidingTabStrip;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;

//classes for implements

//CM classes
import com.android.settings.cyanogenmod.ButtonSettings;
import com.android.settings.cyanogenmod.PerformanceSettings;

//AOSPA-Legacy classes
import com.android.settings.PALP.DisplaySettingsLP;
import com.android.settings.PALP.GeneralLP;

public class LegacyParts extends SettingsPreferenceFragment {

    ViewPager mViewPager;
    String titleString[];
    ViewGroup mContainer;
    PagerSlidingTabStrip mTabs;

    static Bundle mSavedState;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContainer = container;
        final ActionBar actionBar = getActivity().getActionBar();

        //action bar icon for legacy parts
        actionBar.setIcon(R.drawable.ic_settings_palp);

        View view = inflater.inflate(R.layout.legacy_parts, container, false);
        mViewPager = (ViewPager) view.findViewById(R.id.viewPager);
        mTabs = (PagerSlidingTabStrip) view.findViewById(R.id.tabs);
        StatusBarAdapter StatusBarAdapter = new StatusBarAdapter(getFragmentManager());
        mViewPager.setAdapter(StatusBarAdapter);
        mTabs.setViewPager(mViewPager);

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

    class StatusBarAdapter extends FragmentPagerAdapter {
        String titles[] = getTitles();
        private Fragment frags[] = new Fragment[titles.length];

        public StatusBarAdapter(FragmentManager fm) {
            super(fm);
            //add classes here
            frags[0] = new GeneralLP(); // Display Settings (Legacy Parts)
            frags[1] = new DisplaySettingsLP(); // Display Settings (Legacy Parts)
            frags[2] = new ButtonSettings(); // Button Settings
            frags[3] = new PerformanceSettings(); // Performance Settings
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
        titleString = new String[]{
                    //add class titles here
                    getString(R.string.palp_settings_general_title), // General Settings
                    getString(R.string.display_settings_title), // Display Settings (Legacy Parts)
                    getString(R.string.button_settings), // Button Settings
                    getString(R.string.performance_settings_title), // Performance Settings
    };
        return titleString;
    }
}
