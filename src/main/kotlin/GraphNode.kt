import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector2

class GraphNode(
    var position: Vector2,
    val depth: Int = -1,
    var influenceRadius: Double = 10.0,
    var direction: Vector2 = Vector2.ZERO,
    var data: Any? = null,
    val parent: GraphNode? = null,
    val children: MutableList<GraphNode> = mutableListOf()
) {

    var oldPosition = position
    var nextPosition = position
    var smoothPosition = position
    var velocity = Vector2.ZERO

    val isLeaf: Boolean
        get() = children.isEmpty()

    fun draw(drawer: Drawer) {
        drawer.fill = ColorRGBa.WHITE
        drawer.circle(position, 3.0)

        data?.let {
           // drawer.text("${it.dimension}", position)
        }

        drawer.fill = null
        drawer.stroke = if (depth == -1) ColorRGBa.BLUE else ColorRGBa.RED
        drawer.circle(smoothPosition, influenceRadius)

        drawer.stroke = ColorRGBa.YELLOW
        drawer.lineSegment(smoothPosition, smoothPosition + direction * 10.0)

    }

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