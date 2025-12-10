package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
    SettingsShadowResources.class
})
public class DisplayWhiteBalancePreferenceControllerTest {

  private DisplayWhiteBalancePreferenceController mController;

  @Mock
  private ColorDisplayManager mColorDisplayManager;
  private ContentResolver mContentResolver;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Context mContext;
  @Mock
  private PreferenceScreen mScreen;
  @Mock
  private Preference mPreference;

  private final String PREFERENCE_KEY = "display_white_balance";

  @After
  public void tearDown() {
    SettingsShadowResources.reset();
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    doReturn(mock(DevicePolicyManager.class)).when(mContext)
            .getSystemService(Context.DEVICE_POLICY_SERVICE);

    mContentResolver = RuntimeEnvironment.application.getContentResolver();
    when(mContext.getContentResolver()).thenReturn(mContentResolver);
    when(mContext.getResources()).thenReturn(RuntimeEnvironment.application.getResources());
    when(mScreen.findPreference(PREFERENCE_KEY)).thenReturn(mPreference);

    mController = spy(new DisplayWhiteBalancePreferenceController(mContext, PREFERENCE_KEY));
    doReturn(mColorDisplayManager).when(mController).getColorDisplayManager();
  }

  @Test
  public void isAvailable() {
    SettingsShadowResources.overrideResource(
        com.android.internal.R.bool.config_displayWhiteBalanceAvailable, true);

    assertThat(mController.isAvailable()).isTrue();
  }

  @Test
  public void setChecked_true_setSuccessfully() {
    when(mColorDisplayManager.setDisplayWhiteBalanceEnabled(true)).thenReturn(true);
    assertThat(mController.setChecked(true)).isTrue();
  }

  @Test
  public void setChecked_false_setSuccessfully() {
    when(mColorDisplayManager.setDisplayWhiteBalanceEnabled(false)).thenReturn(true);
    assertThat(mController.setChecked(false)).isTrue();
  }

  @Test
  public void isChecked_true() {
    when(mColorDisplayManager.isDisplayWhiteBalanceEnabled()).thenReturn(true);
    assertThat(mController.isChecked()).isTrue();
  }

  @Test
  public void isChecked_false() {
    when(mColorDisplayManager.isDisplayWhiteBalanceEnabled()).thenReturn(false);
    assertThat(mController.isChecked()).isFalse();
  }

  @Test
  public void isNotAvailable_disabledForUser() {
    SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_displayWhiteBalanceAvailable, false);

    assertThat(mController.isAvailable()).isFalse();
    assertThat(mController.getAvailabilityStatus())
            .isEqualTo(DisplayWhiteBalancePreferenceController.DISABLED_FOR_USER);
  }

  @Test
  public void isAvailable_colorModeSaturated() {
    SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_displayWhiteBalanceAvailable, true);
    when(mColorDisplayManager.getColorMode())
            .thenReturn(ColorDisplayManager.COLOR_MODE_SATURATED);

    assertThat(mController.isAvailable()).isFalse();
    assertThat(mController.getAvailabilityStatus())
            .isEqualTo(DisplayWhiteBalancePreferenceController.CONDITIONALLY_UNAVAILABLE);
  }

  @Test
  public void isAvailable_accessibilityInversionEnabled() {
    SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_displayWhiteBalanceAvailable, true);
    when(mColorDisplayManager.getColorMode())
            .thenReturn(ColorDisplayManager.COLOR_MODE_NATURAL);
    toggleAccessibilityInversion(true);

    assertThat(mController.isAvailable()).isFalse();
    assertThat(mController.getAvailabilityStatus())
            .isEqualTo(DisplayWhiteBalancePreferenceController.CONDITIONALLY_UNAVAILABLE);
  }

  @Test
  public void isAvailable_accessibilityInversionDisabled() {
    SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_displayWhiteBalanceAvailable, true);
    when(mColorDisplayManager.getColorMode())
            .thenReturn(ColorDisplayManager.COLOR_MODE_NATURAL);
    toggleAccessibilityInversion(false);

    assertThat(mController.isAvailable()).isTrue();
    assertThat(mController.getAvailabilityStatus())
            .isEqualTo(DisplayWhiteBalancePreferenceController.AVAILABLE);
  }

  @Test
  public void isAvailable_accessibilityDaltonizerEnabled() {
    SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_displayWhiteBalanceAvailable, true);
    when(mColorDisplayManager.getColorMode())
            .thenReturn(ColorDisplayManager.COLOR_MODE_NATURAL);
    toggleAccessibilityDaltonizer(true);

    assertThat(mController.isAvailable()).isFalse();
    assertThat(mController.getAvailabilityStatus())
            .isEqualTo(DisplayWhiteBalancePreferenceController.CONDITIONALLY_UNAVAILABLE);
  }

  @Test
  public void isAvailable_accessibilityDaltonizerDisabled() {
    SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_displayWhiteBalanceAvailable, true);
    when(mColorDisplayManager.getColorMode())
            .thenReturn(ColorDisplayManager.COLOR_MODE_NATURAL);
    toggleAccessibilityDaltonizer(false);

    assertThat(mController.isAvailable()).isTrue();
    assertThat(mController.getAvailabilityStatus())
            .isEqualTo(DisplayWhiteBalancePreferenceController.AVAILABLE);
  }

  private void toggleAccessibilityInversion(boolean enable) {
    Settings.Secure.putInt(mContentResolver,
        Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, enable ? 1 : 0);
  }

  private void toggleAccessibilityDaltonizer(boolean enable) {
    Settings.Secure.putInt(mContentResolver,
            Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, enable ? 1 : 0);
  }
}
