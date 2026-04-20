/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.core.plot.base.render.point.symbol

import org.jetbrains.letsPlot.commons.geometry.DoubleVector
import org.jetbrains.letsPlot.core.plot.base.render.style.PrimitiveStyles

internal class PlusGlyph(location: DoubleVector, size: Double) : TwoShapeGlyph() {

    init {
        val half = size / 2
        val ox = location.x - half
        val oy = location.y - half
        val hLine = PrimitiveStyles.compiledStyle
            .styleSlimLine(
                start = DoubleVector(0 + ox, half + oy),
                end = DoubleVector(size + ox, half + oy)
            )
            .primitive
        val vLine = PrimitiveStyles.compiledStyle
            .styleSlimLine(
                start = DoubleVector(half + ox, 0 + oy),
                end = DoubleVector(half + ox, size + oy)
            )
            .primitive

        setShapes(hLine, vLine)
    }
}
