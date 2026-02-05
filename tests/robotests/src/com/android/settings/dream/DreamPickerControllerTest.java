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
import android.widget.TextView;
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
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
public class DreamPickerControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DreamBackend mBackend;
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    private LayoutPreference mPreference;

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
    @DisableFlags(Flags.FLAG_DREAMS_SWITCHER)
    public void onItemClicked_dreamsSwitcherDisabled_setsActiveDream() {
        final DreamInfo dream1 = createDreamInfo("dream1", true, 0);
        final DreamInfo dream2 = createDreamInfo("dream2", false, -1);
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
    @EnableFlags(Flags.FLAG_DREAMS_SWITCHER)
    public void onItemClicked_notSelected_dreamsSwitcherEnabled_selectsDream() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", false, -1);
        final DreamInfo dream2 = createDreamInfo("dream2", false, -1);
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
    @EnableFlags(Flags.FLAG_DREAMS_SWITCHER)
    public void onItemClicked_selected_dreamsSwitcherEnabled_deselectsDream() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", true, 1);
        final DreamInfo dream2 = createDreamInfo("dream2", true, 0);
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
    @EnableFlags(Flags.FLAG_DREAMS_SWITCHER)
    public void onMove_up_dreamsSwitcherEnabled_reordersDreams() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", true, 0);
        final DreamInfo dream2 = createDreamInfo("dream2", true, 1);
        final DreamInfo dream3 = createDreamInfo("dream3", true, 2);
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
                controller.new DreamItemTouchHelperCallback();
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
    @EnableFlags(Flags.FLAG_DREAMS_SWITCHER)
    public void onMove_down_dreamsSwitcherEnabled_reordersDreams() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", true, 0);
        final DreamInfo dream2 = createDreamInfo("dream2", true, 1);
        final DreamInfo dream3 = createDreamInfo("dream3", true, 2);
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
                controller.new DreamItemTouchHelperCallback();
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
    @EnableFlags(Flags.FLAG_DREAMS_SWITCHER)
    public void onMove_middle_dreamsSwitcherEnabled_reordersDreams() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", true, 0);
        final DreamInfo dream2 = createDreamInfo("dream2", true, 1);
        final DreamInfo dream3 = createDreamInfo("dream3", true, 2);
        final DreamInfo dream4 = createDreamInfo("dream4", true, 3);
        final DreamInfo dream5 = createDreamInfo("dream5", true, 4);
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
                controller.new DreamItemTouchHelperCallback();

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
                controller.new DreamItemTouchHelperCallback();
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
    @EnableFlags(Flags.FLAG_DREAMS_SWITCHER)
    public void displayPreference_dreamsSwitcherEnabled_itemTouchHelperAttached() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", true, 0);
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
    @DisableFlags(Flags.FLAG_DREAMS_SWITCHER)
    public void displayPreference_dreamsSwitcherDisabled_itemTouchHelperNotAttached() {
        // setup
        final DreamInfo dream1 = createDreamInfo("dream1", true, 0);
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
    @EnableFlags(Flags.FLAG_DREAMS_SWITCHER)
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
                controller.new DreamItemTouchHelperCallback();

        // act
        final int flags = callback.getMovementFlags(recyclerView, vh);

        // verify
        assertThat(flags).isEqualTo(0);
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
}
