/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.graphics.shapes

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import androidx.core.graphics.minus
import androidx.core.graphics.plus
import androidx.core.graphics.times
import androidx.core.graphics.div
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The Polygon class allows simple construction of polygonal shapes with optional rounding
 * on the vertices. Polygons can be constructed with either the number of vertices
 * desired or an ordered list of vertices.
 */
open class Polygon {

    /**
     * A Polygon is essentially a CubicShape, which handles all of the functionality around
     * cubic Beziers that are used to create and render the geometry. But subclassing from
     * CubicShape causes a bit of naming confusion, since an actual Polygon, in geometry,
     * is a shape with straight edges and hard corners, whereas CubicShape obviously allows for
     * more general, curved shapes. Therefore, we delegate to CubicShape as an internal
     * implementation detail, and Polygon has no superclass.
     */
    private val cubicShape = CubicShape()

    /**
     * Features are the corners (rounded or not) and edges of a polygon. Retaining the list of
     * per-vertex corner (and the edges between them) allows manipulation of a Polygon with more
     * context for the structure of that polygon, rather than just the list of cubic beziers
     * which are calculated for rendering purposes.
     */
    internal lateinit var features: List<Feature>
        private set

    // TODO center point should not be mutable
    /**
     * The center of this polygon. The center is determined at construction time, either calculated
     * to be an average of all of the vertices of the polygon, or passed in as a parameter. This
     * center may be used in later operations, to help determine (for example) the relative
     * placement of points along the perimeter of the polygon.
     */
    var center: PointF
        private set

    /**
     * The bounds of a shape are a simple min/max bounding box of the points in all of
     * the [Cubic] objects. Note that this is not the same as the bounds of the resulting
     * shape, but is a reasonable (and cheap) way to estimate the bounds. These bounds
     * can be used to, for example, determine the size to scale the object when drawing it.
     */
    var bounds: RectF by cubicShape::bounds

    /**
     * Constructs a Polygon object from a given list of vertices, with optional
     * corner-rounding parameters for all corners or per-corner.
     *
     * @param vertices The list of vertices in this polygon. This should be an ordered list
     * (with the outline of the shape going from each vertex to the next in order of this
     * list), otherwise the results will be undefined.
     * @param center An optionally declared center of the polygon. If null or not supplied, this
     * will be calculated based on the supplied vertices.
     */
    constructor(vertices: List<PointF>, center: PointF? = null) :
        this(vertices, rounding = CornerRounding.Unrounded, perVertexRounding = null, center)

    /**
     * This constructor takes the number of vertices in the resulting polygon. These vertices are
     * positioned on a virtual circle around a given center with each vertex positioned [radius]
     * distance from that center, equally spaced (with equal angles between them).
     *
     * @param numVertices The number of vertices in this polygon.
     * @param radius The radius of the polygon, in pixels. This radius determines the
     * initial size of the object, which can be resized later by setting
     * a [transform matrix][transform].
     * @param center The center of the polygon, around which all vertices will be placed. The
     * default center is at (0,0).
     */
    constructor(numVertices: Int, radius: Float = 1f, center: PointF = PointF(0f, 0f)) :
        this(numVertices, radius = radius, rounding = CornerRounding.Unrounded, center = center)

    /**
     * Constructs a Polygon object from a given list of vertices, with optional
     * corner-rounding parameters for all corners or per-corner.
     *
     * This constructor is internal, only for use by the RoundedPolygon subclass. All of the
     * rounding functionality is handled in Polygon, but it is hidden from the public
     * API to avoid naming confusion, since geometric Polygons have no rounding.
     *
     * @param vertices The list of vertices in this polygon. This should be an ordered list
     * (with the outline of the shape going from each vertex to the next in order of this
     * list), otherwise the results will be undefined.
     * @param rounding The [CornerRounding] properties of every vertex. If some vertices should
     * have different rounding properties, then use [perVertexRounding] instead. The default
     * rounding value is [CornerRounding.Unrounded], meaning that the polygon will use the vertices
     * themselves in the final shape and not curves rounded around the vertices.
     * @param perVertexRounding The [CornerRounding] properties of every vertex. If this
     * parameter is not null, then it must have the same size as [vertices]. If this parameter
     * is null, then the polygon will use the [rounding] parameter for every vertex instead. The
     * default value is null.
     * @param center An optionally declared center of the polygon. If null or not supplied, this
     * will be calculated based on the supplied vertices.
     */
    internal constructor(
        vertices: List<PointF>,
        rounding: CornerRounding = CornerRounding.Unrounded,
        perVertexRounding: List<CornerRounding>? = null,
        center: PointF? = null
    ) {
        this.center = center ?: calculateCenter(vertices)
        setupPolygon(vertices, rounding, perVertexRounding)
    }

