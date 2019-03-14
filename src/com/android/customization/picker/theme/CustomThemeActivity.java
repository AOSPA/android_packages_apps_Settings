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
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.customization.model.theme.OverlayManagerCompat;
import com.android.customization.model.theme.custom.CustomThemeManager;
import com.android.customization.model.theme.custom.FontOptionsProvider;
import com.android.customization.model.theme.custom.IconOptionsProvider;
import com.android.customization.model.theme.custom.ThemeComponentOption;
import com.android.customization.model.theme.custom.ThemeComponentOption.FontOption;
import com.android.customization.model.theme.custom.ThemeComponentOption.IconOption;
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
    private int mCurrentStep;
    private TextView mApplyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector injector = InjectorProvider.getInjector();
        mUserEventLogger = injector.getUserEventLogger(this);
        setContentView(R.layout.activity_custom_theme);
        mApplyButton = findViewById(R.id.next_button);
        mApplyButton.setOnClickListener(view -> onNextOrApply());
        initSteps();

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            // Navigate to the first step
            navigateToStep(0);
        }
    }

    private void navigateToStep(int i) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ComponentStep step = mSteps.get(i);
        Fragment fragment = step.getFragment();

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        // Don't add step 0 to the back stack so that going back from it just finishes the Activity
        if (i > 0) {
            fragmentTransaction.addToBackStack("Step " + i);
        }
        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();
        updateApplyButtonLabel();
    }

    private void initSteps() {
        mSteps = new ArrayList<>();
        OverlayManagerCompat manager = new OverlayManagerCompat(this);
        mSteps.add(new FontStep(new FontOptionsProvider(this, manager), 0, 2));
        mSteps.add(new IconStep(new IconOptionsProvider(this, manager), 1, 2));
        mCurrentStep = 0;
    }

    private void onNextOrApply() {
        if (mCurrentStep < mSteps.size() - 1) {
            // TODO: gather current step's selection
            navigateToStep(mCurrentStep + 1);
        }
        // TODO: handle Apply
    }

    @Override
    public void setCurrentStep(int i) {
        mCurrentStep = i;
        updateApplyButtonLabel();
    }

    private void updateApplyButtonLabel() {
        mApplyButton.setText((mCurrentStep < mSteps.size() -1) ? R.string.custom_theme_next
                : R.string.apply_btn);
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

    private class IconStep extends ComponentStep<IconOption> {

        protected IconStep(ThemeComponentOptionProvider<IconOption> provider,
                int position, int totalSteps) {
            super(R.string.icon_component_title, provider, position, totalSteps);
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
