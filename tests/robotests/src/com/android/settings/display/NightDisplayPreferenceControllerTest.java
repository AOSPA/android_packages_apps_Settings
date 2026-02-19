package com.android.settings.display;

import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.DisplayInfo;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {SettingsShadowResources.class})
public class NightDisplayPreferenceControllerTest {

    private Context mContext;
    private ColorDisplayManager mColorDisplayManager;
    private NightDisplayPreferenceController mPreferenceController;
    private final DisplayInfo mDisplayInfo = new DisplayInfo();

    @Before
    public void setUp() {
        mDisplayInfo.type = Display.TYPE_INTERNAL;
        DisplayAdjustments daj = null;
        Display display = new Display(mock(DisplayManagerGlobal.class),
                Display.DEFAULT_DISPLAY, mDisplayInfo, daj);
        mContext = RuntimeEnvironment.application.createDisplayContext(display);
        mColorDisplayManager = mContext.getSystemService(ColorDisplayManager.class);
        mPreferenceController = new NightDisplayPreferenceController(mContext, "test");
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void nightDisplaySuggestion_isNotCompleted_ifAutoModeDisabled() {
        mColorDisplayManager.setNightDisplayAutoMode(ColorDisplayManager.AUTO_MODE_DISABLED);

        assertThat(NightDisplayPreferenceController.isSuggestionComplete(mContext)).isFalse();
    }

    @Test
    public void nightDisplaySuggestion_isCompleted_ifAutoModeCustom() {
        mColorDisplayManager.setNightDisplayAutoMode(ColorDisplayManager.AUTO_MODE_CUSTOM_TIME);

        assertThat(NightDisplayPreferenceController.isSuggestionComplete(mContext)).isTrue();
    }

    @Test
    public void nightDisplaySuggestion_isCompleted_ifAutoModeTwilight() {
        mColorDisplayManager.setNightDisplayAutoMode(ColorDisplayManager.AUTO_MODE_TWILIGHT);

        assertThat(NightDisplayPreferenceController.isSuggestionComplete(mContext)).isTrue();
    }

    @Test
    public void nightDisplaySuggestion_isCompleted_ifSuggestionDisabled() {
        mColorDisplayManager.setNightDisplayAutoMode(ColorDisplayManager.AUTO_MODE_DISABLED);
        SettingsShadowResources.overrideResource(R.bool.config_night_light_suggestion_enabled,
                false);

        assertThat(NightDisplayPreferenceController.isSuggestionComplete(mContext)).isTrue();
    }

    @Test
    public void configuredNightDisplayAvailableAndNotBlocked_returnAvailable() {
        NightDisplayTestUtils.setNightDisplayAvailableAndNotBlocked();
        assertThat(mPreferenceController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void configuredNightDisplayUnavailableAndNotBlocked_returnUnsupportedOnDevice() {
        NightDisplayTestUtils.setNightDisplayAvailable(false);
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(false);
        assertThat(mPreferenceController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void configuredNightDisplayAvailableAndBlocked_returnUnsupportedOnDevice() {
        NightDisplayTestUtils.setNightDisplayAvailable(true);
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(true);
        assertThat(mPreferenceController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void configuredNightDisplayUnavailableAndBlocked_returnUnsupportedOnDevice() {
        NightDisplayTestUtils.setNightDisplayAvailable(false);
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(true);
        assertThat(mPreferenceController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void configuredNightDisplayAvailableAndNotBlocked_externalDisplay_isUnavailable() {
        NightDisplayTestUtils.setNightDisplayAvailableAndNotBlocked();
        mDisplayInfo.type = Display.TYPE_EXTERNAL;
        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void isChecked_nightDisplayIsActivated_returnTrue() {
        mColorDisplayManager.setNightDisplayActivated(true);

        assertThat(mPreferenceController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_nightDisplayIsNotActivated_returnFalse() {
        mColorDisplayManager.setNightDisplayActivated(false);

        assertThat(mPreferenceController.isChecked()).isFalse();
    }
}