    /**
     * This constructor takes the number of vertices in the resulting polygon. These vertices are
     * positioned on a virtual circle around a given center with each vertex positioned [radius]
     * distance from that center, equally spaced (with equal angles between them).
     *
     * This constructor is internal, only for use by the RoundedPolygon subclass. All of the
     * rounding functionality is handled in Polygon, but it is hidden from the public
     * API to avoid naming confusion, since geometric Polygons have no rounding.
     *
     * @param numVertices The number of vertices in this polygon.
     * @param radius The radius of the polygon, in pixels. This radius determines the
     * initial size of the object, but it can be transformed later by setting
     * a matrix on it.
     * @param rounding The [CornerRounding] properties of every vertex. If some vertices should
     * have different rounding properties, then use [perVertexRounding] instead. The default
     * rounding value is [CornerRounding.Unrounded], meaning that the polygon will use the vertices
     * themselves in the final shape and not curves rounded around the vertices.
     * @param perVertexRounding The [CornerRounding] properties of every vertex. If this
     * parameter is not null, then it must have [numVertices] elements. If this parameter
     * is null, then the polygon will use the [rounding] parameter for every vertex instead. The
     * default value is null.
     * @param center The center of the polygon, around which all vertices will be placed. The
     * default center is at (0,0).
     *
     * @throws IllegalArgumentException If [perVertexRounding] is not null, it must have
     * [numVertices] elements.
     */
    internal constructor(
        numVertices: Int,
        radius: Float = 1f,
        rounding: CornerRounding = CornerRounding.Unrounded,
        perVertexRounding: List<CornerRounding>? = null,
        center: PointF = PointF(0f, 0f)
    ) : this(
        vertices = (0 until numVertices).map {
            radialToCartesian(radius, (FloatPi / numVertices * 2 * it)) + center
        },
        rounding = rounding, perVertexRounding = perVertexRounding, center = center)

    constructor(source: Polygon) {
        val newCubics = mutableListOf<Cubic>()
        for (cubic in source.cubicShape.cubics) {
            newCubics.add(Cubic(cubic))
        }
        val tempFeatures = mutableListOf<Feature>()
        for (feature in source.features) {
            if (feature is Edge) {
                tempFeatures.add(Edge(feature))
            } else {
                tempFeatures.add(Corner(feature as Corner))
            }
        }
        center = PointF(source.center.x, source.center.y)
        cubicShape.updateCubics(newCubics)
    }

