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

package com.android.settings.vpn2;

import android.annotation.AttrRes;
import android.annotation.ColorInt;
import android.annotation.ColorRes;
import android.annotation.DrawableRes;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import java.util.Objects;

class UiUtils {
    /** Returns the color resource id from resource attribute. */
    @ColorRes
    public static int getColorResIdByAttribute(Context context, @AttrRes int attributeId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, typedValue, /* resolveRefs= */ true);
        return typedValue.resourceId;
    }

    /** Returns the color integer from resource attribute. */
    @ColorInt
    public static int getColorByAttribute(Context context, @AttrRes int attributeId) {
        return ContextCompat.getColor(context, getColorResIdByAttribute(context, attributeId));
    }

    /** Returns tinted with the color integer drawable. */
    public static Drawable mutateAndSetTintByColorInt(
            Context context, @DrawableRes int drawableId, @ColorInt int color) {
        Drawable mutated =
                Objects.requireNonNull(ContextCompat.getDrawable(context, drawableId)).mutate();
        mutated.setTint(color);
        return mutated;
    }

    /** Returns tinted with resource attribute drawable. */
    public static Drawable mutateAndSetTintByColorAttr(
            Context context, @DrawableRes int drawableId, @AttrRes int attributeId) {
        @ColorInt int colorInt = getColorByAttribute(context, attributeId);
        return mutateAndSetTintByColorInt(context, drawableId, colorInt);
    }

    private UiUtils() {}
}
