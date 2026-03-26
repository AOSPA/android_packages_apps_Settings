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

package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.StateSet;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;

/**
 * A helper class to programmatically create a focus indicator drawable that correctly handles
 * clipping and padding, with extensive customization options.
 *
 * <p><b>Why this class exists:</b> Static XML drawables are insufficient for focus indicators in
 * complex lists where items can be partially visible (clipped) or require different shapes based on
 * their position. This class solves these problems by:
 *
 * <ol>
 *   <li><b>Dynamic Clipping:</b> It queries the {@link Canvas}'s clip bounds at draw time to ensure
 *       the focus ring never draws outside the visible area of a view.
 *   <li><b>Flexible Sizing:</b> It provides fine-grained control over the focus ring's size and
 *       position relative to the view's bounds.
 *   <li><b>Positional Shaping:</b> The {@link Builder} can automatically calculate corner radii
 *       based on an item's position in a list (e.g., rounding only the top corners for the first
 *       item).
 * </ol>
 *
 * This centralization provides a consistent, powerful, and reusable way to create focus indicators,
 * reducing code duplication and eliminating the need for many specialized XML drawable assets.
 */
public final class FocusIndicatorDrawable {

    private static final int DEFAULT_CORNER_RADIUS_DP = 12; // A large, pronounced curve.
    private static final int DEFAULT_POSITIONAL_CORNER_RADIUS_DP = 3; // A subtle, soft-edge curve.
    private static final int DEFAULT_OUTLINE_WIDTH_DP = 3;
    private static final int DEFAULT_INSET_DP = 6;

    private FocusIndicatorDrawable() {}

    /**
     * A fluent builder for creating {@link Drawable} focus indicators. This is the primary entry
     * point for using the helper.
     */
    public static class Builder {
        private final Context mContext;
        private int mHorizontalPaddingAdjustmentDp = 0;
        private int mTopPaddingAdjustmentDp = 0;
        private int mBottomPaddingAdjustmentDp = 0;
        private int mCornerRadiusDp = DEFAULT_CORNER_RADIUS_DP;
        private float[] mCornerRadiiPx;
        private int mOutlineWidthDp = DEFAULT_OUTLINE_WIDTH_DP;
        private int mInsetDp = DEFAULT_INSET_DP;
        private int[] mStateSet = new int[] {android.R.attr.state_focused};

        private @ColorInt int mColor;

        public Builder(@NonNull Context context) {
            mContext = context;
            mColor = context.getColor(com.android.internal.R.color.materialColorPrimary);
        }

        /**
         * Sets an amount to adjust the horizontal spacing of the focus ring, in dp. A positive
         * value makes the ring tighter (insets from the edge), and a negative value makes it wider.
         *
         * @param dp The padding adjustment value in dp.
         */
        public Builder withHorizontalPaddingAdjustment(int dp) {
            mHorizontalPaddingAdjustmentDp = dp;
            return this;
        }

        /**
         * Sets an amount to adjust the vertical spacing of the focus ring, in dp. A positive value
         * makes the ring tighter (insets from the edge), and a negative value makes it wider.
         *
         * @param dp The padding adjustment value in dp.
         */
        public Builder withVerticalPaddingAdjustment(int dp) {
            mTopPaddingAdjustmentDp = dp;
            mBottomPaddingAdjustmentDp = dp;
            return this;
        }

        /**
         * Sets the top and bottom padding adjustments for the focus ring, in dp.
         *
         * @param topDp The top padding adjustment value in dp.
         * @param bottomDp The bottom padding adjustment value in dp.
         */
        public Builder withVerticalPaddingAdjustments(int topDp, int bottomDp) {
            mTopPaddingAdjustmentDp = topDp;
            mBottomPaddingAdjustmentDp = bottomDp;
            return this;
        }

        /**
         * Sets the state set for which the indicator drawable will be shown.
         * Defaults to {android.R.attr.state_focused}.
         *
         * @param stateSet The array of state attributes.
         */
        public Builder withStateSet(int[] stateSet) {
            mStateSet = stateSet;
            return this;
        }

