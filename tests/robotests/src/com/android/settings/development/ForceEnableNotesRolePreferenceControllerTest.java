/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.development;

import static com.android.settings.development.ForceEnableNotesRolePreferenceController.OVERLAY_PACKAGE_NAME;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.pm.UserInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.systemui.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ForceEnableNotesRolePreferenceControllerTest.ShadowOverlayManagerStub.class,
        ShadowUserManager.class})
public class ForceEnableNotesRolePreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;
    @Mock
    private UserManager mUserManager;
    @Mock
    private static IOverlayManager sOverlayManager;

    private ForceEnableNotesRolePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new ForceEnableNotesRolePreferenceController(
                RuntimeEnvironment.application) {
            private boolean mEnabled;

            protected boolean isEnabled() {
                return mEnabled;
            }

            protected void setEnabled(boolean enabled) {
                mEnabled = enabled;
            }
        };
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void setEnabled_updatesForAllFullAndProfileUsers() throws RemoteException {
        Context context = spy(RuntimeEnvironment.application.getApplicationContext());
        UserInfo user1 = new UserInfo(1, "Name", "Path", 0x0ff0ff,
                UserManager.USER_TYPE_FULL_SYSTEM);
        UserInfo user2 = new UserInfo(2, "Name", "Path", 0x0ff0ff,
                UserManager.USER_TYPE_PROFILE_MANAGED);
        UserInfo user3 = new UserInfo(3, "Name", "Path", 0x0ff0ff, "Some other type");
        when(context.getSystemService(UserManager.class)).thenReturn(mUserManager);

        when(mUserManager.getUsers()).thenReturn(Arrays.asList(user1, user2, user3));

        mController = new ForceEnableNotesRolePreferenceController(context);
        mController.setEnabled(true);

        verify(sOverlayManager).setEnabled(OVERLAY_PACKAGE_NAME, true, 1);
        verify(sOverlayManager).setEnabled(OVERLAY_PACKAGE_NAME, true, 2);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_NOTE_HSUM_DEV_OPTION_FIX)
    public void setEnabled_hsumDevices_fixDisabled_updatesFullAndProfileUsers()
            throws RemoteException {
        Context context = spy(RuntimeEnvironment.application.getApplicationContext());
        UserManager userManager = context.getSystemService(UserManager.class);
        ShadowUserManager shadowUserManager = Shadow.extract(userManager);

        shadowUserManager.setHeadlessSystemUserMode(true);
        shadowUserManager.addUser(1, "Name", UserInfo.FLAG_PROFILE);
        shadowUserManager.addUser(2, "Name", UserInfo.FLAG_FULL);
        shadowUserManager.addUser(3, "Name", UserInfo.FLAG_EPHEMERAL);

        mController = new ForceEnableNotesRolePreferenceController(context);
        mController.setEnabled(true);

        verify(sOverlayManager, never()).setEnabled(eq(OVERLAY_PACKAGE_NAME), eq(true), eq(0));
        verify(sOverlayManager).setEnabled(OVERLAY_PACKAGE_NAME, true, 1);
        verify(sOverlayManager).setEnabled(OVERLAY_PACKAGE_NAME, true, 2);
        verify(sOverlayManager, never()).setEnabled(eq(OVERLAY_PACKAGE_NAME), eq(true), eq(3));
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_NOTE_HSUM_DEV_OPTION_FIX)
    public void setEnabled_hsumDevices_updatesFullAndProfileAndSystemUsers()
            throws RemoteException {
        Context context = spy(RuntimeEnvironment.application.getApplicationContext());
        UserManager userManager = context.getSystemService(UserManager.class);
        ShadowUserManager shadowUserManager = Shadow.extract(userManager);

        shadowUserManager.setHeadlessSystemUserMode(true);
        shadowUserManager.addUser(1, "Name", UserInfo.FLAG_PROFILE);
        shadowUserManager.addUser(2, "Name", UserInfo.FLAG_FULL);
        shadowUserManager.addUser(3, "Name", UserInfo.FLAG_EPHEMERAL);

        mController = new ForceEnableNotesRolePreferenceController(context);
        mController.setEnabled(true);

        verify(sOverlayManager).setEnabled(OVERLAY_PACKAGE_NAME, true, 0);
        verify(sOverlayManager).setEnabled(OVERLAY_PACKAGE_NAME, true, 1);
        verify(sOverlayManager).setEnabled(OVERLAY_PACKAGE_NAME, true, 2);
        verify(sOverlayManager, never()).setEnabled(eq(OVERLAY_PACKAGE_NAME), eq(true), eq(3));
    }

    @Test
    public void updateState_enabled_preferenceShouldBeChecked() {
        mController.setEnabled(true);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_disabled_preferenceShouldBeUnchecked() {
        mController.setEnabled(false);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onPreferenceChange_checked_shouldBeEnabled() {
        mController.onPreferenceChange(mPreference, true);

        assertTrue(mController.isEnabled());
    }

    @Test
    public void onPreferenceChange_unchecked_shouldNotBeEnabled() {
        mController.onPreferenceChange(mPreference, false);

        assertFalse(mController.isEnabled());
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceShouldNotBeEnabled() {
        mController.onDeveloperOptionsSwitchDisabled();

        assertFalse(mController.isEnabled());
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }

    @Implements(IOverlayManager.Stub.class)
    public static class ShadowOverlayManagerStub {
        @Implementation
        public static IOverlayManager asInterface(IBinder iBinder) {
            return sOverlayManager;
        }
    }
}
