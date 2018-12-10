/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.customization.picker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.wallpaper.R;
import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.module.DailyLoggingAlarmScheduler;
import com.android.wallpaper.module.FormFactorChecker;
import com.android.wallpaper.module.Injector;
import com.android.wallpaper.module.InjectorProvider;
import com.android.wallpaper.module.UserEventLogger;
import com.android.wallpaper.picker.CategoryFragment;
import com.android.wallpaper.picker.CategoryFragment.CategoryFragmentHost;
import com.android.wallpaper.picker.MyPhotosLauncher.PermissionChangedListener;
import com.android.wallpaper.picker.TopLevelPickerActivity;
import com.android.wallpaper.picker.WallpaperPickerDelegate;
import com.android.wallpaper.picker.WallpapersUiContainer;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

/**
 *  Main Activity allowing containing a bottom nav bar for the user to switch between the different
 *  Fragments providing customization options.
 */
public class CustomizationPickerActivity extends FragmentActivity implements WallpapersUiContainer,
        CategoryFragmentHost {

    private static final String TAG = "CustomizationPickerActivity";

    private WallpaperPickerDelegate mDelegate;
    private UserEventLogger mUserEventLogger;
    private BottomNavigationView mBottomNav;

    private static final Map<Integer, CustomizationSection> mSections = new HashMap<>();
    private CategoryFragment mWallpaperCategoryFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector injector = InjectorProvider.getInjector();
        mDelegate = new WallpaperPickerDelegate(this, this, injector);
        mUserEventLogger = injector.getUserEventLogger(this);

        initSections();

        if (!supportsCustomization()) {
            Log.w(TAG, "Themes not supported, reverting to Wallpaper Picker");
            Intent intent = new Intent(this, TopLevelPickerActivity.class);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_customization_picker_main);
        setUpBottomNavView();

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        boolean forceCategoryRefresh = false;
        if (fragment == null) {
            // App launch specific logic: log the "app launched" event and set up daily logging.
            mUserEventLogger.logAppLaunched();
            DailyLoggingAlarmScheduler.setAlarm(getApplicationContext());
            navigateToSection(R.id.nav_wallpaper);
            forceCategoryRefresh = true;
        }

        mDelegate.initialize(forceCategoryRefresh);

    }

    private boolean supportsCustomization() {
        //TODO (santie): the check for sections.size() should be > 1: if we only have wallpaper we
        // should default to the Wallpaper only UI
        return mDelegate.getFormFactor() == FormFactorChecker.FORM_FACTOR_MOBILE
                && mSections.size() > 0;
    }

    private void initSections() {
        mSections.put(R.id.nav_wallpaper, new WallpaperSection(R.id.nav_wallpaper));
        //TODO (santie): add other sections if supported by the device
    }

    private void setUpBottomNavView() {
        mBottomNav = findViewById(R.id.main_bottom_nav);
        Menu menu = mBottomNav.getMenu();
        for (int i = menu.size() - 1; i >= 0; i--) {
            MenuItem item = menu.getItem(i);
            if (!mSections.containsKey(item.getItemId())) {
                menu.removeItem(item.getItemId());
            }
        }

        mBottomNav.setOnNavigationItemSelectedListener(item -> {
            switchFragment(item.getItemId());
            return true;
        });
    }

    private void navigateToSection(@IdRes int id) {
        mBottomNav.setSelectedItemId(id);
    }

    private void switchFragment(int id) {
        final FragmentManager fragmentManager = getSupportFragmentManager();

        Fragment fragment = mSections.get(id).getFragment();

        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commitNow();
    }


    @Override
    public void requestExternalStoragePermission(PermissionChangedListener listener) {

    }

    @Override
    public boolean isReadExternalStoragePermissionGranted() {
        return false;
    }

    @Override
    public void showViewOnlyPreview(WallpaperInfo wallpaperInfo) {

    }

    /**
     * Shows the picker activity for the given category.
     */
    @Override
    public void show(String collectionId) {
        mDelegate.show(collectionId);
    }

    @Override
    public void onWallpapersReady() {

    }

    @Nullable
    @Override
    public CategoryFragment getCategoryFragment() {
        return mWallpaperCategoryFragment;
    }

    @Override
    public void doneFetchingCategories() {

    }

    /**
     * Represents a section of the Picker (eg "Theme", "Clock", etc).
     * There should be a concrete subclass per available section, providing the corresponding
     * Fragment to be displayed when switching to each section.
     */
    static abstract class CustomizationSection {

        /**
         * IdRes used to identify this section in the BottomNavigationView menu.
         */
        @IdRes final int id;

        private CustomizationSection(@IdRes int id) {
            this.id = id;
        }

        /**
         * @return the Fragment corresponding to this section.
         */
        abstract Fragment getFragment();

    }

    /**
     * {@link CustomizationSection} corresponding to the "Wallpaper" section of the Picker.
     */
    private class WallpaperSection extends CustomizationSection {

        private WallpaperSection(int id) {
            super(id);
        }

        @Override
        Fragment getFragment() {
            if (mWallpaperCategoryFragment == null) {
                mWallpaperCategoryFragment = new CategoryFragment();
            }
            return mWallpaperCategoryFragment;
        }
    }
}
