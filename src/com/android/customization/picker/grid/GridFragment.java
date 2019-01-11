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

import static androidx.core.view.ViewCompat.LAYOUT_DIRECTION_RTL;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;

import com.android.customization.model.grid.GridOption;
import com.android.customization.model.grid.GridOptionsManager;
import com.android.customization.widget.OptionSelectorController;
import com.android.customization.widget.PreviewPager;
import com.android.wallpaper.R;
import com.android.wallpaper.asset.Asset;
import com.android.wallpaper.asset.ContentUriAsset;
import com.android.wallpaper.picker.ToolbarFragment;

import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that contains the UI for selecting and applying a GridOption.
 */
public class GridFragment extends ToolbarFragment {

    public static GridFragment newInstance(CharSequence title, GridOptionsManager manager) {
        GridFragment fragment = new GridFragment();
        fragment.setManager(manager);
        fragment.setArguments(ToolbarFragment.createArguments(title));
        return fragment;
    }

    private RecyclerView mOptionsContainer;
    private OptionSelectorController mOptionsController;
    private GridOptionsManager mGridManager;
    private GridOption mSelectedOption;
    private PreviewPager mPreviewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.fragment_grid_picker, container, /* attachToRoot */ false);
        setUpToolbar(view);
        mPreviewPager = view.findViewById(R.id.grid_preview_pager);
        mOptionsContainer = view.findViewById(R.id.options_container);
        setUpOptions();

        return view;
    }

    private void setManager(GridOptionsManager manager) {
        mGridManager = manager;
    }

    private void createAdapter() {
        mPreviewPager.setAdapter(new GridPreviewAdapter(mSelectedOption));
    }

    private void setUpOptions() {
        mGridManager.fetchOptions(options -> {
            mOptionsController = new OptionSelectorController(mOptionsContainer, options);

            mOptionsController.addListener(selected -> {
                mSelectedOption = (GridOption) selected;
                createAdapter();
            });
            mOptionsController.initOptions();
            for (GridOption option : options) {
                if (option.isCurrentlySet()) {
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

    /**
     * Adapter class for mPreviewPager.
     * This is a ViewPager as it allows for a nice pagination effect (ie, pages snap on swipe,
     * we don't want to just scroll)
     */
    private class GridPreviewAdapter extends PagerAdapter {

        private class PreviewPage {
            final int pageId;
            final Asset previewAsset;
            final int cols;
            final int rows;

            CardView card;

            private PreviewPage(Context context, int id, Uri previewUri, int rows, int cols) {
                pageId = id;
                previewAsset = new ContentUriAsset(context, previewUri,
                        RequestOptions.fitCenterTransform());
                this.rows = rows;
                this.cols = cols;
            }

            public void setCard(CardView card) {
                this.card = card;
            }

            void bindPreviewContent() {
                previewAsset.loadDrawable(getActivity(), card.findViewById(R.id.grid_preview_image),
                        card.getContext().getResources().getColor(R.color.primary_color, null));
            }
        }

        private final List<PreviewPage> mPages = new ArrayList<>();

        GridPreviewAdapter(GridOption gridOption) {
            for (int i = 0; i < gridOption.previewPagesCount; i++) {
                mPages.add(new PreviewPage(getContext(), i,
                        gridOption.previewImageUri.buildUpon().appendPath("" + i).build(),
                        gridOption.rows, gridOption.cols));
            }
        }

        @Override
        public int getCount() {
            return mPages.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == ((PreviewPage)object).card;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            if (ViewCompat.getLayoutDirection(container) == LAYOUT_DIRECTION_RTL) {
                position = mPages.size() - 1 - position;
            }
            LayoutInflater inflater = LayoutInflater.from(getContext());
            CardView card = (CardView) inflater.inflate(R.layout.grid_preview_card,
                    container, false);
            PreviewPage page = mPages.get(position);

            page.setCard(card);
            page.bindPreviewContent();
            if (card.getParent() != null) {
                container.removeView(card);
            }
            container.addView(card);
            return page;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position,
                @NonNull Object object) {
            ((PreviewPage) object).card = null;
        }
    }
}
