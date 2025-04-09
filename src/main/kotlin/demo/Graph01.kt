package demo

import Graph
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openrndr.application
import org.openrndr.extra.camera.Camera2D
import java.io.File

@Serializable
class ProjectDescription(
    val text: String,
    val evaluations: Map<String, Int?>,
    val observations: Map<String, String>,
    val dimension: String? = null,
    val children: MutableList<ProjectDescription> = mutableListOf()
)

fun main() {
    application {
        configure {
            width = 1280
            height = 1280
        }

        program {

          //  extend(ScreenRecorder())

            extend(Camera2D())

            val projects = Json.decodeFromString<List<ProjectDescription>>(
                File("data/projects-02.json").readText()
            )

            val graph = Graph(drawer.bounds.center)
            graph.init(projects.first())

            extend {

                graph.update()
                graph.draw(drawer)

            }
        }
    }
}