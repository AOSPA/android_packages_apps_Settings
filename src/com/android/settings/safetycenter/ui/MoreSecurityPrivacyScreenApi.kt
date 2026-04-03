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

package com.android.settings.safetycenter.ui

import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.safetycenter.MoreSecurityPrivacyFragment
import com.android.settings.utils.ContentCaptureUtils
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.EnterpriseRestriction
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

/**
 * API definition for the More Security & Privacy screen. This class exposes the screen and its
 * preferences to the Settings API framework.
 */
@ProvidePreferenceScreen(MoreSecurityPrivacyScreenApi.KEY)
class MoreSecurityPrivacyScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SAFETY_CENTER,
        fragment = MoreSecurityPrivacyFragment::class,
        purpose = R.string.more_security_privacy_screen_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        // Show media on lock screen
        preference(
            key = "privacy_media_controls_lockscreen",
            purpose = R.string.privacy_media_controls_lockscreen_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.REQUIRES_CONFIRMATION)

            get {
                execute {
                    Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.MEDIA_CONTROLS_LOCK_SCREEN,
                        1,
                    ) == 1
                }
            }
            set {
                execute { value ->
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.MEDIA_CONTROLS_LOCK_SCREEN,
                        if (value) 1 else 0,
                    )
                }
            }
        }

        // Allow camera software extensions
        preference(
            key = "privacy_camera_extensions_fallback",
            purpose = R.string.privacy_camera_extensions_fallback_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.REQUIRES_CONFIRMATION)

            get {
                execute {
                    Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.CAMERA_EXTENSIONS_FALLBACK,
                        0,
                    ) == 1
                }
            }
            set {
                execute { value ->
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.CAMERA_EXTENSIONS_FALLBACK,
                        if (value) 1 else 0,
                    )
                }
            }
        }

        // Personalize using app data (Content Capture)
        preference(
            key = "content_capture",
            purpose = R.string.content_capture_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.REQUIRES_CONFIRMATION)

            preconditions("The device must support content capture and the feature must not be disabled by any device or enterprise restrictions.") {
                if (
                    !ContentCaptureUtils.isFeatureAvailable() ||
                        ContentCaptureUtils.getServiceSettingsComponentName() != null
                ) {
                    HardwareUnsupported(R.string.content_capture_unavailable)
                } else if (
                    UserManager.get(context)
                        .hasUserRestrictionForUser(
                            UserManager.DISALLOW_CONTENT_CAPTURE,
                            UserHandle.of(UserHandle.myUserId()),
                        )
                ) {
                    EnterpriseRestriction(R.string.content_capture_unavailable)
                } else {
                    Allowed
                }
            }
            get { execute { ContentCaptureUtils.isEnabledForUser(context) } }
            set { execute { value -> ContentCaptureUtils.setEnabledForUser(context, value) } }
        }
    }

    companion object {
        const val KEY = "more_security_privacy_settings"
    }
}
