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

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.PreferenceFrameLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;

public class AppOpsSummary extends Fragment {
    private final static AppOpsState.OpsTemplate[] PAGE_TEMPLATES = new AppOpsState.OpsTemplate[] {
        AppOpsState.LOCATION_TEMPLATE,
        AppOpsState.PERSONAL_TEMPLATE,
        AppOpsState.MESSAGING_TEMPLATE,
        AppOpsState.MEDIA_TEMPLATE,
        AppOpsState.DEVICE_TEMPLATE
    };

    private CharSequence[] mPageNames = null;

    private class AppOpsSummaryAdapter extends FragmentPagerAdapter {
        private final AppOpsCategory[] mCategories = new AppOpsCategory[PAGE_TEMPLATES.length];

        public AppOpsSummaryAdapter(final FragmentManager fm) {
            super(fm);

            for (int i = 0; i < PAGE_TEMPLATES.length; i++) {
                mCategories[i] = new AppOpsCategory(PAGE_TEMPLATES[i]);
            }
        }

        @Override
        public Fragment getItem(final int position) {
            return mCategories[position];
        }

        @Override
        public int getCount() {
            return PAGE_TEMPLATES.length;
        }

        @Override
        public CharSequence getPageTitle(final int position) {
            return mPageNames == null ? null : mPageNames[position];
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        mPageNames = getResources().getTextArray(R.array.app_ops_categories);

        final View view = inflater.inflate(R.layout.app_ops_summary, container, false);

        final ViewPager viewPager = (ViewPager) view.findViewById(R.id.pager);
        viewPager.setAdapter(new AppOpsSummaryAdapter(getChildFragmentManager()));

        final PagerTabStrip tabs = (PagerTabStrip) view.findViewById(R.id.tabs);
        tabs.setTabIndicatorColorResource(R.color.theme_accent);

        if (container instanceof PreferenceFrameLayout) {
            // We force the PreferenceFrameLayout to look at us by doing this magic.
            ((PreferenceFrameLayout.LayoutParams) view.getLayoutParams()).removeBorders = true;
        }

        return view;
    }
}
