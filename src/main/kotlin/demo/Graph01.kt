import data.ProjectDescription
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openrndr.application
import org.openrndr.extra.camera.Camera2D
import org.openrndr.ffmpeg.ScreenRecorder
import java.io.File


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