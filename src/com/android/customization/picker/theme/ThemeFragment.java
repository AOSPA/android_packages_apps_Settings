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
package com.android.customization.picker.theme;

import android.app.Activity;
import android.app.WallpaperColors;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.android.customization.model.CustomizationManager.Callback;
import com.android.customization.model.theme.custom.CustomTheme;
import com.android.customization.model.theme.ThemeBundle;
import com.android.customization.model.theme.ThemeBundle.PreviewInfo;
import com.android.customization.model.theme.ThemeManager;
import com.android.customization.picker.BasePreviewAdapter;
import com.android.customization.picker.BasePreviewAdapter.PreviewPage;
import com.android.customization.widget.OptionSelectorController;
import com.android.customization.widget.PreviewPager;
import com.android.wallpaper.R;
import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.module.CurrentWallpaperInfoFactory;
import com.android.wallpaper.module.InjectorProvider;
import com.android.wallpaper.picker.ToolbarFragment;

/**
 * Fragment that contains the main UI for selecting and applying a ThemeBundle.
 */
public class ThemeFragment extends ToolbarFragment {

    private static final String TAG = "ThemeFragment";
    private static final String KEY_SELECTED_THEME = "ThemeFragment.SelectedThemeBundle";

    /**
     * Interface to be implemented by an Activity hosting a {@link ThemeFragment}
     */
    public interface ThemeFragmentHost {
        ThemeManager getThemeManager();
    }
    public static ThemeFragment newInstance(CharSequence title) {
        ThemeFragment fragment = new ThemeFragment();
        fragment.setArguments(ToolbarFragment.createArguments(title));
        return fragment;
    }

