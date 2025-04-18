package demo

import Graph
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.defaultFontMap
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.color.presets.PURPLE
import org.openrndr.extra.color.presets.TURQUOISE
import org.openrndr.extra.noise.simplex
import org.openrndr.math.smoothstep
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Segment2D
import traverse
import java.io.File
import kotlin.math.pow


fun main() {
    application {
        configure {
            width = 1280
            height = 1280
        }

        @Serializable
        class ProjectDescription(
            val text: String,
            val evaluations: Map<String, Int?>,
            val observations: Map<String, String>,
            val dimension: String? = null,
            val children: MutableList<ProjectDescription> = mutableListOf()
        )

        program {

          //  extend(ScreenRecorder())

            extend(Camera2D())

            val projects = Json.decodeFromString<List<ProjectDescription>>(
                File("data/projects-02.json").readText()
            )

            val graph = Graph(drawer.bounds.center)
            graph.init(projects.first())

            keyboard.character.listen {
                graph.update()
            }

            extend {


                drawer.fontMap = defaultFontMap

                drawer.stroke = ColorRGBa.WHITE

                for (node in graph.nodes) {
                    drawer.stroke = null
                    drawer.fill = ColorRGBa.WHITE
                    drawer.circle(node.smoothPosition, 2.0)
                }

                for (edge in graph.edges) {
                    drawer.stroke = ColorRGBa.WHITE
                    drawer.fill = null
                    drawer.segment(edge.segment)
                }

            }
        }
    }
}