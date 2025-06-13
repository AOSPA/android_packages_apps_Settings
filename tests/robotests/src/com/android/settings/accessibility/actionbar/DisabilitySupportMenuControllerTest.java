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

package com.android.settings.accessibility.actionbar;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.testing.EmptyFragmentActivity;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.android.settings.R;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link DisabilitySupportMenuController} */
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
@RunWith(RobolectricTestRunner.class)
public class DisabilitySupportMenuControllerTest {

    private static final String TEST_URL = "http://www.example.com/disability_support";
    private static final String EMPTY_URL = "";

    @Rule
    public final MockitoRule mMocks = MockitoJUnit.rule();
    @Rule
    public ActivityScenarioRule<EmptyFragmentActivity> mActivityScenario =
            new ActivityScenarioRule<>(EmptyFragmentActivity.class);

    private FragmentActivity mActivity;
    private InstrumentedPreferenceFragment mHost;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private Menu mMenu;
    @Mock
    private MenuItem mMenuItem;

    @Before
    public void setUp() {
        mActivityScenario.getScenario().onActivity(activity -> mActivity = activity);
        mHost = spy(new InstrumentedPreferenceFragment() {
            @Override
            public int getMetricsCategory() {
                return 0; // Not relevant for this controller
            }
        });
        when(mHost.getActivity()).thenReturn(mActivity);
        when(mMenu.add(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(mMenuItem);
        when(mMenuItem.getItemId()).thenReturn(MenusUtils.MenuId.DISABILITY_SUPPORT.getValue());
    }

    @Test
    public void init_shouldAttachToLifecycle() {
        when(mHost.getSettingsLifecycle()).thenReturn(mLifecycle);

        DisabilitySupportMenuController.init(mHost, TEST_URL);

        verify(mLifecycle).addObserver(any(DisabilitySupportMenuController.class));
    }

    @Test
    public void onCreateOptionsMenu_withValidUrl_shouldAddDisabilitySupportMenu() {
        DisabilitySupportMenuController.init(mHost, TEST_URL);

        mHost.getSettingsLifecycle().onCreateOptionsMenu(mMenu, /* inflater= */ null);

        verify(mMenu).add(Menu.NONE, MenusUtils.MenuId.DISABILITY_SUPPORT.getValue(), Menu.NONE,
                R.string.accessibility_disability_support_title);
        verify(mMenuItem).setIcon(com.android.settingslib.widget.help.R.drawable.ic_help_actionbar);
    }

    @Test
    public void onCreateOptionsMenu_withEmptyUrl_shouldNotAddDisabilitySupportMenu() {
        DisabilitySupportMenuController.init(mHost, EMPTY_URL);

        mHost.getSettingsLifecycle().onCreateOptionsMenu(mMenu, /* inflater= */ null);

        verify(mMenu, never()).add(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void onOptionsItemSelected_disabilitySupportMenuSelected_shouldStartBrowserIntent() {
        DisabilitySupportMenuController.init(mHost, TEST_URL);

        mHost.getSettingsLifecycle().onOptionsItemSelected(mMenuItem);

        Intent startedIntent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(startedIntent).isNotNull();
        assertThat(startedIntent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(startedIntent.getCategories()).contains(Intent.CATEGORY_BROWSABLE);
        assertThat(startedIntent.getData()).isEqualTo(Uri.parse(TEST_URL));
    }

    @Test
    public void onOptionsItemSelected_otherMenuItemSelected_shouldNotStartBrowserIntent() {
        DisabilitySupportMenuController.init(mHost, TEST_URL);
        when(mMenuItem.getItemId()).thenReturn(Menu.FIRST);

        mHost.getSettingsLifecycle().onOptionsItemSelected(mMenuItem);

        Intent startedIntent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(startedIntent).isNull();
    }
}
