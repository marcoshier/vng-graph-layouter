package data

import kotlinx.serialization.Serializable

@Serializable
class ProjectDescription(
    val text: String,
    val evaluations: Map<String, Int?>,
    val observations: Map<String, String>,
    val dimension: String? = null,
    val children: MutableList<ProjectDescription> = mutableListOf()
)