    private RecyclerView mOptionsContainer;
    private OptionSelectorController mOptionsController;
    private ThemeManager mThemeManager;
    private ThemeBundle mSelectedTheme;
    private ThemePreviewAdapter mAdapter;
    private PreviewPager mPreviewPager;
    private boolean mUseMyWallpaper;
    private WallpaperInfo mCurrentHomeWallpaper;
    private CurrentWallpaperInfoFactory mCurrentWallpaperFactory;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mThemeManager = ((ThemeFragmentHost) context).getThemeManager();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.fragment_theme_picker, container, /* attachToRoot */ false);
        setUpToolbar(view);

        mCurrentWallpaperFactory = InjectorProvider.getInjector()
                .getCurrentWallpaperFactory(getActivity().getApplicationContext());
        mPreviewPager = view.findViewById(R.id.theme_preview_pager);
        mOptionsContainer = view.findViewById(R.id.options_container);
        view.findViewById(R.id.apply_button).setOnClickListener(v -> {
            mThemeManager.apply(mSelectedTheme, new Callback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), R.string.applied_theme_msg,
                            Toast.LENGTH_LONG).show();
                    getActivity().finish();
                }

                @Override
                public void onError(@Nullable Throwable throwable) {
                    Log.w(TAG, "Error applying theme", throwable);
                    Toast.makeText(getContext(), R.string.apply_theme_error_msg,
                            Toast.LENGTH_LONG).show();
                }
            });

        });
        ((CheckBox)view.findViewById(R.id.use_my_wallpaper)).setOnCheckedChangeListener(
                this::onUseMyWallpaperCheckChanged);

        setUpOptions(savedInstanceState);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadWallpaper();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedTheme != null && !mSelectedTheme.isActive(mThemeManager)) {
            outState.putString(KEY_SELECTED_THEME, mSelectedTheme.getSerializedPackages());
        }
    }

    private void onUseMyWallpaperCheckChanged(CompoundButton checkbox, boolean checked) {
        mUseMyWallpaper = checked;
        reloadWallpaper();
    }

    private void reloadWallpaper() {
        if (mUseMyWallpaper) {
            mCurrentWallpaperFactory.createCurrentWallpaperInfos(
                    (homeWallpaper, lockWallpaper, presentationMode) -> {
                        if (mSelectedTheme != null) {
                            mCurrentHomeWallpaper = homeWallpaper;
                            mSelectedTheme.setOverrideThemeWallpaper(homeWallpaper);
                            if (mAdapter != null) {
                                mAdapter.rebindWallpaperIfAvailable();
                            }
                        }
            }, false);
        } else {
            mCurrentHomeWallpaper = null;
            if (mSelectedTheme != null) {
                mSelectedTheme.setOverrideThemeWallpaper(null);
                if (mAdapter != null) {
                    mAdapter.rebindWallpaperIfAvailable();
                }
            }
        }
    }

    private void createAdapter() {
        mAdapter = new ThemePreviewAdapter(getActivity(), mSelectedTheme);
        mPreviewPager.setAdapter(mAdapter);
    }

    private void setUpOptions(@Nullable Bundle savedInstanceState) {
        mThemeManager.fetchOptions(options -> {
            mOptionsController = new OptionSelectorController(mOptionsContainer, options);
            mOptionsController.addListener(selected -> {
                if (selected instanceof CustomTheme && !((CustomTheme) selected).isDefined()) {
                    navigateToCustomTheme();
                } else {
                    mSelectedTheme = (ThemeBundle) selected;
                    mSelectedTheme.setOverrideThemeWallpaper(mCurrentHomeWallpaper);
                    createAdapter();
                }
            });
            mOptionsController.initOptions(mThemeManager);
            String previouslySelected = savedInstanceState != null
                    ? savedInstanceState.getString(KEY_SELECTED_THEME) : null;
            for (ThemeBundle theme : options) {
                if (previouslySelected != null
                        && previouslySelected.equals(theme.getSerializedPackages())) {
                    mSelectedTheme = theme;
                } else if (theme.isActive(mThemeManager)) {
                    mSelectedTheme = theme;
                    break;
                }
            }
            if (mSelectedTheme == null) {
                // Select the default theme if there is no matching custom enabled theme
                // TODO(b/124796742): default to custom if there is no matching theme bundle
                mSelectedTheme = options.get(0);
            }
            mOptionsController.setSelectedOption(mSelectedTheme);
        });
        createAdapter();
    }

    private void navigateToCustomTheme() {
        Intent intent = new Intent(getActivity(), CustomThemeActivity.class);
        startActivity(intent);
    }

    private static abstract class ThemePreviewPage extends PreviewPage {
        @StringRes final int nameResId;
        @DrawableRes final int iconSrc;
        @LayoutRes final int contentLayoutRes;
        @ColorInt final int accentColor;
        private final LayoutInflater inflater;

        private ThemePreviewPage(Context context, @StringRes int titleResId,
                @DrawableRes int iconSrc, @LayoutRes int contentLayoutRes,
                @ColorInt int accentColor) {
            super(null);
            this.nameResId = titleResId;
            this.iconSrc = iconSrc;
            this.contentLayoutRes = contentLayoutRes;
            this.accentColor = accentColor;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public void bindPreviewContent() {
            TextView header = card.findViewById(R.id.theme_preview_card_header);
            header.setText(nameResId);
            header.setCompoundDrawablesWithIntrinsicBounds(0, iconSrc, 0, 0);
            header.setCompoundDrawableTintList(ColorStateList.valueOf(accentColor));

            ViewGroup body = card.findViewById(R.id.theme_preview_card_body_container);
            inflater.inflate(contentLayoutRes, body, true);
            bindBody(false);
        }

        protected boolean containsWallpaper() {
            return false;
        }

        protected abstract void bindBody(boolean forceRebind);
    }

    /**
     * Adapter class for mPreviewPager.
     * This is a ViewPager as it allows for a nice pagination effect (ie, pages snap on swipe,
     * we don't want to just scroll)
     */
    private static class ThemePreviewAdapter extends BasePreviewAdapter<ThemePreviewPage> {

        private int[] mIconIds = {
                R.id.preview_icon_0, R.id.preview_icon_1, R.id.preview_icon_2, R.id.preview_icon_3,
                R.id.preview_icon_4, R.id.preview_icon_5
        };

        ThemePreviewAdapter(Activity activity, ThemeBundle theme) {
            super(activity, R.layout.theme_preview_card);
            final Resources res = activity.getResources();
            final PreviewInfo previewInfo = theme.getPreviewInfo();
            addPage(new ThemePreviewPage(activity, R.string.preview_name_font, R.drawable.ic_font,
                    R.layout.preview_card_font_content,
                    previewInfo.resolveAccentColor(res)) {
                @Override
                protected void bindBody(boolean forceRebind) {
                    TextView title = card.findViewById(R.id.font_card_title);
                    title.setTypeface(previewInfo.headlineFontFamily);
                    TextView body = card.findViewById(R.id.font_card_body);
                    body.setTypeface(previewInfo.bodyFontFamily);
                }
            });
            if (previewInfo.icons.size() >= mIconIds.length) {
                addPage(new ThemePreviewPage(activity, R.string.preview_name_icon,
                        R.drawable.ic_wifi_24px, R.layout.preview_card_icon_content,
                        previewInfo.resolveAccentColor(res)) {
                    @Override
                    protected void bindBody(boolean forceRebind) {
                        for (int i = 0; i < mIconIds.length; i++) {
                            ((ImageView) card.findViewById(mIconIds[i])).setImageDrawable(
                                    previewInfo.icons.get(i));
                        }
                    }
                });
            }
            if (previewInfo.colorPreviewAsset != null) {
                addPage(new ThemePreviewPage(activity, R.string.preview_name_color,
                        R.drawable.ic_colorize_24px, R.layout.preview_card_static_content,
                        previewInfo.resolveAccentColor(res)) {
                    @Override
                    protected void bindBody(boolean forceRebind) {
                        ImageView staticImage = card.findViewById(R.id.preview_static_image);
                        previewInfo.colorPreviewAsset.loadDrawable(activity,
                                staticImage, card.getCardBackgroundColor().getDefaultColor());
                        staticImage.getLayoutParams().width = res.getDimensionPixelSize(
                                R.dimen.color_preview_image_width);
                        staticImage.getLayoutParams().height = res.getDimensionPixelSize(
                                R.dimen.color_preview_image_height);
                    }
                });
            }
            if (previewInfo.shapePreviewAsset != null) {
                addPage(new ThemePreviewPage(activity, R.string.preview_name_shape,
                        R.drawable.ic_shapes_24px, R.layout.preview_card_static_content,
                        previewInfo.resolveAccentColor(res)) {
                    @Override
                    protected void bindBody(boolean forceRebind) {
                        ImageView staticImage = card.findViewById(R.id.preview_static_image);
                        previewInfo.shapePreviewAsset.loadDrawable(activity,
                                staticImage, card.getCardBackgroundColor().getDefaultColor());

                        staticImage.getLayoutParams().width = res.getDimensionPixelSize(
                                R.dimen.shape_preview_image_width);
                        staticImage.getLayoutParams().height = res.getDimensionPixelSize(
                                R.dimen.shape_preview_image_height);
                    }
                });
            }
            if (previewInfo.wallpaperAsset != null) {
                addPage(new ThemePreviewPage(activity, R.string.preview_name_wallpaper,
                        R.drawable.ic_wallpaper_24px, R.layout.preview_card_wallpaper_content,
                        previewInfo.resolveAccentColor(res)) {

                    private final WallpaperPreviewLayoutListener  mListener =
                            new WallpaperPreviewLayoutListener(theme, previewInfo);

                    @Override
                    protected boolean containsWallpaper() {
                        return true;
                    }

                    @Override
                    protected void bindBody(boolean forceRebind) {
                        if (card == null) {
                            return;
                        }
                        card.addOnLayoutChangeListener(mListener);
                        if (forceRebind) {
                            card.requestLayout();
                        }
                    }
                });
            }
        }

        public void rebindWallpaperIfAvailable() {
            for (ThemePreviewPage page : mPages) {
                if (page.containsWallpaper()) {
                    page.bindBody(true);
                }
            }
        }

        private static class WallpaperPreviewLayoutListener implements OnLayoutChangeListener {
            private final ThemeBundle mTheme;
            private final PreviewInfo mPreviewInfo;

            public WallpaperPreviewLayoutListener(ThemeBundle theme, PreviewInfo previewInfo) {
                mTheme = theme;
                mPreviewInfo = previewInfo;
            }

            @Override
            public void onLayoutChange(View view, int left, int top, int right,
                    int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int targetWidth = right - left;
                int targetHeight = bottom - top;
                if (targetWidth > 0 && targetHeight > 0) {
                    mTheme.getWallpaperPreviewAsset(view.getContext()).decodeBitmap(
                            targetWidth, targetHeight, bitmap -> setWallpaperBitmap(view, bitmap));
                    view.removeOnLayoutChangeListener(this);
                }
            }

            private void setWallpaperBitmap(View view, Bitmap bitmap) {
                Resources res = view.getContext().getResources();
                view.findViewById(
                        R.id.theme_preview_card_background)
                        .setBackground(
                                new BitmapDrawable(res, bitmap));
                int colorsHint = WallpaperColors.fromBitmap(bitmap).getColorHints();
                TextView header = view.findViewById(R.id.theme_preview_card_header);
                if ((colorsHint & WallpaperColors.HINT_SUPPORTS_DARK_TEXT) == 0) {
                    int colorLight = res.getColor(R.color.text_color_light, null);
                    header.setTextColor(colorLight);
                    header.setCompoundDrawableTintList(ColorStateList.valueOf(colorLight));
                } else {
                    header.setTextColor(res.getColor(R.color.text_color_dark, null));
                    header.setCompoundDrawableTintList(ColorStateList.valueOf(
                            mPreviewInfo.colorAccentLight));
                }
            }
        }
    }
}
