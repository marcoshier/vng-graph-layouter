import lib.PVector
import micycle.pgs.PGS_PointSet.weightedMedian
import micycle.pgs.commons.FarthestPointPair
import org.openrndr.extra.kdtree.buildKDTree
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Segment2D
import org.openrndr.shape.intersections
import kotlin.math.max
import kotlin.reflect.full.memberProperties

/**
    XPBD-based Graph Layouter
 */

class Graph(val origin: Vector2) {

    val nodes = mutableListOf<GraphNode>()
    val edges = mutableListOf<GraphEdge>()

    val nodesById = mutableMapOf<Int, GraphNode>()

    private var kd = buildKDTree(nodes, 2, ::nodeMapper)

    var circleBounds = Circle(origin, 10E4)
    var branches = listOf<List<GraphNode>>()

    var iterations = 5
    val damping = 0.999
    val stiffness = 1.0
    val acceleration = 0.1

    fun init(data: Any) {
        val root = GraphNode(origin, data = data)

        nodes.add(root)
        nodesById[root.id] = root

        kd = buildKDTree(nodes, 2, ::nodeMapper)

        populate(root)

       // repeat(15) { update() }


        for (node in nodes ){
            node.influenceRadius = 20.0
        }

        for (edge in edges) {
            edge.targetLength = 50.0
            // when(edge.depth) {
            //                -1 -> 30.0
            //                0 -> 50.0
            //                1 -> 70.0
            //                2 -> 45.0
            //                3 -> 30.0
            //                4 -> 15.0
            //                else -> 7.0
            //            }
        }

//   update()

        branches = findBranches()
    }

    private var nodeId = 0
    var maxDepth = -1

    private fun populate(parent: GraphNode) {
        if (parent.data == null) return

        val childrenProp = parent.data!!::class.memberProperties.firstOrNull {
            it.name == "children"
        } as? kotlin.reflect.KProperty1<Any, *>

        val children = childrenProp?.get(parent.data!!) as List<*>

        for (i in 0 until children.size) {
            val direction = if (parent.depth == -1) {
                Polar(i.toDouble() / children.size * 360.0 + Double.uniform(-5.0, 5.0), 1.0).cartesian
            } else {
                val dir = (parent.position - parent.parent!!.position).normalized * 50.0
                dir.normalized.rotate((1.0 * (i - (children.size / 2.0))) * parent.children.size * 2.0)
            }

            val influenceRadius = 10.0
            var distance = 0.0

            fun computeNextPosition(): Vector2 {
                var hit = false

                fun intersects(): Boolean {
                    val newPos = parent.position + direction * distance
                    val intersections = edges
                        .filter { it.a != parent && it.b != parent }
                        .map { it.segment }
                        .any { it.intersections(Segment2D(parent.position, newPos)).isNotEmpty() }

                    return intersections
                }

                while (distance < 50.0 && !hit) {
                    distance += 2.0

                    if (intersects()) {
                        hit = true
                    }
                }

                return parent.position + direction * distance
            }

            val position = computeNextPosition()
            val child = GraphNode(
                position,
                parent.depth + 1,
                distance,
                direction,
                children[i],
                parent
            )

            maxDepth = max(maxDepth, child.depth)

            nodeId++
            child.id = nodeId

            val edge = GraphEdge(parent, child, distance * 1.1)

            nodes.add(child)
            nodesById[child.id] = child
            edges.add(edge)

            parent.children.add(child)

            populate(child)
        }

    }

    private fun findBranches(): List<List<GraphNode>> {
        val result = mutableListOf<List<GraphNode>>()
        val leaves = nodes.filter { it.isLeaf }
        for (leaf in leaves) {
            val stack = mutableListOf<GraphNode>()

            fun visit(node: GraphNode) {
                stack.add(node)
                if (node.parent == null)
                    return

                visit(node.parent)
            }

            visit(leaf)
            result.add(stack)
        }

        return result
    }

    private fun findCircleBounds(): Circle {
        val ppositions = nodes.map { PVector(it.position) }
        val median = weightedMedian(ppositions)
        val centroid = lib.Vector2(median)
        val radius = (centroid - lib.Vector2(FarthestPointPair(ppositions).either())).length

        return Circle(centroid, radius)
    }


    private var displacer: (GraphNode) -> Vector2 = { Vector2.ZERO }

    fun displace(v: Vector2) {
        displacer = { v }
    }

    fun displace(f: (GraphNode) -> Vector2) {
        displacer = f
    }

    fun update(dt: Double = 1.0 / iterations) {
        repeat(iterations) {
            kd = buildKDTree(nodes, 2, ::nodeMapper)

            for (node in nodes) {
                val distanceToCenter = node.position.distanceTo(origin).coerceAtMost(500.0) / 500.0
                if (node.isLeaf) {
                    node.nextPosition -= (origin - node.position) * 0.008 * (1.0 - distanceToCenter) * dt
                }

                node.nextPosition += node.initialDirection.normalized * 10.0 * dt

               //

                val inRange = kd.findAllInRadius(node, node.influenceRadius * 2.0)

                for (other in inRange) {
                    val pos = node.position
                    val otherPos = other.position
                    val direction = (otherPos - pos)
                    val distance = direction.length
                    val radiiSum = node.influenceRadius + other.influenceRadius

                    if (distance > 0.0) {

                        val repulsion = direction.normalized * dt * 1.5
                        node.nextPosition -= repulsion
                        other.nextPosition += repulsion

                        if (distance < radiiSum  && node.depth != -1) {
                            val collision = direction * ((distance - radiiSum) / distance * 0.5)
                            node.nextPosition += collision * (node.influenceRadius * 0.01)
                        }
                    }
                }

                if (node.depth == -1) {
                    node.nextPosition += (origin - node.position) * 1.5 * dt
                }
            }


             for (edge in edges) {
                 val diff = edge.a.position - edge.b.position
                 if (diff.length > 0.0) {
                     val force = (diff * ((diff.length - edge.targetLength) / diff.length * dt)) * stiffness * 1.1
                     edge.a.nextPosition -= force
                     edge.b.nextPosition += force

                     if (diff.length > edge.targetLength) {
                         val direction = diff.normalized
                         val moveDistance = diff.length - edge.targetLength
                         edge.b.nextPosition += direction * moveDistance * dt * stiffness
                     }
                 }
             }

            for (node in nodes) {
                node.nextPosition += displacer(node)
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

}