package demo

import Graph
import kotlinx.serialization.json.Json
import org.openrndr.application
import org.openrndr.extra.camera.Camera2D
import java.io.File

fun main() {
    application {
        configure {
            width = 1280
            height = 1280
        }

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

               // graph.update()
                graph.draw(drawer)
            }
        }
    }
}