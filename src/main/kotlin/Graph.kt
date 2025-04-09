import lib.PVector
import micycle.pgs.PGS_PointSet.weightedMedian
import micycle.pgs.commons.FarthestPointPair
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.defaultFontMap
import org.openrndr.extra.color.presets.PURPLE
import org.openrndr.extra.color.presets.TURQUOISE
import org.openrndr.extra.kdtree.buildKDTree
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.listParameters
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.smoothstep
import org.openrndr.shape.Circle
import org.openrndr.shape.ContourIntersection
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Segment2D
import kotlin.collections.get
import kotlin.reflect.full.memberProperties

/**
    XPBD-based Graph Layouter
 */

class Graph(val origin: Vector2) {

    val nodes = mutableListOf<GraphNode>()
    val edges = mutableListOf<GraphEdge>()

    var kd = buildKDTree(nodes, 2, ::nodeMapper)

    var circleBounds = Circle(origin, 10E4)

    fun init(data: Any) {
        val root = GraphNode(origin, data = data)

        nodes.add(root)
        kd = buildKDTree(nodes, 2, ::nodeMapper)

        populate(root)
//        repeat(iterations * 60) {
//            update()
//        }

    }

    private fun populate(parent: GraphNode) {
        if (parent.data == null) return
        val childrenProp = parent.data!!::class.memberProperties.firstOrNull { it.name == "children" } as? kotlin.reflect.KProperty1<Any, *>
        val children = childrenProp?.get(parent.data!!) as List<*>

        for (i in 0 until children.size) {
            val direction = if (parent.depth == -1) {
                Polar(i.toDouble() / children.size * 360.0 + Double.uniform(-20.0, 20.0), 1.0).cartesian
            } else {
                val dir = (parent.position - parent.parent!!.position).normalized * 50.0
                dir.normalized.rotate((5.0 * (i - (children.size / 2.0))) * parent.children.size * 2.0)
            }

            val position = parent.position + direction * 100.0
            val child = GraphNode(
                position,
                parent.depth + 1,
                20.0,
                direction,
                children[i],
                parent
            )

            val edge = GraphEdge(parent, child, 10.0)

            nodes.add(child)
            edges.add(edge)

            parent.children.add(child)

            populate(child)
        }

    }

    fun findCircleBounds(): Circle {
        val ppositions = nodes.map { PVector(it.position) }
        val median = weightedMedian(ppositions)
        val centroid = lib.Vector2(median)
        val radius = (centroid - lib.Vector2(FarthestPointPair(ppositions).either())).length

        return Circle(centroid, radius)
    }

    fun branches(node: GraphNode = nodes.first()): List<List<GraphNode>> {
        val paths = mutableListOf<List<GraphNode>>()

        if (node.isLeaf) {
            paths.add(listOf(node))
            return paths
        }

        for (child in node.children) {
            for (childPath in branches(child)) {
                val fullPath = mutableListOf<GraphNode>()
                fullPath.add(node)
                fullPath.addAll(childPath)
                paths.add(fullPath)
            }
        }

        return paths
    }

    val iterations = 5
    val damping = 0.999
    val stiffness = 1.0
    val acceleration = 0.1

    var intersections = mutableListOf<ContourIntersection>()

    fun rotate(amt: Double) {
        for (node in nodes) {
            node.nextPosition = node.nextPosition.rotate(amt, origin)
            node.direction = node.direction.rotate(amt)
        }
    }

    fun update(dt: Double = 1.0 / iterations) {
        repeat(iterations) {
            kd = buildKDTree(nodes, 2, ::nodeMapper)

            for (node in nodes) {
                val inRange = kd.findAllInRadius(node, node.influenceRadius * 2.0)

                for (other in inRange) {
                    val pos = node.position
                    val otherPos = other.position
                    val direction = (otherPos - pos)
                    val distance = direction.length
                    val radiiSum = node.influenceRadius + other.influenceRadius

                    if (distance > 0.0) {

                        /*val repulsion = direction.normalized * dt * 1.5
                        node.nextPosition -= repulsion
                        other.nextPosition += repulsion*/

                        if (distance < radiiSum) {
                            val collision = direction * ((distance - radiiSum) / distance * 0.5)
                            node.nextPosition += collision * (node.influenceRadius * 0.01)
                        }
                    }
                }

                val attraction = if (node.depth == -1) 1.6 else 0.0
                node.nextPosition += (origin - node.position) * attraction * dt
            }


             for (edge in edges) {
                 val diff = edge.a.position - edge.b.position

                 if (diff.length > 0.0) {
                     val force = (diff * ((diff.length - edge.targetLength) / diff.length * dt)) * stiffness
                     edge.a.nextPosition -= force
                     edge.b.nextPosition += force
                 }
             }


            for (node in nodes) {

                val velocity = (node.position - node.oldPosition) * acceleration
                node.oldPosition = node.position - velocity * damping * (1.0 - dt)
                node.position = node.nextPosition + velocity * damping * dt
                node.smoothPosition = node.smoothPosition.mix(node.position, 0.05)
                node.nextPosition = node.position
            }

        }

        circleBounds = findCircleBounds()
    }

    fun draw(drawer: Drawer) {
        drawer.fontMap = defaultFontMap

        drawer.stroke = ColorRGBa.WHITE
        for (node in nodes) {
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
        drawer.circle(circleBounds)
    }

}