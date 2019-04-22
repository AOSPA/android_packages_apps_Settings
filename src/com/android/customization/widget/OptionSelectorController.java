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

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.customization.model.CustomizationManager;
import com.android.customization.model.CustomizationOption;
import com.android.wallpaper.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Simple controller for a RecyclerView-based widget to hold the options for each customization
 * section (eg, thumbnails for themes, clocks, grid sizes).
 * To use, just pass the RV that will contain the tiles and the list of {@link CustomizationOption}
 * representing each option, and call {@link #initOptions(CustomizationManager)} to populate the
 * widget.
 */
public class OptionSelectorController<T extends CustomizationOption<T>> {

    /**
     * Interface to be notified when an option is selected by the user.
     */
    public interface OptionSelectedListener {
        /**
         * Called when an option has been selected (and marked as such in the UI)
         */
        void onOptionSelected(CustomizationOption selected);
    }

    private final RecyclerView mContainer;
    private final List<T> mOptions;

    private final Set<OptionSelectedListener> mListeners = new HashSet<>();
    private RecyclerView.Adapter<TileViewHolder> mAdapter;
    private CustomizationOption mSelectedOption;

    public OptionSelectorController(RecyclerView container, List<T> options) {
        mContainer = container;
        mOptions = options;
    }

    public void addListener(OptionSelectedListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(OptionSelectedListener listener) {
        mListeners.remove(listener);
    }

    public void setSelectedOption(CustomizationOption option) {
        if (!mOptions.contains(option)) {
            throw new IllegalArgumentException("Invalid option");
        }
        updateActivatedStatus(mSelectedOption, false);
        updateActivatedStatus(option, true);
        mSelectedOption = option;
        notifyListeners();
    }

    private void updateActivatedStatus(CustomizationOption option, boolean isActivated) {
        int index = mOptions.indexOf(option);
        if (index < 0) {
            return;
        }
        RecyclerView.ViewHolder holder = mContainer.findViewHolderForAdapterPosition(index);
        if (holder != null && holder.itemView != null) {
            holder.itemView.setActivated(isActivated);
        }
    }

    /**
     * Initializes the UI for the options passed in the constructor of this class.
     */
    public void initOptions(final CustomizationManager<T> manager) {
        mAdapter = new RecyclerView.Adapter<TileViewHolder>() {
            @Override
            public int getItemViewType(int position) {
                return mOptions.get(position).getLayoutResId();
            }

            @NonNull
            @Override
            public TileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
                return new TileViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull TileViewHolder holder, int position) {
                CustomizationOption option = mOptions.get(position);
                if (mSelectedOption == null && option.isActive(manager)) {
                    mSelectedOption = option;
                }
                if (holder.labelView != null) {
                    holder.labelView.setText(option.getTitle());
                }
                option.bindThumbnailTile(holder.tileView);
                holder.itemView.setActivated(option.equals(mSelectedOption));
                holder.itemView.setOnClickListener(view -> setSelectedOption(option));
            }

            @Override
            public int getItemCount() {
                return mOptions.size();
            }
        };

        mContainer.setLayoutManager(new LinearLayoutManager(mContainer.getContext(),
                LinearLayoutManager.HORIZONTAL, false));
        Resources res = mContainer.getContext().getResources();
        mContainer.addItemDecoration(new HorizontalSpacerItemDecoration(
                res.getDimensionPixelOffset(R.dimen.option_tile_margin_horizontal),
                res.getDimensionPixelOffset(R.dimen.option_tile_margin_horizontal_ends)));
        mContainer.setAdapter(mAdapter);
    }

    public void resetOptions(List<T> options) {
        mOptions.clear();
        mOptions.addAll(options);
        mAdapter.notifyDataSetChanged();
    }

    private void notifyListeners() {
        if (mListeners.isEmpty()) {
            return;
        }
        CustomizationOption option = mSelectedOption;
        Set<OptionSelectedListener> iterableListeners = new HashSet<>(mListeners);
        for (OptionSelectedListener listener : iterableListeners) {
            listener.onOptionSelected(option);
        }
    }

    private static class TileViewHolder extends RecyclerView.ViewHolder {
        TextView labelView;
        View tileView;

        TileViewHolder(@NonNull View itemView) {
            super(itemView);
            labelView = itemView.findViewById(R.id.option_label);
            tileView = itemView.findViewById(R.id.option_tile);
        }
    }
}
