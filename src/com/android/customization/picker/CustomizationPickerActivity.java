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

import androidx.annotation.Nullable;

import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.module.FormFactorChecker;
import com.android.wallpaper.module.Injector;
import com.android.wallpaper.module.InjectorProvider;
import com.android.wallpaper.module.UserEventLogger;
import com.android.wallpaper.picker.BaseActivity;
import com.android.wallpaper.picker.CategoryFragment;
import com.android.wallpaper.picker.CategoryFragment.CategoryFragmentHost;
import com.android.wallpaper.picker.MyPhotosLauncher.PermissionChangedListener;
import com.android.wallpaper.picker.TopLevelPickerActivity;
import com.android.wallpaper.picker.WallpaperPickerDelegate;
import com.android.wallpaper.picker.WallpapersUiContainer;

//TODO(santie): implement
public class CustomizationPickerActivity extends BaseActivity implements WallpapersUiContainer,
        CategoryFragmentHost {

    private static final String TAG = "CustomizationPickerActivity";

    private WallpaperPickerDelegate mDelegate;
    private UserEventLogger mUserEventLogger;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector injector = InjectorProvider.getInjector();
        mDelegate = new WallpaperPickerDelegate(this, this, injector);
        mUserEventLogger = injector.getUserEventLogger(this);

        if (!supportsCustomization()) {
            Log.w(TAG, "Themes not supported, reverting to Wallpaper Picker");
            Intent intent = new Intent(this, TopLevelPickerActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private boolean supportsCustomization() {
        // TODO (santie): check for actual themes support
        return mDelegate.getFormFactor() == FormFactorChecker.FORM_FACTOR_MOBILE;
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
        return null;
    }

    @Override
    public void doneFetchingCategories() {

    }
}
