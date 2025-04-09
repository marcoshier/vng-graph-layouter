package demo.lab

import lib.Vector2
import lib.assignPoints
import micycle.pgs.PGS_PointSet.phyllotaxis
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.LIGHT_GRAY
import org.openrndr.extra.noise.shapes.uniform
import org.openrndr.ffmpeg.ScreenRecorder

fun main() {
    application {
        configure {
            width = 1200
            height = 1200
        }

        program {

            extend(ScreenRecorder())

            val gridPoints = (0 until 30 * 30).map { drawer.bounds.offsetEdges(-250.0).uniform() }
            val shapePoints = phyllotaxis(width/ 2.0, height / 2.0, 30 * 30, width / 2.0 - 30.0, 60.0).map { Vector2(it) }

            val assign = assignPoints(gridPoints, shapePoints)

            extend {
                drawer.stroke = null
                drawer.fill = ColorRGBa.WHITE.opacify(0.2)
              //  drawer.circles(gridPoints, 2.0)

                drawer.fill = ColorRGBa.LIGHT_GRAY.opacify(0.2)
             //   drawer.circles(shapePoints, 2.0)

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