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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.customization.model.theme.custom.ThemeComponentOption;
import com.android.customization.model.theme.custom.ThemeComponentOptionProvider;
import com.android.customization.widget.OptionSelectorController;
import com.android.wallpaper.R;
import com.android.wallpaper.picker.ToolbarFragment;

public class CustomThemeNameFragment extends CustomThemeStepFragment {

    public static CustomThemeNameFragment newInstance(CharSequence toolbarTitle, int position,
            int titleResId) {
        CustomThemeNameFragment fragment = new CustomThemeNameFragment();
        Bundle arguments = ToolbarFragment.createArguments(toolbarTitle);
        arguments.putInt(ARG_KEY_POSITION, position);
        arguments.putInt(ARG_KEY_TITLE_RES_ID, titleResId);
        fragment.setArguments(arguments);
        return fragment;
    }

    private EditText mNameEditor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mTitle = view.findViewById(R.id.component_options_title);
        mTitle.setText(mTitleResId);
        mNameEditor = view.findViewById(R.id.custom_theme_name);
        mNameEditor.setText(mCustomThemeManager.getOriginalTheme().getTitle());
        bindCover();

        return view;
    }

    private void bindCover() {

    }

    @Override
    protected int getFragmentLayoutResId() {
        return R.layout.fragment_custom_theme_name;
    }

    public String getThemeName() {
        return mNameEditor.getText().toString();
    }
}
