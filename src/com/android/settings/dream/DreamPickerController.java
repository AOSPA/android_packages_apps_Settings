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

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.ArraySet;
import android.view.View;
import android.view.ViewParent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.DreamInfo;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.Collections;
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
    /**
     * The fraction of the parent RecyclerView's height used to define the scroll threshold.
     * When a dragged item is within this fraction of the top or bottom edge of the parent,
     * auto-scrolling will be triggered.
     */
    private static final float SCROLL_THRESHOLD_FRACTION = 0.1f;
    private static final long SCROLL_NOT_STARTED = Long.MIN_VALUE;

    private final DreamBackend mBackend;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final List<DreamInfo> mDreamInfos = new ArrayList<>();
    @Nullable
    private DreamInfo mActiveDream;
    private List<DreamInfo> mSelectedDreams = new ArrayList<>();
    private DreamAdapter<DreamItem> mAdapter;

    private final ArraySet<Callback> mCallbacks = new ArraySet<>();

    public DreamPickerController(Context context) {
        this(context, DreamBackend.getInstance(context));
    }

    public DreamPickerController(Context context, DreamBackend backend) {
        super(context, PREF_KEY);
        mBackend = backend;
        mDreamInfos.addAll(mBackend.getDreamInfos());
        if (mBackend.isDreamSwitcherEnabled()) {
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
        final LayoutPreference pref = screen.findPreference(getPreferenceKey());
        if (pref == null) {
            return;
        }

        mAdapter = new DreamAdapter<>(
                R.layout.dream_preference_layout,
                mDreamInfos.stream().map(DreamItem::new).collect(Collectors.toList()),
                /* allowMultiSelection= */ mBackend.isDreamSwitcherEnabled());

        mAdapter.setEnabled(mBackend.isEnabled());

        final RecyclerView recyclerView = pref.findViewById(R.id.dream_list);
        recyclerView.setLayoutManager(new AutoFitGridLayoutManager(mContext));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
        setupRecyclerView(recyclerView);
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        if (!mBackend.isDreamSwitcherEnabled()) {
            recyclerView.addItemDecoration(
                    new GridSpacingItemDecoration(mContext, R.dimen.dream_preference_card_padding));
            return;
        }

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
        new ItemTouchHelper(new DreamItemTouchHelperCallback(recyclerView))
                .attachToRecyclerView(recyclerView);

        // Known issue of RecyclerView: avoid losing a11y focus when the item is moved.
        final RecyclerView.ItemAnimator itemAnimator = recyclerView.getItemAnimator();
        if (itemAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }
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
        if (mBackend.isDreamSwitcherEnabled()) {
            mDreamInfos.sort(DREAM_INFO_COMPARATOR);
            mSelectedDreams = filterSelectedDreamInfos(mDreamInfos);
        }
        if (mAdapter != null) {
            mAdapter.setItemList(mDreamInfos
                    .stream()
                    .map(DreamItem::new)
                    .collect(Collectors.toList()));
            mAdapter.notifyDataSetChanged();
        }
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

    @VisibleForTesting
    class DreamItemTouchHelperCallback extends ItemTouchHelper.Callback {
        private final RecyclerView mRecyclerView;
        @Nullable private RecyclerView mParentRecyclerView;
        @Nullable private Rect mParentViewRect;
        private long mDragScrollStartTimeInMs = SCROLL_NOT_STARTED;
        private int mInitialDragPosition = RecyclerView.NO_POSITION;

        DreamItemTouchHelperCallback(@NonNull RecyclerView recyclerView) {
            super();
            mRecyclerView = recyclerView;
            mParentRecyclerView = findParentRecyclerView(recyclerView);
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder) {
            // Only allow drag if the item is active
            final int position = viewHolder.getBindingAdapterPosition();
            if (position < 0 || position >= mDreamInfos.size()) return 0;

            final DreamInfo item = mDreamInfos.get(position);
            if (!item.isActive) return 0;

            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START
                    | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, 0); // No swipe, only drag.
        }

        @Override
        public void onSelectedChanged(
            @Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (viewHolder != null && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                mInitialDragPosition = viewHolder.getBindingAdapterPosition();
                viewHolder.itemView.setActivated(true);
                mParentRecyclerView = findParentRecyclerView(mRecyclerView);
                if (mParentRecyclerView != null) {
                    mParentViewRect = new Rect();
                    mParentRecyclerView.getGlobalVisibleRect(mParentViewRect);
                }
            }
        }

        @Nullable
        private RecyclerView findParentRecyclerView(RecyclerView view) {
            ViewParent viewParent = view.getParent();
            while (viewParent != null) {
                if (viewParent instanceof RecyclerView) {
                    return (RecyclerView) viewParent;
                }
                viewParent = viewParent.getParent();
            }
            return null;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            if (actionState != ItemTouchHelper.ACTION_STATE_DRAG || !isCurrentlyActive) {
                return;
            }

            scrollIfNecessary(recyclerView, viewHolder);
        }

        private void scrollIfNecessary(
                @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if (mParentRecyclerView == null || mParentViewRect == null) {
                return;
            }

            final View itemView = viewHolder.itemView;
            final Rect itemViewRect = new Rect();
            itemView.getGlobalVisibleRect(itemViewRect);

            final int relativeTop = itemViewRect.top - mParentViewRect.top;
            final int relativeBottom = itemViewRect.bottom - mParentViewRect.top;

            final int parentHeight = mParentRecyclerView.getHeight();
            final int scrollThreshold = (int) (parentHeight * SCROLL_THRESHOLD_FRACTION);

            int outOfBounds = 0;
            if (relativeBottom > parentHeight - scrollThreshold) {
                outOfBounds = relativeBottom - (parentHeight - scrollThreshold);
                final Rect innerRecyclerViewRect = new Rect();
                recyclerView.getGlobalVisibleRect(innerRecyclerViewRect);
                // Positive when the inner RecyclerView is above the parent.
                final int scrollBottomOvershoot =
                    mParentViewRect.bottom - innerRecyclerViewRect.bottom;
                if (scrollBottomOvershoot > 0) {
                    // "Brake" when we're reaching the bottom of the inner RecyclerView.
                    // We don't want to scroll down beyond the bottom of the inner RecyclerView.
                    outOfBounds = Math.max(0, outOfBounds - scrollBottomOvershoot);
                }
            } else if (relativeTop < scrollThreshold) {
                outOfBounds = relativeTop - scrollThreshold;
                final Rect innerRecyclerViewRect = new Rect();
                recyclerView.getGlobalVisibleRect(innerRecyclerViewRect);
                 // Negative when the inner RecyclerView is below the parent.
                final int scrollTopOvershoot = mParentViewRect.top - innerRecyclerViewRect.top;
                if (scrollTopOvershoot < 0) {
                    // "Brake" when we're reaching the top of the inner RecyclerView.
                    // We don't want to scroll up beyond the top of the inner RecyclerView.
                    outOfBounds = Math.min(0, outOfBounds - scrollTopOvershoot);
                }
            }

            int scrollAmount = 0;
            if (outOfBounds != 0) {
                final long currentTimeMs = System.currentTimeMillis();
                final long scrollDurationMs = mDragScrollStartTimeInMs == SCROLL_NOT_STARTED
                        ? 0
                        : currentTimeMs - mDragScrollStartTimeInMs;
                if (mDragScrollStartTimeInMs == SCROLL_NOT_STARTED) {
                    // Start accelerating for interpolateOutOfBoundsScroll.
                    mDragScrollStartTimeInMs = currentTimeMs;
                }
                scrollAmount = interpolateOutOfBoundsScroll(recyclerView, itemView.getHeight(),
                        outOfBounds, itemView.getHeight(), scrollDurationMs);
            }

            if (scrollAmount != 0) {
                final int finalScrollAmount = scrollAmount;
                // Dispatch scroll event asynchronously to avoid race condition in the onChildDraw.
                mParentRecyclerView.post(() -> {
                    if (mParentRecyclerView != null) {
                        mParentRecyclerView.scrollBy(0, finalScrollAmount);
                    }
                });
            } else {
                // Reset the scroll start time to reset acceleration.
                mDragScrollStartTimeInMs = SCROLL_NOT_STARTED;
            }
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder,
                @NonNull RecyclerView.ViewHolder target) {
            final int fromPos = viewHolder.getBindingAdapterPosition();
            final int toPos = target.getBindingAdapterPosition();

            // Prevent dragging into inactive items section.
            if (toPos < 0 || toPos >= mDreamInfos.size() || !mDreamInfos.get(toPos).isActive) {
                return false;
            }

            // Move in Adapter
            if (mAdapter != null) {
                mAdapter.rotateItems(fromPos, toPos);
            }

            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // No swipe
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.setActivated(false);
            commitReordering();
            if (mAdapter != null) {
                // Notify the adapter to update the displayed order on views.
                final int currentPosition = viewHolder.getBindingAdapterPosition();
                if (mInitialDragPosition != RecyclerView.NO_POSITION
                        && currentPosition != mInitialDragPosition) {
                    logDreamReordered(currentPosition);
                }
                mAdapter.notifyItemRangeChanged(0, mSelectedDreams.size());
            }
            mParentRecyclerView = null;
            mParentViewRect = null;
            mDragScrollStartTimeInMs = SCROLL_NOT_STARTED;
            mInitialDragPosition = RecyclerView.NO_POSITION;
        }

        private void commitReordering() {
            if (mAdapter == null) {
                return;
            }

            // Get the current order of items from the adapter,
            // which is the source of truth for the displayed order (modified in onMove).
            // Assume the items of the adapter are DreamItem instances.
            final List<DreamItem> items = mAdapter.getItems();

            // Rebuild mDreamInfos according to the displayed order in the adapter.
            mDreamInfos.clear();
            items.stream().map(DreamItem::getDreamInfo).forEach(mDreamInfos::add);

            commitReorderingForSelectedDreams();
        }

        private void logDreamReordered(int position) {
            if (position < 0 || position >= mSelectedDreams.size()) {
                return;
            }

            final DreamInfo item = mSelectedDreams.get(position);
            mMetricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                    SettingsEnums.ACTION_DREAM_MOVE_ORDER,
                    SettingsEnums.DREAM,
                    item.componentName.flattenToString(),
                    item.order);
        }
    }

    private void commitReorderingForSelectedDreams() {
        // Since active items are at the top and we only reordered active items,
        // we can just iterate and update order.
        // Also update mSelectedDreams.
        mSelectedDreams.clear();
        mDreamInfos.stream()
                .filter(d -> d.isActive)
                .forEach(mSelectedDreams::add);

        // Update order in objects
        for (int i = 0; i < mSelectedDreams.size(); i++) {
            mSelectedDreams.get(i).order = i;
        }

        final ComponentName[] activeComponents = mSelectedDreams.stream()
                .map(d -> d.componentName)
                .toArray(ComponentName[]::new);

        // Persist the new order.
        mBackend.setActiveDreams(activeComponents);
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

        final List<DreamItem> newAdapterItems = mDreamInfos.stream()
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

        DreamInfo getDreamInfo() {
            return mDreamInfo;
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
            int logValue;
            if (mBackend.isDreamSwitcherEnabled()) {
                updateDreamSelection(mDreamInfo);
                logValue = mDreamInfo.isActive ? 1 : 0;
            } else {
                mActiveDream = mDreamInfo;
                mBackend.setActiveDream(mDreamInfo.componentName);
                logValue = 1;
            }
            mCallbacks.forEach(Callback::onActiveDreamChanged);
            mMetricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                    SettingsEnums.ACTION_DREAM_SELECT_TYPE, SettingsEnums.DREAM,
                    mDreamInfo.componentName.flattenToString(), logValue);
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

            if (mBackend.isDreamSwitcherEnabled()) {
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
