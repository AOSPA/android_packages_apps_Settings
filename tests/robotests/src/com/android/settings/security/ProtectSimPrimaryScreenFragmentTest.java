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

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.network.telephony.ConvertToEsimPreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.IntroPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ProtectSimPrimaryScreenFragmentTest {

    private TestFragment mFragment;
    private Context mContext;

    @Mock
    private AutoManagedSimPinHelper mAutoManagedSimPinHelper;
    @Mock
    private SubscriptionManager mSubscriptionManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mFragment = spy(new TestFragment());
        doReturn(mContext).when(mFragment).getContext();
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        mFragment.setAutoManagedSimPinHelperForTesting(mAutoManagedSimPinHelper);
        FakeFeatureFactory.setupForTest();
    }

    @Test
    public void onAttach_shouldSetSlotIndexOnControllers() {
        SimPinProtectionToggleController controller0 = mock(SimPinProtectionToggleController.class);
        when(controller0.getPreferenceKey()).thenReturn("toggle_0");
        SimPinProtectionToggleController controller1 = mock(SimPinProtectionToggleController.class);
        when(controller1.getPreferenceKey()).thenReturn("toggle_1");

        List<SimPinProtectionToggleController> toggleControllers = new ArrayList<>();
        toggleControllers.add(controller0);
        toggleControllers.add(controller1);

        ChangeSimPinPreferenceController changeController0 =
                mock(ChangeSimPinPreferenceController.class);
        when(changeController0.getPreferenceKey()).thenReturn("change_0");
        ChangeSimPinPreferenceController changeController1 =
                mock(ChangeSimPinPreferenceController.class);
        when(changeController1.getPreferenceKey()).thenReturn("change_1");

        List<ChangeSimPinPreferenceController> changeControllers = new ArrayList<>();
        changeControllers.add(changeController0);
        changeControllers.add(changeController1);

        doReturn(toggleControllers).when(mFragment).useAll(SimPinProtectionToggleController.class);
        doReturn(changeControllers).when(mFragment).useAll(ChangeSimPinPreferenceController.class);
        doReturn(null).when(mFragment).use(ConvertToEsimPreferenceController.class);

        mFragment.onAttach(mContext);

        verify(controller0).setSlotIndex(0);
        verify(controller1).setSlotIndex(1);
        verify(changeController0).setSlotIndex(0);
        verify(changeController1).setSlotIndex(1);
    }

    @Test
    public void getPreferenceScreenResId_shouldBeCorrect() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.automatic_sim_lock_protection_settings);
    }

    @Test
    public void onResume_shouldInitSlotHeadersWithCarrierName() {
        doReturn(new ArrayList<>()).when(mFragment).useAll(any());
        mFragment.onAttach(mContext);
        mFragment.setAutoManagedSimPinHelperForTesting(mAutoManagedSimPinHelper);

        IntroPreference intro0 = new IntroPreference(mContext);
        PreferenceCategory category0 = new PreferenceCategory(mContext);
        IntroPreference intro1 = new IntroPreference(mContext);
        PreferenceCategory category1 = new PreferenceCategory(mContext);

        doReturn(intro0).when(mFragment).findPreference("first_active_slot_intro");
        doReturn(category0).when(mFragment).findPreference("category_first_sim_card_slot");
        doReturn(intro1).when(mFragment).findPreference("second_active_slot_intro");
        doReturn(category1).when(mFragment).findPreference("category_second_sim_card_slot");

        when(mAutoManagedSimPinHelper.getActiveSlots()).thenReturn(new int[]{0, 1});

        SubscriptionInfo subInfo0 = mock(SubscriptionInfo.class);
        when(subInfo0.getCarrierName()).thenReturn("Carrier 0");
        when(mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0)).thenReturn(subInfo0);

        SubscriptionInfo subInfo1 = mock(SubscriptionInfo.class);
        when(subInfo1.getCarrierName()).thenReturn("Carrier 1");
        when(mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(1)).thenReturn(subInfo1);

        mFragment.onResume();

        assertThat(intro0.getTitle().toString()).isEqualTo("Carrier 0");
        assertThat(intro1.getTitle().toString()).isEqualTo("Carrier 1");
    }

    @Test
    public void onResume_noActiveSlots_shouldHideHeaders() {
        doReturn(new ArrayList<>()).when(mFragment).useAll(any());
        mFragment.onAttach(mContext);
        mFragment.setAutoManagedSimPinHelperForTesting(mAutoManagedSimPinHelper);

        IntroPreference intro0 = new IntroPreference(mContext);
        PreferenceCategory category0 = new PreferenceCategory(mContext);

        doReturn(intro0).when(mFragment).findPreference("first_active_slot_intro");
        doReturn(category0).when(mFragment).findPreference("category_first_sim_card_slot");

        when(mAutoManagedSimPinHelper.getActiveSlots()).thenReturn(new int[]{});

        mFragment.onResume();

        assertThat(intro0.isVisible()).isFalse();
        assertThat(category0.isVisible()).isFalse();
    }

    public static class TestFragment extends ProtectSimPrimaryScreenFragment {
        @Override
        public <T extends AbstractPreferenceController> T use(Class<T> clazz) {
            return super.use(clazz);
        }

        @Override
        public <T extends AbstractPreferenceController> List<T> useAll(Class<T> clazz) {
            return super.useAll(clazz);
        }

        @Override
        public <T extends Preference> T findPreference(CharSequence key) {
            return super.findPreference(key);
        }
    }
}
