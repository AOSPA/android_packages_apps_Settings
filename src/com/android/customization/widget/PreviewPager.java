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
package com.android.customization.widget;

import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.android.wallpaper.R;

/**
 * A Widget consisting of a ViewPager linked to a PageIndicator and previous/next arrows that can be
 * used to page over that ViewPager.
 * To use it, set a {@link PagerAdapter} using {@link #setAdapter(PagerAdapter)}, and optionally use
 * a {@link #setOnPageChangeListener(OnPageChangeListener)} to listen for page changes.
 */
public class PreviewPager extends LinearLayout {

    private final ViewPager mViewPager;
    private final PageIndicator mPageIndicator;
    private final View mPreviousArrow;
    private final View mNextArrow;
    private final ViewPager.OnPageChangeListener mPageListener;

    private PagerAdapter mAdapter;
    private ViewPager.OnPageChangeListener mExternalPageListener;

    public PreviewPager(Context context) {
        this(context, null);
    }

    public PreviewPager(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewPager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.preview_pager, this);
        Resources res = context.getResources();

        mViewPager = findViewById(R.id.preview_viewpager);
        mViewPager.setPageMargin(res.getDimensionPixelOffset(R.dimen.preview_page_gap));
        mViewPager.setClipToPadding(false);
        mViewPager.setPadding(res.getDimensionPixelOffset(R.dimen.preview_page_horizontal_margin),
                res.getDimensionPixelOffset(R.dimen.preview_page_top_margin),
                res.getDimensionPixelOffset(R.dimen.preview_page_horizontal_margin),
                res.getDimensionPixelOffset(R.dimen.preview_page_bottom_margin));
        mPageIndicator = findViewById(R.id.page_indicator);
        mPreviousArrow = findViewById(R.id.arrow_previous);
        mPreviousArrow.setOnClickListener(v -> {
            final int previousPos = mViewPager.getCurrentItem() - 1;
            mViewPager.setCurrentItem(previousPos, true);
        });
        mNextArrow = findViewById(R.id.arrow_next);
        mNextArrow.setOnClickListener(v -> {
            final int NextPos = mViewPager.getCurrentItem() + 1;
            mViewPager.setCurrentItem(NextPos, true);
        });
        mPageListener = createPageListener();
        mViewPager.addOnPageChangeListener(mPageListener);
    }

    public void forceCardWidth(int widthPixels) {
        mViewPager.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int hPadding = (mViewPager.getWidth() - widthPixels) / 2;
                mViewPager.setPadding(hPadding, mViewPager.getPaddingTop(),
                        hPadding, mViewPager.getPaddingBottom());
                mViewPager.removeOnLayoutChangeListener(this);
            }
        });
        mViewPager.invalidate();
    }

    /**
     * Call this method to set the {@link PagerAdapter} backing the {@link ViewPager} in this
     * widget.
     */
    public void setAdapter(PagerAdapter adapter) {
        mAdapter = adapter;
        mViewPager.setAdapter(adapter);

        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                initIndicator();
            }
        });
        initIndicator();
        updateIndicator(mViewPager.getCurrentItem());
    }

    /**
     * Set a {@link OnPageChangeListener} to be notified when the ViewPager's page state changes
     */
    public void setOnPageChangeListener(@Nullable ViewPager.OnPageChangeListener listener) {
        mExternalPageListener = listener;
    }

    private void initIndicator() {
        mPageIndicator.setNumPages(mAdapter.getCount());
        mPageIndicator.setLocation(mViewPager.getCurrentItem());
    }

    private ViewPager.OnPageChangeListener createPageListener() {
        return new ViewPager.OnPageChangeListener() {
             @Override
             public void onPageScrolled(
                     int position, float positionOffset, int positionOffsetPixels) {
                 // For certain sizes, positionOffset never makes it to 1, so round it as we don't
                 // need that much precision
                 float location = (float)Math.round((position + positionOffset) * 100) / 100;
                 mPageIndicator.setLocation(location);
                 if (mExternalPageListener != null) {
                     mExternalPageListener.onPageScrolled(position, positionOffset,
                             positionOffsetPixels);
                 }
             }

             @Override
             public void onPageSelected(int position) {
                 int adapterCount = mAdapter.getCount();
                 if (position < 0 || position >= adapterCount) {
                     return;
                 }

                 updateIndicator(position);
                 if (mExternalPageListener != null) {
                     mExternalPageListener.onPageSelected(position);
                 }
             }

             @Override
             public void onPageScrollStateChanged(int state) {
                 if (mExternalPageListener != null) {
                     mExternalPageListener.onPageScrollStateChanged(state);
                 }
             }
        };
    }

    private void updateIndicator(int position) {
        int adapterCount = mAdapter.getCount();
        if (adapterCount > 1) {
            mPreviousArrow.setVisibility(View.VISIBLE);
            mNextArrow.setVisibility(View.VISIBLE);
            mPreviousArrow.setEnabled(position != 0);
            ((ViewGroup) mPreviousArrow).getChildAt(0).setEnabled(position != 0);
            mNextArrow.setEnabled(position != (adapterCount - 1));
            ((ViewGroup) mNextArrow).getChildAt(0).setEnabled(position != (adapterCount - 1));
        } else {
            mPageIndicator.setVisibility(View.GONE);
            mPreviousArrow.setVisibility(View.GONE);
            mNextArrow.setVisibility(View.GONE);
        }
    }
}
