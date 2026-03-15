
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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.androidx.fragment.FragmentController;
import org.robolectric.annotation.Config;

import static org.robolectric.Shadows.shadowOf;
import android.os.Looper;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowAlertDialogCompat.class,
})
public class StopAndroidAutoDialogFragmentTest {

    private static final String DEVICE_ADDRESS = "00:11:22:33:44:55";
    private static final String TITLE = "Test Title";
    private static final String MESSAGE = "Test Message";
    private static final String POSITIVE_BUTTON_TEXT = "Confirm";

    private TestFragment mTargetFragment;
    private StopAndroidAutoDialogFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        RuntimeEnvironment.application.setTheme(androidx.appcompat.R.style.Theme_AppCompat);

        mTargetFragment = new TestFragment();
        mFragment = StopAndroidAutoDialogFragment.newInstance(DEVICE_ADDRESS, TITLE, MESSAGE,
                POSITIVE_BUTTON_TEXT);
        mFragment.setTargetFragment(mTargetFragment, 0);

        FragmentActivity activity = Robolectric.setupActivity(FragmentActivity.class);
        activity.getSupportFragmentManager().beginTransaction()
                .add(mTargetFragment, "target")
                .add(mFragment, "dialog")
                .commitNow();
    }

    @Test
    public void onCreateDialog_setsCorrectLabels() {
        final AlertDialog dialog = (AlertDialog) mFragment.getDialog();
        assertThat(dialog).isNotNull();

        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowDialog.getTitle().toString()).isEqualTo(TITLE);
        assertThat(shadowDialog.getMessage().toString()).isEqualTo(MESSAGE);
        assertThat(dialog.getButton(DialogInterface.BUTTON_POSITIVE).getText().toString())
                .isEqualTo(POSITIVE_BUTTON_TEXT);
    }

    @Test
    public void onPositiveButtonClick_callsListener() {
        final AlertDialog dialog = (AlertDialog) mFragment.getDialog();
        assertThat(dialog).isNotNull();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mTargetFragment.mConfirmedDeviceAddress).isEqualTo(DEVICE_ADDRESS);
        assertThat(mTargetFragment.mCanceled).isFalse();
    }

    @Test
    public void onNegativeButtonClick_callsListener() {
        final AlertDialog dialog = (AlertDialog) mFragment.getDialog();
        assertThat(dialog).isNotNull();
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mTargetFragment.mConfirmedDeviceAddress).isNull();
        assertThat(mTargetFragment.mCanceled).isTrue();
    }

    @Test
    public void onCancelDialog_callsListener() {
        final AlertDialog dialog = (AlertDialog) mFragment.getDialog();
        assertThat(dialog).isNotNull();
        dialog.cancel();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mTargetFragment.mConfirmedDeviceAddress).isNull();
        assertThat(mTargetFragment.mCanceled).isTrue();
    }


    public static class TestFragment extends Fragment implements
            StopAndroidAutoDialogFragment.StopAndroidAutoDialogListener {
        public String mConfirmedDeviceAddress = null;
        public boolean mCanceled = false;

        @Override
        public void onDialogConfirmed(String deviceAddress) {
            mConfirmedDeviceAddress = deviceAddress;
        }

        @Override
        public void onDialogCanceled() {
            mCanceled = true;
        }
    }
}
