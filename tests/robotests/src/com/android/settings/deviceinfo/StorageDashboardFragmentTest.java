/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.storage.StorageManager;
import android.provider.SearchIndexableResource;
import android.util.SparseArray;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class StorageDashboardFragmentTest {
    private static final String FREE_UP_SPACE_KEY = "free_up_space";
    @Mock
    private PackageManager mMockPackageManager;
    private Context mContext;
    private StorageDashboardFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = new StorageDashboardFragment();
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mMockPackageManager);
    }

    @Test
    public void testCategory_isConnectedDevice() {
        assertThat(mFragment.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_STORAGE);
    }

    @Test
    public void test_initializeOptionsMenuInvalidatesExistingMenu() {
        Activity activity = mock(Activity.class);

        mFragment.initializeOptionsMenu(activity);

        verify(activity).invalidateOptionsMenu();
    }

    @Test
    public void test_loadWhenQuotaOffIfVolumeInfoNotLoaded() {
        View fakeView = mock(View.class, RETURNS_DEEP_STUBS);
        RecyclerView fakeRecyclerView = mock(RecyclerView.class, RETURNS_DEEP_STUBS);
        when(fakeView.findViewById(anyInt())).thenReturn(fakeView);
        mFragment = spy(mFragment);
        when(mFragment.getView()).thenReturn(fakeView);
        when(mFragment.getListView()).thenReturn(fakeRecyclerView);

        mFragment.maybeSetLoading(false);

        verify(mFragment).setLoading(true, false);
    }

    @Test
    public void test_dontLoadWhenQuotaOffIfVolumeInfoNotLoaded() {
        View fakeView = mock(View.class, RETURNS_DEEP_STUBS);
        RecyclerView fakeRecyclerView = mock(RecyclerView.class, RETURNS_DEEP_STUBS);
        when(fakeView.findViewById(anyInt())).thenReturn(fakeView);
        mFragment = spy(mFragment);
        when(mFragment.getView()).thenReturn(fakeView);
        when(mFragment.getListView()).thenReturn(fakeRecyclerView);

        PrivateStorageInfo info = new PrivateStorageInfo(0, 0);
        mFragment.setPrivateStorageInfo(info);

        mFragment.maybeSetLoading(false);

        verify(mFragment, never()).setLoading(true, false);
    }

    @Test
    public void test_loadWhenQuotaOnAndVolumeInfoLoadedButAppsMissing() {
        View fakeView = mock(View.class, RETURNS_DEEP_STUBS);
        RecyclerView fakeRecyclerView = mock(RecyclerView.class, RETURNS_DEEP_STUBS);
        when(fakeView.findViewById(anyInt())).thenReturn(fakeView);
        mFragment = spy(mFragment);
        when(mFragment.getView()).thenReturn(fakeView);
        when(mFragment.getListView()).thenReturn(fakeRecyclerView);

        PrivateStorageInfo info = new PrivateStorageInfo(0, 0);
        mFragment.setPrivateStorageInfo(info);

        mFragment.maybeSetLoading(true);

        verify(mFragment).setLoading(true, false);
    }

    @Test
    public void test_loadWhenQuotaOnAndAppsLoadedButVolumeInfoMissing() {
        View fakeView = mock(View.class, RETURNS_DEEP_STUBS);
        RecyclerView fakeRecyclerView = mock(RecyclerView.class, RETURNS_DEEP_STUBS);
        when(fakeView.findViewById(anyInt())).thenReturn(fakeView);
        mFragment = spy(mFragment);
        when(mFragment.getView()).thenReturn(fakeView);
        when(mFragment.getListView()).thenReturn(fakeRecyclerView);
        mFragment.setStorageResult(new SparseArray<>());

        mFragment.maybeSetLoading(true);

        verify(mFragment).setLoading(true, false);
    }

    @Test
    public void test_dontLoadWhenQuotaOnAndAllLoaded() {
        View fakeView = mock(View.class, RETURNS_DEEP_STUBS);
        RecyclerView fakeRecyclerView = mock(RecyclerView.class, RETURNS_DEEP_STUBS);
        when(fakeView.findViewById(anyInt())).thenReturn(fakeView);
        mFragment = spy(mFragment);
        when(mFragment.getView()).thenReturn(fakeView);
        when(mFragment.getListView()).thenReturn(fakeRecyclerView);

        mFragment.setStorageResult(new SparseArray<>());
        PrivateStorageInfo storageInfo = new PrivateStorageInfo(0, 0);
        mFragment.setPrivateStorageInfo(storageInfo);

        mFragment.maybeSetLoading(true);

        verify(mFragment, never()).setLoading(true, false);
    }

    @Test
    public void testSearchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                StorageDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                    .getXmlResourcesToIndex(RuntimeEnvironment.application, true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mFragment.getPreferenceScreenResId());
    }

    @Test
    public void searchIndexProvider_manageStorageIntentHandled_FreeUpSpaceIsSearchable() {
        setupIntentHandling(true);

        BaseSearchIndexProvider indexProvider = StorageDashboardFragment.SEARCH_INDEX_DATA_PROVIDER;
        List<String> nonIndexableKeys = indexProvider.getNonIndexableKeys(mContext);

        assertThat(nonIndexableKeys).doesNotContain(FREE_UP_SPACE_KEY);
    }

    @Test
    public void searchIndexProvider_manageStorageIntentNotHandled_FreeUpIsNotSearchable() {
        setupIntentHandling(false);

        BaseSearchIndexProvider indexProvider = StorageDashboardFragment.SEARCH_INDEX_DATA_PROVIDER;
        List<String> nonIndexableKeys = indexProvider.getNonIndexableKeys(mContext);

        assertThat(nonIndexableKeys).contains(FREE_UP_SPACE_KEY);
    }

    private void setupIntentHandling(boolean canHandle) {
        List<ResolveInfo> resolveInfoList = new ArrayList<>();
        if (canHandle) {
            resolveInfoList.add(new ResolveInfo()); // Simulate an activity can handle the intent
        }

        when(mMockPackageManager.queryIntentActivitiesAsUser(
                any(Intent.class),
                eq(PackageManager.MATCH_DEFAULT_ONLY), anyInt()))
                .thenAnswer(invocation -> {
                    Intent intent = invocation.getArgument(0);
                    if (StorageManager.ACTION_MANAGE_STORAGE.equals(intent.getAction())) {
                        return resolveInfoList;
                    }
                    return Collections.emptyList();
                });
    }
}
