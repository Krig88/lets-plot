package org.jetbrains.letsPlot.core.plot.base.render.style

import org.jetbrains.letsPlot.commons.values.FontFamily

fun interface FontOverride {
    fun override(base: FontFamily): FontFamily
}
