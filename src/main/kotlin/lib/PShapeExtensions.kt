package lib

import lib.Vector2
import micycle.pgs.PGS_PointSet
import org.dyn4j.geometry.Vector3
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import processing.core.PVector

fun PVector(v: Vector2): PVector = PVector(v.x.toFloat(), v.y.toFloat())
fun PVector(v: Vector3): PVector = PVector(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())

fun Vector2(p: PVector): Vector2 = Vector2(p.x.toDouble(), p.y.toDouble())

fun phyllotaxis(p: Vector2, nPoints: Int, radius: Double, angle: Double): List<Vector2> {
    return PGS_PointSet.phyllotaxis(p.x, p.y, nPoints, radius, angle).map { Vector2(it) }
}