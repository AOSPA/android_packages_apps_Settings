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

package com.android.settings.accessibility.setupwizard.items

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R
import com.android.settings.accessibility.setupwizard.clearPartnerConfigCache
import com.android.settings.accessibility.setupwizard.items.IllustrationItem.OnBindListener
import com.android.settings.accessibility.setupwizard.setupMockLottieAnimationView
import com.android.settings.accessibility.setupwizard.setupPartnerConfigMock
import com.android.settingslib.widget.preference.illustration.R as IllustrationR
import com.google.android.setupcompat.partnerconfig.PartnerConfigHelper
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import java.io.ByteArrayInputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf

/** Tests for [IllustrationItem]. */
@RunWith(RobolectricTestParameterInjector::class)
class IllustrationItemTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val illustrationItem: IllustrationItem = IllustrationItem()
    private val rootView: View =
        LayoutInflater.from(context).inflate(R.layout.setup_illustration_item, null)

    @Test
    fun onBindView_setsImageDrawable() {
        val drawable = ColorDrawable(Color.RED)
        illustrationItem.imageDrawable = drawable

        illustrationItem.onBindView(rootView)

        val illustrationView =
            rootView.findViewById<LottieAnimationView>(R.id.sud_item_illustration)
        assertThat(illustrationView.drawable).isEqualTo(drawable)
    }

    @Test
    @TestParameters(
        value =
            [
                "{resId: ${R.raw.accessibility_color_inversion_banner}}",
                "{resId: ${R.raw.accessibility_magnification_banner}}",
                "{resId: ${R.raw.accessibility_magnification_banner_expressive}}",
            ]
    )
    fun onBindView_setsImageResource(resId: Int) {
        illustrationItem.imageResId = resId

        illustrationItem.onBindView(rootView)

        val illustrationView =
            rootView.findViewById<LottieAnimationView>(R.id.sud_item_illustration)
        assertThat(illustrationView.drawable).isNotNull()
    }

    @Test
    fun onBindView_triggersOnBindListener() {
        val listener = mock<OnBindListener>()
        illustrationItem.onBindListener = listener

        illustrationItem.onBindView(rootView)

        val illustrationView =
            rootView.findViewById<LottieAnimationView>(R.id.sud_item_illustration)
        verify(listener).onBind(illustrationView)
    }

    @Test
    fun onBindView_setsContentDescription() {
        val testDescription = "Test Description"
        illustrationItem.contentDescription = testDescription

        illustrationItem.onBindView(rootView)

        val illustrationView =
            rootView.findViewById<LottieAnimationView>(R.id.sud_item_illustration)
        assertThat(illustrationView.contentDescription.toString()).isEqualTo(testDescription)
    }

    @Test
    fun onBindView_isLottieAnimationButNoDescription_setsDefaultContentDescription() {
        val imageUri = Uri.parse("content://test/lottie.json")
        val rootView =
            setupMockLottieAnimationView<AnimationDrawable>(
                context,
                imageUri,
                isLottieAnimatable = true,
            )
        val illustrationView =
            rootView.findViewById<LottieAnimationView>(R.id.sud_item_illustration)
        illustrationItem.imageUri = imageUri
        illustrationItem.contentDescription = null

        illustrationItem.onBindView(rootView)

        val defaultDescription =
            context.getString(IllustrationR.string.settingslib_illustration_content_description)
        assertThat(illustrationView.contentDescription.toString()).isEqualTo(defaultDescription)
    }

    @Test
    fun onBindView_updatesAccessibilityActions() {
        val imageUri = Uri.parse("content://test/lottie.json")
        val rootView =
            setupMockLottieAnimationView<AnimationDrawable>(
                context,
                imageUri,
                isLottieAnimatable = true,
            )
        val illustrationView =
            rootView.findViewById<LottieAnimationView>(R.id.sud_item_illustration)

        illustrationItem.imageUri = imageUri
        illustrationItem.onBindView(rootView)

        assertThat(illustrationView.accessibilityDelegate).isNotNull()
    }

    @Test
    fun isGroupDivider_returnsTrue() {
        assertThat(illustrationItem.isGroupDivider).isTrue()
    }

    @Test
    fun isEnabled_returnsFalse() {
        assertThat(illustrationItem.isEnabled).isFalse()
    }

    @Test
    fun setImageUri_clearsOtherResources() {
        illustrationItem.imageDrawable = ColorDrawable(Color.RED)

        val uri = Uri.parse("content://test/image.png")
        illustrationItem.imageUri = uri

        assertOnlyThisResourceIsSet(expectedUri = uri)
    }

    @Test
    fun setImageDrawable_clearsOtherResources() {
        illustrationItem.imageUri = Uri.parse("content://test")

        val drawable = ColorDrawable(Color.BLUE)
        illustrationItem.imageDrawable = drawable

        assertOnlyThisResourceIsSet(expectedDrawable = drawable)
    }

    @Test
    fun setImageResId_clearsOtherResources() {
        illustrationItem.imageUri = Uri.parse("content://test")

        val resId = 101
        illustrationItem.imageResId = resId

        assertOnlyThisResourceIsSet(expectedResId = resId)
    }

    @Test
    fun playAnimationWithUri_animatedVectorDrawable_success() {
        val mockDrawable = mock<AnimatedVectorDrawable>()
        val imageUri = Uri.parse("content://test")
        val rootView = setupMockLottieAnimationView(context, imageUri, mockDrawable)

        illustrationItem.imageUri = imageUri
        illustrationItem.onBindView(rootView)

        verify(mockDrawable).start()
    }

    @Test
    fun playAnimationWithUri_animatedImageDrawable_success() {
        val mockDrawable = mock<AnimatedImageDrawable>()
        val imageUri = Uri.parse("content://test")
        val rootView = setupMockLottieAnimationView(context, imageUri, mockDrawable)

        illustrationItem.imageUri = imageUri
        illustrationItem.onBindView(rootView)

        verify(mockDrawable).start()
    }

    @Test
    fun playAnimationWithUri_animationDrawable_success() {
        val mockDrawable = mock<AnimationDrawable>()
        val imageUri = Uri.parse("content://test")
        val rootView = setupMockLottieAnimationView(context, imageUri, mockDrawable)

        illustrationItem.imageUri = imageUri
        illustrationItem.onBindView(rootView)

        verify(mockDrawable).start()
    }

    @Test
    fun playLottieAnimationWithUri_verifyFailureListener() {
        val imageUri = Uri.parse("content://test")
        val rootView =
            setupMockLottieAnimationView<AnimatedVectorDrawable>(
                context,
                imageUri,
                isLottieAnimatable = true,
            )
        val animationView = rootView.findViewById<LottieAnimationView>(R.id.sud_item_illustration)

        illustrationItem.imageUri = imageUri
        illustrationItem.onBindView(rootView)

        verify(animationView).setFailureListener(any())
    }

    @Test
    fun handleAnimationControl_animatedDrawable_togglesPauseAndResume() {
        val imageUri = Uri.parse("content://test/lottie.json")
        val contentResolver = shadowOf(context.contentResolver)
        val validLottieJson = "{}".toByteArray()
        contentResolver.registerInputStream(imageUri, ByteArrayInputStream(validLottieJson))
        val rootView =
            setupMockLottieAnimationView<AnimationDrawable>(
                context,
                imageUri,
                isLottieAnimatable = true,
            )
        val illustrationView =
            rootView.findViewById<LottieAnimationView>(R.id.sud_item_illustration)
        illustrationItem.imageUri = imageUri
        illustrationItem.onBindView(rootView)
        assertThat(illustrationItem.isLottieAnimation).isTrue()
        clearInvocations(illustrationView)

        illustrationView.performClick()
        verify(illustrationView).pauseAnimation()

        illustrationView.performClick()
        verify(illustrationView).resumeAnimation()
    }

    @Test
    fun handleAnimationControl_staticDrawable_doesNotSetClickListener() {
        val drawable = ColorDrawable(Color.RED)
        illustrationItem.imageDrawable = drawable
        illustrationItem.onBindView(rootView)
        val illustrationView =
            spy(rootView.findViewById<LottieAnimationView>(R.id.sud_item_illustration))

        illustrationView.performClick()

        verify(illustrationView, never()).pauseAnimation()
        verify(illustrationView, never()).resumeAnimation()
    }

    @Test
    fun handleImageWithAnimation_withLottieRes_setsIsLottieAnimationTrue() {
        val imageUri = Uri.parse("content://test/lottie.json")
        val rootView =
            setupMockLottieAnimationView<AnimationDrawable>(
                context,
                imageUri,
                isLottieAnimatable = true,
            )

        illustrationItem.imageUri = imageUri
        illustrationItem.onBindView(rootView)

        assertThat(illustrationItem.isLottieAnimation).isTrue()
    }

    @Test
    fun handleImageWithAnimation_withStaticDrawable_setsIsLottieAnimationFalse() {
        val drawable = ColorDrawable(Color.RED)
        illustrationItem.imageDrawable = drawable

        illustrationItem.onBindView(rootView)

        assertThat(illustrationItem.isLottieAnimation).isFalse()
    }

    @Test
    @TestParameters(
        "{resId: ${R.raw.accessibility_color_inversion_banner}, data: [], expected: ${View.GONE}}",
        "{resId: ${R.raw.accessibility_color_inversion_banner}, data: [1], expected: ${View.VISIBLE}}",
    )
    fun handleImageWithAnimation_verifyVisibility(resId: Int, data: List<Int>, expected: Int) {
        val illustrationView =
            rootView.findViewById<LottieAnimationView>(R.id.sud_item_illustration)
        val spyRes =
            spy(context.resources).stub {
                on { openRawResource(resId) } doReturn
                    ByteArrayInputStream(data.map { it.toByte() }.toByteArray())
            }
        val spyView = spy(illustrationView).stub { on { resources } doReturn spyRes }
        val spyRootView =
            spy(rootView).stub {
                on { findViewById<View>(R.id.sud_item_illustration) } doReturn spyView
            }

        illustrationItem.imageResId = resId
        illustrationItem.onBindView(spyRootView)

        assertThat(spyView.visibility).isEqualTo(expected)
    }

    @Test
    fun handleImageWithAnimation_animatedIconEnabled_startsAnimationAfterDelay() {
        val spyContext = spy(context)
        try {
            setupPartnerConfigMock(
                spyContext,
                PartnerConfigHelper.IS_ANIMATED_ICON_ENABLED,
                enabled = true,
            )
            val mockDrawable = mock<AnimationDrawable>()
            val illustrationView =
                rootView.findViewById<LottieAnimationView>(R.id.sud_item_illustration)
            val runnableCaptor = argumentCaptor<Runnable>()
            val spyView =
                spy(illustrationView).stub {
                    on { context } doReturn spyContext
                    on { drawable } doReturn mockDrawable
                    on { postDelayed(runnableCaptor.capture(), any()) } doReturn true
                }
            val spyRootView =
                spy(rootView).stub {
                    on { findViewById<View>(R.id.sud_item_illustration) } doReturn spyView
                }

            illustrationItem.imageResId = R.raw.accessibility_color_inversion_banner
            illustrationItem.onBindView(spyRootView)

            verify(mockDrawable, never()).start()
            verify(spyView).postDelayed(any(), any())
            runnableCaptor.firstValue.run()
            verify(mockDrawable).start()
        } finally {
            clearPartnerConfigCache()
        }
    }

    @Test
    fun handleImageWithAnimation_animatedIconDisabled_startsAnimationImmediately() {
        val spyContext = spy(context)
        try {
            setupPartnerConfigMock(
                spyContext,
                PartnerConfigHelper.IS_ANIMATED_ICON_ENABLED,
                enabled = false,
            )
            val mockDrawable = mock<AnimationDrawable>()
            val illustrationView =
                rootView.findViewById<LottieAnimationView>(R.id.sud_item_illustration)
            val spyView =
                spy(illustrationView).stub {
                    on { context } doReturn spyContext
                    on { drawable } doReturn mockDrawable
                }
            val spyRootView =
                spy(rootView).stub {
                    on { findViewById<View>(R.id.sud_item_illustration) } doReturn spyView
                }

            illustrationItem.imageResId = R.raw.accessibility_color_inversion_banner
            illustrationItem.onBindView(spyRootView)

            verify(mockDrawable).start()
        } finally {
            clearPartnerConfigCache()
        }
    }

    /** Helper to verify that only one resource is active at a time. */
    private fun assertOnlyThisResourceIsSet(
        expectedDrawable: Drawable? = null,
        expectedUri: Uri? = null,
        expectedResId: Int = 0,
    ) {
        assertThat(illustrationItem.imageDrawable).isEqualTo(expectedDrawable)
        assertThat(illustrationItem.imageUri).isEqualTo(expectedUri)
        assertThat(illustrationItem.imageResId).isEqualTo(expectedResId)
    }
}
