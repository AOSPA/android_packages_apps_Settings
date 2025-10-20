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
package com.android.settings.display

import com.android.settings.testutils.shadow.SettingsShadowResources

/** Utility object for common Night Display test setup functions. */
object NightDisplayTestUtils {
  val CONFIG_NIGHT_DISPLAY_AVAILABLE_RES_ID = NightDisplayConstants.NIGHT_DISPLAY_AVAILABLE_RES_ID

  val CONFIG_NIGHT_DISPLAY_SETTINGS_PAGE_BLOCKER_RES_ID =
    NightDisplayConstants.NIGHT_DISPLAY_SETTINGS_PAGE_BLOCKER_RES_ID

  /**
   * Sets the mock state for Night Display settings availability.
   *
   * @param isAvailable True to set config_nightDisplayAvailable to true (available).
   */
  @JvmStatic
  fun setNightDisplayAvailable(isAvailable: Boolean) {
    SettingsShadowResources.overrideResource(CONFIG_NIGHT_DISPLAY_AVAILABLE_RES_ID, isAvailable)
  }

  /**
   * Sets the mock state for the Night Display settings page blocker.
   *
   * @param isBlocked True to set the blocker resource to true (blocked).
   */
  @JvmStatic
  fun setNightDisplaySettingsBlocked(isBlocked: Boolean) {
    SettingsShadowResources.overrideResource(
      CONFIG_NIGHT_DISPLAY_SETTINGS_PAGE_BLOCKER_RES_ID,
      isBlocked,
    )
  }

  /** Sets the default state where Night Display is available and NOT blocked. */
  @JvmStatic
  fun setNightDisplayAvailableAndNotBlocked() {
    setNightDisplayAvailable(true)
    setNightDisplaySettingsBlocked(false)
  }
}