    /**
     * This function takes the vertices (either supplied or calculated, depending on the
     * constructor called), plus [CornerRounding] parameters, and creates the actual
     * [Polygon] shape, rounding around the vertices (or not) as specified. The result
     * is a list of [Cubic] curves which represent the geometry of the final shape.
     *
     * @param vertices The list of vertices in this polygon. This should be an ordered list
     * (with the outline of the shape going from each vertex to the next in order of this
     * list), otherwise the results will be undefined.
     * @param rounding The [CornerRounding] properties of every vertex. If some vertices should
     * have different rounding properties, then use [perVertexRounding] instead. The default
     * rounding value is [CornerRounding.Unrounded], meaning that the polygon will use the vertices
     * themselves in the final shape and not curves rounded around the vertices.
     * @param perVertexRounding The [CornerRounding] properties of every vertex. If this
     * parameter is not null, then it must have the same size as [vertices]. If this parameter
     * is null, then the polygon will use the [rounding] parameter for every vertex instead. The
     * default value is null.
     */
    private fun setupPolygon(
        vertices: List<PointF>,
        rounding: CornerRounding = CornerRounding.Unrounded,
        perVertexRounding: List<CornerRounding>? = null
    ) {
        if (perVertexRounding != null && perVertexRounding.size != vertices.size) {
            throw IllegalArgumentException("perVertexRounding list should be either null or " +
                    "the same size as the vertices list")
        }
        val cubics = mutableListOf<Cubic>()
        val corners = mutableListOf<List<Cubic>>()
        val n = vertices.size
        val roundedCorners = mutableListOf<RoundedCorner>()
        for (i in 0 until n) {
            val vtxRounding = perVertexRounding?.get(i) ?: rounding
            roundedCorners.add(
                RoundedCorner(
                    vertices[(i + n - 1) % n],
                    vertices[i],
                    vertices[(i + 1) % n],
                    vtxRounding
                )
            )
        }
        val cutAdjusts = (0 until n).map { ix ->
            // TODO: check expectedRoundCut first, and ensure we fulfill rounding needs first for
            // both corners before using space for smoothing
            val expectedCut = roundedCorners[ix].expectedCut +
                    roundedCorners[(ix + 1) % n].expectedCut
            val sideSize = (vertices[ix] - vertices[(ix + 1) % n]).getDistance()
            if (expectedCut > sideSize) {
                sideSize / expectedCut
            } else {
                1f
            }
        }
        // Create and store list of beziers for each [potentially] rounded corner
        for (i in 0 until n) {
            corners.add(
                roundedCorners[i].getCubics(
                    allowedCut0 = roundedCorners[i].expectedCut * cutAdjusts[(i + n - 1) % n],
                    allowedCut1 = roundedCorners[i].expectedCut * cutAdjusts[i]
                )
            )
        }
        // Finally, store the calculated cubics. This includes all of the rounded corners
        // from above, along with new cubics representing the edges between those corners.
        val tempFeatures = mutableListOf<Feature>()
        for (i in 0 until n) {
            cubics.addAll(corners[i])
            // TODO: determine and pass convexity flag
            tempFeatures.add(Corner(corners[i], roundedCorners[i].center, vertices[i]))
            cubics.add(Cubic.straightLine(corners[i].last().p3, corners[(i + 1) % n].first().p0))
            tempFeatures.add(Edge(listOf(cubics[cubics.size - 1])))
        }
        features = tempFeatures
        cubicShape.updateCubics(cubics)
    }

    // Transforms as usual, plus the polygon's center
    fun transform(matrix: Matrix) {
        cubicShape.transform(matrix)
        val point = scratchTransformPoint
        point[0] = center.x
        point[1] = center.y
        matrix.mapPoints(point)
        center.x = point[0]
        center.y = point[1]
    }

    /**
     * Internally, the Polygon is stored as a [CubicShape] object. This function returns a copy
     * of that object.
     */
    fun toCubicShape(): CubicShape {
        return CubicShape(cubicShape)
    }

    /**
     * A Polygon is rendered as a [Path]. A copy of the underlying [Path] object can be
     * retrieved for use outside of this class. Note that this function returns a copy of
     * the internal [Path] to maintain immutability, thus there is some overhead in retrieving
     * and using the path with this function.
     */
    fun toPath(): Path {
        return cubicShape.toPath()
    }

    internal fun draw(canvas: Canvas, paint: Paint) {
        cubicShape.draw(canvas, paint)
    }

    /**
     * Calculates an estimated center position for the polygon, storing it in the [center] property.
     * This function should only be called if the center is not already calculated or provided.
     * The Polygon constructor which takes `numVertices` calculates its own center, since it
     * knows exactly where it starts out (0, 0).
     *
     * Note that this center will be transformed whenever the shape itself is transformed.
     * Any transforms that occur before the center is calculated will be taken into account
     * automatically since the center calculation is an average of the current location of
     * all cubic anchor points.
     */
    private fun calculateCenter(vertices: List<PointF>): PointF {
        var cumulativeX = 0f
        var cumulativeY = 0f
        for (vertex in vertices) {
            // Only care about anchor points, and since all cubics share one of their anchors,
            // only need one anchor per cubic
            cumulativeX += vertex.x
            cumulativeY += vertex.y
        }
        return PointF(cumulativeX / vertices.size, cumulativeY / vertices.size)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Polygon) return false

        if (!cubicShape.equals(other.cubicShape)) return false

        return true
    }

    override fun hashCode(): Int {
        return cubicShape.hashCode()
    }
}

/**
 * The RoundedPolygon is a Polygon which has optional rounding for each vertex. Like [Polygon],
 * a RoundedPolygon can be constructed with either the number of vertices desired or an ordered
 * list of vertices, along with optional rounding parameters.
 */
