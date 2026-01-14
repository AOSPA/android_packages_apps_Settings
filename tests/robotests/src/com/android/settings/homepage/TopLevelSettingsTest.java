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

package com.android.settings.homepage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.WindowInsets;

import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TopLevelSettingsTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private Context mContext;
    private TopLevelSettings mSettings;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mSettings = spy(new TopLevelSettings());
        when(mSettings.getContext()).thenReturn(mContext);
        final FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.dashboardFeatureProvider
                .getTilesForCategory(nullable(String.class)))
                .thenReturn(null);
        mSettings.onAttach(mContext);
    }

    @Test
    public void setPaddingHorizontal_freeformWindow_setsBottomPadding() {
        final RecyclerView recyclerView = mock(RecyclerView.class);
        doReturn(recyclerView).when(mSettings).getListView();
        doReturn(true).when(mSettings).isRunningInFreeformWindow(recyclerView);
        final int padding = 10;
        final int paddingBottom = mContext.getResources().getDimensionPixelSize(
                R.dimen.dashboard_padding_bottom);

        mSettings.setPaddingHorizontal(padding);

        verify(recyclerView).setPadding(padding, 0, padding, paddingBottom);
    }

    @Test
    public void setPaddingHorizontal_notFreeformWindow_setsZeroBottomPadding() {
        final RecyclerView recyclerView = mock(RecyclerView.class);
        doReturn(recyclerView).when(mSettings).getListView();
        doReturn(false).when(mSettings).isRunningInFreeformWindow(recyclerView);
        final int padding = 10;

        mSettings.setPaddingHorizontal(padding);

        verify(recyclerView).setPadding(padding, 0, padding, 0);
    }

    @Test
    public void isRunningInFreeformWindow_hasCaptionBar_returnsTrue() {
        final RecyclerView recyclerView = mock(RecyclerView.class);
        final WindowInsets insets = mock(WindowInsets.class);
        when(recyclerView.getRootWindowInsets()).thenReturn(insets);
        when(insets.isVisible(WindowInsets.Type.captionBar())).thenReturn(true);

        assertThat(mSettings.isRunningInFreeformWindow(recyclerView)).isTrue();
    }

    @Test
    public void isRunningInFreeformWindow_noCaptionBar_returnsFalse() {
        final RecyclerView recyclerView = mock(RecyclerView.class);
        final WindowInsets insets = mock(WindowInsets.class);
        when(recyclerView.getRootWindowInsets()).thenReturn(insets);
        when(insets.isVisible(WindowInsets.Type.captionBar())).thenReturn(false);

        assertThat(mSettings.isRunningInFreeformWindow(recyclerView)).isFalse();
    }
}
