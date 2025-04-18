import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.shape.Segment2D

class GraphEdge(
    val a: GraphNode,
    val b: GraphNode,
    var targetLength: Double
) {

    val depth: Int
        get() = a.depth

    val length: Double
        get() = (a.smoothPosition - b.smoothPosition).length

    val segment: Segment2D
        get() = Segment2D(a.smoothPosition, b.smoothPosition)

}