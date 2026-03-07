/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.nfc;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class SecureNfcPreferenceControllerTest {

    private Context mContext;
    private SecureNfcPreferenceController mController;

    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());

        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);

        mController = new SecureNfcPreferenceController(mContext, "nfc_secure_settings");
    }

    @Test
    public void isPublicSlice_returnsTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }

    @Test
    public void setChecked_guestUser_shouldReturnFalse() {
        when(mUserManager.isGuestUser()).thenReturn(true);

        boolean result = mController.setChecked(true);

        assertThat(result).isFalse();
    }
}
