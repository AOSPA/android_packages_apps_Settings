/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.accessibility;

import static android.view.View.OVER_SCROLL_NEVER;
import static com.google.common.truth.Truth.assertThat;

import android.app.Dialog;
import android.content.Context;
import android.widget.ListView;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.accessibility.ItemInfoArrayAdapter.ItemInfo;
import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

/** Tests for {@link AccessibilityDialogUtils} */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityDialogUtilsTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
    }

    @Test
    public void showDialog_createCustomDialog_isShowing() {
        final Dialog dialog = AccessibilityDialogUtils.createCustomDialog(mContext,
                "Title", /* customView= */ null, "positiveButton", /* positiveListener= */ null,
                "negativeButton", /* negativeListener= */ null);
        dialog.show();

        assertThat(dialog.isShowing()).isTrue();
    }

    @Test
    public void createSingleChoiceListView_overScrollModeIsNever() {
        // Create a list of items for the ListView.
        final List<ItemInfo> items = ImmutableList.of(
                new ItemInfo("Test Item 1", /* summary= */ null, /* drawable= */ null),
                new ItemInfo("Test Item 2", /* summary= */ null, /* drawable= */ null)
        );

        // Call the method under test.
        final ListView listView = AccessibilityDialogUtils.createSingleChoiceListView(
                mContext, items, /* itemListener= */ null);

        // Verify that the over-scroll mode is set to OVER_SCROLL_NEVER.
        // This is the main assertion for the change being tested.
        assertThat(listView.getOverScrollMode()).isEqualTo(OVER_SCROLL_NEVER);

        // Also verify other properties are set correctly as a general sanity check.
        assertThat(listView.getChoiceMode()).isEqualTo(ListView.CHOICE_MODE_SINGLE);
        assertThat(listView.getAdapter()).isNotNull();
        assertThat(listView.getAdapter().getCount()).isEqualTo(items.size());
        assertThat(listView.getDivider()).isNull();
    }
}