class RoundedPolygon : Polygon {
    /**
     * Constructs a RoundedPolygon object from a given list of vertices, with optional
     * corner-rounding parameters for all corners or per-corner.
     *
     * A RoundedPolygon without any rounding parameters is equivalent to a [Polygon] constructed
     * with the same [vertices] and [center].
     *
     * @param vertices The list of vertices in this polygon. This should be an ordered list
     * (with the outline of the shape going from each vertex to the next in order of this
     * list), otherwise the results will be undefined.
     * @param rounding The [CornerRounding] properties of every vertex. If some vertices should
     * have different rounding properties, then use [perVertexRounding] instead. The default
     * rounding value is [CornerRounding.Unrounded], meaning that the polygon will use the vertices
     * themselves in the final shape and not curves rounded around the vertices.
     * @param perVertexRounding The [CornerRounding] properties of every vertex. If this
     * parameter is not null, then it must have the same size as [vertices]. If this parameter
     * is null, then the polygon will use the [rounding] parameter for every vertex instead. The
     * default value is null.
     * @param center An optionally declared center of the polygon. If null or not supplied, this
     * will be calculated based on the supplied vertices.
     *
     * @throws IllegalArgumentException If [perVertexRounding] is not null, it must be
     * the same size as the [vertices] list.
     */
    constructor(
        vertices: List<PointF>,
        rounding: CornerRounding = CornerRounding.Unrounded,
        perVertexRounding: List<CornerRounding>? = null,
        center: PointF? = null
    ) : super(vertices, rounding, perVertexRounding, center) {}

    /**
     * This constructor takes the number of vertices in the resulting polygon. These vertices are
     * positioned around a given center with the specified radius, equally spaced (with equal
     * angles between them).
     *
     * A RoundedPolygon without any rounding parameters is equivalent to a [Polygon] constructed
     * with the same [numVertices], [radius], and [center].
     *
     * @param numVertices The number of vertices in this polygon.
     * @param radius The radius of the polygon, in pixels. This radius determines the
     * initial size of the object, but it can be transformed later by setting
     * a matrix on it.
     * @param rounding The [CornerRounding] properties of every vertex. If some vertices should
     * have different rounding properties, then use [perVertexRounding] instead. The default
     * rounding value is [CornerRounding.Unrounded], meaning that the polygon will use the vertices
     * themselves in the final shape and not curves rounded around the vertices.
     * @param perVertexRounding The [CornerRounding] properties of every vertex. If this
     * parameter is not null, then it must have [numVertices] elements. If this parameter
     * is null, then the polygon will use the [rounding] parameter for every vertex instead. The
     * default value is null.
     * @param center The center of the polygon, around which all vertices will be placed. The
     * default center is at (0,0).
     *
     * @throws IllegalArgumentException If [perVertexRounding] is not null, it must have
     * [numVertices] elements.
     */
    constructor(
        numVertices: Int,
        radius: Float = 1f,
        rounding: CornerRounding = CornerRounding.Unrounded,
        perVertexRounding: List<CornerRounding>? = null,
        center: PointF = PointF(0f, 0f)
    ) : super(numVertices, radius, rounding, perVertexRounding, center)
}

/**
 * This class holds information about a corner (rounded or not) or an edge of a given
 * polygon. The features of a Polygon can be used to manipulate the shape with more context
 * of what the shape actually is, rather than simply manipulating the raw curves and lines
 * which describe it.
 */
internal sealed class Feature(val cubics: List<Cubic>)

/**
 * Edges have only a list of the cubic curves which make up the edge. Edges lie between
 * corners and have no vertex or concavity; the curves are simply straight lines (represented
 * by Cubic curves).
 */
internal class Edge(cubics: List<Cubic>) : Feature(cubics) {
    constructor(source: Edge) : this(source.cubics)
}

/**
 * Corners contain the list of cubic curves which describe how the corner is rounded (or
 * not), plus the vertex at the corner (which the cubics may or may not pass through, depending
 * on whether the corner is rounded) and a flag indicating whether the corner is convex. A regular
 * polygon has all convex corners, while a star polygon generally (but not necessarily) has both
 * convex (outer) and concave (inner) corners.
 */
internal class Corner(
    cubics: List<Cubic>,
    // TODO: parameters here should be immutable
    val vertex: PointF,
    val roundedCenter: PointF,
    val convex: Boolean = true
) : Feature(cubics) {
    constructor(source: Corner) : this(
        source.cubics,
        source.vertex,
        source.roundedCenter,
        source.convex
    )
}

