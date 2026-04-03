/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.android.settings.personalcontext;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.service.personalcontext.PersonalContextManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PersonalContextAppPreferenceControllerTest {
    private static final String TEST_PACKAGE_NAME = "com.foo.bar";
    @Mock
    private Context mContext;
    @Mock
    private PersonalContextManager mManager;

    @Mock
    private Resources mResources;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(eq(PersonalContextManager.class))).thenReturn(mManager);
        when(mManager.isEnabled()).thenReturn(true);
        when(mManager.isPersonalContextModeEnabled(eq(TEST_PACKAGE_NAME))).thenReturn(true);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(
                eq(com.android.internal.R.bool.config_enablePersonalContextManagerService)))
                .thenReturn(true);
    }

    @Test
    public void testPersonalContextManagerNull_presenterReturnsFalse() {
        when(mContext.getSystemService(eq(PersonalContextManager.class))).thenReturn(null);
        final PersonalContextAppPreferenceController controller = new
                PersonalContextAppPreferenceController(mContext, TEST_PACKAGE_NAME);
        assertThat(controller.isPersonalContextForAppEnabled()).isFalse();
        assertThat(controller.isPersonalContextServiceEnabled()).isFalse();
        assertThat(controller.isPersonalContextAvailable()).isFalse();
    }

    @Test
    public void testPersonalContextManagerUnavailable_presenterReturnsFalse() {
        when(mContext.getSystemService(eq(PersonalContextManager.class))).thenReturn(null);
        final PersonalContextAppPreferenceController controller = new
                PersonalContextAppPreferenceController(mContext, TEST_PACKAGE_NAME);
        assertThat(controller.isPersonalContextForAppEnabled()).isFalse();
        assertThat(controller.isPersonalContextServiceEnabled()).isFalse();
        assertThat(controller.isPersonalContextAvailable()).isFalse();
    }

    @Test
    public void testPersonalContextManagerAvailableAndAppEnabled_presenterReturnsTrue() {
        final PersonalContextAppPreferenceController controller = new
                PersonalContextAppPreferenceController(mContext, TEST_PACKAGE_NAME);
        assertThat(controller.isPersonalContextForAppEnabled()).isTrue();
        assertThat(controller.isPersonalContextServiceEnabled()).isTrue();
        assertThat(controller.isPersonalContextAvailable()).isTrue();
    }
}
