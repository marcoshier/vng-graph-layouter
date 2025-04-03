package lib

import ProjectDescription
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.defaultFontMap
import org.openrndr.extra.kdtree.KDTreeNode
import org.openrndr.extra.kdtree.buildKDTree
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.pow

/**
    XPBD-based Graph Layouter
 */

class Graph(val origin: Vector2) {

    val nodes = mutableListOf<Node>()
    val edges = mutableListOf<Edge>()

    var kd: KDTreeNode<Node>

    init {
        nodes.add(Node(origin))
        kd = buildKDTree(nodes, 2, ::nodeMapper)
    }

    private fun addChildren(parent: Node, data: List<String>) {

    }

    fun addChild(parent: Node, data: String, userDirection: Vector2? = null): Node {

        var direction = userDirection ?: (parent.position - (parent.parent?.position ?: origin))

        val newPosition = parent.position + direction.normalized * 100.0
        val influenceRadius = 10.0 //5.0 + (5.0 * (5 - (parent.depth + 1))).pow(2)

        val child = Node(
            newPosition,
            parent.depth + 1,
            influenceRadius,
            label = data,
            parent = parent
        )

        nodes.add(child)
        edges.add(Edge(parent, child, 60.0))

        parent.children.add(child)

        return child
    }

    val iterations = 5
    val damping = 0.999
    val stiffness = 1.0
    val acceleration = 0.1


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

                        val repulsion = direction.normalized * dt
                        node.nextPosition -= repulsion
                        other.nextPosition += repulsion

                        if (distance < radiiSum) {
                            val collision = direction * ((distance - radiiSum) / distance * 0.5)
                            node.nextPosition += collision * (node.influenceRadius * 0.01)
                        }
                    }
                }

                val attraction = if (node.depth == -1) 0.6 else 0.001
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

    }

    fun draw(drawer: Drawer) {
        drawer.fontMap = defaultFontMap

        for (node in nodes) {
            node.draw(drawer)
        }

        for (edge in edges) {
            edge.draw(drawer)
        }
    }

}