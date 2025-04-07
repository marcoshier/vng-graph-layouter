package lab

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.LIGHT_GRAY
import org.openrndr.extra.noise.shapes.uniform
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.ffmpeg.ScreenRecorder

fun main() {
    application {
        configure {
            width = 1200
            height = 1200
        }

        program {

            extend(ScreenRecorder())

            val shape = hobbyCurve((0..10).map { drawer.bounds.offsetEdges(-200.0).uniform() }, true)

            val gridPoints = drawer.bounds.grid(30, 30).flatten().map { it.center }
            val shapePoints = shape.equidistantPositions(30 * 30)

            val assign = assignPoints(gridPoints, shapePoints)

            extend {
                drawer.stroke = null
                drawer.fill = ColorRGBa.WHITE.opacify(0.2)
                drawer.circles(gridPoints, 2.0)

                drawer.fill = ColorRGBa.LIGHT_GRAY.opacify(0.2)
                drawer.circles(shapePoints, 2.0)

                for ((first, second) in assign) {
                    if (second != null) {
                        drawer.fill = ColorRGBa.YELLOW
                        drawer.circle(first.mix(second, mouse.position.x / width), 3.0)
                    }
                }

            }
        }
    }
}