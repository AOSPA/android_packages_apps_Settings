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

import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_CONFIRM_SYSTEM_DEFAULT;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_REMOVE_LOCALE;
import static com.android.settings.localepicker.LocaleUtils.getUserLocaleList;
import static com.android.settings.localepicker.LocaleUtils.mayAppendUnicodeTags;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.localepicker.LocaleDialogFragment;
import com.android.settings.localepicker.NotificationController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.R;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.OrderMenuPreference;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

public class UserPreferredLocalePreferenceController extends BasePreferenceController {
    private static final String TAG = "UserPreferredLocalePreferenceController";
    private static final String KEY_PREFERENCE_CATEGORY_LANGUAGES =
            "languages_category";
    private static final String KEY_PREFERENCE_USER_PREFERRED_LOCALE_LIST =
            "user_preferred_locale_list";
    private static final String TAG_DIALOG_CONFIRM_SYSTEM_DEFAULT = "dialog_confirm_system_default";
    private static final String TAG_DIALOG_REMOVE_LOCALE = "dialog_remove_locale";
    private static final String TAG_DIALOG_NOT_AVAILABLE = "dialog_not_available_locale";

    private PreferenceCategory mPreferenceCategory;
    private Map<String, OrderMenuPreference> mPreferences;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private FragmentManager mFragmentManager;
    private List<LocaleStore.LocaleInfo> mUpdatedLocaleInfoList;
    private LocaleStore.LocaleInfo mSelectedLocaleInfo;
    private int mMenuItemId;

