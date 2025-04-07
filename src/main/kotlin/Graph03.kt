import kotlinx.serialization.json.Json
import lib.Graph
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.color.spaces.ColorOKHSLa
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.noise.uniform
import org.openrndr.shape.ShapeContour
import java.io.File
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 1280
            height = 1280
        }

        program {

          //  extend(ScreenRecorder())

            extend(Camera2D())

            val projects = Json.decodeFromString<List<ProjectDescription>>(
                File("data/projects-02.json").readText()
            )

            val graph = Graph(drawer.bounds.center)
            graph.init(projects.first())

            extend {

                graph.update()
                graph.draw(drawer)
            }
        }
    }
}