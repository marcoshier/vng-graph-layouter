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

                val targetPoints = (1 until graph.maxDepth).flatMap {
                    val circle = Circle(graph.circleBounds.center, graph.circleBounds.radius / graph.maxDepth * (it * 0.4).pow(2.2))
                    circle.contour.equidistantPositions(50 * it).mapIndexed { j, it ->
                        val n = simplex(123, j * 0.03) * 1.5
                        it.mix(graph.circleBounds.center, n * 0.1)
                    }
                }

                drawer.stroke = null
                drawer.fill = ColorRGBa.WHITE.opacify(0.5)
                drawer.circles(targetPoints, 1.0)

            }
        }
    }
}