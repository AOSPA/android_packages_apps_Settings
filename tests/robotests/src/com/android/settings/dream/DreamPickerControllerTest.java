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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.dreams.Flags;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.dream.FixedSpaceAroundItemDecoration;
import com.android.settings.dream.GridSpacingItemDecoration;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.DreamInfo;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.google.testing.junit.testparameterinjector.TestParameter;

import org.robolectric.RobolectricTestParameterInjector;
import org.robolectric.shadows.ShadowLooper;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewParent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(RobolectricTestParameterInjector.class)
public class DreamPickerControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final int PARENT_HEIGHT = 1000;
    private static final int PARENT_SCROLL_THRESHOLD = (int) (PARENT_HEIGHT * 0.1f);
    private static final int PARENT_WIDTH = 500;
    private static final int ITEM_HEIGHT = 100;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DreamBackend mBackend;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private RecyclerView mParentRecyclerView;
    @Mock
    private RecyclerView mInnerRecyclerView;
    @Mock
    private View mItemView;

    private Context mContext;
    private LayoutPreference mPreference;
    private RecyclerView.ViewHolder mViewHolder;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        mPreference = new LayoutPreference(mContext, R.layout.dream_picker_layout);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
    }

    private DreamPickerController buildController() {
        final DreamPickerController controller = new DreamPickerController(mContext, mBackend);
        controller.displayPreference(mScreen);
        return controller;
    }

    @Test
    public void isDisabledIfNoDreamsAvailable() {
        when(mBackend.getDreamInfos()).thenReturn(new ArrayList<>(0));
        final DreamPickerController controller = buildController();
        assertThat(controller.isAvailable()).isFalse();
    }

    @Test
    public void isEnabledIfDreamsAvailable() {
        when(mBackend.getDreamInfos()).thenReturn(Collections.singletonList(new DreamInfo()));
        final DreamPickerController controller = buildController();
        assertThat(controller.isAvailable()).isTrue();
    }

    @Test
    public void testDreamDisplayedInList() {
        when(mBackend.getDreamInfos()).thenReturn(Collections.singletonList(new DreamInfo()));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);

        RecyclerView view = mPreference.findViewById(R.id.dream_list);
        assertThat(view.getAdapter().getItemCount()).isEqualTo(1);
    }

    @Test
    public void refreshDreamsListUpdatesAdapter() {
        when(mBackend.getDreamInfos()).thenReturn(Collections.singletonList(new DreamInfo()));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);

        RecyclerView view = mPreference.findViewById(R.id.dream_list);
        assertThat(view.getAdapter().getItemCount()).isEqualTo(1);

        when(mBackend.getDreamInfos()).thenReturn(Arrays.asList(new DreamInfo(), new DreamInfo()));
        controller.refreshDreamsList();
        assertThat(view.getAdapter().getItemCount()).isEqualTo(2);
    }

    @Test
    public void onItemClicked_dreamsSwitcherDisabled_setsActiveDream() {
        final DreamInfo dream1 = createDreamInfo("dream1", true, 0);
        final DreamInfo dream2 = createDreamInfo("dream2", false, -1);
        when(mBackend.isDreamSwitcherEnabled()).thenReturn(false);
        when(mBackend.getDreamInfos()).thenReturn(Arrays.asList(dream1, dream2));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);
        final RecyclerView recyclerView = mPreference.findViewById(R.id.dream_list);
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 1000);
        assertThat(recyclerView.getItemDecorationCount()).isEqualTo(1);
        assertThat(recyclerView.getItemDecorationAt(0)).isInstanceOf(
                GridSpacingItemDecoration.class);
        assertThat(controller.getActiveDreamInfo()).isEqualTo(dream1);

        // act: select dream2
        recyclerView.findViewHolderForAdapterPosition(1).itemView.performClick();

        // verify
        verify(mBackend).setActiveDream(dream2.componentName);
        assertThat(controller.getActiveDreamInfo()).isEqualTo(dream2);
    }

    @Test
    public void onItemClicked_notSelected_dreamsSwitcherEnabled_selectsDream() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", false, -1);
        final DreamInfo dream2 = createDreamInfo("dream2", false, -1);
        when(mBackend.isDreamSwitcherEnabled()).thenReturn(true);
        when(mBackend.getDreamInfos()).thenReturn(Arrays.asList(dream1, dream2));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);
        final RecyclerView recyclerView = mPreference.findViewById(R.id.dream_list);
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 1000);
        assertThat(recyclerView.getItemDecorationCount()).isAtLeast(1);
        assertThat(recyclerView.getItemDecorationAt(0)).isInstanceOf(
                FixedSpaceAroundItemDecoration.class);

        // act: select dream2
        recyclerView.findViewHolderForAdapterPosition(1).itemView.performClick();

        // verify dream selection
        final ArgumentCaptor<ComponentName[]> captor =
                ArgumentCaptor.forClass(ComponentName[].class);
        verify(mBackend).setActiveDreams(captor.capture());

        final List<ComponentName> activeComponents = Arrays.asList(captor.getValue());
        assertThat(activeComponents).containsExactly(dream2.componentName);
        assertThat(controller.getSelectedDreams().stream().map(
                d -> d.componentName).collect(Collectors.toList()))
                .containsExactly(dream2.componentName);

        // verify item moved
        final TextView title1 =
                recyclerView.findViewHolderForAdapterPosition(0).itemView.findViewById(
                        R.id.title_text);
        assertThat(title1.getText()).isEqualTo(dream2.caption);
        final TextView title2 =
                recyclerView.findViewHolderForAdapterPosition(1).itemView.findViewById(
                        R.id.title_text);
        assertThat(title2.getText()).isEqualTo(dream1.caption);
    }

    @Test
    public void onItemClicked_selected_dreamsSwitcherEnabled_deselectsDream() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", true, 1);
        final DreamInfo dream2 = createDreamInfo("dream2", true, 0);
        when(mBackend.isDreamSwitcherEnabled()).thenReturn(true);
        when(mBackend.getDreamInfos()).thenReturn(Arrays.asList(dream1, dream2));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);
        final RecyclerView recyclerView = mPreference.findViewById(R.id.dream_list);
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 1000);
        assertThat(recyclerView.getItemDecorationCount()).isAtLeast(1);
        assertThat(recyclerView.getItemDecorationAt(0)).isInstanceOf(
                FixedSpaceAroundItemDecoration.class);

        // act: deselect an active dream (dream2 at index 0)
        recyclerView.findViewHolderForAdapterPosition(0).itemView.performClick();

        // verify dream selection
        final ArgumentCaptor<ComponentName[]> captor =
                ArgumentCaptor.forClass(ComponentName[].class);
        verify(mBackend).setActiveDreams(captor.capture());

        final List<ComponentName> activeComponents = Arrays.asList(captor.getValue());
        assertThat(activeComponents).containsExactly(dream1.componentName);
        assertThat(controller.getSelectedDreams().stream().map(
                d -> d.componentName).collect(Collectors.toList()))
                .containsExactly(dream1.componentName);

        // verify item moved
        final TextView title1 =
                recyclerView.findViewHolderForAdapterPosition(0).itemView.findViewById(
                        R.id.title_text);
        assertThat(title1.getText()).isEqualTo(dream1.caption);
        final TextView title2 =
                recyclerView.findViewHolderForAdapterPosition(1).itemView.findViewById(
                        R.id.title_text);
        assertThat(title2.getText()).isEqualTo(dream2.caption);
    }

    @Test
    public void onMove_up_dreamsSwitcherEnabled_reordersDreams() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", true, 0);
        final DreamInfo dream2 = createDreamInfo("dream2", true, 1);
        final DreamInfo dream3 = createDreamInfo("dream3", true, 2);
        when(mBackend.isDreamSwitcherEnabled()).thenReturn(true);
        when(mBackend.getDreamInfos()).thenReturn(
                new ArrayList<>(Arrays.asList(dream1, dream2, dream3)));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);
        final RecyclerView recyclerView = mPreference.findViewById(R.id.dream_list);
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 1000);
        final RecyclerView.ViewHolder vhFrom = recyclerView.findViewHolderForAdapterPosition(2);
        final RecyclerView.ViewHolder vhTo = recyclerView.findViewHolderForAdapterPosition(0);
        final DreamPickerController.DreamItemTouchHelperCallback callback =
                controller.new DreamItemTouchHelperCallback(recyclerView);
        // act: move item from pos 2 to pos 0
        callback.onSelectedChanged(vhFrom, ItemTouchHelper.ACTION_STATE_DRAG);
        callback.onMove(recyclerView, vhFrom, vhTo);
        callback.clearView(recyclerView, vhFrom);
        // verify dream reordering
        final ArgumentCaptor<ComponentName[]> captor =
                ArgumentCaptor.forClass(ComponentName[].class);
        verify(mBackend).setActiveDreams(captor.capture());
        final List<ComponentName> activeComponents = Arrays.asList(captor.getValue());
        assertThat(activeComponents).containsExactly(
                dream3.componentName, dream1.componentName, dream2.componentName).inOrder();
        assertThat(controller.getSelectedDreams().stream().map(
                d -> d.componentName).collect(Collectors.toList()))
                .containsExactly(dream3.componentName, dream1.componentName, dream2.componentName)
                .inOrder();
        assertThat(dream3.order).isEqualTo(0);
        assertThat(dream1.order).isEqualTo(1);
        assertThat(dream2.order).isEqualTo(2);
    }

    @Test
    public void onMove_down_dreamsSwitcherEnabled_reordersDreams() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", true, 0);
        final DreamInfo dream2 = createDreamInfo("dream2", true, 1);
        final DreamInfo dream3 = createDreamInfo("dream3", true, 2);
        when(mBackend.isDreamSwitcherEnabled()).thenReturn(true);
        when(mBackend.getDreamInfos()).thenReturn(
                new ArrayList<>(Arrays.asList(dream1, dream2, dream3)));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);
        final RecyclerView recyclerView = mPreference.findViewById(R.id.dream_list);
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 1000);
        final RecyclerView.ViewHolder vhFrom = recyclerView.findViewHolderForAdapterPosition(0);
        final RecyclerView.ViewHolder vhTo = recyclerView.findViewHolderForAdapterPosition(2);
        final DreamPickerController.DreamItemTouchHelperCallback callback =
                controller.new DreamItemTouchHelperCallback(recyclerView);
        // act: move item from pos 0 to pos 2
        callback.onSelectedChanged(vhFrom, ItemTouchHelper.ACTION_STATE_DRAG);
        callback.onMove(recyclerView, vhFrom, vhTo);
        callback.clearView(recyclerView, vhFrom);
        // verify dream reordering
        final ArgumentCaptor<ComponentName[]> captor =
                ArgumentCaptor.forClass(ComponentName[].class);
        verify(mBackend).setActiveDreams(captor.capture());
        final List<ComponentName> activeComponents = Arrays.asList(captor.getValue());
        assertThat(activeComponents).containsExactly(
                dream2.componentName, dream3.componentName, dream1.componentName).inOrder();
        assertThat(controller.getSelectedDreams().stream().map(
                d -> d.componentName).collect(Collectors.toList()))
                .containsExactly(dream2.componentName, dream3.componentName, dream1.componentName)
                .inOrder();
        assertThat(dream2.order).isEqualTo(0);
        assertThat(dream3.order).isEqualTo(1);
        assertThat(dream1.order).isEqualTo(2);
    }

    @Test
    public void onMove_middle_dreamsSwitcherEnabled_reordersDreams() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", true, 0);
        final DreamInfo dream2 = createDreamInfo("dream2", true, 1);
        final DreamInfo dream3 = createDreamInfo("dream3", true, 2);
        final DreamInfo dream4 = createDreamInfo("dream4", true, 3);
        final DreamInfo dream5 = createDreamInfo("dream5", true, 4);
        when(mBackend.isDreamSwitcherEnabled()).thenReturn(true);
        when(mBackend.getDreamInfos()).thenReturn(
                new ArrayList<>(Arrays.asList(dream1, dream2, dream3, dream4, dream5)));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);
        final RecyclerView recyclerView = mPreference.findViewById(R.id.dream_list);
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 1000);
        final RecyclerView.ViewHolder vhFrom = recyclerView.findViewHolderForAdapterPosition(1);
        final RecyclerView.ViewHolder vhTo = recyclerView.findViewHolderForAdapterPosition(3);
        final DreamPickerController.DreamItemTouchHelperCallback callback =
                controller.new DreamItemTouchHelperCallback(recyclerView);

        // act: move item from pos 1 to pos 3
        callback.onSelectedChanged(vhFrom, ItemTouchHelper.ACTION_STATE_DRAG);
        callback.onMove(recyclerView, vhFrom, vhTo);
        callback.clearView(recyclerView, vhFrom);

        // verify dream reordering
        final ArgumentCaptor<ComponentName[]> captor =
                ArgumentCaptor.forClass(ComponentName[].class);
        verify(mBackend).setActiveDreams(captor.capture());
        final List<ComponentName> activeComponents = Arrays.asList(captor.getValue());
        assertThat(activeComponents).containsExactly(
                dream1.componentName,
                dream3.componentName,
                dream4.componentName,
                dream2.componentName,
                dream5.componentName).inOrder();
        assertThat(dream1.order).isEqualTo(0);
        assertThat(dream3.order).isEqualTo(1);
        assertThat(dream4.order).isEqualTo(2);
        assertThat(dream2.order).isEqualTo(3);
        assertThat(dream5.order).isEqualTo(4);
    }

    @Test
    @EnableFlags(Flags.FLAG_DREAMS_SWITCHER)
    public void onMove_toInactiveItem_shouldNotReorder() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", true, 0);
        final DreamInfo dream2 = createDreamInfo("dream2", false, -1);
        when(mBackend.getDreamInfos()).thenReturn(
                new ArrayList<>(Arrays.asList(dream1, dream2)));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);
        final RecyclerView recyclerView = mPreference.findViewById(R.id.dream_list);
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 1000);
        final RecyclerView.ViewHolder vhFrom = recyclerView.findViewHolderForAdapterPosition(0);
        final RecyclerView.ViewHolder vhTo = recyclerView.findViewHolderForAdapterPosition(1);
        final DreamPickerController.DreamItemTouchHelperCallback callback =
                controller.new DreamItemTouchHelperCallback(recyclerView);
        // act: move item from pos 0 to pos 1
        callback.onSelectedChanged(vhFrom, ItemTouchHelper.ACTION_STATE_DRAG);
        final boolean moved = callback.onMove(recyclerView, vhFrom, vhTo);
        callback.clearView(recyclerView, vhFrom);
        // verify no reordering
        assertThat(moved).isFalse();
        final ArgumentCaptor<ComponentName[]> captor =
                ArgumentCaptor.forClass(ComponentName[].class);
        verify(mBackend).setActiveDreams(captor.capture());
        final List<ComponentName> activeComponents = Arrays.asList(captor.getValue());
        assertThat(activeComponents).containsExactly(dream1.componentName);
        assertThat(dream1.order).isEqualTo(0);
    }

    @Test
    public void displayPreference_dreamsSwitcherEnabled_itemTouchHelperAttached() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", true, 0);
        when(mBackend.isDreamSwitcherEnabled()).thenReturn(true);
        when(mBackend.getDreamInfos()).thenReturn(new ArrayList<>(List.of(dream1)));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);
        final RecyclerView recyclerView = mPreference.findViewById(R.id.dream_list);
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 1000);

        // verify ItemTouchHelper is attached.
        // With switcher on, there should be two item decorations: the spacing one and one
        // from the ItemTouchHelper
        assertThat(recyclerView.getItemDecorationCount()).isEqualTo(2);
        assertThat(recyclerView.getItemDecorationAt(0)).isInstanceOf(
                FixedSpaceAroundItemDecoration.class);
    }

    @Test
    public void displayPreference_dreamsSwitcherDisabled_itemTouchHelperNotAttached() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", true, 0);
        when(mBackend.isDreamSwitcherEnabled()).thenReturn(false);
        when(mBackend.getDreamInfos()).thenReturn(new ArrayList<>(List.of(dream1)));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);
        final RecyclerView recyclerView = mPreference.findViewById(R.id.dream_list);
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 1000);

        // verify ItemTouchHelper is not attached.
        assertThat(recyclerView.getItemDecorationCount()).isEqualTo(1);
        assertThat(recyclerView.getItemDecorationAt(0)).isInstanceOf(
                GridSpacingItemDecoration.class);
    }

    @Test
    public void getMovementFlags_forInactiveItem_returnsZero() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", true, 0);
        final DreamInfo dream2 = createDreamInfo("dream2", false, -1);
        when(mBackend.getDreamInfos()).thenReturn(new ArrayList<>(Arrays.asList(dream1, dream2)));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);
        final RecyclerView recyclerView = mPreference.findViewById(R.id.dream_list);
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 1000);
        final RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(1);
        final DreamPickerController.DreamItemTouchHelperCallback callback =
                controller.new DreamItemTouchHelperCallback(recyclerView);

        // act
        final int flags = callback.getMovementFlags(recyclerView, vh);

        // verify
        assertThat(flags).isEqualTo(0);
    }

    @Test
    @EnableFlags({Flags.FLAG_DREAMS_SWITCHER, Flags.FLAG_DREAMS_V2})
    public void onBindView_customizeButton_setsCorrectAccessibilityInfo() {
        // Setup
        final DreamInfo dream = createDreamInfo("dream1", true, 0);
        dream.settingsComponentName = new ComponentName("pkg", "cls");
        final View itemView = setUpSingleDreamCard(dream);
        final Button customizeButton = itemView.findViewById(R.id.customize_button_new);

        final AccessibilityDelegateCompat delegate =
                ViewCompat.getAccessibilityDelegate(customizeButton);
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();
        info.setSelected(true);

        // Verify
        delegate.onInitializeAccessibilityNodeInfo(customizeButton, info);
        assertThat(info.isSelected()).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_DREAMS_SWITCHER, Flags.FLAG_DREAMS_V2})
    public void onBindView_previewButton_setsCorrectContentDescription() {
        // Setup
        final DreamInfo dream = createDreamInfo("dream1", true, 0);
        final View itemView = setUpSingleDreamCard(dream);
        final View previewButton = itemView.findViewById(R.id.preview_button);

        // Verify
        assertThat(previewButton.getContentDescription().toString()).isEqualTo(
                mContext.getString(R.string.dream_preview_button_description, dream.caption));
    }

    @Test
    @EnableFlags({Flags.FLAG_DREAMS_SWITCHER, Flags.FLAG_DREAMS_V2})
    public void onBindView_customizeButton_setsCorrectContentDescription() {
        // Setup
        final DreamInfo dream = createDreamInfo("dream1", true, 0);
        dream.settingsComponentName = new ComponentName("pkg", "cls");
        final View itemView = setUpSingleDreamCard(dream);
        final Button customizeButton = itemView.findViewById(R.id.customize_button_new);

        // Verify
        assertThat(customizeButton.getContentDescription().toString()).isEqualTo(
                mContext.getString(R.string.customize_button_description, dream.caption));
    }

    @Test
    @EnableFlags(Flags.FLAG_DREAMS_SWITCHER)
    public void onBindView_setsCorrectAccessibilityInfo(@TestParameter boolean isActive) {
        // Setup
        final DreamInfo dream = createDreamInfo("dream1", isActive, isActive ? 0 : -1);
        final View itemView = setUpSingleDreamCard(dream);
        final AccessibilityDelegateCompat delegate =
                ViewCompat.getAccessibilityDelegate(itemView);
        final AccessibilityNodeInfoCompat info = AccessibilityNodeInfoCompat.obtain();

        // Verify
        delegate.onInitializeAccessibilityNodeInfo(itemView, info);
        assertThat(info.isCheckable()).isTrue();
        assertThat(info.isChecked()).isEqualTo(isActive);
        assertThat(info.isSelected()).isFalse();
    }

    private DreamInfo createDreamInfo(String caption, boolean isActive, int order) {
        final DreamInfo dreamInfo = new DreamInfo();
        dreamInfo.caption = caption;
        dreamInfo.componentName = new ComponentName("pkg", caption);
        dreamInfo.isActive = isActive;
        dreamInfo.order = order;
        dreamInfo.icon = mock(Drawable.class);
        when(dreamInfo.icon.mutate()).thenReturn(dreamInfo.icon);
        return dreamInfo;
    }

    private View setUpSingleDreamCard(DreamInfo dream) {
        when(mBackend.isDreamSwitcherEnabled()).thenReturn(true);
        when(mBackend.getDreamInfos()).thenReturn(new ArrayList<>(List.of(dream)));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);
        final RecyclerView recyclerView = mPreference.findViewById(R.id.dream_list);
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 1000);
        return recyclerView.findViewHolderForAdapterPosition(0).itemView;
    }

    // ===========================================================================================
    // Start auto-scrolling tests
    // ===========================================================================================
    private DreamPickerController.DreamItemTouchHelperCallback setupAutoScrollTest() {
        final List<DreamInfo> mockDreamInfos =
                new ArrayList<>(Collections.singletonList(createDreamInfo("d1", true, 0)));
        when(mBackend.getDreamInfos()).thenReturn(mockDreamInfos);
        DreamPickerController controller = buildController();

        ViewParent mockParent = mock(ViewParent.class);
        when(mInnerRecyclerView.getParent()).thenReturn(mockParent);
        when(mockParent.getParent()).thenReturn(mParentRecyclerView);

        final DreamPickerController.DreamItemTouchHelperCallback callback =
                controller.new DreamItemTouchHelperCallback(mInnerRecyclerView);

        mViewHolder = new RecyclerView.ViewHolder(mItemView) {};
        when(mParentRecyclerView.getHeight()).thenReturn(PARENT_HEIGHT);
        // Set up parent to be at top of screen.
        when(mParentRecyclerView.getGlobalVisibleRect(any()))
                .thenAnswer(invocation -> {
                    Rect rect = invocation.getArgument(0);
                    rect.set(0, 0, PARENT_WIDTH, PARENT_HEIGHT);
                    return null;
                });
        when(mInnerRecyclerView.getResources()).thenReturn(mContext.getResources());
        when(mParentRecyclerView.post(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        });

        return callback;
    }

    @Test
    @EnableFlags(Flags.FLAG_DREAMS_SWITCHER)
    public void onChildDraw_dragDown_shouldScrollDown() {
        final DreamPickerController.DreamItemTouchHelperCallback callback = setupAutoScrollTest();
        // Item is in the scroll-down zone.
        setItemBottom(PARENT_HEIGHT - 50);
        // Inner RV is not at the bottom yet, it is far below the parent's viewable area.
        setInnerRecyclerViewBounds(200, PARENT_HEIGHT + 200);

        callback.onSelectedChanged(mViewHolder, ItemTouchHelper.ACTION_STATE_DRAG);
        callback.onChildDraw(mock(Canvas.class), mInnerRecyclerView, mViewHolder, 0, 0,
                ItemTouchHelper.ACTION_STATE_DRAG, true);
        ShadowLooper.runUiThreadTasks();

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(mParentRecyclerView).scrollBy(eq(0), captor.capture());
        assertThat(captor.getValue()).isGreaterThan(0);
    }

    @Test
    @EnableFlags(Flags.FLAG_DREAMS_SWITCHER)
    public void onChildDraw_dragUp_shouldScrollUp() {
        final DreamPickerController.DreamItemTouchHelperCallback callback = setupAutoScrollTest();
        // Item is in the scroll-up zone.
        setItemTop(50);
        // Inner RV is not at the top yet, it is far above the parent's viewable area.
        setInnerRecyclerViewBounds(-200, PARENT_HEIGHT - 200);

        callback.onSelectedChanged(mViewHolder, ItemTouchHelper.ACTION_STATE_DRAG);
        callback.onChildDraw(mock(Canvas.class), mInnerRecyclerView, mViewHolder, 0, 0,
                ItemTouchHelper.ACTION_STATE_DRAG, true);
        ShadowLooper.runUiThreadTasks();

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(mParentRecyclerView).scrollBy(eq(0), captor.capture());
        assertThat(captor.getValue()).isLessThan(0);
    }

    @Test
    @EnableFlags(Flags.FLAG_DREAMS_SWITCHER)
    public void onChildDraw_dragDownAtBottom_shouldNotScroll() {
        final DreamPickerController.DreamItemTouchHelperCallback callback = setupAutoScrollTest();
        // Item is in the scroll-down zone.
        setItemBottom(PARENT_HEIGHT - 50);
        // Inner RV is scrolled to the bottom (its bottom edge is above the parent).
        setInnerRecyclerViewBounds(
            -PARENT_SCROLL_THRESHOLD, PARENT_HEIGHT - PARENT_SCROLL_THRESHOLD);

        callback.onSelectedChanged(mViewHolder, ItemTouchHelper.ACTION_STATE_DRAG);
        callback.onChildDraw(mock(Canvas.class), mInnerRecyclerView, mViewHolder, 0, 0,
                ItemTouchHelper.ACTION_STATE_DRAG, true);
        ShadowLooper.runUiThreadTasks();

        verify(mParentRecyclerView, never()).scrollBy(anyInt(), anyInt());
    }

    @Test
    @EnableFlags(Flags.FLAG_DREAMS_SWITCHER)
    public void onChildDraw_dragUpAtTop_shouldNotScroll() {
        final DreamPickerController.DreamItemTouchHelperCallback callback = setupAutoScrollTest();
        // Item is in the scroll-up zone.
        setItemTop(50);
        // Inner RV is scrolled to the top (its top edge is below the parent).
        setInnerRecyclerViewBounds(
            PARENT_SCROLL_THRESHOLD, PARENT_HEIGHT + PARENT_SCROLL_THRESHOLD);

        callback.onSelectedChanged(mViewHolder, ItemTouchHelper.ACTION_STATE_DRAG);
        callback.onChildDraw(mock(Canvas.class), mInnerRecyclerView, mViewHolder, 0, 0,
                ItemTouchHelper.ACTION_STATE_DRAG, true);
        ShadowLooper.runUiThreadTasks();

        verify(mParentRecyclerView, never()).scrollBy(anyInt(), anyInt());
    }

    private void setItemTop(int top) {
        when(mItemView.getGlobalVisibleRect(any()))
                .thenAnswer(invocation -> {
                    Rect rect = invocation.getArgument(0);
                    rect.set(0, top, PARENT_WIDTH, top + ITEM_HEIGHT);
                    return null;
                });
    }

    private void setItemBottom(int bottom) {
        when(mItemView.getGlobalVisibleRect(any()))
                .thenAnswer(invocation -> {
                    Rect rect = invocation.getArgument(0);
                    rect.set(0, bottom - ITEM_HEIGHT, PARENT_WIDTH, bottom);
                    return null;
                });
    }

    private void setInnerRecyclerViewBounds(int top, int bottom) {
        when(mInnerRecyclerView.getGlobalVisibleRect(any()))
                .thenAnswer(invocation -> {
                    Rect rect = invocation.getArgument(0);
                    rect.set(0, top, PARENT_WIDTH, bottom);
                    return null;
                });
    }
    // ===========================================================================================
    // End auto-scrolling tests
    // ===========================================================================================
}
