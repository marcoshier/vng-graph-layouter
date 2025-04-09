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
        get() = (a.position - b.position).length

    val segment: Segment2D
        get() = Segment2D(a.position, b.position)


    private val initTime = System.currentTimeMillis()

    fun draw(drawer: Drawer) {
        drawer.stroke = ColorRGBa.WHITE
        drawer.segment(segment)

        val t = (System.currentTimeMillis() - initTime) / 2500.0
     //   targetLength = sin(t + 1.5 * (a.depth + 1)) * 50.0 + 60.0
    }

}