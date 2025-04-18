package demo

import Graph
import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lib.Vector2
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.imageFit.fit
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.noise.simplex1D
import org.openrndr.extra.noise.simplex2D
import org.openrndr.extra.noise.simplex3D
import org.openrndr.extra.noise.withVector2Input
import org.openrndr.extra.noise.withVector2Output
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.launch
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.smoothstep
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.bounds
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


fun main() {
    application {
        configure {
            width = 1080
            height = 1080
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

            //   extend(ScreenRecorder())

            val projects = Json.decodeFromString<List<ProjectDescription>>(
                File("data/projects-02.json").readText()
            )

            val colorMap = listOf(
                "Embodying" to "#FFC5C9",
                "Learning" to "#CC9EA1",
                "Imagining" to "#F85057",
                "Caring" to "#97ECC3",
                "Organising" to "#07E77C",
                "Inspiring" to "#12AF46",
                "Co-creating" to "#D1A6F4",
                "Empowering" to "#9230E0",
                "Subverting" to "#8603B0"
            ).associate { it.first to ColorRGBa.fromHex(it.second) }

            val graphBounds = drawer.bounds.offsetEdges(-150.0)

            val graph = Graph(graphBounds.center)

            graph.init(projects.first())



            class NodeState: Animatable() {
                var t = 0.0

                val visible: Boolean
                    get() = t > 0.0

                fun reveal() {
                    ::t.animate(1.0, 1000, Easing.CubicInOut, 1000)
                }

                fun update() {
                    updateAnimation()
                }
            }

            val states = mutableMapOf<Int, NodeState>()
            val initialNodes = graph.nodes.filter { it.depth < 1 }

            initialNodes.forEach {
                states[it.id] = NodeState()//.apply { reveal() }
            }

            for (node in graph.nodes - initialNodes) {
                states[node.id] = NodeState()
            }

            var viewBox = graph.nodes.map { it.smoothPosition }.bounds.scaledBy(0.5)
            var oldViewBox = viewBox

            var zoomStart = System.currentTimeMillis()

            var zoomTargetId = -1
            var allTargets = listOf<Int>()

            var zoom = 0.0
            var zoomed = false
            var zooming = false

            var drawnBranches = mutableMapOf<Int, ShapeContour>()

            fun reveal(id: Int) {
                val children = graph.nodesById[id]!!.children
                for (c in children) {
                    states[c.id]?.reveal()
                }
            }

            fun revealAll() {

                suspend fun visit(id: Int) {
                    val node = graph.nodesById[id]!!
                    states[id]?.reveal()
                    for (i in 0 until node.depth + 1) {
                        yield()
                    }

                    for (child in node.children) {
                        visit(child.id)
                    }
                }

                val initial = graph.nodes.filter { it.depth < 2 }
                for (n in initial) {
                    launch {
                        visit(n.id)
                    }
                }

            }

            fun focus(id: Int) {
                zoomStart = System.currentTimeMillis()
                zoomTargetId = id

                val children = graph.nodesById[id]!!.children

                allTargets = children.map { it.id }
                oldViewBox = viewBox

                launch {
                    while (System.currentTimeMillis() - zoomStart < 2000) {
                        zooming = true
                        zoom = (System.currentTimeMillis() - zoomStart) / 2000.0
                        window.requestDraw()
                        yield()
                    }
                    zoomed = true
                    zooming = false
                }
            }

            fun unfocus() {
                oldViewBox = drawer.bounds.sub(0.0, 0.0, 0.5, 1.0)
                zoomStart = System.currentTimeMillis()
                launch {
                    while (System.currentTimeMillis() - zoomStart < 2000) {
                        zooming = true
                        zoom = 1.0 - ((System.currentTimeMillis() - zoomStart) / 2000.0)
                        window.requestDraw()
                        yield()
                    }

                    zoomTargetId = -1
                    allTargets = listOf()
                    zoomed = false
                    zooming = false
                }

            }

            val vbs = graph.edges.associate {
                it.b.id to vertexBuffer(vertexFormat {
                    position(3)
                    color(4)
                }, 500)
            }

            val camera = extend(Camera2D())

            revealAll()
            extend {
                var t = 0.0
                if (seconds > 5.0) {
                    t = ((seconds - 5.0) / 5.0).coerceAtMost(1.0)
                }

                graph.displace {
                    val f = simplex3D.withVector2Output()
                    f(123, it.position.x * 0.005, it.position.y * 0.005, seconds * 0.05)
                }
                graph.update()


                drawer.clear(ColorRGBa.fromHex("0D0D27"))


                for (state in states.values) {
                    state.update()
                }

                drawer.isolated {
                    drawnBranches.clear()

                    viewBox = if (zoomTargetId == -1) drawer.bounds else allTargets.map {
                        graph.nodesById[it]!!.smoothPosition
                    }.bounds.scaledBy(1.5)

                    drawer.ortho()
                    drawer.stroke = ColorRGBa.YELLOW
                    camera.view = drawer.bounds.fit( viewBox ).inversed


                    drawer.strokeWeight = if (!zoomed) 1.0 - zoom.coerceAtMost(0.99) else 0.01
                    drawer.stroke = ColorRGBa.WHITE.opacify(0.5)

                    for (branch in graph.branches) {
                        val smoothContour = hobbyCurve(branch.map { it.smoothPosition }).reversed

                        for (node in branch) {
                            val state = states[node.id]!!
                            val data = node.data as ProjectDescription?
                            if (state.visible && data != null) {
                                if (node.parent == null || drawnBranches[node.id] != null) continue

                                val t0 = smoothContour.nearest(node.smoothPosition).contourT
                                val t1 = smoothContour.nearest(node.parent!!.smoothPosition).contourT

                                val trim = smoothContour.sub(t0, t1)

                                drawer.isolated {
                                    // drawer.defaults()

                                    drawer.shadeStyle = shadeStyle {
                                        fragmentTransform = "x_fill = va_color;"
                                    }

                                    val contour = trim.sub(0.0, state.t * 0.98)

                                    // https://openrndr.discourse.group/t/drawing-per-vertex-colored-meshes-and-variable-thickness-lines/266/3

                                    vbs[node.id]!!.put {
                                        for (i in 0 until 250) {
                                            val t = i / 249.0
                                            val targetColor = data.dimension?.let { colorMap[it] } ?: run { ColorRGBa.WHITE }
                                            val color = ColorRGBa.TRANSPARENT.mix(targetColor, smoothstep(0.0, 0.7, t))
                                            val pos = contour.position(t)
                                            val normal = contour.normal(t).normalized * (1.0 - 1.0 * cos(t * PI * 2).smoothstep(0.9, 1.0))

                                            write((pos + normal).vector3(z = 0.0))
                                            write(color)
                                            write((pos - normal).vector3(z = 0.0))
                                            write(color)
                                        }
                                    }

                                    drawer.vertexBuffer(vbs[node.id]!!, DrawPrimitive.TRIANGLE_STRIP)
                                    drawer.shadeStyle = null
                                }


                                drawnBranches[node.id] = trim
                            }
                        }
                    }


                    drawer.stroke = null
                    drawer.fill = ColorRGBa.WHITE
                    drawer.fontMap = loadFont("data/fonts/default.otf", 2.0, contentScale = 32.0)


                    for (n in graph.nodes) {
                        val state = states[n.id]!!
                        if (state.visible) {
                            val data = n.data as ProjectDescription?
                            val observations = data?.observations["difference-keywords"]?.split("\n")
                            observations?.let {
                                for ((i, o) in it.withIndex()) {
                                    val theta = i.toDouble() / it.size * 360.0
                                    val targetPos = Polar(theta, o.length * 1.2).cartesian + n.smoothPosition
                                    var c = LineSegment(n.smoothPosition, targetPos).contour
                                    var text = ""

                                    if (theta in 90.0..270.0) {
                                        c = c.reversed.sub(0.0, 0.9)
                                    } else {
                                        c = c.sub(0.2, 1.0)
                                    }

                                    /*   drawer.textOnContour(
                                           o.take((state.t * o.length).toInt()), c.rectified(), 0.0
                                       )*/
                                }
                            }

                            drawer.fill = ColorRGBa.WHITE
                            drawer.circle(n.smoothPosition, 1.0 * state.t)
                        }
                    }


                }

            }
        }
    }
}