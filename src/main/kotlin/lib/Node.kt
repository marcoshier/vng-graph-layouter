package lib

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.color.fettepalette.ColorRamp
import org.openrndr.math.Vector2

class Node(
    var position: Vector2,
    val depth: Int = -1,
    var influenceRadius: Double = 10.0,
    var label: String = "",
    val children: MutableList<Node> = mutableListOf(),
    val parent: Node? = null
) {

    var oldPosition = position
    var nextPosition = position
    var smoothPosition = position
    var velocity = Vector2.ZERO

    fun draw(drawer: Drawer) {
        drawer.fill = ColorRGBa.WHITE
        drawer.circle(position, 3.0)
        drawer.text(label, position)

        drawer.fill = null
        drawer.stroke = if (depth == -1) ColorRGBa.BLUE else ColorRGBa.RED
        drawer.circle(smoothPosition, influenceRadius)
    }

}



fun traverse(node: Node, block: (Node) -> Unit) {
    block(node)
    for (child in node.children) {
        block(child)
    }
}

/**
 * mapper function for kd-tree lookup
 */

fun nodeMapper(v: Node, dimension: Int): Double {
    return when (dimension) {
        0 -> v.position.x
        else -> v.position.y
    }
}