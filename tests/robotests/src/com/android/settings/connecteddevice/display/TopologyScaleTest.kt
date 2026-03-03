/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.display

import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Assert as many points as possible in a single assert since a tweak can change every assertion
// in a small way and we don't want to re-compile and re-test for *every* individual float.
fun assertPointF(comparisons: List<Pair<PointF, PointF>>, delta: Float) {
    val errors = StringBuilder()
    comparisons.forEachIndexed { i, (a, b) ->
        if (abs(b.x - a.x) > delta) {
            errors.append("x value at index $i - ${a.x} != ${b.x}\n")
        }
        if (abs(b.y - a.y) > delta) {
            errors.append("y value at index $i - ${a.y} != ${b.y}\n")
        }
    }
    assertEquals("", errors.toString())
}

@RunWith(RobolectricTestRunner::class)
class TopologyScaleTest {
    @Test
    fun oneDisplay4to3Aspect() {
        val scale =
            TopologyScale(
                paneWidth = 640,
                minEdgeLength = 48f,
                maxEdgeLength = 64f,
                paddingTop = 24,
                paddingBottom = 24,
                listOf(RectF(0f, 0f, 640f, 480f)),
            )

        // blockRatio is set in order to make the smallest display edge (480 dp) 48dp in the pane
        // Ratio=0.1 (minEdge/minDim=48/480). Height=Ratio*480 + 24 + 24 = 48 + 48 = 96.
        // OriginY = paddingTop - top*ratio = 24 - 0 = 24.
        assertEquals(
            "{TopologyScale blockRatio=0.100000 originPaneXY=288.0,24.0 paneHeight=96.0}",
            "" + scale,
        )

        assertPointF(
            listOf(
                // x: 640*0.1 + 288 = 352. y: 480*0.1 + 24 = 72.
                PointF(352f, 72f) to scale.displayToPaneCoor(640f, 480f),
                // x: 320*0.1 + 288 = 320. y: 240*0.1 + 24 = 48.
                PointF(320f, 48f) to scale.displayToPaneCoor(320f, 240f),
                // x: (352-288)/0.1 = 640. y: (96-24)/0.1 = 720.
                PointF(640f, 720f) to scale.paneToDisplayCoor(352f, 96f),
            ),
            0.001f,
        )
    }

    @Test
    fun twoUnalignedDisplays() {
        val scale =
            TopologyScale(
                paneWidth = 300,
                minEdgeLength = 48f,
                maxEdgeLength = 96f,
                paddingTop = 84,
                paddingBottom = 84,
                listOf(RectF(0f, 0f, 1920f, 1200f), RectF(1920f, -300f, 3840f, 900f)),
            )

        // Ratio=(300 * 0.6) / 3840 = 0.046875.
        // Pane Height = 70.3125 + 84 + 84 = 238.3125.
        assertEquals(
            "{TopologyScale blockRatio=0.046875 originPaneXY=60.0,98.1 paneHeight=238.3}",
            "" + scale,
        )

        assertPointF(
            listOf(
                // x: 400 * 0.046875 + 60 = 18.75 + 60 = 78.75
                // y: 400 * 0.046875 + 98.0625 = 18.75 + 98.0625 = 116.8125
                PointF(78.75f, 116.8125f) to scale.displayToPaneCoor(400f, 400f),
                // x: -400 * 0.046875 + 60 = -18.75 + 60 = 41.25
                // y: 0 * 0.046875 + 98.0625 = 98.0625
                PointF(41.25f, 98.0625f) to scale.displayToPaneCoor(-400f, 0f),
                // x: (42 - 60) / 0.046875 = -18 / 0.046875 = -384
                // y: (42 - 98.0625) / 0.046875 = -56.0625 / 0.046875 = -1196
                PointF(-384f, -1196f) to scale.paneToDisplayCoor(42f, 42f),
            ),
            0.001f,
        )
    }

