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
package com.android.customization.picker.grid;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.android.customization.model.grid.GridOption;
import com.android.customization.model.grid.GridOptionsManager;
import com.android.customization.picker.BasePreviewAdapter;
import com.android.customization.picker.BasePreviewAdapter.PreviewPage;
import com.android.customization.widget.OptionSelectorController;
import com.android.customization.widget.PreviewPager;
import com.android.wallpaper.R;
import com.android.wallpaper.asset.Asset;
import com.android.wallpaper.asset.ContentUriAsset;
import com.android.wallpaper.model.WallpaperInfo;
import com.android.wallpaper.module.CurrentWallpaperInfoFactory;
import com.android.wallpaper.module.InjectorProvider;
import com.android.wallpaper.picker.ToolbarFragment;

import com.bumptech.glide.request.RequestOptions;

/**
 * Fragment that contains the UI for selecting and applying a GridOption.
 */
public class GridFragment extends ToolbarFragment {

    /**
     * Interface to be implemented by an Activity hosting a {@link GridFragment}
     */
    public interface GridFragmentHost {
        GridOptionsManager getGridOptionsManager();
    }

    public static GridFragment newInstance(CharSequence title) {
        GridFragment fragment = new GridFragment();
        fragment.setArguments(ToolbarFragment.createArguments(title));
        return fragment;
    }

    private boolean mIsWallpaperInfoReady;
    private WallpaperInfo mHomeWallpaper;
    private float mScreenAspectRatio;
    private int mPageHeight;
    private int mPageWidth;
    private GridPreviewAdapter mAdapter;
    private RecyclerView mOptionsContainer;
    private OptionSelectorController mOptionsController;
    private GridOptionsManager mGridManager;
    private GridOption mSelectedOption;
    private PreviewPager mPreviewPager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mGridManager = ((GridFragmentHost) context).getGridOptionsManager();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.fragment_grid_picker, container, /* attachToRoot */ false);
        setUpToolbar(view);
        mPreviewPager = view.findViewById(R.id.grid_preview_pager);
        mOptionsContainer = view.findViewById(R.id.options_container);
        final Resources res = getContext().getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        mScreenAspectRatio = (float) dm.heightPixels / dm.widthPixels;
        setUpOptions();
        view.findViewById(R.id.apply_button).setOnClickListener(v -> {
            mGridManager.apply(mSelectedOption);
            getActivity().finish();
        });
        CurrentWallpaperInfoFactory factory = InjectorProvider.getInjector()
                .getCurrentWallpaperFactory(getActivity().getApplicationContext());

        factory.createCurrentWallpaperInfos((homeWallpaper, lockWallpaper, presentationMode) -> {
            mHomeWallpaper = homeWallpaper;
            mIsWallpaperInfoReady = true;
            if (mAdapter != null) {
                mAdapter.onWallpaperInfoLoaded();
            }
        }, false);
        view.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                mPageHeight = mPreviewPager.getHeight() - mPreviewPager.getPaddingTop() -
                        res.getDimensionPixelSize(R.dimen.indicator_container_height);
                mPageWidth = (int) (mPageHeight / mScreenAspectRatio);
                mPreviewPager.forceCardWidth(mPageWidth);
                view.removeOnLayoutChangeListener(this);
            }
        });
        return view;
    }

    private void createAdapter() {
        mAdapter = new GridPreviewAdapter(mSelectedOption);
        mPreviewPager.setAdapter(mAdapter);
    }

    private void setUpOptions() {
        mGridManager.fetchOptions(options -> {
            mOptionsController = new OptionSelectorController(mOptionsContainer, options);

            mOptionsController.addListener(selected -> {
                mSelectedOption = (GridOption) selected;
                createAdapter();
            });
            mOptionsController.initOptions(mGridManager);
            for (GridOption option : options) {
                if (option.isActive(mGridManager)) {
                    mSelectedOption = option;
                }
            }
            // For development only, as there should always be a grid set.
            if (mSelectedOption == null) {
                mSelectedOption = options.get(0);
            }
            createAdapter();
        });
    }

    private class GridPreviewPage extends PreviewPage {
        private final int mPageId;
        private final Asset mPreviewAsset;
        private final int mCols;
        private final int mRows;
        private final Activity mActivity;

		private ImageView mPreview;

        private GridPreviewPage(Activity activity, int id, Uri previewUri, int rows, int cols) {
            super(null);
            mPageId = id;
            mPreviewAsset = new ContentUriAsset(activity, previewUri,
                    RequestOptions.fitCenterTransform());
            mRows = rows;
            mCols = cols;
            mActivity = activity;
        }

        @Override
        public void setCard(CardView card) {
        	super.setCard(card);
        	mPreview = card.findViewById(R.id.grid_preview_image);
        }

        public void bindPreviewContent() {
            Resources resources = card.getResources();
            bindWallpaperIfAvailable();
            mPreviewAsset.loadDrawable(mActivity, mPreview,
                    resources.getColor(android.R.color.transparent, null));
        }

        void bindWallpaperIfAvailable() {
            if (card != null && mIsWallpaperInfoReady && mHomeWallpaper != null) {
                mHomeWallpaper.getThumbAsset(card.getContext()).decodeBitmap(mPageWidth,
                        mPageHeight,
                        bitmap -> {
                            mPreview.setBackground(
                                    new BitmapDrawable(card.getResources(), bitmap));
                        });
            }
        }
    }
    /**
     * Adapter class for mPreviewPager.
     * This is a ViewPager as it allows for a nice pagination effect (ie, pages snap on swipe,
     * we don't want to just scroll)
     */
    class GridPreviewAdapter extends BasePreviewAdapter<GridPreviewPage> {

        GridPreviewAdapter(GridOption gridOption) {
            super(getContext(), R.layout.grid_preview_card);
            for (int i = 0; i < gridOption.previewPagesCount; i++) {
                addPage(new GridPreviewPage(getActivity(), i,
                        gridOption.previewImageUri.buildUpon().appendPath("" + i).build(),
                        gridOption.rows, gridOption.cols));
            }
        }

        void onWallpaperInfoLoaded() {
            for (GridPreviewPage page : mPages) {
                page.bindWallpaperIfAvailable();
            }
        }
    }
}
