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

package com.android.settings.notification;

import static android.provider.Settings.Secure.NOTIFICATION_BUNDLES_ALWAYS_EXPAND;
import static android.service.notification.Adjustment.KEY_SENSITIVE_CONTENT;
import static android.service.notification.Adjustment.KEY_TYPE;
import static android.service.notification.Adjustment.TYPE_CONTENT_RECOMMENDATION;
import static android.service.notification.Adjustment.TYPE_NEWS;
import static android.service.notification.Adjustment.TYPE_PROMOTION;
import static android.service.notification.Adjustment.TYPE_SOCIAL_MEDIA;

import static com.android.settings.notification.BundleCombinedPreferenceController.ALWAYS_EXPAND_KEY;
import static com.android.settings.notification.BundleCombinedPreferenceController.ALWAYS_EXPAND_VALUE;
import static com.android.settings.notification.BundleCombinedPreferenceController.AUTO_EXPAND_KEY;
import static com.android.settings.notification.BundleCombinedPreferenceController.AUTO_EXPAND_VALUE;
import static com.android.settings.notification.BundleCombinedPreferenceController.EXCLUDED_APPS_CATEGORY_KEY;
import static com.android.settings.notification.BundleCombinedPreferenceController.GLOBAL_KEY;
import static com.android.settings.notification.BundleCombinedPreferenceController.NEVER_EXPAND_KEY;
import static com.android.settings.notification.BundleCombinedPreferenceController.NEVER_EXPAND_VALUE;
import static com.android.settings.notification.BundleCombinedPreferenceController.TYPE_CATEGORY_KEY;
import static com.android.settings.notification.BundleCombinedPreferenceController.WORK_PREF_KEY;
import static com.android.settings.notification.BundlePreferenceFragment.BUNDLE_CATEGORY_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.app.INotificationManager;
import android.content.Context;
import android.os.RemoteException;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.service.notification.DynamicBundle;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BundleCombinedPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private BundleCombinedPreferenceController mController;

    @Mock
    private INotificationManager mNotificationManager;

    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;
    private PreferenceCategory mPrefCategory;
    private PreferenceCategory mTypesPrefCategory;
    private MainSwitchPreference mGlobalSwitch;
    private SwitchPreferenceCompat mWorkSwitch;
    private SelectorWithWidgetPreference mExpandAlwaysPref;
    private SelectorWithWidgetPreference mExpandAutoPref;
    private SelectorWithWidgetPreference mExpandNeverPref;
    private PreferenceCategory mExcludedAppsPrefCategory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.getApplication();

        NotificationBackend nb = new NotificationBackend();
        nb.setNm(mNotificationManager);

        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.inflateFromResource(
                mContext, R.xml.bundle_notifications_settings, mPreferenceScreen);
        mPrefCategory = mPreferenceScreen.findPreference(BUNDLE_CATEGORY_KEY);
        mController = new BundleCombinedPreferenceController(
                mContext, BUNDLE_CATEGORY_KEY, new NotificationBackend());

        // preference category/controller initiation
        mGlobalSwitch = mPreferenceScreen.findPreference(GLOBAL_KEY);
        mWorkSwitch = mPreferenceScreen.findPreference(WORK_PREF_KEY);
        mExpandAlwaysPref = mPreferenceScreen.findPreference(ALWAYS_EXPAND_KEY);
        mExpandAutoPref = mPreferenceScreen.findPreference(AUTO_EXPAND_KEY);
        mExpandNeverPref = mPreferenceScreen.findPreference(NEVER_EXPAND_KEY);
        mTypesPrefCategory = mPreferenceScreen.findPreference(TYPE_CATEGORY_KEY);
        mExcludedAppsPrefCategory = mPreferenceScreen.findPreference(EXCLUDED_APPS_CATEGORY_KEY);

        mController.updateState(mPrefCategory);
    }

    private TwoStatePreference getTypePreference(int type) {
        return mTypesPrefCategory.findPreference(String.valueOf(type));
    }

    @Test
    public void isAvailable_flagEnabledNasSupports_shouldReturnTrue() throws RemoteException {
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_TYPE));
        when(mNotificationManager.getUnsupportedAdjustmentTypes()).thenReturn(new ArrayList<>());
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_flagEnabledNasDoesNotSupport_shouldReturnFalse()
            throws RemoteException {
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_TYPE));
        when(mNotificationManager.getUnsupportedAdjustmentTypes()).thenReturn(List.of(KEY_TYPE));
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updatePrefValues_reflectsSettings() throws RemoteException {
        // bundling is enabled globally
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_TYPE));

        // allowed key types are promos & news
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(
                new int[] {TYPE_PROMOTION, TYPE_NEWS});

        mController.updateState(mPrefCategory);
        assertThat(mGlobalSwitch.isChecked()).isTrue();
        assertThat(getTypePreference(TYPE_PROMOTION).isChecked()).isTrue();
        assertThat(getTypePreference(TYPE_NEWS).isChecked()).isTrue();
        assertThat(getTypePreference(TYPE_SOCIAL_MEDIA).isChecked()).isFalse();
        assertThat(getTypePreference(TYPE_CONTENT_RECOMMENDATION).isChecked()).isFalse();
    }

    @Test
    public void updateState_noManagedProfile_workSwitchNotVisible() throws RemoteException {
        // bundling is enabled globally with non-zero types
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_TYPE));
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(
                new int[] {TYPE_SOCIAL_MEDIA});
        mController.setManagedProfile(null);

        mController.updateState(mPrefCategory);
        assertThat(mWorkSwitch.isVisible()).isFalse();
    }

    @Test
    public void updateState_hasManagedProfile_reflectsSettings() throws RemoteException {
        // bundling is enabled globally with non-zero types
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_TYPE));
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(
                new int[] {TYPE_SOCIAL_MEDIA});

        // set up a work profile with userId 12345, with bundling enabled
        mController.setManagedProfile(new UserHandle(12345));
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(12345))
                .thenReturn(List.of(KEY_TYPE));

        // need to re-update state and not just pref values so that the work pref is actually set
        // up correctly
        mController.updateState(mPrefCategory);
        assertThat(mWorkSwitch.isChecked()).isTrue();

        // now disabled
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(12345))
                .thenReturn(List.of(KEY_SENSITIVE_CONTENT));
        mController.updatePrefValues();
        assertThat(mWorkSwitch.isChecked()).isFalse();
    }

    @Test
    public void updatePrefValues_otherPrefsGoneWhenGlobalOff() throws RemoteException {
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_SENSITIVE_CONTENT));
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(
                new int[] {TYPE_PROMOTION, TYPE_NEWS});

        mController.updateState(mPrefCategory);
        assertThat(mGlobalSwitch.isChecked()).isFalse();
        assertThat(mWorkSwitch.isVisible()).isFalse();
        assertThat(mTypesPrefCategory.isVisible()).isFalse();
        assertThat(mExpandAlwaysPref.isVisible()).isFalse();
        assertThat(mExpandAutoPref.isVisible()).isFalse();
        assertThat(mExpandNeverPref.isVisible()).isFalse();
    }

    @Test
    public void turnOffGlobalSwitch_updatesBackendAndOtherSwitches() throws RemoteException {
        // Initial state: global allowed + some types set. Work profile exists
        mController.setManagedProfile(new UserHandle(12345));
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_TYPE));
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(
                new int[] {TYPE_PROMOTION, TYPE_NEWS});
        mController.updateState(mPrefCategory);

        // Simulate the global switch turning off. This also requires telling the mock backend to
        // start returning false before the click listener updates pref values
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(new ArrayList<>());
        mGlobalSwitch.getOnPreferenceChangeListener().onPreferenceChange(mGlobalSwitch, false);
        verify(mNotificationManager, times(1))
                .disallowAssistantAdjustment(mContext.getUserId(), KEY_TYPE);

        // All individual type checkboxes should now not be visible.
        assertThat(mWorkSwitch.isVisible()).isFalse();
        assertThat(mTypesPrefCategory.isVisible()).isFalse();
        assertThat(mExcludedAppsPrefCategory.isVisible()).isFalse();
        assertThat(mExpandAlwaysPref.isVisible()).isFalse();
        assertThat(mExpandAutoPref.isVisible()).isFalse();
        assertThat(mExpandNeverPref.isVisible()).isFalse();
    }

    @Test
    public void turnOnGlobalSwitch_updatesBackendAndTypeSwitches() throws RemoteException {
        mController.setManagedProfile(new UserHandle(12345));
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_SENSITIVE_CONTENT));
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(
                new int[] {TYPE_PROMOTION, TYPE_NEWS});
        mController.updateState(mPrefCategory);

        // simulate globally enabled but work profile setting still disabled
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_SENSITIVE_CONTENT, KEY_TYPE));
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(12345))
                .thenReturn(List.of(KEY_SENSITIVE_CONTENT));
        mGlobalSwitch.getOnPreferenceChangeListener().onPreferenceChange(mGlobalSwitch, true);
        verify(mNotificationManager, times(1))
                .allowAssistantAdjustment(mContext.getUserId(), KEY_TYPE);

        // type checkboxes should now exist & be checked accordingly to their state
        assertThat(mWorkSwitch.isChecked()).isFalse();
        assertThat(mTypesPrefCategory.isVisible()).isTrue();
        assertThat(getTypePreference(TYPE_PROMOTION).isChecked()).isTrue();
        assertThat(getTypePreference(TYPE_NEWS).isChecked()).isTrue();
        assertThat(getTypePreference(TYPE_SOCIAL_MEDIA).isChecked()).isFalse();
        assertThat(getTypePreference(TYPE_CONTENT_RECOMMENDATION).isChecked()).isFalse();
        assertThat(mExcludedAppsPrefCategory.isVisible()).isTrue();
        assertThat(mExpandAlwaysPref.isVisible()).isTrue();
        assertThat(mExpandAutoPref.isVisible()).isTrue();
        assertThat(mExpandNeverPref.isVisible()).isTrue();
    }

    @Test
    public void toggleWorkSwitch_updatesBackend() throws RemoteException {
        // default setting: bundling is enabled, with some type enabled as well
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_TYPE));
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(
                new int[] {TYPE_SOCIAL_MEDIA});

        // work profile exists, user id 12345
        // re-update state to make sure the work profile switch is actually set up
        mController.setManagedProfile(new UserHandle(12345));
        mController.updateState(mPrefCategory);

        mWorkSwitch.getOnPreferenceChangeListener().onPreferenceChange(mWorkSwitch, true);
        verify(mNotificationManager, times(1)).allowAssistantAdjustment(12345, KEY_TYPE);

        // no changes should be made to the main user switch
        verify(mNotificationManager, never())
                .allowAssistantAdjustment(mContext.getUserId(), KEY_TYPE);
    }

    @Test
    public void turnOnTypeBundle_updatesBackend_doesNotChangeGlobalSwitch() throws RemoteException {
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_TYPE));
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(
                new int[] {TYPE_SOCIAL_MEDIA});
        mController.updateState(mPrefCategory);

        TwoStatePreference p = getTypePreference(TYPE_CONTENT_RECOMMENDATION);
        p.getOnPreferenceChangeListener().onPreferenceChange(p, true);

        // recs bundle setting should be updated in the backend, and global switch unchanged
        verify(mNotificationManager).setAssistantClassificationTypeState(
                TYPE_CONTENT_RECOMMENDATION, true);

        verify(mNotificationManager, never()).allowAssistantAdjustment(anyInt(), anyString());
        verify(mNotificationManager, never()).disallowAssistantAdjustment(anyInt(), anyString());
    }

    @Test
    public void turnOffTypeBundle_lastOneChangesGlobalSwitch() throws RemoteException {
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_TYPE));
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(
                new int[] {TYPE_SOCIAL_MEDIA, TYPE_CONTENT_RECOMMENDATION});
        mController.updateState(mPrefCategory);

        // Turning off one should update state, but not turn off the global setting
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(
                new int[] {TYPE_SOCIAL_MEDIA});
        TwoStatePreference p = getTypePreference(TYPE_CONTENT_RECOMMENDATION);
        p.getOnPreferenceChangeListener().onPreferenceChange(p, false);
        verify(mNotificationManager).setAssistantClassificationTypeState(
                TYPE_CONTENT_RECOMMENDATION, false);

        // Now turn off the second
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(new int[] {});
        TwoStatePreference p2 = getTypePreference(TYPE_SOCIAL_MEDIA);
        p2.getOnPreferenceChangeListener().onPreferenceChange(p2, false);
        verify(mNotificationManager).setAssistantClassificationTypeState(TYPE_SOCIAL_MEDIA, false);

        // This update should trigger a call to turn off the global switch
        verify(mNotificationManager).disallowAssistantAdjustment(mContext.getUserId(), KEY_TYPE);
    }

    @Test
    public void expansionSettings_reflectsSettings() throws RemoteException {
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_TYPE));
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(
                new int[] {TYPE_SOCIAL_MEDIA});
        mController.updateState(mPrefCategory);
        assertThat(mExpandAlwaysPref.isVisible()).isTrue();
        assertThat(mExpandAutoPref.isVisible()).isTrue();
        assertThat(mExpandNeverPref.isVisible()).isTrue();

        // Always expand setting is true
        Settings.Secure.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUNDLES_ALWAYS_EXPAND,
                ALWAYS_EXPAND_VALUE);
        mController.updatePrefValues();
        assertThat(mExpandAlwaysPref.isChecked()).isTrue();
        assertThat(mExpandAutoPref.isChecked()).isFalse();
        assertThat(mExpandNeverPref.isChecked()).isFalse();

        // Auto expand is true
        Settings.Secure.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUNDLES_ALWAYS_EXPAND,
                AUTO_EXPAND_VALUE);
        mController.updatePrefValues();
        assertThat(mExpandAutoPref.isChecked()).isTrue();
        assertThat(mExpandAlwaysPref.isChecked()).isFalse();
        assertThat(mExpandNeverPref.isChecked()).isFalse();

        // Never expand is true
        Settings.Secure.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUNDLES_ALWAYS_EXPAND,
                NEVER_EXPAND_VALUE);
        mController.updatePrefValues();
        assertThat(mExpandNeverPref.isChecked()).isTrue();
        assertThat(mExpandAlwaysPref.isChecked()).isFalse();
        assertThat(mExpandAutoPref.isChecked()).isFalse();
    }

    @Test
    public void toggleExpansionSettings_updatesSetting() throws RemoteException {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUNDLES_ALWAYS_EXPAND,
                AUTO_EXPAND_VALUE);
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_TYPE));
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(
                new int[] {TYPE_SOCIAL_MEDIA, TYPE_CONTENT_RECOMMENDATION});
        mController.updateState(mPrefCategory);

        // Toggle always
        mExpandAlwaysPref.onClick();
        assertThat(mExpandAlwaysPref.isChecked()).isTrue();
        assertThat(mExpandAutoPref.isChecked()).isFalse();
        assertThat(mExpandNeverPref.isChecked()).isFalse();
        // Check that the secure setting was updated
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUNDLES_ALWAYS_EXPAND, AUTO_EXPAND_VALUE))
                .isEqualTo(ALWAYS_EXPAND_VALUE);

        // Toggle never
        mExpandNeverPref.onClick();
        // Check that the secure setting was updated
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUNDLES_ALWAYS_EXPAND, AUTO_EXPAND_VALUE))
                .isEqualTo(NEVER_EXPAND_VALUE);
        assertThat(mExpandNeverPref.isChecked()).isTrue();
        assertThat(mExpandAlwaysPref.isChecked()).isFalse();
        assertThat(mExpandAutoPref.isChecked()).isFalse();

        // Toggle auto
        mExpandAutoPref.onClick();
        // Check that the secure setting was updated
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUNDLES_ALWAYS_EXPAND, AUTO_EXPAND_VALUE))
                .isEqualTo(AUTO_EXPAND_VALUE);
        assertThat(mExpandAutoPref.isChecked()).isTrue();
        assertThat(mExpandAlwaysPref.isChecked()).isFalse();
        assertThat(mExpandNeverPref.isChecked()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_NM_CONTEXTUAL_DISPLAY)
    public void displayDynamicBundles() throws RemoteException {
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_TYPE));
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(
                new int[] {TYPE_SOCIAL_MEDIA, TYPE_CONTENT_RECOMMENDATION, 100});
        when(mNotificationManager.getDynamicBundles(null, mContext.getUser())).thenReturn(
                List.of(new DynamicBundle(100, "work notifications"),
                        new DynamicBundle(101, "group chats")));

        mController.updateState(mPrefCategory);

        assertThat(getTypePreference(TYPE_CONTENT_RECOMMENDATION).isVisible()).isTrue();
        assertThat(getTypePreference(TYPE_CONTENT_RECOMMENDATION).isChecked()).isTrue();
        assertThat(getTypePreference(TYPE_SOCIAL_MEDIA).isVisible()).isTrue();
        assertThat(getTypePreference(TYPE_SOCIAL_MEDIA).isChecked()).isTrue();
        assertThat(getTypePreference(100).isVisible()).isTrue();
        assertThat(getTypePreference(100).isChecked()).isTrue();
        assertThat(getTypePreference(101).isVisible()).isTrue();
        assertThat(getTypePreference(101).isChecked()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_NM_CONTEXTUAL_DISPLAY)
    public void toggleDynamicBundles() throws RemoteException {
        when(mNotificationManager.getAllowedAssistantAdjustmentsForUser(anyInt()))
                .thenReturn(List.of(KEY_TYPE));
        when(mNotificationManager.getAllowedClassificationTypes()).thenReturn(
                new int[] {TYPE_SOCIAL_MEDIA, TYPE_CONTENT_RECOMMENDATION, 100});
        when(mNotificationManager.getDynamicBundles(null, mContext.getUser())).thenReturn(
                List.of(new DynamicBundle(100, "work notifications"),
                        new DynamicBundle(101, "group chats")));

        mController.updateState(mPrefCategory);


        getTypePreference(100).performClick();
        assertThat(getTypePreference(100).isChecked()).isFalse();
        verify(mNotificationManager).setAssistantClassificationTypeState(100, false);

        getTypePreference(101).performClick();
        assertThat(getTypePreference(101).isChecked()).isTrue();
        verify(mNotificationManager).setAssistantClassificationTypeState(101, true);
    }
}
