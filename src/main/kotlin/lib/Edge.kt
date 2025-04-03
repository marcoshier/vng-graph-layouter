package lib

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.shape.Segment2D

class Edge(
    val a: Node,
    val b: Node,
    val targetLength: Double
) {

    val depth: Int
        get() = a.depth

    val length: Double
        get() = (a.position - b.position).length


    fun draw(drawer: Drawer) {
        drawer.stroke = ColorRGBa.WHITE
        drawer.segment(Segment2D(a.position, b.position))
    }

}