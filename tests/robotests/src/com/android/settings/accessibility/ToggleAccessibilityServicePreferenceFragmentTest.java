/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.internal.accessibility.common.ShortcutConstants.USER_SHORTCUT_TYPES;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment;
import com.android.settings.testutils.shadow.ShadowAccessibilityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

/** Tests for {@link ToggleAccessibilityServicePreferenceFragment} */
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowAccessibilityManager.class
})
@RunWith(RobolectricTestRunner.class)
public class ToggleAccessibilityServicePreferenceFragmentTest {

    private static final String PLACEHOLDER_PACKAGE_NAME = "com.placeholder.example";
    private static final String PLACEHOLDER_PACKAGE_NAME2 = "com.placeholder.example2";
    private static final String PLACEHOLDER_SERVICE_CLASS_NAME = "a11yservice1";
    private static final String PLACEHOLDER_SERVICE_CLASS_NAME2 = "a11yservice2";
    private static final String PLACEHOLDER_TILE_CLASS_NAME =
            PLACEHOLDER_PACKAGE_NAME + "tile.placeholder";
    private static final ComponentName PLACEHOLDER_TILE_COMPONENT_NAME = new ComponentName(
            PLACEHOLDER_PACKAGE_NAME, PLACEHOLDER_TILE_CLASS_NAME);

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private ToggleAccessibilityServicePreferenceFragment mFragment;
    private Context mContext;
    private ShadowAccessibilityManager mShadowAccessibilityManager;
    private FragmentScenario<ToggleAccessibilityServicePreferenceFragment> mFragScenario = null;

    @Before
    public void setUpTestFragment() {
        mContext = ApplicationProvider.getApplicationContext();
        mShadowAccessibilityManager = Shadow.extract(
                mContext.getSystemService(AccessibilityManager.class));
    }

    @After
    public void cleanUp() {
        if (mFragScenario != null) {
            mFragScenario.close();
        }
    }

