package demo

import Graph
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toContour
import java.io.File
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

                graph.update()
                //graph.draw(drawer)

                drawer.stroke = ColorRGBa.YELLOW
                for (b in graph.branches) {
                    val catmullRom = b.map { it.smoothPosition }.catmullRom(closed = false)
                    val smoothContour = catmullRom.toContour()

                    drawer.contour(smoothContour)
                }

            }
        }
    }
}