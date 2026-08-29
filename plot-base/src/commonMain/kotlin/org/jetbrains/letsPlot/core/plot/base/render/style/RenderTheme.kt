package org.jetbrains.letsPlot.core.plot.base.render.style

data class RenderTheme(
    val paths: PathStylizer? = null,
    val fonts: FontOverride? = null,
    val strokes: StrokeAdjuster? = null,
    val fills: FillDecorator? = null,
) {
    companion object {
        val DEFAULT: RenderTheme = RenderTheme()
    }
}
