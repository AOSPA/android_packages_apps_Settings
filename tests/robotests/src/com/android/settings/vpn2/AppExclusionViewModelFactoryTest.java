/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law of an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.vpn2;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.app.Application;
import android.os.UserHandle;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AppExclusionViewModelFactoryTest {
    private static final String VPN_PACKAGE = "com.test.vpn";
    private static final int USER_ID = UserHandle.myUserId();

    @Mock private Application mApplication;

    private AppExclusionViewModelFactory mFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFactory = new AppExclusionViewModelFactory(mApplication, USER_ID, VPN_PACKAGE);
    }

    @Test
    public void create_shouldReturnAppExclusionViewModel() {
        ViewModel viewModel = mFactory.create(AppExclusionViewModel.class);
        assertThat(viewModel).isInstanceOf(AppExclusionViewModel.class);
    }

    @Test
    public void create_withUnsupportedModel_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> mFactory.create(AndroidViewModel.class));
    }
}