/**
 * Private utility class that holds the information about each corner in a polygon. The shape
 * of the corner can be returned by calling the [getCubics] function, which will return a list
 * of curves representing the corner geometry. The shape of the corner depends on the [rounding]
 * constructor parameter.
 *
 * If rounding is null, there is no rounding; the corner will simply be a single point at [p1].
 * This point will be represented by a [Cubic] of length 0 at that point.
 *
 * If rounding is not null, the corner will be rounded either with a curve approximating a circular
 * arc of the radius specified in [rounding], or with three curves if [rounding] has a nonzero
 * smoothing parameter. These three curves are a circular arc in the middle and two symmetrical
 * flanking curves on either side. The smoothing parameter determines the curvature of the
 * flanking curves.
 *
 * This is a class because we usually need to do the work in 2 steps, and prefer to keep state
 * between: first we determine how much we want to cut to comply with the parameters, then we are
 * given how much we can actually cut (because of space restrictions outside this corner)
 *
 * @param p0 the vertex before the one being rounded
 * @param p1 the vertex of this rounded corner
 * @param p2 the vertex after the one being rounded
 * @param rounding the optional parameters specifying how this corner should be rounded
 */
private class RoundedCorner(
    val p0: PointF,
    val p1: PointF,
    val p2: PointF,
    val rounding: CornerRounding? = null
) {
    val d1 = (p0 - p1).getDirection()
    val d2 = (p2 - p1).getDirection()
    val cornerRadius = rounding?.radius ?: 0f
    val smoothing = rounding?.smoothing ?: 0f

    // cosine of angle at p1 is dot product of unit vectors to the other two vertices
    val cosAngle = d1.dotProduct(d2)
    // identity: sin^2 + cos^2 = 1
    // sinAngle gives us the intersection
    val sinAngle = sqrt(1 - square(cosAngle))
    // How much we need to cut, as measured on a side, to get the required radius
    // calculating where the rounding circle hits the edge
    // This uses the identity of tan(A/2) = sinA/(1 + cosA), where tan(A/2) = radius/cut
    val expectedRoundCut =
        if (sinAngle > 1e-3) { cornerRadius * (cosAngle + 1) / sinAngle } else { 0f }
    // smoothing changes the actual cut. 0 is same as expectedRoundCut, 1 doubles it
    val expectedCut: Float
        get() = ((1 + smoothing) * expectedRoundCut) // TODO: coerceAtMost(maxCut)?
    // the center of the circle approximated by the rounding curve (or the middle of the three
    // curves if smoothing is requested). The center is the same as p0 if there is no rounding.
    lateinit var center: PointF

    @JvmOverloads
    fun getCubics(allowedCut0: Float, allowedCut1: Float = allowedCut0):
        List<Cubic> {
        // We use the minimum of both cuts to determine the radius, but if there is more space
        // in one side we can use it for smoothing.
        val allowedCut = min(allowedCut0, allowedCut1)
        // Nothing to do, just use lines, or a point
        if (expectedRoundCut < DistanceEpsilon ||
            allowedCut < DistanceEpsilon ||
            cornerRadius < DistanceEpsilon
        ) {
            center = p1
            return listOf(Cubic.straightLine(p1, p1))
        }
        // How much of the cut is required for the rounding part.
        val actualRoundCut = min(allowedCut, expectedRoundCut)
        // We have two smoothing values, one for each side of the vertex
        // Space is used for rounding values first. If there is space left over, then we
        // apply smoothing, if it was requested
        val actualSmoothing0 = calculateActualSmoothingValue(allowedCut0)
        val actualSmoothing1 = calculateActualSmoothingValue(allowedCut1)
        // Scale the radius if needed
        val actualR = cornerRadius * actualRoundCut / expectedRoundCut
        // Distance from the corner (p1) to the center
        val centerDistance = sqrt(square(actualR) + square(actualRoundCut))
        // Center of the arc we will use for rounding
        center = p1 + ((d1 + d2) / 2f).getDirection() * centerDistance
        val circleIntersection0 = p1 + d1 * actualRoundCut
        val circleIntersection2 = p1 + d2 * actualRoundCut
        val flanking0 = computeFlankingCurve(actualRoundCut, actualSmoothing0, p1, p0,
            circleIntersection0, circleIntersection2, center, actualR)
        val flanking2 = computeFlankingCurve(actualRoundCut, actualSmoothing1, p1, p2,
            circleIntersection2, circleIntersection0, center, actualR).reverse()
        val roundingCurves = listOf(
            flanking0,
            Cubic.circularArc(center, flanking0.p3, flanking2.p0),
            flanking2
        )
        return roundingCurves
    }

    /**
     * If allowedCut (the amount we are able to cut) is greater than the expected cut
     * (without smoothing applied yet), then there is room to apply smoothing and we
     * calculate the actual smoothing value here.
     */
    private fun calculateActualSmoothingValue(allowedCut: Float): Float {
        return if (allowedCut > expectedCut) {
            smoothing
        } else if (allowedCut > expectedRoundCut) {
            smoothing * (allowedCut - expectedRoundCut) / (expectedCut - expectedRoundCut)
        } else {
            0f
        }
    }

    /**
     * Compute a Bezier to connect the linear segment defined by corner and sideStart
     * with the circular segment defined by circleCenter, circleSegmentIntersection,
     * otherCircleSegmentIntersection and actualR.
     * The bezier will start at the linear segment and end on the circular segment.
     *
     * @param actualRoundCut How much we are cutting of the corner to add the circular segment
     * (this is before smoothing, that will cut some more).
     * @param actualSmoothingValues How much we want to smooth (this is the smooth parameter,
     * adjusted down if there is not enough room).
     * @param corner The point at which the linear side ends
     * @param sideStart The point at which the linear side starts
     * @param circleSegmentIntersection The point at which the linear side and the circle intersect.
     * @param otherCircleSegmentIntersection The point at which the opposing linear side and the
     * circle intersect.
     * @param circleCenter The center of the circle.
     * @param actualR The radius of the circle.
     *
     * @return a Bezier cubic curve that connects from the (cut) linear side and the (cut) circular
     * segment in a smooth way.
     */
    private fun computeFlankingCurve(
        actualRoundCut: Float,
        actualSmoothingValues: Float,
        corner: PointF,
        sideStart: PointF,
        circleSegmentIntersection: PointF,
        otherCircleSegmentIntersection: PointF,
        circleCenter: PointF,
        actualR: Float
    ): Cubic {
        // sideStart is the anchor, 'anchor' is actual control point
        val sideDirection = (sideStart - corner).getDirection()
        val curveStart = corner + sideDirection * actualRoundCut * (1 + actualSmoothingValues)
        // We use an approximation to cut a part of the circle section proportional to 1 - smooth,
        // When smooth = 0, we take the full section, when smooth = 1, we take nothing.
        // TODO: revisit this, it can be problematic as it approaches 19- degrees
        val p = interpolate(circleSegmentIntersection,
            (circleSegmentIntersection + otherCircleSegmentIntersection) / 2f,
            actualSmoothingValues
        )
        // The flanking curve ends on the circle
        val curveEnd = circleCenter + (p - circleCenter).getDirection() * actualR
        // The anchor on the circle segment side is in the intersection between the tangent to the
        // circle in the circle/flanking curve boundary and the linear segment.
        val circleTangent = (curveEnd - circleCenter).rotate90()
        val anchorEnd = lineIntersection(sideStart, sideDirection, curveEnd, circleTangent)
            ?: circleSegmentIntersection
        // From what remains, we pick a point for the start anchor.
        // 2/3 seems to come from design tools?
        val anchorStart = (curveStart + anchorEnd * 2f) / 3f
        return Cubic(curveStart, anchorStart, anchorEnd, curveEnd)
    }

    /**
     * Returns the intersection point of the two lines d0->d1 and p0->p1, or null if the
     * lines do not intersect
     */
    private fun lineIntersection(p0: PointF, d0: PointF, p1: PointF, d1: PointF): PointF? {
        val rotatedD1 = d1.rotate90()
        val den = d0.dotProduct(rotatedD1)
        if (abs(den) < AngleEpsilon) return null
        val k = (p1 - p0).dotProduct(rotatedD1) / den
        return p0 + d0 * k
    }
}

/**
 * Extension function which draws the given [Polygon] object into this [Canvas]. Rendering
 * occurs by drawing the underlying path for the object; callers can optionally retrieve the
 * path and draw it directly via [Polygon.toPath] (though that function copies the underlying
 * path. This extension function avoids that overhead when rendering).
 *
 * @param polygon The object to be drawn
 * @param paint The attributes
 */
fun Canvas.drawPolygon(polygon: Polygon, paint: Paint) {
    polygon.draw(this, paint)
}

private val scratchTransformPoint = floatArrayOf(0f, 0f)