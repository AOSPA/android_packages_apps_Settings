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

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.android.customization.model.theme.custom.CustomThemeManager;
import com.android.customization.model.theme.custom.ThemeComponentOption;
import com.android.customization.model.theme.custom.ThemeComponentOptionProvider;
import com.android.customization.widget.OptionSelectorController;
import com.android.wallpaper.R;
import com.android.wallpaper.picker.ToolbarFragment;

public class CustomThemeComponentFragment extends ToolbarFragment {
    private static final String ARG_KEY_POSITION = "CustomThemeComponentFragment.position";
    private static final String ARG_KEY_TITLE_RES_ID = "CustomThemeComponentFragment.title_res";
    private CustomThemeComponentFragmentHost mHost;

    public interface CustomThemeComponentFragmentHost {
        void delete();
        void cancel();
        ThemeComponentOptionProvider<? extends ThemeComponentOption> getComponentOptionProvider(
                int position);

        CustomThemeManager getCustomThemeManager();

        void setCurrentStep(int step);
    }

    public static CustomThemeComponentFragment newInstance(CharSequence toolbarTitle, int position,
            int titleResId) {
        CustomThemeComponentFragment fragment = new CustomThemeComponentFragment();
        Bundle arguments = ToolbarFragment.createArguments(toolbarTitle);
        arguments.putInt(ARG_KEY_POSITION, position);
        arguments.putInt(ARG_KEY_TITLE_RES_ID, titleResId);
        fragment.setArguments(arguments);
        return fragment;
    }

    private ThemeComponentOptionProvider<? extends ThemeComponentOption> mProvider;
    private CustomThemeManager mCustomThemeManager;
    private int mPosition;
    @StringRes private int mTitleResId;

    private RecyclerView mOptionsContainer;
    private OptionSelectorController<ThemeComponentOption> mOptionsController;
    private CardView mPreviewCard;
    private TextView mTitle;
    private ThemeComponentOption mSelectedOption;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPosition = getArguments().getInt(ARG_KEY_POSITION);
        mTitleResId = getArguments().getInt(ARG_KEY_TITLE_RES_ID);
        mProvider = mHost.getComponentOptionProvider(mPosition);
        mCustomThemeManager = mHost.getCustomThemeManager();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mHost = (CustomThemeComponentFragmentHost) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.fragment_custom_theme_component, container, /* attachToRoot */ false);
        // No original theme means it's a new one, so no toolbar icon for deleting it is needed
        if (mCustomThemeManager.getOriginalTheme() == null) {
            setUpToolbar(view);
        } else {
            setUpToolbar(view, R.menu.custom_theme_editor_menu);
        }
        mToolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_close_24px, null));
        mToolbar.setNavigationContentDescription(R.string.cancel);
        mToolbar.setNavigationOnClickListener(v -> mHost.cancel());
        mOptionsContainer = view.findViewById(R.id.options_container);
        mPreviewCard = view.findViewById(R.id.component_preview_card);
        mTitle = view.findViewById(R.id.component_options_title);
        mTitle.setText(mTitleResId);
        setUpOptions();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mHost.setCurrentStep(mPosition);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.custom_theme_delete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.delete_custom_theme_confirmation)
                    .setPositiveButton(R.string.delete_custom_theme_button,
                            (dialogInterface, i) -> mHost.delete())
                    .setNegativeButton(R.string.cancel, null)
                    .create()
                    .show();
            return true;
        }
        return super.onMenuItemClick(item);
    }

    public ThemeComponentOption getSelectedOption() {
        return mSelectedOption;
    }

    private void bindPreview() {
        mSelectedOption.bindPreview(mPreviewCard);
    }

    private void setUpOptions() {
        mProvider.fetch(options -> {
            mOptionsController = new OptionSelectorController(mOptionsContainer, options);

            mOptionsController.addListener(selected -> {
                mSelectedOption = (ThemeComponentOption) selected;
                bindPreview();
            });
            mOptionsController.initOptions(mCustomThemeManager);

            for (ThemeComponentOption option : options) {
                if (option.isActive(mCustomThemeManager)) {
                    mSelectedOption = option;
                    break;
                }
            }
            if (mSelectedOption == null) {
                mSelectedOption = options.get(0);
            }
            mOptionsController.setSelectedOption(mSelectedOption);
        }, false);
    }
}
