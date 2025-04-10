package demo

import Graph
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.color.spaces.ColorOKHSLa
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toContour
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.shape.ShapeContour
import java.io.File
import kotlin.random.Random


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
                graph.update()


                val drawnBranches = mutableMapOf<Int, Boolean>()
                drawer.stroke = ColorRGBa.WHITE.opacify(0.5)
                for ((i, branch) in graph.branches.withIndex()) {
                    val smoothContour = hobbyCurve(branch.map { it.smoothPosition })

                    for ((j, node) in branch.withIndex()) {
                        if (node.parent == null || drawnBranches[node.id] == true) continue

                        drawnBranches[node.id] = true

                        val t0 = smoothContour.nearest(node.smoothPosition).contourT
                        val t1 = smoothContour.nearest(node.parent.smoothPosition).contourT

                        val trim = smoothContour.sub(t0, t1)
                        drawer.stroke = ColorRGBa.RED.shiftHue<ColorOKHSLa>((i + j) * 10.0).opacify(0.3)
                        drawer.contour(trim)
                    }

                    //drawer.contour(smoothContour)
                }

             /*   for ((i, n) in graph.nodes.withIndex()) {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.RED.shiftHue<ColorOKHSLa>(Double.uniform(0.0, 360.0, Random(n.id))).opacify(0.5)
                    drawer.circle(n.smoothPosition, n.influenceRadius)
                }

                drawer.stroke = ColorRGBa.WHITE
                drawer.segments(graph.edges.map { it.segment })
*/

            }
        }
    }
}