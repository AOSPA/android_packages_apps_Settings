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
package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.sharedconnectivity.app.NetworkProviderInfo;
import android.os.UserManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.flags.Flags;
import com.android.settingslib.R;
import com.android.settingslib.wifi.WifiUtils;
import com.android.wifitrackerlib.HotspotNetworkEntry;
import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class WifiEntryPreferenceTest {

    private Context mContext;

    @Mock
    private WifiEntry mMockWifiEntry;
    @Mock
    private WifiConfiguration mMockWifiConfig;
    @Mock
    private HotspotNetworkEntry mHotspotNetworkEntry;
    @Mock
    private WifiUtils.InternetIconInjector mMockIconInjector;
    @Mock
    private UserManager mMockUserManager;

    @Mock
    private Drawable mMockDrawable0;
    @Mock
    private Drawable mMockDrawable1;
    @Mock
    private Drawable mMockDrawable2;
    @Mock
    private Drawable mMockDrawable3;
    @Mock
    private Drawable mMockDrawable4;

    @Mock
    private Drawable mMockShowXDrawable0;
    @Mock
    private Drawable mMockShowXDrawable1;
    @Mock
    private Drawable mMockShowXDrawable2;
    @Mock
    private Drawable mMockShowXDrawable3;
    @Mock
    private Drawable mMockShowXDrawable4;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String MOCK_TITLE = "title";
    private static final String MOCK_SUMMARY = "summary";
    private static final String FAKE_URI_STRING = "fakeuri";
    private static final int LOCK_ICON_RES_ID =
            com.android.settings.R.drawable.ic_friction_lock_closed;
    private static final int SHARED_ICON_RES_ID =
            com.android.settings.R.drawable.ic_group_24dp;

    private WifiEntryPreference mPref;
    private View mView;
    private PreferenceViewHolder mHolder;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);

        MockitoAnnotations.initMocks(this);

        when(mContext.getSystemService(UserManager.class)).thenReturn(mMockUserManager);

        when(mMockWifiEntry.getTitle()).thenReturn(MOCK_TITLE);
        when(mMockWifiEntry.getSummary(false /* concise */)).thenReturn(MOCK_SUMMARY);
        when(mMockWifiEntry.getLevel()).thenReturn(0);
        when(mMockWifiEntry.shouldShowXLevelIcon()).thenReturn(false);
        when(mMockWifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_DISCONNECTED);

        when(mMockIconInjector.getIcon(false /* showX */, 0)).thenReturn(mMockDrawable0);
        when(mMockIconInjector.getIcon(false /* showX */, 1)).thenReturn(mMockDrawable1);
        when(mMockIconInjector.getIcon(false /* showX */, 2)).thenReturn(mMockDrawable2);
        when(mMockIconInjector.getIcon(false /* showX */, 3)).thenReturn(mMockDrawable3);
        when(mMockIconInjector.getIcon(false /* showX */, 4)).thenReturn(mMockDrawable4);

        when(mMockIconInjector.getIcon(true /* showX */, 0))
                .thenReturn(mMockShowXDrawable0);
        when(mMockIconInjector.getIcon(true /* showX */, 1))
                .thenReturn(mMockShowXDrawable1);
        when(mMockIconInjector.getIcon(true /* showX */, 2))
                .thenReturn(mMockShowXDrawable2);
        when(mMockIconInjector.getIcon(true /* showX */, 3))
                .thenReturn(mMockShowXDrawable3);
        when(mMockIconInjector.getIcon(true /* showX */, 4))
                .thenReturn(mMockShowXDrawable4);

        mPref = spy(new WifiEntryPreference(mContext, mMockWifiEntry, mMockIconInjector));
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        mView = inflater.inflate(mPref.getLayoutResource(),
                new LinearLayout(mContext), false);
        mHolder = PreferenceViewHolder.createInstanceForTests(mView);
    }

    @Test
    public void constructor_shouldSetWifiEntryTitleAndSummary() {
        final WifiEntryPreference pref =
                new WifiEntryPreference(mContext, mMockWifiEntry, mMockIconInjector);

        assertThat(pref.getTitle()).isEqualTo(MOCK_TITLE);
        assertThat(pref.getSummary()).isEqualTo(MOCK_SUMMARY);
    }

    @Test
    public void constructor_shouldSetIcon() {
        when(mMockWifiEntry.getLevel()).thenReturn(0);

        final WifiEntryPreference pref =
                new WifiEntryPreference(mContext, mMockWifiEntry, mMockIconInjector);

        assertThat(pref.getIcon()).isEqualTo(mMockDrawable0);
    }

    @Test
    public void setWifiEntry_connectedStateChanged_setIconAndSummary() {
        when(mMockWifiEntry.getLevel()).thenReturn(4);
        when(mMockWifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_CONNECTED);

        mPref.setWifiEntry(mMockWifiEntry);

        verify(mPref).setIcon(any());
        verify(mPref).setSummary(anyString());

        // Only the connection state changes.
        when(mMockWifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_DISCONNECTED);
        reset(mPref);

        mPref.setWifiEntry(mMockWifiEntry);

        // The icon and summary should be set in case.
        verify(mPref).setIcon(any());
        verify(mPref).setSummary(anyString());
    }

    @Test
    public void titleChanged_refresh_shouldUpdateTitle() {
        final String updatedTitle = "updated title";
        when(mMockWifiEntry.getTitle()).thenReturn(updatedTitle);

        mPref.refresh();

        assertThat(mPref.getTitle().toString()).isEqualTo(updatedTitle);
    }

    @Test
    public void summaryChanged_refresh_shouldUpdateSummary() {
        final String updatedSummary = "updated summary";
        when(mMockWifiEntry.getSummary(false /* concise */)).thenReturn(updatedSummary);

        mPref.refresh();

        assertThat(mPref.getSummary().toString()).isEqualTo(updatedSummary);
    }

    @Test
    public void levelChanged_refresh_shouldUpdateLevelIcon() {
        final List<Drawable> iconList = new ArrayList<>();

        when(mMockWifiEntry.getLevel()).thenReturn(0);
        mPref.refresh();
        iconList.add(mPref.getIcon());
        when(mMockWifiEntry.getLevel()).thenReturn(1);
        mPref.refresh();
        iconList.add(mPref.getIcon());
        when(mMockWifiEntry.getLevel()).thenReturn(2);
        mPref.refresh();
        iconList.add(mPref.getIcon());
        when(mMockWifiEntry.getLevel()).thenReturn(3);
        mPref.refresh();
        iconList.add(mPref.getIcon());
        when(mMockWifiEntry.getLevel()).thenReturn(4);
        mPref.refresh();
        iconList.add(mPref.getIcon());
        when(mMockWifiEntry.getLevel()).thenReturn(-1);
        mPref.refresh();
        iconList.add(mPref.getIcon());

        assertThat(iconList).containsExactly(mMockDrawable0, mMockDrawable1,
                mMockDrawable2, mMockDrawable3, mMockDrawable4, null);
    }

    @Test
    public void levelChanged_showXWifiRefresh_shouldUpdateLevelIcon() {
        final List<Drawable> iconList = new ArrayList<>();
        when(mMockWifiEntry.shouldShowXLevelIcon()).thenReturn(true);

        when(mMockWifiEntry.getLevel()).thenReturn(0);
        mPref.refresh();
        iconList.add(mPref.getIcon());
        when(mMockWifiEntry.getLevel()).thenReturn(1);
        mPref.refresh();
        iconList.add(mPref.getIcon());
        when(mMockWifiEntry.getLevel()).thenReturn(2);
        mPref.refresh();
        iconList.add(mPref.getIcon());
        when(mMockWifiEntry.getLevel()).thenReturn(3);
        mPref.refresh();
        iconList.add(mPref.getIcon());
        when(mMockWifiEntry.getLevel()).thenReturn(4);
        mPref.refresh();
        iconList.add(mPref.getIcon());
        when(mMockWifiEntry.getLevel()).thenReturn(-1);
        mPref.refresh();
        iconList.add(mPref.getIcon());

        assertThat(iconList).containsExactly(mMockShowXDrawable0, mMockShowXDrawable1,
                mMockShowXDrawable2, mMockShowXDrawable3, mMockShowXDrawable4, null);
    }

    @Test
    public void notNull_whenGetHelpUriString_shouldSetImageButtonVisible() {
        when(mMockWifiEntry.getHelpUriString()).thenReturn(FAKE_URI_STRING);

        mPref.onBindViewHolder(mHolder);

        assertThat(mView.findViewById(
                R.id.icon_button).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void helpButton_whenGetHelpUriStringNotNull_shouldSetCorrectContentDescription() {
        when(mMockWifiEntry.getHelpUriString()).thenReturn(FAKE_URI_STRING);

        mPref.onBindViewHolder(mHolder);

        assertThat(mView.findViewById(
                R.id.icon_button).getContentDescription()).isEqualTo(
                mContext.getString(R.string.help_label));
    }

    @Test
    public void subscriptionEntry_shouldSetImageButtonGone() {
        when(mMockWifiEntry.isSubscription()).thenReturn(true);
        mPref.onBindViewHolder(mHolder);

        assertThat(mView.findViewById(
                R.id.icon_button).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void pskNetwork_shouldSetLockIcon() {
        mMockWifiConfig.shared = true;
        when(mMockWifiEntry.getWifiConfiguration()).thenReturn(mMockWifiConfig);
        when(mMockWifiEntry.getHelpUriString()).thenReturn(null);
        when(mMockWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_PSK);
        LinearLayout endIcons = new LinearLayout(mContext);

        mPref.updateEndIcons(endIcons);

        assertThat(endIcons.getChildCount()).isEqualTo(1);
        final ImageView endIcon = (ImageView) endIcons.getChildAt(0);
        assertThat(Shadows.shadowOf(
                endIcon.getDrawable()).getCreatedFromResId()).isEqualTo(
                LOCK_ICON_RES_ID);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void sharedNetwork_shouldSetSharedIcon() {
        mMockWifiConfig.shared = true;
        when(mMockWifiEntry.getWifiConfiguration()).thenReturn(mMockWifiConfig);
        when(mMockWifiEntry.getHelpUriString()).thenReturn(null);
        when(mMockUserManager.getUserCount()).thenReturn(3);
        LinearLayout endIcons = new LinearLayout(mContext);

        mPref.updateEndIcons(endIcons);

        assertThat(endIcons.getChildCount()).isEqualTo(1);
        final ImageView endIcon = (ImageView) endIcons.getChildAt(0);
        assertThat(Shadows.shadowOf(
                endIcon.getDrawable()).getCreatedFromResId()).isEqualTo(
                SHARED_ICON_RES_ID);
        assertThat(endIcon.getTooltipText()).isNull();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void sharedPskNetwork_shouldSetSharedIconAndLockIcon() {
        mMockWifiConfig.shared = true;
        when(mMockWifiEntry.getWifiConfiguration()).thenReturn(mMockWifiConfig);
        when(mMockWifiEntry.getHelpUriString()).thenReturn(null);
        when(mMockWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_PSK);
        when(mMockUserManager.getUserCount()).thenReturn(3);
        LinearLayout endIcons = new LinearLayout(mContext);

        mPref.updateEndIcons(endIcons);

        assertThat(endIcons.getChildCount()).isEqualTo(2);
        final ImageView firstEndIcon = (ImageView) endIcons.getChildAt(0);
        final ImageView secondEndIcon = (ImageView) endIcons.getChildAt(1);
        assertThat(Shadows.shadowOf(
                firstEndIcon.getDrawable()).getCreatedFromResId()).isEqualTo(
                SHARED_ICON_RES_ID);
        assertThat(Shadows.shadowOf(
                secondEndIcon.getDrawable()).getCreatedFromResId()).isEqualTo(
                LOCK_ICON_RES_ID);
        assertThat(firstEndIcon.getTooltipText()).isNull();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void sharedNetwork_singleUser_shouldNotSetSharedIcon() {
        mMockWifiConfig.shared = true;
        when(mMockWifiEntry.getWifiConfiguration()).thenReturn(mMockWifiConfig);
        when(mMockWifiEntry.getHelpUriString()).thenReturn(null);
        when(mMockUserManager.getUserCount()).thenReturn(1);
        LinearLayout endIcons = new LinearLayout(mContext);

        mPref.updateEndIcons(endIcons);

        assertThat(endIcons.getChildCount()).isEqualTo(0);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WIFI_MULTIUSER)
    public void sharedNetwork_notShared_shouldNotSetSharedIcon() {
        mMockWifiConfig.shared = false;
        when(mMockWifiEntry.getWifiConfiguration()).thenReturn(mMockWifiConfig);
        when(mMockWifiEntry.getHelpUriString()).thenReturn(null);
        when(mMockUserManager.getUserCount()).thenReturn(3);
        LinearLayout endIcons = new LinearLayout(mContext);

        mPref.updateEndIcons(endIcons);

        assertThat(endIcons.getChildCount()).isEqualTo(0);
    }

    @Test
    public void updateIcon_shouldSetTintListForDrawable() {
        reset(mMockDrawable4);

        mPref.updateIcon(false /* showX */, 4 /* level */);

        verify(mMockDrawable4).setTintList(any());
    }

    @Test
    public void getSecondTargetResId_shouldNotReturnZero() {
        assertThat(mPref.getSecondTargetResId()).isNotEqualTo(0);
    }

    @Test
    public void refresh_itsHotspotNetworkEntry_shouldUpdateHotspotIcon() {
        int deviceType = NetworkProviderInfo.DEVICE_TYPE_PHONE;
        when(mHotspotNetworkEntry.getDeviceType()).thenReturn(deviceType);
        WifiEntryPreference pref = spy(
                new WifiEntryPreference(mContext, mHotspotNetworkEntry, mMockIconInjector));

        pref.refresh();

        verify(pref).updateHotspotIcon(deviceType);
    }

    @Test
    public void refresh_notHotspotNetworkEntry_shouldNotUpdateHotspotIcon() {
        WifiEntryPreference pref = spy(
                new WifiEntryPreference(mContext, mMockWifiEntry, mMockIconInjector));

        pref.refresh();

        verify(pref, never()).updateHotspotIcon(anyInt());
    }
}
