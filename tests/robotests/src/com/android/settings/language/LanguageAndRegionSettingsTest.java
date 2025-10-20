/**
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.language;

import static com.google.common.truth.Truth.assertThat;

import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_CONFIRM_SYSTEM_DEFAULT;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_NOT_AVAILABLE_LOCALE;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_REMOVE_LOCALE;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.LocaleList;

import android.app.IActivityManager;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.localepicker.LocaleDialogFragment;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowActivityManager;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowAlertDialogCompat.class,
        ShadowActivityManager.class,
        ShadowFragment.class,
})
public class LanguageAndRegionSettingsTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String ARG_DIALOG_TYPE = "arg_dialog_type";
    private static final String
            ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED = "arg_show_dialog_for_not_translated";
    private static final String TAG_DIALOG_NOT_AVAILABLE = "dialog_not_available_locale";

    private FragmentActivity mActivity;
    private LanguageAndRegionSettings mFragment;
    private Context mContext;
    private Intent mIntent = new Intent();
    private List<LocaleStore.LocaleInfo> mLocaleInfoList;
    private LocaleList mLocaleList;
    private LocaleStore.LocaleInfo mEnLocale;
    private LocaleStore.LocaleInfo mFrLocale;

    @Mock
    private UserPreferredLocalePreferenceController mController;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private IActivityManager mActivityService;
    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;
    @Mock
    private LanguageAndRegionViewModel mViewModel;
    @Mock
    private ViewModelProvider mViewModelProvider;
    @Captor
    private ArgumentCaptor<LocaleDialogFragment> mDialogCaptor;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        mContext = spy(context);
        mFragment = spy(new LanguageAndRegionSettings());
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.isAdded()).thenReturn(true);
        mActivity = spy(Robolectric.buildActivity(FragmentActivity.class).get());
        when(mFragment.getActivity()).thenReturn(mActivity);

        // Mock ViewModel
        when(mViewModelProvider.get(LanguageAndRegionViewModel.class)).thenReturn(mViewModel);
        ReflectionHelpers.setField(mFragment, "mViewModel", mViewModel);

        ReflectionHelpers.setField(mFragment, "mRestrictionsManager",
                context.getSystemService(Context.RESTRICTIONS_SERVICE));
        ReflectionHelpers.setField(mFragment, "mUserManager",
                context.getSystemService(Context.USER_SERVICE));
        ReflectionHelpers.setField(mFragment, "mFragmentManager", mFragmentManager);
        ReflectionHelpers.setField(mFragment, "mMetricsFeatureProvider", mMetricsFeatureProvider);
        ReflectionHelpers.setField(mFragment, "mUserPreferredLocalePreferenceController",
                mController);
        doReturn(mFragmentManager).when(mFragment).getChildFragmentManager();
        when(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);
        ShadowActivityManager.setService(mActivityService);
        final Configuration config = new Configuration();
        setUpLocaleConditions();
        config.setLocales(mLocaleList);
        when(mActivityService.getConfiguration()).thenReturn(config);
        FakeFeatureFactory.setupForTest();
    }

    @After
    public void tearDown() throws Exception {
        ShadowAlertDialogCompat.reset();
    }

    private void setUpLocaleConditions() {
        mEnLocale = LocaleStore.getLocaleInfo(Locale.forLanguageTag("en-US"));
        mFrLocale = LocaleStore.getLocaleInfo(Locale.forLanguageTag("fr-FR"));
        LocaleStore.LocaleInfo akLocale = LocaleStore.getLocaleInfo(
                Locale.forLanguageTag("ak-GH"));
        LocaleStore.LocaleInfo esLocale = LocaleStore.getLocaleInfo(
                Locale.forLanguageTag("es-US"));

        mLocaleInfoList = new ArrayList<>(List.of(mEnLocale, akLocale, esLocale));
        final Locale[] newList = mLocaleInfoList.stream()
                .map(LocaleStore.LocaleInfo::getLocale)
                .toArray(Locale[]::new);
        mLocaleList = new LocaleList(newList);
    }

    @Test
    public void onActivityResult_ResultCodeIsOk_showNotAvailableDialog() {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_DIALOG_TYPE, DIALOG_CONFIRM_SYSTEM_DEFAULT);
        bundle.putInt(LocaleDialogFragment.ARG_MENU_ITEM_ID, R.id.move_down);
        bundle.putSerializable(LocaleDialogFragment.ARG_SELECTED_LOCALE, mEnLocale);
        mIntent.putExtras(bundle);
        mIntent.putExtra(ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED, true);
        setUpLocaleConditions();
        mFragment.onActivityResult(DIALOG_CONFIRM_SYSTEM_DEFAULT, Activity.RESULT_OK, mIntent);

        verify(mFragmentTransaction).add(any(LocaleDialogFragment.class),
                eq(TAG_DIALOG_NOT_AVAILABLE));
    }

    @Test
    public void onActivityResult_ResultCodeIsOk_removeDialog_updatePreference() {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_DIALOG_TYPE, DIALOG_REMOVE_LOCALE);
        bundle.putInt(LocaleDialogFragment.ARG_MENU_ITEM_ID, R.id.move_down);
        bundle.putSerializable(LocaleDialogFragment.ARG_SELECTED_LOCALE, mEnLocale);
        mIntent.putExtras(bundle);
        mIntent.putExtra(ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED, false);
        setUpLocaleConditions();
        mFragment.onActivityResult(DIALOG_REMOVE_LOCALE, Activity.RESULT_OK, mIntent);

        verify(mController).doTheUpdate();
        verify(mController).updatePreferences();
    }

    @Test
    public void onActivityResult_ResultCodeIsOk_notAvailableDialog_updatePreference() {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_DIALOG_TYPE, DIALOG_NOT_AVAILABLE_LOCALE);
        bundle.putInt(LocaleDialogFragment.ARG_MENU_ITEM_ID, R.id.move_down);
        bundle.putSerializable(LocaleDialogFragment.ARG_SELECTED_LOCALE,
                LocaleStore.getLocaleInfo(Locale.forLanguageTag("ak-GH")));
        mIntent.putExtras(bundle);
        mIntent.putExtra(ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED, false);
        setUpLocaleConditions();
        mFragment.onActivityResult(DIALOG_NOT_AVAILABLE_LOCALE, Activity.RESULT_OK, mIntent);

        verify(mController).doTheUpdate();
        verify(mController).updatePreferences();
    }

    @Test
    public void showConfirmDialogByType_removeLocale_showsCorrectDialog() {
        when(mViewModel.getDefaultAfterChange()).thenReturn(mFrLocale);
        when(mFragmentTransaction.add(any(LocaleDialogFragment.class), any()))
                .thenReturn(mFragmentTransaction);

        mFragment.showConfirmDialogByType(DIALOG_REMOVE_LOCALE);

        verify(mFragmentTransaction).add(mDialogCaptor.capture(), eq("dialog_remove_locale"));
        LocaleDialogFragment dialog = mDialogCaptor.getValue();
        Bundle args = dialog.getArguments();
        LocaleDialogFragment.LocaleDialogController controller =
                dialog.getLocaleDialogController(mContext, dialog, mFragment);

        LocaleDialogFragment.LocaleDialogController.DialogContent content =
                controller.getDialogContent();
        String expectedTitle = mContext.getString(R.string.dlg_title_delete_preferred_locale,
                mFrLocale.getFullNameNative());
        String expectedMessage = mContext.getString(R.string.dlg_desc_delete_preferred_locale,
                mFrLocale.getFullNameNative());

        assertThat(args.getInt(LocaleDialogFragment.ARG_DIALOG_TYPE))
                .isEqualTo(DIALOG_REMOVE_LOCALE);
        assertThat(args.getSerializable(LocaleDialogFragment.ARG_TARGET_LOCALE))
                .isEqualTo(mFrLocale);
        assertThat(content.mTitle).isEqualTo(expectedTitle);
        assertThat(content.mMessage).isEqualTo(expectedMessage);

    }

    @Test
    public void showConfirmDialogByType_removeAndChangeSystem_showsCorrectDialog() {
        when(mViewModel.getDefaultAfterChange()).thenReturn(mFrLocale); // New default
        when(mViewModel.getSelectedLocaleInfo()).thenReturn(mEnLocale); // Locale to remove
        when(mFragmentTransaction.add(any(LocaleDialogFragment.class), any()))
                .thenReturn(mFragmentTransaction);

        mFragment.showConfirmDialogByType(DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE);

        verify(mFragmentTransaction).add(mDialogCaptor.capture(),
                eq("dialog_remove_and_change_locale"));
        LocaleDialogFragment dialog = mDialogCaptor.getValue();
        Bundle args = dialog.getArguments();
        LocaleDialogFragment.LocaleDialogController.DialogContent content =
                dialog.getLocaleDialogController(mContext, dialog, mFragment).getDialogContent();
        String expectedTitle = mContext.getString(R.string.title_change_system_locale,
                mFrLocale.getFullNameNative());
        String expectedMessage = mContext.getString(R.string.dlg_desc_delete_preferred_locale,
                mEnLocale.getFullNameNative());

        assertThat(args.getInt(LocaleDialogFragment.ARG_DIALOG_TYPE))
                .isEqualTo(DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE);
        assertThat(args.getSerializable(LocaleDialogFragment.ARG_TARGET_LOCALE))
                .isEqualTo(mFrLocale);
        assertThat(args.getSerializable(LocaleDialogFragment.ARG_SELECTED_LOCALE))
                .isEqualTo(mEnLocale);
        assertThat(content.mTitle).isEqualTo(expectedTitle);
        assertThat(content.mMessage).isEqualTo(expectedMessage);
    }

    @Test
    public void showConfirmDialogByType_confirmSystemDefault_showsCorrectDialog() {
        when(mViewModel.getDefaultAfterChange()).thenReturn(mFrLocale); // New default
        when(mViewModel.getSelectedLocaleInfo()).thenReturn(mFrLocale); // Locale that was moved
        when(mFragmentTransaction.add(any(LocaleDialogFragment.class), any()))
                .thenReturn(mFragmentTransaction);

        mFragment.showConfirmDialogByType(DIALOG_CONFIRM_SYSTEM_DEFAULT);

        verify(mFragmentTransaction).add(mDialogCaptor.capture(),
                eq("dialog_confirm_system_default"));
        LocaleDialogFragment dialog = mDialogCaptor.getValue();
        Bundle args = dialog.getArguments();
        LocaleDialogFragment.LocaleDialogController.DialogContent content =
                dialog.getLocaleDialogController(mContext, dialog, mFragment).getDialogContent();
        String expectedTitle = mContext.getString(R.string.title_change_system_locale,
                mFrLocale.getFullNameNative());
        String expectedMessage = mContext.getString(
                R.string.desc_notice_device_locale_and_region_settings_change,
                mFrLocale.getLocale().getDisplayLanguage(),
                mFrLocale.getLocale().getDisplayCountry());

        assertThat(args.getInt(LocaleDialogFragment.ARG_DIALOG_TYPE))
                .isEqualTo(DIALOG_CONFIRM_SYSTEM_DEFAULT);
        assertThat(args.getSerializable(LocaleDialogFragment.ARG_TARGET_LOCALE))
                .isEqualTo(mFrLocale);
        assertThat(args.getSerializable(LocaleDialogFragment.ARG_SELECTED_LOCALE))
                .isEqualTo(mFrLocale);
        assertThat(content.mTitle).isEqualTo(expectedTitle);
        assertThat(content.mMessage).isEqualTo(expectedMessage);
    }
}
