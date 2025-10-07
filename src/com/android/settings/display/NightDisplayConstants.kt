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

import com.android.internal.R

object NightDisplayConstants {

  /* XML resource id for the night display available. Usually received via
   * ColorDisplayManager.isNightDisplayAvailable(Context)
   */
  @JvmField val NIGHT_DISPLAY_AVAILABLE_RES_ID = R.bool.config_nightDisplayAvailable

  /* XML resource id for the night display settings page blocker.
   * If true - Night Light settings page is not available.
   */
  @JvmField val NIGHT_DISPLAY_SETTINGS_PAGE_BLOCKER_RES_ID = R.bool.config_cv_available
}