    @SuppressWarnings("NullAway")
    public UserPreferredLocalePreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mPreferences = new ArrayMap<>();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(KEY_PREFERENCE_CATEGORY_LANGUAGES);
        updatePreferences();
    }

    void updatePreferences() {
        if (mPreferenceCategory == null) {
            Log.d(TAG, "updatePreferences, mPreferenceCategory is null");
            return;
        }

        final Map<String, OrderMenuPreference> existingPreferences = mPreferences;
        mPreferences = new ArrayMap<>();
        setupPreference(existingPreferences);

        for (OrderMenuPreference pref : existingPreferences.values()) {
            mPreferenceCategory.removePreference(pref);
        }
    }

    @VisibleForTesting
    void setupPreference(Map<String, OrderMenuPreference> existingPreferences) {
        Log.d(TAG, "setupPreference");
        List<LocaleStore.LocaleInfo> localeInfoList = getUserLocaleList();
        int listSize = localeInfoList.size();
        for (int i = 0; i < listSize; i++) {
            LocaleStore.LocaleInfo localeInfo = localeInfoList.get(i);
            OrderMenuPreference pref = existingPreferences.remove(localeInfo.getId());
            if (pref == null) {
                pref = new OrderMenuPreference(mContext);
                mPreferenceCategory.addPreference(pref);
            }
            String localeName = localeInfo.getFullNameNative();
            pref.setTitle(localeName);
            String summary = null;
            if (!localeInfo.isTranslated()) {
                summary = mContext.getString(R.string.locale_not_translated);
            } else if (localeInfo.getLocale().equals(Locale.getDefault())) {
                summary = mContext.getString(R.string.desc_current_default_language);
            }
            pref.setSummary(summary);
            pref.setKey(localeInfo.toString());
            if (listSize == 1) {
                pref.setMenuButtonVisible(false);
            }

            int menuId = R.menu.preferred_locale_menu;
            if (i == 0) {
                menuId = R.menu.preferred_locale_menu_down;
            } else if (i == (localeInfoList.size() - 1)) {
                menuId = R.menu.preferred_locale_menu_up;
            }
            pref.setMenuResId(menuId);
            pref.setMenuItemClickListener((item, preference) -> {
                int menuItemId = item.getItemId();
                mMenuItemId = menuItemId;
                if (menuItemId == R.id.move_up || menuItemId == R.id.move_down) {
                    LocaleStore.LocaleInfo saved = localeInfo;
                    mSelectedLocaleInfo = saved;
                    int position = localeInfoList.indexOf(localeInfo);
                    localeInfoList.remove(position);
                    localeInfoList.add(menuItemId == R.id.move_up ? position - 1 : position + 1,
                            saved);
                    mUpdatedLocaleInfoList = localeInfoList;
                    showConfirmDialog(localeInfoList, null);
                    mMetricsFeatureProvider.action(mContext,
                            SettingsEnums.ACTION_REORDER_LANGUAGE);
                } else {
                    NotificationController controller = NotificationController.getInstance(
                            mContext);
                    controller.removeNotificationInfo(
                            localeInfo.getLocale().toLanguageTag());
                    final Locale defaultBeforeRemoval = Locale.getDefault();
                    mSelectedLocaleInfo = localeInfo;
                    int position = localeInfoList.indexOf(localeInfo);
                    localeInfoList.remove(position);
                    mUpdatedLocaleInfoList = localeInfoList;

                    if (position == 0) {
                        showConfirmDialog(localeInfoList, defaultBeforeRemoval);
                    } else {
                        displayRemovalDialogFragment(localeInfo);
                    }
                }
                updatePreferences();
                return true;
            });
            pref.setNumber(i + 1);
            pref.setShowIconsInPopupMenu(true);
            mPreferences.put(localeInfo.getId(), pref);
        }
    }

    protected void showConfirmDialog(List<LocaleStore.LocaleInfo> localeInfoList,
            @Nullable Locale defaultLocaleBeforeRemoval) {
        Locale currentSystemLocale = LocalePicker.getLocales().get(0);
        LocaleStore.LocaleInfo defaultAfterChange = localeInfoList.get(0);
        if (!defaultAfterChange.getLocale().equals(currentSystemLocale)) {
            if (Locale.getDefault().equals(defaultAfterChange.getLocale())) {
                if (mMenuItemId == R.id.remove) {
                    displayRemovalDialogFragment(mSelectedLocaleInfo);
                } else {
                    doTheUpdate();
                }
            } else {
                if (mMenuItemId == R.id.move_up && !defaultAfterChange.isTranslated()) {
                    showUnavailableDialog(defaultAfterChange);
                } else {
                    displaySystemDialogFragment(defaultAfterChange, true);
                }
            }
        } else {
            if (!defaultAfterChange.isTranslated()) {
                if (defaultLocaleBeforeRemoval == null) {
                    showDialogDueToMove(localeInfoList);
                } else {
                    showDialogDueToRemoval(localeInfoList, defaultLocaleBeforeRemoval);
                }
            } else {
                doTheUpdate();
            }
        }
    }

    private void showDialogDueToMove(List<LocaleStore.LocaleInfo> localeInfoList) {
        LocaleStore.LocaleInfo newLocale = localeInfoList.stream().filter(
                i -> i.isTranslated()).findFirst().orElse(null);
        if (newLocale == null) {
            return;
        }
        LocaleStore.LocaleInfo oldLocale = null;
        final LocaleList localeList = LocalePicker.getLocales();
        for (int i = 0; i < localeList.size(); i++) {
            LocaleStore.LocaleInfo temp = LocaleStore.getLocaleInfo(localeList.get(i));
            if (temp.isTranslated()) {
                oldLocale = temp;
                break;
            }
        }
        if (oldLocale != null && !newLocale.getLocale().equals(
                oldLocale.getLocale())) {
            displaySystemDialogFragment(newLocale, false);
        } else {
            doTheUpdate();
        }
    }

    private void showDialogDueToRemoval(List<LocaleStore.LocaleInfo> localeInfoList,
            Locale preDefault) {
        if (preDefault == null) {
            return;
        }
        LocaleStore.LocaleInfo currentDefault = localeInfoList.stream().filter(
                i -> i.isTranslated()).findFirst().orElse(null);
        if (currentDefault != null && !preDefault.equals(currentDefault.getLocale())) {
            displaySystemDialogFragment(currentDefault, false);
        }
    }

    private void displaySystemDialogFragment(LocaleStore.LocaleInfo localeInfo,
            boolean showDialogForNotTranslated) {
        LocaleDialogFragment localeDialogFragment = LocaleDialogFragment.newInstance();
        Bundle args = new Bundle();
        args.putBoolean(LocaleDialogFragment.ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED,
                showDialogForNotTranslated);
        args.putInt(LocaleDialogFragment.ARG_DIALOG_TYPE, DIALOG_CONFIRM_SYSTEM_DEFAULT);
        args.putSerializable(LocaleDialogFragment.ARG_TARGET_LOCALE, localeInfo);
        args.putSerializable(LocaleDialogFragment.ARG_SELECTED_LOCALE, mSelectedLocaleInfo);
        args.putInt(LocaleDialogFragment.ARG_MENU_ITEM_ID, mMenuItemId);
        localeDialogFragment.setArguments(args);
        localeDialogFragment.show(mFragmentManager, TAG_DIALOG_CONFIRM_SYSTEM_DEFAULT);
    }

    private void showUnavailableDialog(LocaleStore.LocaleInfo localeInfo) {
        Log.d(TAG, "showUnavailableDialog");
        Bundle args = new Bundle();
        args.putInt(LocaleDialogFragment.ARG_DIALOG_TYPE,
                LocaleDialogFragment.DIALOG_NOT_AVAILABLE_LOCALE);
        args.putSerializable(LocaleDialogFragment.ARG_TARGET_LOCALE, localeInfo);
        args.putSerializable(LocaleDialogFragment.ARG_SELECTED_LOCALE, mSelectedLocaleInfo);
        args.putInt(LocaleDialogFragment.ARG_MENU_ITEM_ID, mMenuItemId);
        LocaleDialogFragment localeDialogFragment = LocaleDialogFragment.newInstance();
        localeDialogFragment.setArguments(args);
        localeDialogFragment.show(mFragmentManager, TAG_DIALOG_NOT_AVAILABLE);
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_NOT_SUPPORTED_SYSTEM_LANGUAGE);
    }

    private void displayRemovalDialogFragment(LocaleStore.LocaleInfo localeInfo) {
        LocaleDialogFragment localeDialogFragment = LocaleDialogFragment.newInstance();
        Bundle args = new Bundle();
        args.putInt(LocaleDialogFragment.ARG_DIALOG_TYPE, DIALOG_REMOVE_LOCALE);
        args.putSerializable(LocaleDialogFragment.ARG_TARGET_LOCALE, localeInfo);
        localeDialogFragment.setArguments(args);
        localeDialogFragment.show(mFragmentManager, TAG_DIALOG_REMOVE_LOCALE);
    }

    public void doTheUpdate() {
        LocaleList localeList = new LocaleList(mUpdatedLocaleInfoList.stream()
                .map(LocaleStore.LocaleInfo::getLocale)
                .toArray(Locale[]::new));
        // Update the Settings application to make things feel more responsive.
        LocaleList.setDefault(localeList);
        // Update the system.
        LocalePicker.updateLocales(localeList);
        updatePreferences();
    }

    protected List<LocaleStore.LocaleInfo> setUpdatedLocaleList(
            LocaleStore.LocaleInfo selectedLocaleInfo, int menuId) {
        mUpdatedLocaleInfoList = getUserLocaleList();
        if (menuId == R.id.move_up || menuId == R.id.move_down) {
            LocaleStore.LocaleInfo saved = selectedLocaleInfo;
            mSelectedLocaleInfo = saved;
            for (int i = 0; i < mUpdatedLocaleInfoList.size(); i++) {
                if (mUpdatedLocaleInfoList.get(i).toString().equals(
                        selectedLocaleInfo.toString())) {
                    mUpdatedLocaleInfoList.remove(i);
                    mUpdatedLocaleInfoList.add(menuId == R.id.move_up ? i - 1 : i + 1, saved);
                    break;
                }
            }
        } else {
            for (int i = 0; i < mUpdatedLocaleInfoList.size(); i++) {
                if (mUpdatedLocaleInfoList.get(i).toString().equals(
                        selectedLocaleInfo.toString())) {
                    mUpdatedLocaleInfoList.remove(i);
                    break;
                }
            }
        }
        return mUpdatedLocaleInfoList;
    }

    protected void setFragmentManager(@NonNull FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public @NonNull String getPreferenceKey() {
        return KEY_PREFERENCE_USER_PREFERRED_LOCALE_LIST;
    }
}
