import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lib.Graph
import lib.Node
import lib.traverse
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2D
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import java.io.File

@Serializable
class ProjectDescription(
    val text: String,
    val evaluations: Map<String, Int?>,
    val observations: Map<String, String>,
    val dimension: String? = null,
    val children: MutableList<ProjectDescription> = mutableListOf()
)


fun main() {
    application {
        configure {
            width = 1280
            height = 1280
        }

        program {

            extend(Camera2D())

            val projects = Json.decodeFromString<List<ProjectDescription>>(
                File("data/projects-02.json").readText()
            )


            val graph = Graph(drawer.bounds.center)

            fun expand(nodes: List<Node>, data: )

            fun expand(node: Node, data: ProjectDescription, userDirection: Vector2? = null) {
                val new = graph.addChild(node, data.dimension ?: "no dimension", userDirection)

                for (child in data.children) {
                    expand(new, child)
                }
            }


            for ((i, data) in projects[0].children.withIndex()) {
                val p = Polar(i.toDouble() / projects[0].children.size * 360.0, 150.0).cartesian
                expand(graph.nodes[0], data, p)
            }

            extend {

                graph.update()
                graph.draw(drawer)

            }
        }
    }
}