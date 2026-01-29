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
        final DreamInfo dream1 = new DreamInfo();
        dream1.componentName = new ComponentName("pkg", "dream1");
        dream1.isActive = true;
        dream1.icon = mock(Drawable.class);
        when(dream1.icon.mutate()).thenReturn(dream1.icon);
        final DreamInfo dream2 = new DreamInfo();
        dream2.componentName = new ComponentName("pkg", "dream2");
        dream2.isActive = false;
        dream2.icon = mock(Drawable.class);
        when(dream2.icon.mutate()).thenReturn(dream2.icon);
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
        final DreamInfo dream1 = new DreamInfo();
        dream1.caption = "dream1";
        dream1.componentName = new ComponentName("pkg", "dream1");
        dream1.isActive = false;
        dream1.icon = mock(Drawable.class);
        when(dream1.icon.mutate()).thenReturn(dream1.icon);
        final DreamInfo dream2 = new DreamInfo();
        dream2.caption = "dream2";
        dream2.componentName = new ComponentName("pkg", "dream2");
        dream2.isActive = false;
        dream2.icon = mock(Drawable.class);
        when(dream2.icon.mutate()).thenReturn(dream2.icon);
        when(mBackend.getDreamInfos()).thenReturn(Arrays.asList(dream1, dream2));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);
        final RecyclerView recyclerView = mPreference.findViewById(R.id.dream_list);
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 1000);
        assertThat(recyclerView.getItemDecorationCount()).isEqualTo(1);
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
        final DreamInfo dream1 = new DreamInfo();
        dream1.caption = "dream1";
        dream1.componentName = new ComponentName("pkg", "dream1");
        dream1.isActive = true;
        dream1.order = 1;
        dream1.icon = mock(Drawable.class);
        when(dream1.icon.mutate()).thenReturn(dream1.icon);
        final DreamInfo dream2 = new DreamInfo();
        dream2.caption = "dream2";
        dream2.componentName = new ComponentName("pkg", "dream2");
        dream2.isActive = true;
        dream2.order = 0;
        dream2.icon = mock(Drawable.class);
        when(dream2.icon.mutate()).thenReturn(dream2.icon);
        when(mBackend.getDreamInfos()).thenReturn(Arrays.asList(dream1, dream2));
        final DreamPickerController controller = buildController();
        controller.updateState(mPreference);
        final RecyclerView recyclerView = mPreference.findViewById(R.id.dream_list);
        recyclerView.measure(0, 0);
        recyclerView.layout(0, 0, 100, 1000);
        assertThat(recyclerView.getItemDecorationCount()).isEqualTo(1);
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
}
