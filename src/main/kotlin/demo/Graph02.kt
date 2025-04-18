package demo

import Graph
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.color.presets.PURPLE
import org.openrndr.extra.color.presets.TURQUOISE
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.smoothstep
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Segment2D
import traverse
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
                    //rotate(Double.uniform(0.0, 360.0))
                }
            }

            val grid = drawer.bounds.grid(2, 2).flatten()

            extend {


                for ((i, graph) in graphs.withIndex()) {
                    drawer.isolated {
                        drawer.translate(grid[i].corner)
                        drawer.scale(0.5)
                        graph.update()

                        for (node in graph.nodes) {
                            traverse(node) {
                                if (it.children.isNotEmpty()) {
                                    val mid = LineSegment(it.children.first().position, it.children.last().position).position(0.5)

                                    if (it.children.size == 1) {
                                        val positions = Segment2D(it.position, it.children[0].position).equidistantPositions(20)
                                        drawer.circles {
                                            for ((i, p) in positions.withIndex()) {
                                                val t = smoothstep(0.0, 0.8, i.toDouble() / positions.size)
                                                this.fill = ColorRGBa.BLACK.mix(ColorRGBa.TURQUOISE, t)
                                                this.circle(p, t)
                                            }
                                        }
                                        return@traverse
                                    }

                                    for (i in 0 until it.children.size) {
                                        val next = if (i < it.children.size - 1) it.children[i + 1].position else it.children[i].position * 2.0 - it.children[i - 1].position
                                        val c0 = Segment2D(it.position, mid).position(0.5)
                                        val c1 =  Segment2D(it.children[i].position, next).normal(0.5) * -10.0 * (0.5) + it.children[i].position

                                        val positions = Segment2D(it.position, c0, c1, it.children[i].position).equidistantPositions(20)
                                        drawer.stroke = null
                                        drawer.circles {
                                            for ((i, p) in positions.withIndex()) {
                                                val t = smoothstep(0.0, 0.8, i.toDouble() / positions.size)
                                                this.fill = ColorRGBa.BLACK.mix(ColorRGBa.TURQUOISE, t)
                                                this.circle(p, t)
                                            }
                                        }


                                    }
                                }
                            }
                        }

//        for (edge in edges) {
//            edge.draw(drawer)
//        }

                        drawer.stroke = ColorRGBa.PURPLE
                        drawer.fill = null
                        drawer.circle(graph.circleBounds)
                    }
                }


            }
        }
    }
}