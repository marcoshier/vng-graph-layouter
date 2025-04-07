package lib

import ProjectDescription
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.defaultFontMap
import org.openrndr.extra.kdtree.buildKDTree
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.shape.SegmentIntersection

/**
    XPBD-based Graph Layouter
 */

class Graph(val origin: Vector2) {

    val nodes = mutableListOf<GraphNode>()
    val edges = mutableListOf<GraphEdge>()

    var kd = buildKDTree(nodes, 2, ::nodeMapper)

    fun init(data: ProjectDescription) {
        val root = GraphNode(origin, data = data)

        nodes.add(root)
        kd = buildKDTree(nodes, 2, ::nodeMapper)

        populate(root)
    }

    private fun populate(parent: GraphNode) {
        val children = parent.data?.children!!

        for (i in 0 until children.size) {
            val direction = if (parent.depth == -1) {
                Polar(i.toDouble() / children.size * 360.0 + Double.uniform(-20.0, 20.0), 1.0).cartesian
            } else {
                val dir = (parent.position - parent.parent!!.position).normalized * 50.0
                dir.normalized.rotate(10.0 * (i - (children.size / 2.0)))
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

            nodes.add(child)
            edges.add(GraphEdge(parent, child, 60.0))

            parent.children.add(child)

            populate(child)
        }

    }

    val iterations = 5
    val damping = 0.999
    val stiffness = 1.0
    val acceleration = 0.1

    val intersections = mutableListOf<SegmentIntersection>()

    fun rotate(amt: Double) {
        for (node in nodes) {
            node.nextPosition = node.nextPosition.rotate(amt, origin)
            node.direction = node.direction.rotate(amt)
        }
    }

    fun update(dt: Double = 1.0 / iterations) {
        intersections.clear()

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

                        val repulsion = direction.normalized * dt
                        node.nextPosition -= repulsion
                        other.nextPosition += repulsion

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
                node.nextPosition += node.direction * 0.2

                val velocity = (node.position - node.oldPosition) * acceleration
                node.oldPosition = node.position - velocity * damping * (1.0 - dt)
                node.position = node.nextPosition + velocity * damping * dt
                node.smoothPosition = node.smoothPosition.mix(node.position, 0.05)
                node.nextPosition = node.position
            }

        }

    }

    fun draw(drawer: Drawer) {
        drawer.fontMap = defaultFontMap

        for (node in nodes) {
            node.draw(drawer)
        }

        for (edge in edges) {
            edge.draw(drawer)
        }

        for (i in intersections) {
            drawer.fill = ColorRGBa.YELLOW
            drawer.circle(i.position, 3.0)
        }
    }

}