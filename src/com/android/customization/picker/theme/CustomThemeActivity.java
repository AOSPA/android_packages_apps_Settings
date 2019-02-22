/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.customization.picker.theme;

import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.customization.model.theme.OverlayManagerCompat;
import com.android.customization.model.theme.custom.CustomThemeManager;
import com.android.customization.model.theme.custom.FontOptionsProvider;
import com.android.customization.model.theme.custom.ThemeComponentOption;
import com.android.customization.model.theme.custom.ThemeComponentOption.FontOption;
import com.android.customization.model.theme.custom.ThemeComponentOptionProvider;
import com.android.customization.picker.theme.CustomThemeComponentFragment.CustomThemeComponentFragmentHost;
import com.android.wallpaper.R;
import com.android.wallpaper.module.Injector;
import com.android.wallpaper.module.InjectorProvider;
import com.android.wallpaper.module.UserEventLogger;

import java.util.ArrayList;
import java.util.List;

public class CustomThemeActivity extends FragmentActivity implements
        CustomThemeComponentFragmentHost {

    private UserEventLogger mUserEventLogger;
    private List<ComponentStep<?>> mSteps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector injector = InjectorProvider.getInjector();
        mUserEventLogger = injector.getUserEventLogger(this);
        setContentView(R.layout.activity_custom_theme);
        initSteps();

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            // Navigate to the first step
            navigateToStep(0);
        }
    }

    private void navigateToStep(int i) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        ComponentStep step = mSteps.get(i);
        Fragment fragment = step.getFragment();

        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commitNow();
    }

    private void initSteps() {
        mSteps = new ArrayList<>();
        OverlayManagerCompat manager = new OverlayManagerCompat(this);
        mSteps.add(new FontStep(new FontOptionsProvider(this, manager), 0, mSteps.size() + 1));
    }


    @Override
    public void delete() {

    }

    @Override
    public void cancel() {

    }

    @Override
    public ThemeComponentOptionProvider<? extends ThemeComponentOption> getComponentOptionProvider(
            int position) {
        return mSteps.get(position).provider;
    }

    @Override
    public CustomThemeManager getCustomThemeManager() {
        return null;
    }

    /**
     * Represents a step in selecting a custom theme, picking a particular component (eg font,
     * color, shape, etc).
     * Each step has a Fragment instance associated that instances of this class will provide.
     */
    private static abstract class ComponentStep<T extends ThemeComponentOption> {
        @StringRes final int titleResId;
        final ThemeComponentOptionProvider<T> provider;
        final int totalSteps;
        final int position;
        private CustomThemeComponentFragment mFragment;

        protected ComponentStep(@StringRes int titleResId, ThemeComponentOptionProvider<T> provider,
                int position, int totalSteps) {
            this.titleResId = titleResId;
            this.provider = provider;
            this.position = position;
            this.totalSteps = totalSteps;
        }

        CustomThemeComponentFragment getFragment() {
            if (mFragment == null) {
                mFragment = createFragment();
            }
            return mFragment;
        }

        /**
         * @return a newly created fragment that will handle this step's UI.
         */
        abstract CustomThemeComponentFragment createFragment();
    }

    private class FontStep extends ComponentStep<FontOption> {

        protected FontStep(ThemeComponentOptionProvider<FontOption> provider,
                int position, int totalSteps) {
            super(R.string.font_component_title, provider, position, totalSteps);
        }

        @Override
        CustomThemeComponentFragment createFragment() {
            return CustomThemeComponentFragment.newInstance(
                    CustomThemeActivity.this.getString(R.string.custom_theme_fragment_title),
                    position,
                    totalSteps,
                    titleResId);
        }
    }
}
