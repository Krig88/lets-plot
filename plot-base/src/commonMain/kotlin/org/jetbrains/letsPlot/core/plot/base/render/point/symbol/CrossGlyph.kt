/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.base.render.point.symbol

import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.core.plot.base.render.style.PrimitiveStyles
import kotlin.jvm.JvmOverloads
import kotlin.math.PI
import kotlin.math.cos

internal class CrossGlyph @JvmOverloads constructor(location: DoubleVector, size: Double, inscribedInCircle: Boolean = true) : TwoShapeGlyph() {

    init {
        val cx = location.x
        val cy = location.y
        val w = if (inscribedInCircle)
            size * CIRCLE_WIDTH_ADJUST_RATIO
        else
            size
        val half = w / 2 // half width of inner square

        val backSlashLine = PrimitiveStyles.compiledStyle
            .styleSlimLine(
                start = DoubleVector(cx - half, cy - half),
                end = DoubleVector(cx + half, cy + half)
            )
            .primitive
        val slashLine = PrimitiveStyles.compiledStyle
            .styleSlimLine(
                start = DoubleVector(cx - half, cy + half),
                end = DoubleVector(cx + half, cy - half)
            )
            .primitive

        setShapes(backSlashLine, slashLine)
    }

    companion object {
        val CIRCLE_WIDTH_ADJUST_RATIO = cos(PI / 4)   // cos(45)
    }
}
