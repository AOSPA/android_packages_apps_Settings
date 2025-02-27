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

package com.android.settings.network.telephony;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.network.telephony.SatelliteAppListCategoryController.MAXIMUM_OF_PREFERENCE_AMOUNT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.platform.test.annotations.EnableFlags;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.network.SatelliteRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;

public class SatelliteAppListCategoryControllerTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final List<String> PACKAGE_NAMES = List.of("com.android.settings",
            "com.android.apps.messaging", "com.android.dialer", "com.android.systemui");
    private static final String KEY = "SatelliteAppListCategoryControllerTest";

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private SatelliteRepository mRepository;

    private Context mContext;
    private SatelliteAppListCategoryController mController;


    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    @EnableFlags(Flags.FLAG_SATELLITE_25Q4_APIS)
    public void displayPreference_has4SatSupportedApps_showMaxPreference() throws Exception {
        when(mRepository.getSatelliteDataOptimizedApps()).thenReturn(PACKAGE_NAMES);
        when(mPackageManager.getApplicationInfoAsUser(any(), anyInt(), anyInt())).thenReturn(
                new ApplicationInfo());
        mController = new SatelliteAppListCategoryController(mContext, KEY);
        mController.init(mRepository);
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        PreferenceCategory category = new PreferenceCategory(mContext);
        category.setKey(mController.getPreferenceKey());
        preferenceScreen.addPreference(category);

        mController.displayPreference(preferenceScreen);

        assertThat(category.getPreferenceCount() == MAXIMUM_OF_PREFERENCE_AMOUNT).isTrue();
    }


    @Test
    @EnableFlags(Flags.FLAG_SATELLITE_25Q4_APIS)
    public void getAvailabilityStatus_hasSatSupportedApps_returnAvailable() {
        when(mRepository.getSatelliteDataOptimizedApps()).thenReturn(PACKAGE_NAMES);
        mController = new SatelliteAppListCategoryController(mContext, KEY);
        mController.init(mRepository);

        int result = mController.getAvailabilityStatus();

        assertThat(result).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_SATELLITE_25Q4_APIS)
    public void getAvailabilityStatus_noSatSupportedApps_returnUnavailable() {
        List<String> packageNames = Collections.emptyList();
        when(mRepository.getSatelliteDataOptimizedApps()).thenReturn(packageNames);
        mController = new SatelliteAppListCategoryController(mContext, KEY);
        mController.init(mRepository);

        int result = mController.getAvailabilityStatus();

        assertThat(result).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }
}
