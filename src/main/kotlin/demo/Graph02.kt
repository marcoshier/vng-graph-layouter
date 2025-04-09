package demo

import Graph
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openrndr.application
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.primitives.grid
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

            val projects0 = Json.decodeFromString<List<ProjectDescription>>(
                File("data/projects-02.json").readText()
            )
            val projects1 = Json.decodeFromString<List<ProjectDescription>>(
                File("data/projects-02.json").readText()
            )

            val allProjects = listOf(projects0, projects1, projects1, projects0)

            val graphs = allProjects.map {
                val graph = Graph(drawer.bounds.center)
                graph.apply {
                    init(it.first())
                    rotate(Double.uniform(0.0, 360.0))
                }
            }

            val grid = drawer.bounds.grid(2, 2).flatten()

            extend {


                for ((i, graph) in graphs.withIndex()) {
                    drawer.isolated {
                        drawer.translate(grid[i].corner)
                        drawer.scale(0.5)
                        graph.update()
                        graph.draw(drawer)
                    }
                }


            }
        }
    }
}