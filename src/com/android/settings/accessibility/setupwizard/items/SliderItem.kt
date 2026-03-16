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
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.android.settings.R
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import com.google.android.material.slider.TickVisibilityMode
import com.google.android.setupcompat.partnerconfig.PartnerConfigHelper
import com.google.android.setupdesign.items.Item
import kotlin.math.abs

/** A custom slider item used in Setup Wizard accessibility screens. */
class SliderItem : Item {

    private lateinit var dimensions: SliderDimensions

    // Data class to host track-related dimensions
    private data class SliderDimensions(
        val trackHeight: Int,
        val trackInsideCornerSize: Int,
        val trackStopIndicatorSize: Int,
        val thumbWidth: Int,
        val thumbHeight: Int,
        val thumbElevation: Int,
        val thumbStrokeWidth: Int,
        val thumbTrackGapSize: Int,
        val tickRadius: Int,
    )

    var sliderValue: Int = 0
        set(value) {
            val validated = value.coerceIn(min, max)
            if (field != validated) {
                field = validated
                notifyChanged()
            }
        }

    private var _sliderIncrement: Int = 0
    var sliderIncrement: Int
        get() = _sliderIncrement
        set(value) {
            val validated = (max - min).coerceAtMost(abs(value))
            if (_sliderIncrement != validated) {
                _sliderIncrement = validated
                notifyChanged()
            }
        }

    var min: Int = 0
        set(value) {
            if (field != value) {
                field = value
                // Re-validate sliderValue if min moves above it
                sliderValue = sliderValue
                notifyChanged()
            }
        }

    var max: Int = 100
        set(value) {
            val validated = value.coerceAtLeast(min)
            if (field != validated) {
                field = validated
                // Re-validate sliderValue if max moves below it
                sliderValue = sliderValue
                notifyChanged()
            }
        }

    var iconStartId: Int = 0
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    var iconStartContentDescriptionId: Int = 0
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    var iconEndId: Int = 0
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    var iconEndContentDescriptionId: Int = 0
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    var tickVisible: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    var showSliderValue: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        context.obtainStyledAttributes(attrs, R.styleable.SliderItem).use { a ->
            // The ordering of these two statements are important. If we want to set max first, we
            // need to perform the same steps by changing min/max to max/min as following:
            // mMax = a.getInt(...) and setMin(...).
            min = a.getInt(R.styleable.SliderItem_min, 0)
            max = a.getInt(R.styleable.SliderItem_max, 100)
            sliderIncrement = a.getInt(R.styleable.SliderItem_seekBarIncrement, 0)
            showSliderValue = a.getBoolean(R.styleable.SliderItem_showSliderValue, false)
            iconStartId = a.getResourceId(R.styleable.SliderItem_iconStart, 0)
            iconStartContentDescriptionId =
                a.getResourceId(R.styleable.SliderItem_iconStartContentDescription, 0)
            iconEndId = a.getResourceId(R.styleable.SliderItem_iconEnd, 0)
            iconEndContentDescriptionId =
                a.getResourceId(R.styleable.SliderItem_iconEndContentDescription, 0)
        }
    }

    override fun getDefaultLayoutResource(): Int = R.layout.setup_items_slider

    override fun onBindView(view: View) {
        super.onBindView(view)
        if (!::dimensions.isInitialized) {
            initDimensions(view.context)
        }

        view.isClickable = false
        view.findViewById<Slider>(R.id.slider)?.apply {
            labelBehavior =
                if (showSliderValue) {
                    LabelFormatter.LABEL_FLOATING
                } else {
                    LabelFormatter.LABEL_GONE
                }

            if (sliderIncrement != 0) {
                stepSize = sliderIncrement.toFloat()
                tickVisibilityMode = TickVisibilityMode.TICK_VISIBILITY_AUTO_LIMIT
            } else {
                // If no increment provided, sync model with default slider step
                _sliderIncrement = stepSize.toInt()
            }

            valueFrom = min.toFloat()
            valueTo = max.toFloat()
            value = sliderValue.toFloat()

            clearOnChangeListeners()
            addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    // Update the model only when the user interacts
                    sliderValue = value.toInt()
                }
            }

            if (PartnerConfigHelper.isGlifExpressiveEnabled(context)) {
                applyExpressiveStyles(dimensions)
            }
        }

        updateIconStartIfNeeded(view.findViewById(R.id.icon_start))
        updateIconEndIfNeeded(view.findViewById(R.id.icon_end))
    }

    private fun initDimensions(context: Context) {
        val res = context.resources
        dimensions =
            SliderDimensions(
                trackHeight = res.getDimensionPixelSize(R.dimen.sud_slider_track_height),
                trackInsideCornerSize =
                    res.getDimensionPixelSize(R.dimen.sud_slider_track_inside_corner_size),
                trackStopIndicatorSize =
                    res.getDimensionPixelSize(R.dimen.sud_slider_track_stop_indicator_size),
                thumbWidth = res.getDimensionPixelSize(R.dimen.sud_slider_thumb_width),
                thumbHeight = res.getDimensionPixelSize(R.dimen.sud_slider_thumb_height),
                thumbElevation = res.getDimensionPixelSize(R.dimen.sud_slider_thumb_elevation),
                thumbStrokeWidth = res.getDimensionPixelSize(R.dimen.sud_slider_thumb_stroke_width),
                thumbTrackGapSize =
                    res.getDimensionPixelSize(R.dimen.sud_slider_thumb_track_gap_size),
                tickRadius = res.getDimensionPixelSize(R.dimen.sud_slider_tick_radius),
            )
    }

    private fun Slider.applyExpressiveStyles(dims: SliderDimensions) {
        trackHeight = dims.trackHeight
        trackInsideCornerSize = dims.trackInsideCornerSize
        trackStopIndicatorSize = dims.trackStopIndicatorSize
        thumbWidth = dims.thumbWidth
        thumbHeight = dims.thumbHeight
        thumbElevation = dims.thumbElevation.toFloat()
        thumbStrokeWidth = dims.thumbStrokeWidth.toFloat()
        thumbTrackGapSize = dims.thumbTrackGapSize
        tickActiveRadius = dims.tickRadius
        tickInactiveRadius = dims.tickRadius
    }

    private fun updateIconStartIfNeeded(iconView: ImageView?) {
        val iconFrame = iconView?.parent as? ViewGroup ?: return
        if (iconStartId == 0 || sliderIncrement == 0) {
            iconFrame.visibility = View.GONE
            return
        }

        iconView.setImageResource(iconStartId)
        if (iconStartContentDescriptionId != 0) {
            iconFrame.contentDescription =
                iconFrame.context.getString(iconStartContentDescriptionId)
        }

        iconFrame.setOnClickListener { if (sliderValue > min) sliderValue -= sliderIncrement }
        iconFrame.visibility = View.VISIBLE
    }

    private fun updateIconEndIfNeeded(iconView: ImageView?) {
        val iconFrame = iconView?.parent as? ViewGroup ?: return
        if (iconEndId == 0 || sliderIncrement == 0) {
            iconFrame.visibility = View.GONE
            return
        }

        iconView.setImageResource(iconEndId)
        if (iconEndContentDescriptionId != 0) {
            iconFrame.contentDescription = iconFrame.context.getString(iconEndContentDescriptionId)
        }

        iconFrame.setOnClickListener { if (sliderValue < max) sliderValue += sliderIncrement }
        iconFrame.visibility = View.VISIBLE
    }
}
