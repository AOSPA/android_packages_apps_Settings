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

package com.android.settings.accessibility.setupwizard

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R
import com.android.settings.testutils.AccessibilityTestUtils
import com.google.android.setupcompat.partnerconfig.PartnerConfigHelper
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf

/** Utility for creating mocked Accessibility Services for Setup Wizard tests. */
fun createMockServiceInfo(
    context: Context,
    componentName: ComponentName,
    label: String,
    description: String,
    isAlwaysOnService: Boolean = false,
): AccessibilityServiceInfo {
    return spy(
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                context,
                componentName,
                isAlwaysOnService,
            )
        )
        .apply {
            isAccessibilityTool = true
            doReturn(description).whenever(this).loadDescription(any())
            resolveInfo =
                resolveInfo?.let {
                    spy(it).apply {
                        doReturn(label).whenever(this).loadLabel(any())
                        doReturn(ColorDrawable(0)).whenever(this).loadIcon(any())
                    }
                }
        }
}

/**
 * Sets up a mocked [LottieAnimationView].
 *
 * @param isLottieAnimatable If true, the [mockDrawable] is ignored (set to null). This is used to
 *   simulate a state where the Lottie view is expected to handle its own animation logic or
 *   transition from a URI, rather than using a static mock.
 */
fun <T> setupMockLottieAnimationView(
    context: Context,
    imageUri: Uri,
    mockDrawable: T? = null,
    isLottieAnimatable: Boolean = false,
): View where T : Drawable, T : Animatable {
    shadowOf(context.contentResolver).registerInputStream(imageUri, "".byteInputStream())

    // IllustrationItem sets isLottieAnimation = true ONLY if getDrawable() returns null
    // after setting a URI/ResId. We return null here to trigger that logic branch.
    val effectiveDrawable = if (isLottieAnimatable) null else mockDrawable

    return spy(LottieAnimationView(context)).apply {
        id = R.id.sud_item_illustration
        stub { on { drawable } doReturn effectiveDrawable }
    }
}

/**
 * Configures a mocked [ContentResolver] to simulate Partner Customization flags.
 *
 * This setup is essential for testing UI components that change behavior based on external partner
 * configurations (e.g., Setup Wizard or OEM-specific themes).
 *
 * @param spyContext A [Context] spy used to intercept [Context.getContentResolver].
 * @param flagName The name of the boolean configuration flag to mock.
 * @param enabled The value to be returned when the [flagName] is queried.
 */
fun setupPartnerConfigMock(spyContext: Context, flagName: String, enabled: Boolean) {
    clearPartnerConfigCache()
    val resultBundle = Bundle().apply { putBoolean(flagName, enabled) }
    val mockContentResolver =
        mock<ContentResolver> {
            on {
                call(any<Uri>(), any<String>(), anyOrNull<String>(), anyOrNull<Bundle>())
            } doReturn resultBundle
        }
    spyContext.stub { on { contentResolver } doReturn mockContentResolver }
}

/**
 * Force-clears the internal static caches of [PartnerConfigHelper] using reflection.
 *
 * [PartnerConfigHelper] often caches configuration values in static fields to improve performance.
 * In a unit test environment, these cached values can leak between tests, causing flaky results.
 * This method resets the animation-related bundles to null to ensure the next test fetch hits the
 * mocked [ContentResolver].
 *
 * @throws NoSuchFieldException If the field names in [PartnerConfigHelper] is not exist
 */
fun clearPartnerConfigCache() {
    val fields = listOf("enableAnimatedIconBundle", "enableAnimatedQrCodeBundle")
    fields.forEach { fieldName ->
        runCatching {
                val field = PartnerConfigHelper::class.java.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(null, null)
            }
            .onFailure { println("Could not clear field: $fieldName") }
    }
}