    @Test
    public void getAccessibilityServiceInfo() throws Throwable {
        final AccessibilityServiceInfo info1 = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME);
        final AccessibilityServiceInfo info2 = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME2);
        final AccessibilityServiceInfo info3 = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME2,
                PLACEHOLDER_SERVICE_CLASS_NAME);
        final AccessibilityServiceInfo info4 = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME2,
                PLACEHOLDER_SERVICE_CLASS_NAME2);
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(
                List.of(info1, info2, info3, info4));

        showFragment(info3);

        assertThat(mFragment.getAccessibilityServiceInfo()).isEqualTo(info3);
    }

    @Test
    public void getAccessibilityServiceInfo_notFound() throws Throwable {
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(List.of());

        showFragment(getFakeAccessibilityServiceInfo(PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME));

        assertThat(mFragment.getAccessibilityServiceInfo()).isNull();
    }

    @Test
    public void serviceSupportsAccessibilityButton() throws Throwable {
        final AccessibilityServiceInfo infoWithA11yButton = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME);
        infoWithA11yButton.flags = AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;
        final AccessibilityServiceInfo infoWithoutA11yButton = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME2,
                PLACEHOLDER_SERVICE_CLASS_NAME2);
        infoWithoutA11yButton.flags = 0;
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(
                List.of(infoWithA11yButton, infoWithoutA11yButton));

        showFragment(infoWithA11yButton);
        assertThat(mFragment.serviceSupportsAccessibilityButton()).isTrue();
        showFragment(infoWithoutA11yButton);
        assertThat(mFragment.serviceSupportsAccessibilityButton()).isFalse();
    }

    @Test
    public void enableService_warningRequired_showWarning() throws Throwable {
        final AccessibilityServiceInfo serviceInfo = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME);
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(List.of(serviceInfo));
        showFragment(serviceInfo);
        assertThat(mFragment.mToggleServiceSwitchPreference.isChecked()).isFalse();

        mFragment.onCheckedChanged(null, true);
        ShadowLooper.idleMainLooper();

        assertWarningDialogShown(serviceInfo,
                AccessibilityDialogUtils.DialogEnums.ENABLE_WARNING_FROM_TOGGLE);
        assertThat(mFragment.mToggleServiceSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void enableService_warningNotRequired_dontShowWarning() throws Throwable {
        final AccessibilityServiceInfo serviceInfo = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME);
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(List.of(serviceInfo));
        mShadowAccessibilityManager.setAccessibilityServiceWarningExempted(
                serviceInfo.getComponentName());
        showFragment(serviceInfo);
        assertThat(mFragment.mToggleServiceSwitchPreference.isChecked()).isFalse();

        mFragment.onCheckedChanged(null, true);
        ShadowLooper.idleMainLooper();

        Dialog dialog = ShadowDialog.getLatestDialog();
        assertThat(dialog).isNull();
        assertThat(mFragment.mToggleServiceSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void toggleShortcutPreference_warningRequired_showWarning() throws Throwable {
        final AccessibilityServiceInfo serviceInfo = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME);
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(List.of(serviceInfo));
        showFragment(serviceInfo);

        mFragment.mShortcutPreference.setChecked(true);
        mFragment.onToggleClicked(mFragment.mShortcutPreference);
        ShadowLooper.idleMainLooper();

        assertWarningDialogShown(
                serviceInfo,
                AccessibilityDialogUtils.DialogEnums.ENABLE_WARNING_FROM_SHORTCUT_TOGGLE);
        assertThat(mFragment.mShortcutPreference.isChecked()).isFalse();
    }

    @Test
    public void toggleShortcutPreference_warningNotRequired_dontShowWarning() throws Throwable {
        final AccessibilityServiceInfo serviceInfo = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME);
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(List.of(serviceInfo));
        mShadowAccessibilityManager.setAccessibilityServiceWarningExempted(
                serviceInfo.getComponentName());
        showFragment(serviceInfo);

        mFragment.mShortcutPreference.setChecked(true);
        mFragment.onToggleClicked(mFragment.mShortcutPreference);
        ShadowLooper.idleMainLooper();

        assertShortcutsTutorialDialogShown();
    }

    @Test
    public void clickShortcutSettingsPreference_warningRequired_showWarning() throws Throwable {
        final AccessibilityServiceInfo serviceInfo = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME);
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(List.of(serviceInfo));
        showFragment(serviceInfo);

        mFragment.onSettingsClicked(mFragment.mShortcutPreference);
        ShadowLooper.idleMainLooper();

        assertWarningDialogShown(serviceInfo,
                AccessibilityDialogUtils.DialogEnums.ENABLE_WARNING_FROM_SHORTCUT);
    }

    @Test
    public void clickShortcutSettingsPreference_warningNotRequired_dontShowWarning_launchActivity()
            throws Throwable {
        final AccessibilityServiceInfo serviceInfo = getFakeAccessibilityServiceInfo(
                PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME);
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(List.of(serviceInfo));
        mShadowAccessibilityManager.setAccessibilityServiceWarningExempted(
                serviceInfo.getComponentName());

        showFragment(serviceInfo);
        mFragment.onSettingsClicked(mFragment.mShortcutPreference);

        Intent intent = shadowOf((ContextWrapper) mFragment.getContext()).peekNextStartedActivity();
        assertThat(intent).isNotNull();
        assertThat(intent.getExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                EditShortcutsPreferenceFragment.class.getName());
    }

    @Test
    public void getDefaultShortcutTypes_isAccessibilityTool_hasAssociatedTile_qsTypeIsDefault()
            throws Throwable {
        PreferredShortcuts.clearPreferredShortcuts(mContext);
        final AccessibilityServiceInfo serviceInfo = getFakeAccessibilityInfo(
                /* isAccessibilityTool= */ true, PLACEHOLDER_TILE_COMPONENT_NAME);
        showFragment(serviceInfo);

        assertThat(mFragment.getDefaultShortcutTypes())
                .isEqualTo(ShortcutConstants.UserShortcutType.QUICK_SETTINGS);
    }

    @Test
    public void getDefaultShortcutTypes_isNotAccessibilityTool_hasAssociatedTile_softwareTypeIsDefault()
            throws Throwable {
        PreferredShortcuts.clearPreferredShortcuts(mContext);
        final AccessibilityServiceInfo serviceInfo = getFakeAccessibilityInfo(
                /* isAccessibilityTool= */ false, PLACEHOLDER_TILE_COMPONENT_NAME);
        showFragment(serviceInfo);

        assertThat(mFragment.getDefaultShortcutTypes())
                .isEqualTo(ShortcutConstants.UserShortcutType.SOFTWARE);
    }

    @Test
    public void getDefaultShortcutTypes_isAccessibilityTool_noAssociatedTile_softwareTypeIsDefault()
            throws Throwable {
        PreferredShortcuts.clearPreferredShortcuts(mContext);
        final AccessibilityServiceInfo serviceInfo = getFakeAccessibilityInfo(
                /* isAccessibilityTool= */ true, /* tileService= */ null);
        showFragment(serviceInfo);

        assertThat(mFragment.getDefaultShortcutTypes())
                .isEqualTo(ShortcutConstants.UserShortcutType.SOFTWARE);
    }

    @Test
    public void getDefaultShortcutTypes_isNotAccessibilityTool_noAssociatedTile_softwareTypeIsDefault()
            throws Throwable {
        PreferredShortcuts.clearPreferredShortcuts(mContext);
        final AccessibilityServiceInfo serviceInfo = getFakeAccessibilityInfo(
                /* isAccessibilityTool= */ false, /* tileService= */ null);
        showFragment(serviceInfo);

        assertThat(mFragment.getDefaultShortcutTypes())
                .isEqualTo(ShortcutConstants.UserShortcutType.SOFTWARE);
    }

    @Test
    public void toggleShortcutPreference_noUserPreferredShortcut_hasQsTile_enableQsShortcut()
            throws Throwable {
        PreferredShortcuts.clearPreferredShortcuts(mContext);
        final AccessibilityServiceInfo serviceInfo = getFakeAccessibilityInfo(
                /* isAccessibilityTool= */ true, PLACEHOLDER_TILE_COMPONENT_NAME);
        showFragment(serviceInfo);

        mFragment.mShortcutPreference.setChecked(true);
        mFragment.onToggleClicked(mFragment.mShortcutPreference);

        assertEnabledShortcuts(serviceInfo.getComponentName(),
                ShortcutConstants.UserShortcutType.QUICK_SETTINGS);
    }

    @Test
    public void toggleShortcutPreference_noUserPreferredShortcut_noQsTile_enableSoftwareShortcut()
            throws Throwable {
        PreferredShortcuts.clearPreferredShortcuts(mContext);
        final AccessibilityServiceInfo serviceInfo = getFakeAccessibilityInfo(
                /* isAccessibilityTool= */ true, /* tileService= */ null);
        showFragment(serviceInfo);

        mFragment.mShortcutPreference.setChecked(true);
        mFragment.onToggleClicked(mFragment.mShortcutPreference);

        assertEnabledShortcuts(serviceInfo.getComponentName(),
                ShortcutConstants.UserShortcutType.SOFTWARE);
    }

    @Test
    public void toggleShortcutPreference_userPreferVolumeKeysShortcut_noQsTile_enableVolumeKeysShortcut()
            throws Throwable {
        final AccessibilityServiceInfo serviceInfo = getFakeAccessibilityInfo(
                /* isAccessibilityTool= */ true, /* tileService= */ null);
        PreferredShortcuts.saveUserShortcutType(
                mContext,
                new PreferredShortcut(serviceInfo.getComponentName().flattenToString(),
                        ShortcutConstants.UserShortcutType.HARDWARE));
        showFragment(serviceInfo);

        mFragment.mShortcutPreference.setChecked(true);
        mFragment.onToggleClicked(mFragment.mShortcutPreference);

        assertEnabledShortcuts(serviceInfo.getComponentName(),
                ShortcutConstants.UserShortcutType.HARDWARE);
    }

    @Test
    public void toggleShortcutPreference_userPreferVolumeKeysShortcut_hasQsTile_enableVolumeKeysShortcut()
            throws Throwable {
        final AccessibilityServiceInfo serviceInfo = getFakeAccessibilityInfo(
                /* isAccessibilityTool= */ true, PLACEHOLDER_TILE_COMPONENT_NAME);
        PreferredShortcuts.saveUserShortcutType(
                mContext,
                new PreferredShortcut(serviceInfo.getComponentName().flattenToString(),
                        ShortcutConstants.UserShortcutType.HARDWARE));
        showFragment(serviceInfo);

        mFragment.mShortcutPreference.setChecked(true);
        mFragment.onToggleClicked(mFragment.mShortcutPreference);

        assertEnabledShortcuts(serviceInfo.getComponentName(),
                ShortcutConstants.UserShortcutType.HARDWARE);
    }

    private AccessibilityServiceInfo getFakeAccessibilityInfo(
            boolean isAccessibilityTool, @Nullable ComponentName tileService) throws Throwable {
        AccessibilityServiceInfo info = getFakeAccessibilityServiceInfo(PLACEHOLDER_PACKAGE_NAME,
                PLACEHOLDER_SERVICE_CLASS_NAME);
        info.setAccessibilityTool(isAccessibilityTool);
        if (tileService != null) {
            ReflectionHelpers.setField(info, "mTileServiceName", tileService.flattenToString());
        }
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(List.of(info));
        mShadowAccessibilityManager.setAccessibilityServiceWarningExempted(info.getComponentName());
        return info;
    }

    private static class FakeResolveInfo extends ResolveInfo {
        final CharSequence mLabelName;

        FakeResolveInfo(CharSequence labelName) {
            mLabelName = labelName;
        }

        @Override
        public CharSequence loadLabel(PackageManager pm) {
            return mLabelName;
        }
    }

    /**
     * Launch ToggleAccessibilityServicePreferenceFragment with the given AccessibilityServiceInfo.
     * The launched fragment will be assigned to mFragment
     */
    private void showFragment(AccessibilityServiceInfo a11yServiceInfo) {
        Bundle bundle = new Bundle();
        bundle.putString(AccessibilitySettings.EXTRA_PREFERENCE_KEY,
                a11yServiceInfo.getComponentName().flattenToString());
        bundle.putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME,
                a11yServiceInfo.getComponentName());
        if (a11yServiceInfo.getTileServiceName() != null) {
            bundle.putString(AccessibilitySettings.EXTRA_TILE_SERVICE_COMPONENT_NAME,
                    a11yServiceInfo.getTileServiceName());
        }
        mFragScenario = FragmentScenario.launch(
                ToggleAccessibilityServicePreferenceFragment.class,
                bundle,
                androidx.appcompat.R.style.Theme_AppCompat, (FragmentFactory) null
        ).moveToState(Lifecycle.State.RESUMED);
        mFragScenario.onFragment(fragment -> mFragment = fragment);
    }

    @NonNull
    private AccessibilityServiceInfo getFakeAccessibilityServiceInfo(String packageName,
            String className) throws Throwable {
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        final ServiceInfo serviceInfo = new ServiceInfo();
        applicationInfo.packageName = packageName;
        serviceInfo.packageName = packageName;
        serviceInfo.name = className;
        serviceInfo.applicationInfo = applicationInfo;
        final ResolveInfo resolveInfo = new FakeResolveInfo(className);
        resolveInfo.serviceInfo = serviceInfo;
        final AccessibilityServiceInfo info = new AccessibilityServiceInfo(resolveInfo, mContext);
        ComponentName componentName = ComponentName.createRelative(packageName, className);
        info.setComponentName(componentName);
        return info;
    }

    private void assertWarningDialogShown(AccessibilityServiceInfo a11yServiceInfo,
            int dialogEnum) {
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog).isInstanceOf(AlertDialog.class);

        TextView title = dialog.findViewById(
                com.android.internal.R.id.accessibility_permissionDialog_title);
        assertThat(title).isNotNull();
        assertThat(title.getText().toString()).isEqualTo(mFragment.getString(
                com.android.internal.R.string.accessibility_enable_service_title,
                a11yServiceInfo.getResolveInfo().loadLabel(
                        mFragment.getContext().getPackageManager())));

        Fragment dialogFragment = mFragment.getChildFragmentManager().getFragments().getFirst();
        assertThat(dialogFragment).isInstanceOf(
                SettingsPreferenceFragment.SettingsDialogFragment.class);
        int dialogId =
                ((SettingsPreferenceFragment.SettingsDialogFragment) dialogFragment).getDialogId();
        assertThat(dialogId).isEqualTo(dialogEnum);
    }

    private void assertShortcutsTutorialDialogShown() {
        List<Fragment> fragments = mFragment.getChildFragmentManager().getFragments();
        assertThat(fragments).isNotEmpty();
        assertThat(fragments).hasSize(1);
        assertThat(fragments.getFirst()).isInstanceOf(
                AccessibilityShortcutsTutorial.DialogFragment.class);
    }

    private void assertEnabledShortcuts(ComponentName componentName, int expectedShortcutTypes) {
        int enabledShortcuts = 0;
        for (int shortcutType : USER_SHORTCUT_TYPES) {
            if (mShadowAccessibilityManager.getAccessibilityShortcutTargets(shortcutType).contains(
                    componentName.flattenToString())) {
                enabledShortcuts |= shortcutType;
            }
        }
        assertThat(enabledShortcuts).isEqualTo(expectedShortcutTypes);
    }
}
