/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

/** Tests for {@link PresetListPreference}. */
@RunWith(RobolectricTestRunner.class)
public class PresetListPreferenceTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private PresetListPreference mPreference;

    @Mock
    PresetListPreference.PresetArrayAdapter mAdapter;
    private static final CharSequence[] TEST_ENTRY_VALUES = {"1", "2", "3"};

    @Before
    public void setUp() {
        mPreference = new PresetListPreference(mContext);
        mPreference.setAdapter(mAdapter);
        mPreference.setEntryValues(TEST_ENTRY_VALUES);
    }

    @Test
    public void setValue_refreshAdapter() {
        mPreference.setValue("2");

        List<String> entryStringList = Arrays.stream(TEST_ENTRY_VALUES)
                .map(entry -> entry == null ? null : entry.toString())
                .toList();
        verify(mAdapter).setSelectedIndex(entryStringList.indexOf("2"));
    }
}