        /**
         * Sets the base corner radius to be used for calculations, in dp.
         *
         * @param dp The corner radius in dp.
         */
        public Builder withCornerRadius(int dp) {
            mCornerRadiusDp = dp;
            float cornerRadiusPx = dpToPx(mContext, dp);
            mCornerRadiiPx =
                    new float[] {
                        cornerRadiusPx, cornerRadiusPx, cornerRadiusPx, cornerRadiusPx,
                        cornerRadiusPx, cornerRadiusPx, cornerRadiusPx, cornerRadiusPx
                    };
            return this;
        }

        /**
         * Sets the corner radii for each corner individually, in dp. This overrides any value set
         * by {@link #withCornerRadius(int)} or {@link #withPositionalCornerRadii(int, int)}.
         *
         * @param topLeftDp Radius for the top-left corner, in dp.
         * @param topRightDp Radius for the top-right corner, in dp.
         * @param bottomRightDp Radius for the bottom-right corner, in dp.
         * @param bottomLeftDp Radius for the bottom-left corner, in dp.
         */
        public Builder withCornerRadii(
                int topLeftDp, int topRightDp, int bottomRightDp, int bottomLeftDp) {
            float tl = dpToPx(mContext, topLeftDp);
            float tr = dpToPx(mContext, topRightDp);
            float br = dpToPx(mContext, bottomRightDp);
            float bl = dpToPx(mContext, bottomLeftDp);
            mCornerRadiiPx = new float[] {tl, tl, tr, tr, br, br, bl, bl};
            return this;
        }

        /**
         * Calculates and sets the corner radii based on the item's position in a list. This method
         * uses the radius set via {@link #withCornerRadius(int)}.
         *
         * @param position The adapter position of the item.
         * @param itemCount The total number of items in the adapter.
         */
        public Builder withPositionalCornerRadii(int position, int itemCount) {
            float cornerRadiusPx = dpToPx(mContext, mCornerRadiusDp);
            float defaultRadiusPx = dpToPx(mContext, DEFAULT_POSITIONAL_CORNER_RADIUS_DP);
            float[] cornerRadii =
                    new float[] {
                        defaultRadiusPx,
                        defaultRadiusPx,
                        defaultRadiusPx,
                        defaultRadiusPx,
                        defaultRadiusPx,
                        defaultRadiusPx,
                        defaultRadiusPx,
                        defaultRadiusPx
                    };

            boolean isFirst = position == 0;
            boolean isLast = position == itemCount - 1;

            if (isFirst) {
                cornerRadii[0] = cornerRadiusPx; // top-left-x
                cornerRadii[1] = cornerRadiusPx; // top-left-y
                cornerRadii[2] = cornerRadiusPx; // top-right-x
                cornerRadii[3] = cornerRadiusPx; // top-right-y
            }

            if (isLast) {
                cornerRadii[4] = cornerRadiusPx; // bottom-right-x
                cornerRadii[5] = cornerRadiusPx; // bottom-right-y
                cornerRadii[6] = cornerRadiusPx; // bottom-left-x
                cornerRadii[7] = cornerRadiusPx; // bottom-left-y
            }
            mCornerRadiiPx = cornerRadii;
            return this;
        }

        /**
         * Sets the color attribute to be used for the focus ring.
         *
         * @param colorAttr The color attribute resource ID.
         */
        public Builder withColor(@AttrRes int colorAttr) {
            mColor = resolveColorAttr(mContext, colorAttr);
            return this;
        }

        /**
         * Sets the color integer to be used for the focus ring.
         *
         * @param color The color integer.
         */
        public Builder withColorInt(@ColorInt int color) {
            mColor = color;
            return this;
        }

        /**
         * Sets the color resource to be used for the focus ring.
         *
         * @param colorRes The color resource ID.
         */
        public Builder withColorRes(@ColorRes int colorRes) {
            mColor = mContext.getColor(colorRes);
            return this;
        }

        /**
         * Sets the width of the focus outline, in dp.
         *
         * @param dp The outline width in dp.
         */
        public Builder withOutlineWidth(int dp) {
            mOutlineWidthDp = dp;
            return this;
        }

        /**
         * Sets the inset of the focus outline from the calculated bounds, in dp. A larger value
         * moves the ring further inside the view's content area.
         *
         * @param dp The inset value in dp.
         */
        public Builder withInset(int dp) {
            mInsetDp = dp;
            return this;
        }