    @Test
    fun twoDisplaysBlockRatioBumpedForGarSizeMinimumHorizontal() {
        val scale =
            TopologyScale(
                paneWidth = 192,
                minEdgeLength = 48f,
                maxEdgeLength = 64f,
                paddingTop = 84,
                paddingBottom = 84,
                listOf(RectF(0f, 0f, 240f, 320f), RectF(-240f, -320f, 0f, 0f)),
            )

        // blockRatio is higher than 0.05 in order to make the smallest display edge (240 dp) 48dp
        // in the pane.
        // Ratio=0.2 (minEdge/minDim=48/240). Height=0.2*640 + 84 + 84 = 128+168 = 296.
        assertEquals(
            "{TopologyScale blockRatio=0.200000 originPaneXY=96.0,148.0 paneHeight=296.0}",
            "" + scale,
        )

        assertPointF(
            listOf(
                // x: 480*0.2 + 96 = 192. y: 640*0.2 + 148 = 276.
                PointF(192f, 276f) to scale.displayToPaneCoor(480f, 640f),
                // x: 0*0.2 + 96 = 96. y: -320*0.2 + 148 = 84.
                PointF(96f, 84f) to scale.displayToPaneCoor(0f, -320f),
                // x: (140-96)/0.2 = 220. y: (42-148)/0.2 = -530.
                PointF(220f, -530f) to scale.paneToDisplayCoor(140f, 42f),
            ),
            0.001f,
        )
    }

    @Test
    fun paneVerticalPaddingSetByMinEdgeLength() {
        val scale =
            TopologyScale(
                paneWidth = 300,
                minEdgeLength = 48f,
                maxEdgeLength = 80f,
                paddingTop = 84,
                paddingBottom = 84,
                listOf(
                    RectF(0f, 0f, 640f, 480f),
                    RectF(0f, 480f, 640f, 960f),
                    RectF(0f, 960f, 640f, 1440f),
                    RectF(0f, 1440f, 640f, 1920f),
                    RectF(0f, 1920f, 640f, 2400f),
                    RectF(0f, 2400f, 640f, 2880f),
                ),
            )

        // Ratio=0.125 (maxEdge/maxDim=80/640). Height=0.125*2880 + 84 + 84 = 360+168 = 528.
        assertEquals(
            "{TopologyScale blockRatio=0.125000 originPaneXY=110.0,84.0 paneHeight=528.0}",
            "" + scale,
        )
        assertPointF(
            listOf(
                // x: 320*0.125 + 110 = 150. y: 0*0.125 + 84 = 84.
                PointF(150f, 84f) to scale.displayToPaneCoor(320f, 0f),
                // x: (100-110)/0.125 = -80. y: (336-84)/0.125 = 2016.
                PointF(-80f, 2016f) to scale.paneToDisplayCoor(100f, 336f),
            ),
            0.001f,
        )
    }

    @Test
    fun limitedByCustomMaxBlockRatio() {
        val scale =
            TopologyScale(
                paneWidth = 300,
                minEdgeLength = 24f,
                maxEdgeLength = 77f,
                paddingTop = 42,
                paddingBottom = 42,
                listOf(RectF(0f, 0f, 640f, 480f), RectF(0f, 480f, 640f, 960f)),
            )

        // Ratio=0.1203125 (77/640). Height=Ratio*960 + 42 + 42 = 115.5+84 = 199.5.
        assertEquals(
            "{TopologyScale blockRatio=0.120312 originPaneXY=111.5,42.0 paneHeight=199.5}",
            "" + scale,
        )
        assertPointF(
            listOf(
                // x: 320*Ratio + 111.5 = 150. y: 0 + 42 = 42.
                PointF(150f, 42.0f) to scale.displayToPaneCoor(320f, 0f),
                // x: (100-111.5)/Ratio = -95.58... y: (336-42)/Ratio = 2443.636...
                PointF(-95.58442f, 2443.6364f) to scale.paneToDisplayCoor(100f, 336f),
            ),
            0.001f,
        )
    }

    @Test
    fun largeCustomMinEdgeLength() {
        // minBlockEdgeLength/minDisplayEdgeLength = 80/480 = 1/6, so the block ratio will be 1/6
        val scale =
            TopologyScale(
                paneWidth = 300,
                minEdgeLength = 80f,
                maxEdgeLength = 100f,
                paddingTop = 140,
                paddingBottom = 140,
                listOf(RectF(0f, 0f, 640f, 480f), RectF(0f, 480f, 640f, 960f)),
            )

        // Ratio=1/6. Height=(1/6)*960 + 140 + 140 = 160 + 280 = 440.
        assertEquals(
            "{TopologyScale blockRatio=0.166667 originPaneXY=96.7,140.0 paneHeight=440.0}",
            "" + scale,
        )
        assertPointF(
            listOf(
                // x: 320*(1/6) + 96.666 = 150. y: 0 + 140 = 140.
                PointF(150f, 140f) to scale.displayToPaneCoor(320f, 0f),
                // x: (100-96.666)*6 = 20. y: (336-140)*6 = 1176.
                PointF(20f, 1176f) to scale.paneToDisplayCoor(100f, 336f),
            ),
            0.001f,
        )
    }
}
