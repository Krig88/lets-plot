/*
 * Copyright (c) 2023. JetBrains s.r.o.
 * Use of this source code is governed by the MIT license that can be found in the LICENSE file.
 */

package org.jetbrains.letsPlot.livemap.chart.gauge

import org.jetbrains.letsPlot.commons.intern.typedGeometry.*
import org.jetbrains.letsPlot.livemap.Client
import org.jetbrains.letsPlot.livemap.Client.Companion.px
import org.jetbrains.letsPlot.livemap.chart.*
import org.jetbrains.letsPlot.livemap.core.ecs.EcsEntity
import org.jetbrains.letsPlot.livemap.mapengine.RenderHelper
import org.jetbrains.letsPlot.livemap.mapengine.placement.WorldOriginComponent

object GaugeLocator : Locator {
    override fun search(coord: Vec<Client>, target: EcsEntity, renderHelper: RenderHelper): HoverObject? {
        if (REQUIRED_COMPONENTS !in target) {
            return null
        }

        val origin = target.get<WorldOriginComponent>().origin
        val gaugeSpec = target.get<GaugeSpecComponent>()
        val chartElement = target.get<ChartElementComponent>()
        val radius = renderHelper.dimToWorld(gaugeSpec.radius * chartElement.scalingSizeFactor)

        val distance = (renderHelper.posToWorld(coord) - origin).length
        if (distance <= radius + renderHelper.dimToWorld(EXTRA_RADIUS)) {
            return HoverObject(
                kind = HoverObjectKind.POINT,
                layerIndex = target.get<IndexComponent>().layerIndex,
                index = target.get<IndexComponent>().index,
                distance = 0.0,
                locator = this,
                targetPosition = renderHelper.worldToPos(origin).toDoubleVector(),
                targetRadius = renderHelper.dimToClient(radius).value,
            )
        }

        return null
    }

    override fun reduce(hoverObjects: Collection<HoverObject>) = hoverObjects.minByOrNull(HoverObject::distance)

    private val REQUIRED_COMPONENTS = listOf(GaugeSpecComponent::class, ChartElementComponent::class)
    private val EXTRA_RADIUS = 6.0.px
}