        /**
         * Builds the final {@link Drawable}. The returned drawable is a {@link StateListDrawable}
         * that will show the focus ring only when the view has focus.
         *
         * @return A {@link StateListDrawable} ready to be set as the view's foreground.
         */
        public Drawable build() {
            // If no specific radii were ever configured, default to a uniform radius.
            if (mCornerRadiiPx == null) {
                withCornerRadius(mCornerRadiusDp);
            }

            Drawable focusedDrawable =
                    new ClippedBoundsOutlineDrawable(
                            mCornerRadiiPx,
                            dpToPx(mContext, mOutlineWidthDp),
                            dpToPx(mContext, mInsetDp),
                            dpToPx(mContext, mHorizontalPaddingAdjustmentDp),
                            dpToPx(mContext, mTopPaddingAdjustmentDp),
                            dpToPx(mContext, mBottomPaddingAdjustmentDp),
                            mColor);
            // In the unfocused state, we draw nothing in the foreground.
            Drawable defaultDrawable = new ColorDrawable(Color.TRANSPARENT);

            StateListDrawable foregroundStateList = new StateListDrawable();
            foregroundStateList.addState(mStateSet, focusedDrawable);
            foregroundStateList.addState(StateSet.WILD_CARD, defaultDrawable);

            return foregroundStateList;
        }
    }

    /**
     * The internal drawable implementation that performs the actual drawing during the focused
     * state. It handles all the clipping and padding logic.
     */
    private static class ClippedBoundsOutlineDrawable extends Drawable {
        private final GradientDrawable mGradientDrawable = new GradientDrawable();
        private final Rect mTempRect = new Rect();
        private final int mInsetPx;
        private final int mHorizontalPaddingAdjustmentPx;
        private final int mTopPaddingAdjustmentPx;
        private final int mBottomPaddingAdjustmentPx;

        ClippedBoundsOutlineDrawable(
                float[] cornerRadii,
                int outlineWidthPx,
                int insetPx,
                int horizontalPaddingAdjustmentPx,
                int topPaddingAdjustmentPx,
                int bottomPaddingAdjustmentPx,
                @ColorInt int outlineColor) {
            mInsetPx = insetPx;
            mHorizontalPaddingAdjustmentPx = horizontalPaddingAdjustmentPx;
            mTopPaddingAdjustmentPx = topPaddingAdjustmentPx;
            mBottomPaddingAdjustmentPx = bottomPaddingAdjustmentPx;

            mGradientDrawable.setShape(GradientDrawable.RECTANGLE);
            mGradientDrawable.setColor(Color.TRANSPARENT); // The stroke is the visible part.
            mGradientDrawable.setStroke(outlineWidthPx, outlineColor);
            mGradientDrawable.setCornerRadii(cornerRadii);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            // This is the core logic. We get the visible portion of the view from the canvas
            // itself at the exact moment of drawing. This is the only reliable way to handle
            // clipping during scrolling.
            if (canvas.getClipBounds(mTempRect)) {
                // Calculate the final bounds by starting with the visible clip rect and then
                // applying the relevant padding, adjustment, and inset values.
                int drawableLeft = mTempRect.left + mHorizontalPaddingAdjustmentPx + mInsetPx;
                int drawableTop = mTempRect.top + mTopPaddingAdjustmentPx + mInsetPx;
                int drawableRight = mTempRect.right - mHorizontalPaddingAdjustmentPx - mInsetPx;
                int drawableBottom = mTempRect.bottom - mBottomPaddingAdjustmentPx - mInsetPx;

                // Only draw if the calculated bounds are valid.
                if (drawableLeft < drawableRight && drawableTop < drawableBottom) {
                    mGradientDrawable.setBounds(
                            drawableLeft, drawableTop, drawableRight, drawableBottom);
                    mGradientDrawable.draw(canvas);
                }
            }
        }

        @Override
        public void setAlpha(int alpha) {
            mGradientDrawable.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            mGradientDrawable.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private static int dpToPx(Context context, int dp) {
        return (int)
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        (float) dp,
                        context.getResources().getDisplayMetrics());
    }

    @ColorInt
    private static int resolveColorAttr(Context context, @AttrRes int colorAttr) {
        TypedArray ta = context.obtainStyledAttributes(new int[] {colorAttr});
        @ColorInt int color = ta.getColor(0, Color.MAGENTA);
        ta.recycle();
        return color;
    }
}
