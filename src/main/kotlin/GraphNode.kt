import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2

data class GraphNode(
    var position: Vector2,
    val depth: Int = -1,
    var influenceRadius: Double = 10.0,
    var initialDirection: Vector2 = Vector2.ZERO,
    var data: Any? = null,
    val parent: GraphNode? = null,
    val children: MutableList<GraphNode> = mutableListOf()
) {

    var id = 0

    var oldPosition = position
    var nextPosition = position
    var smoothPosition = position

    val isLeaf: Boolean
        get() = children.isEmpty()

}


fun traverse(node: GraphNode, block: (GraphNode) -> Unit) {
    block(node)
    for (child in node.children) {
        block(child)
    }
}

/**
 * mapper function for kd-tree lookup
 */

fun nodeMapper(v: GraphNode, dimension: Int): Double {
    return when (dimension) {
        0 -> v.position.x
        else -> v.position.y
    }
}