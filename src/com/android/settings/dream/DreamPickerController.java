/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.dream;

import static android.service.dreams.Flags.dreamsSwitcher;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;

import android.util.Log;
import android.util.ArraySet;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.DreamInfo;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Controller for the dream picker where the user can select a screensaver.
 */
public class DreamPickerController extends BasePreferenceController {
    public static final String PREF_KEY = "dream_picker";

    private static final String TAG = "DreamPickerController";
    private static final DreamInfoComparator DREAM_INFO_COMPARATOR = new DreamInfoComparator();

    private final DreamBackend mBackend;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final List<DreamInfo> mDreamInfos = new ArrayList<>();
    @Nullable
    private DreamInfo mActiveDream;
    private List<DreamInfo> mSelectedDreams = new ArrayList<>();
    private DreamAdapter mAdapter;

    private final ArraySet<Callback> mCallbacks = new ArraySet<>();

    public DreamPickerController(Context context) {
        this(context, DreamBackend.getInstance(context));
    }

    public DreamPickerController(Context context, DreamBackend backend) {
        super(context, PREF_KEY);
        mBackend = backend;
        mDreamInfos.addAll(mBackend.getDreamInfos());
        if (dreamsSwitcher()) {
            mDreamInfos.sort(DREAM_INFO_COMPARATOR);
            mSelectedDreams = filterSelectedDreamInfos(mDreamInfos);
        } else {
            mActiveDream = getActiveDreamInfo(mDreamInfos);
        }
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return !mDreamInfos.isEmpty() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mAdapter = new DreamAdapter(R.layout.dream_preference_layout,
                mDreamInfos.stream()
                        .map(DreamItem::new)
                        .collect(Collectors.toList()));

        mAdapter.setEnabled(mBackend.isEnabled());

        final LayoutPreference pref = screen.findPreference(getPreferenceKey());
        if (pref == null) {
            return;
        }
        final RecyclerView recyclerView = pref.findViewById(R.id.dream_list);
        recyclerView.setLayoutManager(new AutoFitGridLayoutManager(mContext));
        if (dreamsSwitcher()) {
            recyclerView.addItemDecoration(
                new FixedSpaceAroundItemDecoration(
                    mContext, R.dimen.dream_preference_card_fixed_space_around)
            );
            // Apply negative padding on RecyclerView to compensate spacing of items on the edge
            final int spacingCompensation = mContext.getResources().getDimensionPixelSize(
                        R.dimen.dream_preference_card_space_compensation);
            final int targetBottomPadding = mContext.getResources().getDimensionPixelSize(
                        R.dimen.dream_preference_card_padding);
            recyclerView.setPaddingRelative(
                spacingCompensation,
                spacingCompensation,
                spacingCompensation,
                targetBottomPadding + spacingCompensation
            );
        } else {
            recyclerView.addItemDecoration(
                    new GridSpacingItemDecoration(mContext, R.dimen.dream_preference_card_padding));
        }
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mAdapter != null) {
            mAdapter.setEnabled(preference.isEnabled());
        }
    }

    @Nullable
    public DreamInfo getActiveDreamInfo() {
        return mActiveDream;
    }

    List<DreamInfo> getSelectedDreams() {
        return mSelectedDreams;
    }

    void refreshDreamsList() {
        mDreamInfos.clear();
        mDreamInfos.addAll(mBackend.getDreamInfos());
        if (dreamsSwitcher()) {
            mDreamInfos.sort(DREAM_INFO_COMPARATOR);
            mSelectedDreams = filterSelectedDreamInfos(mDreamInfos);
        }
        mAdapter.setItemList(mDreamInfos
                .stream()
                .map(DreamItem::new)
                .collect(Collectors.toList()));
        mAdapter.notifyDataSetChanged();
    }

    private List<DreamInfo> filterSelectedDreamInfos(List<DreamInfo> dreamInfos) {
        return dreamInfos.stream()
                .filter(d -> d.isActive)
                .collect(Collectors.toList());
    }

    private static class DreamInfoComparator implements Comparator<DreamInfo> {
        @Override
        public int compare(DreamInfo d1, DreamInfo d2) {
            // 1. Selected dreams are smaller than unselected ones.
            // 2. If both items are not selected, return 0 to preserve order (assume stable sort).
            // 3. If both are selected and have the same order, log a conflicted orders warning.
            // 4. Otherwise compare their `order` field.
            if (d1.isActive != d2.isActive) {
                return d1.isActive ? -1 : 1;
            }
            if (!d1.isActive) {
                return 0;
            }
            if (d1.order == d2.order) {
                Log.w(TAG, "Duplicate order=" + d1.order
                        + " for " + d1.componentName + " and " + d2.componentName);
            }
            return d1.order - d2.order;
        }
    }

    @Nullable
    private static DreamInfo getActiveDreamInfo(List<DreamInfo> dreamInfos) {
        return dreamInfos
                .stream()
                .filter(d -> d.isActive)
                .findFirst()
                .orElse(null);
    }

    void addCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    void removeCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    interface Callback {
        // Triggered when the selected dream changes.
        void onActiveDreamChanged();
    }

    /** Holds the DreamInfo state snapshot for DiffUtil to detect changes. */
    private static class DreamItemState {
        final ComponentName componentName;
        final boolean isActive;
        final int order;

        DreamItemState(DreamInfo dreamInfo) {
            this.componentName = dreamInfo.componentName;
            this.isActive = dreamInfo.isActive;
            this.order = dreamInfo.order;
        }
    }

    private static class DreamItemDiffCallback extends DiffUtil.Callback {
        private final List<DreamItemState> mOldList;
        private final List<DreamItemState> mNewList;

        DreamItemDiffCallback(List<DreamItemState> oldList, List<DreamItemState> newList) {
            mOldList = oldList;
            mNewList = newList;
        }

        @Override
        public int getOldListSize() {
            return mOldList.size();
        }

        @Override
        public int getNewListSize() {
            return mNewList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldList.get(oldItemPosition).componentName.equals(
                    mNewList.get(newItemPosition).componentName);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            final DreamItemState oldItem = mOldList.get(oldItemPosition);
            final DreamItemState newItem = mNewList.get(newItemPosition);
            return oldItem.isActive == newItem.isActive
                    && oldItem.order == newItem.order;
        }
    }

    private void updateDreamSelection(DreamInfo dreamInfo) {
        final List<DreamItemState> oldItemStates = mDreamInfos.stream()
                .map(DreamItemState::new)
                .collect(Collectors.toList());

        final int selectedIndex = findIndex(mSelectedDreams,
                d -> d.componentName.equals(dreamInfo.componentName));
        if (selectedIndex == -1) {
            // Select the dream if it was not selected, and set the order of the dream to
            // the last index in the mSelectedDreams list.
            mSelectedDreams.add(dreamInfo);
            dreamInfo.isActive = true;
            dreamInfo.order = mSelectedDreams.size() - 1;
        } else {
            // Unselect the dream if it was selected.
            mSelectedDreams.remove(selectedIndex);
            dreamInfo.isActive = false;
            dreamInfo.order = DreamInfo.ORDER_UNSELECTED;
            // Update the order of the dreams which were after the unselected dream.
            for (int i = selectedIndex; i < mSelectedDreams.size(); i++) {
                mSelectedDreams.get(i).order = i;
            }
        }

        final ComponentName[] activeComponents = mSelectedDreams.stream()
                .map(d -> d.componentName)
                .toArray(ComponentName[]::new);

        mBackend.setActiveDreams(activeComponents);

        // Update the mDreamInfos list and apply it to the adapter.
        mDreamInfos.sort(DREAM_INFO_COMPARATOR);

        final List<IDreamItem> newAdapterItems = mDreamInfos.stream()
                .map(DreamItem::new)
                .collect(Collectors.toList());
        mAdapter.setItemList(newAdapterItems);

        final List<DreamItemState> newItemStates = mDreamInfos.stream()
                .map(DreamItemState::new)
                .collect(Collectors.toList());
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new DreamItemDiffCallback(oldItemStates, newItemStates));
        diffResult.dispatchUpdatesTo(mAdapter);
    }

    private class DreamItem implements IDreamItem {
        DreamInfo mDreamInfo;

        DreamItem(DreamInfo dreamInfo) {
            mDreamInfo = dreamInfo;
        }

        @Override
        public CharSequence getTitle() {
            return mDreamInfo.caption;
        }

        @Override
        public CharSequence getSummary() {
            return mDreamInfo.description;
        }

        @Override
        public Drawable getIcon() {
            return mDreamInfo.icon;
        }

        @Override
        public void onItemClicked() {
            if (dreamsSwitcher()) {
                updateDreamSelection(mDreamInfo);
            } else {
                mActiveDream = mDreamInfo;
                mBackend.setActiveDream(mDreamInfo.componentName);
            }
            mCallbacks.forEach(Callback::onActiveDreamChanged);
            mMetricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                    SettingsEnums.ACTION_DREAM_SELECT_TYPE, SettingsEnums.DREAM,
                    mDreamInfo.componentName.flattenToString(), 1);
        }

        @Override
        public void onPreviewClicked() {
            mBackend.preview(mDreamInfo.componentName);
        }

        @Override
        public void onCustomizeClicked() {
            mBackend.launchSettings(mContext, mDreamInfo);
        }

        @Override
        public Drawable getPreviewImage() {
            return mDreamInfo.previewImage;
        }

        @Override
        public boolean isActive() {
            if (!mAdapter.getEnabled()) {
                return false;
            }

            if (dreamsSwitcher()) {
                return mDreamInfo.isActive;
            }

            return mActiveDream != null
                    && mDreamInfo.componentName.equals(mActiveDream.componentName);
        }

        @Override
        public boolean allowCustomization() {
            return isActive() && mDreamInfo.settingsComponentName != null;
        }

        @Override
        public int getOrder() {
            return mDreamInfo.order;
        }
    }

    /**
     * Returns the index of the first element in the list that matches the given predicate,
     * or -1 if no such element is found.
     */
    private static int findIndex(List<DreamInfo> dreamInfos, Predicate<DreamInfo> predicate) {
        for (int i = 0; i < dreamInfos.size(); i++) {
            if (predicate.test(dreamInfos.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
