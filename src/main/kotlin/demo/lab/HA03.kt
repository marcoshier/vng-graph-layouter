package demo.lab

import Graph
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lib.assignPoints
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.LIGHT_GRAY
import org.openrndr.extra.noise.simplex
import org.openrndr.shape.Circle
import java.io.File
import kotlin.math.pow

fun main() {
    application {
        @Serializable
        class ProjectDescription(
            val text: String,
            val evaluations: Map<String, Int?>,
            val observations: Map<String, String>,
            val dimension: String? = null,
            val children: MutableList<ProjectDescription> = mutableListOf()
        )

        configure {
            width = 1080
            height = 1080
        }

        program {

            val projects = Json.decodeFromString<List<ProjectDescription>>(
                File("data/projects-02.json").readText()
            )

            val graph = Graph(drawer.bounds.center)
            graph.init(projects.first())
            repeat(15) {
                graph.update()
            }

            val nodesByDepth = graph.nodes.groupBy { it.depth }.filter { it.key > 0 }


            val points0 = graph.nodes.map { it.position }
            val points1 = nodesByDepth.keys.flatMap {
                val circle = Circle(graph.circleBounds.center, graph.circleBounds.radius / graph.maxDepth * it)
                circle.contour.equidistantPositions(nodesByDepth[it]!!.size).mapIndexed { j, it ->
                    val n = simplex(123, j * 0.03) * 1.5
                    it.mix(graph.circleBounds.center, n * 0.1)
                }
            }

            val assignment = assignPoints(points0, points1)

            val pointsToNodes = graph.nodes.associateBy { it.position }

            extend {


                val newPositions = assignment.mapNotNull { (p0, p1) ->
                    if (p1 != null) {
                        val p = p0.mix(p1, mouse.position.x / drawer.bounds.width)
                        drawer.stroke = null
                        drawer.fill = ColorRGBa.WHITE
                        drawer.circle(p, 2.0)
                        p0 to p
                    } else null
                }.toMap()

                for ((ogp, np) in newPositions) {
                    drawer.stroke = ColorRGBa.LIGHT_GRAY
                    val node = pointsToNodes[ogp]!!

                    for (c in node.children) {
                        if (newPositions[c.position] != null) {
                            drawer.lineSegment(np, newPositions[c.position]!!)
                        }
                    }
                }

            }
        }
    }
}