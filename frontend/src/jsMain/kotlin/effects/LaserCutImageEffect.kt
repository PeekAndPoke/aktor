package de.peekandpoke.aktor.frontend.effects

import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.Uint8ClampedArray
import org.khronos.webgl.get
import org.w3c.dom.*
import org.w3c.dom.events.Event
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class LaserCutImageEffect(
    val imageUrl: String,
    val canvas: HTMLCanvasElement,
    val edgeThreshold: Int = 100,
    val kMeansK: Int = 50,  // Number of line clusters to detect
    val kMeansMaxIterations: Int = 50,
) {
    private inner class CutLine(points: List<Pair<Int, Int>>) {
        // Smooth out the path with more interpolated points for steadier movement
        private val smoothedPoints = smoothPath(points)
        private var currentIndex = 0
        private var progress = 0.0 // 0.0 to 1.0 between current point and next

        private val cutSpeed = 0.03 // Slightly slower, more consistent speed

        // Fading properties
        private val fadeOutTime = 800.0 // Faster fade for sharper appearance
        private val trailLength = 20 // Longer but faster-fading trail
        private val segmentFadeProgress = mutableListOf<Double>()

        // Much thinner lines for more precision
        private val trailColor = "rgba(255, 255, 255, 0.9)" // Brighter blue
        private val trailWidth = 1.0 // Very thin line

        // Laser point properties
        private val laserPointSize = 1.5 // Tiny, sharp point
        private val laserGlowSize = 1.0 // Small glow for emphasis
        private val laserIntensity = "rgba(255, 255, 255, 0.75)" // Very bright center

        init {
            // Initialize all segments to invisible
            for (i in 0 until smoothedPoints.size - 1) {
                segmentFadeProgress.add(0.0)
            }
        }

        /**
         * Creates a smoother path by interpolating between points
         */
        private fun smoothPath(points: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
            if (points.size < 2) return points

            val result = mutableListOf<Pair<Int, Int>>()
            result.add(points.first())

            for (i in 0 until points.size - 1) {
                val start = points[i]
                val end = points[i + 1]

                // Calculate distance between points
                val distance = sqrt(
                    (end.first - start.first).toDouble().pow(2) +
                            (end.second - start.second).toDouble().pow(2)
                )

                // Add intermediate points for longer segments
                if (distance > 8) {
                    val steps = (distance / 4).toInt() // One point every ~4 pixels
                    for (step in 1 until steps) {
                        val ratio = step.toDouble() / steps
                        val x = start.first + ((end.first - start.first) * ratio).toInt()
                        val y = start.second + ((end.second - start.second) * ratio).toInt()
                        result.add(x to y)
                    }
                }

                result.add(end)
            }

            return result
        }

        fun update(deltaTime: Double) {
            // Update cutting progress
            if (currentIndex < smoothedPoints.size - 1) {
                progress += cutSpeed * deltaTime

                // When we reach a new segment, make it visible
                if (progress >= 1.0) {
                    segmentFadeProgress[currentIndex] = 1.0
                    progress = 0.0
                    currentIndex++
                } else if (currentIndex < segmentFadeProgress.size) {
                    // Update the current segment's visibility based on progress
                    segmentFadeProgress[currentIndex] = progress
                }
            }

            // Update fade progress for all segments
            for (i in 0 until currentIndex) {
                val distanceFromCurrent = currentIndex - i

                if (distanceFromCurrent > trailLength) {
                    // This segment is beyond the trail length, fade it out quickly
                    segmentFadeProgress[i] = max(0.0, segmentFadeProgress[i] - (deltaTime / fadeOutTime))
                } else {
                    // Segments in the trail fade proportionally to their distance
                    val fadeFactor = 1.0 - (distanceFromCurrent.toDouble() / trailLength)
                    segmentFadeProgress[i] = max(segmentFadeProgress[i], fadeFactor)
                }
            }
        }

        fun draw(ctx: CanvasRenderingContext2D) {
            ctx.save()

            // Draw each segment based on its fade progress
            for (i in 0 until smoothedPoints.size - 1) {
                if (i >= segmentFadeProgress.size) break

                val opacity = segmentFadeProgress[i]
                if (opacity <= 0.01) continue // Skip nearly invisible segments

                val startX = smoothedPoints[i].first.toDouble()
                val startY = smoothedPoints[i].second.toDouble()
                val endX = smoothedPoints[i + 1].first.toDouble()
                val endY = smoothedPoints[i + 1].second.toDouble()

                // For the current active segment, consider the progress
                val actualEndX = if (i == currentIndex) startX + (endX - startX) * progress else endX
                val actualEndY = if (i == currentIndex) startY + (endY - startY) * progress else endY

                // Draw the main line (thin and precise)
                ctx.lineWidth = trailWidth
                ctx.strokeStyle = trailColor.replace("0.7", (0.7 * opacity).toString())
                ctx.lineCap = CanvasLineCap.BUTT // Sharp edges

                ctx.beginPath()
                ctx.moveTo(startX, startY)
                ctx.lineTo(actualEndX, actualEndY)
                ctx.stroke()
            }

            // Draw the active laser point if still cutting
            if (currentIndex < smoothedPoints.size - 1) {
                val startX = smoothedPoints[currentIndex].first.toDouble()
                val startY = smoothedPoints[currentIndex].second.toDouble()
                val endX = smoothedPoints[currentIndex + 1].first.toDouble()
                val endY = smoothedPoints[currentIndex + 1].second.toDouble()

                val currentX = startX + (endX - startX) * progress
                val currentY = startY + (endY - startY) * progress

                // Small outer glow for the laser point
                ctx.shadowBlur = laserGlowSize
                ctx.shadowColor = "rgba(255, 50, 50, 0.7)"

                // Tiny bright white laser point
                ctx.fillStyle = laserIntensity
                ctx.beginPath()
                ctx.arc(currentX, currentY, laserPointSize, 0.0, 2 * PI)
                ctx.fill()

                // Reset shadow effects
                ctx.shadowBlur = 0.0
            }

            ctx.restore()
        }

        fun isDone(): Boolean {
            return currentIndex >= smoothedPoints.size - 1 &&
                    segmentFadeProgress.all { it <= 0.01 }
        }
    }

    private lateinit var ctx: CanvasRenderingContext2D
    private lateinit var imageData: ImageData
    private lateinit var edgeGroups: List<List<Pair<Int, Int>>>

    // Animation properties
    private val laserColor = "rgba(255, 255, 255, 0.8)"
    private val laserGlowColor = "rgba(128, 128, 255, 0.5)"
    private val laserWidth = 1.0
    private val laserGlowWidth = 4.0

    // Keep track of cut lines and their fade state
    private val cutLines = mutableListOf<CutLine>()

    // Animation variables
    private var animationFrameId: Int? = null
    private var lastFrameTime: Double = 0.0
    private val newLineInterval = 500 // ms between starting new lines
    private var timeSinceLastLine = 0.0

    fun run() {
        val image = document.createElement("img") as HTMLImageElement

        // Set crossOrigin attribute to allow getImageData to work
        image.crossOrigin = "anonymous"
        image.src = imageUrl

        image.addEventListener("load", { _: Event ->
            // Set canvas dimensions to match the image
            canvas.width = image.width
            canvas.height = image.height

            ctx = canvas.getContext("2d") as CanvasRenderingContext2D

            // Draw the image on the canvas
            ctx.drawImage(image, 0.0, 0.0)

            // Get image data
            imageData = ctx.getImageData(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())

            // Detect edges and extract points
            val edgeData = detectEdges(imageData)
            val edgePoints = extractEdgePoints(edgeData, edgeThreshold)

            // Use k-means to group points into lines
            edgeGroups = kMeansPointGrouping(edgePoints)

            // Start the animation
            startAnimation()
        })

        // Add error handling for cross-origin issues
        image.addEventListener("error", { _: Event ->
            console.error("Failed to load image. If this is a cross-origin issue, ensure the server allows CORS for the image.")
        })
    }


    private fun startAnimation() {
        lastFrameTime = window.performance.now()
        animationFrameId = window.requestAnimationFrame { animate(it) }
    }

    private fun animate(timestamp: Double) {
        val deltaTime = timestamp - lastFrameTime
        lastFrameTime = timestamp

        // Clear the canvas and redraw the original image
        ctx.clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
        ctx.putImageData(imageData, 0.0, 0.0)

        // Check if it's time to add a new line
        timeSinceLastLine += deltaTime
        if (timeSinceLastLine >= newLineInterval && edgeGroups.isNotEmpty()) {
            timeSinceLastLine = 0.0

            // Select a random line segment
            val randomGroup = edgeGroups[Random.nextInt(edgeGroups.size)]
            if (randomGroup.size >= 2) {
                cutLines.add(CutLine(randomGroup))
            }
        }

        // Update and draw all active cut lines
        val iterator = cutLines.iterator()
        while (iterator.hasNext()) {
            val line = iterator.next()
            line.update(deltaTime)
            line.draw(ctx)

            // Remove fully faded lines
            if (line.isDone()) {
                iterator.remove()
            }
        }

        // Continue animation
        animationFrameId = window.requestAnimationFrame { animate(it) }
    }

    fun stop() {
        animationFrameId?.let { window.cancelAnimationFrame(it) }
    }

    /**
     * Groups edge points into lines using K-means clustering algorithm
     */
    private fun kMeansPointGrouping(points: List<Pair<Int, Int>>): List<List<Pair<Int, Int>>> {
        if (points.isEmpty() || kMeansK <= 0) {
            return emptyList()
        }

        // If we have fewer points than clusters, just return each point as its own group
        if (points.size <= kMeansK) {
            return points.map { listOf(it) }
        }

        // Initialize K centroids randomly from the point set
        val centroids = mutableListOf<Pair<Double, Double>>()
        val random = Random
        val usedIndices = mutableSetOf<Int>()

        // Select K unique random points as initial centroids
        while (centroids.size < kMeansK) {
            val idx = random.nextInt(points.size)
            if (idx !in usedIndices) {
                usedIndices.add(idx)
                val point = points[idx]
                centroids.add(Pair(point.first.toDouble(), point.second.toDouble()))
            }
        }

        // Initialize cluster assignments
        val clusters = Array(points.size) { -1 }
        var changed = true
        var iteration = 0

        // K-means iteration
        while (changed && iteration < kMeansMaxIterations) {
            changed = false
            iteration++

            // Assign points to nearest centroid
            for (i in points.indices) {
                val point = points[i]
                var minDist = Double.MAX_VALUE
                var minIdx = -1

                for (j in centroids.indices) {
                    val centroid = centroids[j]
                    val dist = squaredDistance(
                        point.first.toDouble(),
                        point.second.toDouble(),
                        centroid.first,
                        centroid.second
                    )

                    if (dist < minDist) {
                        minDist = dist
                        minIdx = j
                    }
                }

                if (clusters[i] != minIdx) {
                    clusters[i] = minIdx
                    changed = true
                }
            }

            // Skip centroid update in the final iteration
            if (!changed || iteration >= kMeansMaxIterations) {
                break
            }

            // Update centroids based on assigned points
            val sumX = DoubleArray(kMeansK) { 0.0 }
            val sumY = DoubleArray(kMeansK) { 0.0 }
            val counts = IntArray(kMeansK) { 0 }

            for (i in points.indices) {
                val cluster = clusters[i]
                val point = points[i]

                sumX[cluster] += point.first.toDouble()
                sumY[cluster] += point.second.toDouble()
                counts[cluster]++
            }

            for (i in 0 until kMeansK) {
                if (counts[i] > 0) {
                    centroids[i] = Pair(sumX[i] / counts[i], sumY[i] / counts[i])
                }
            }
        }

        // Group points by cluster
        val result = Array<MutableList<Pair<Int, Int>>>(kMeansK) { mutableListOf() }
        for (i in points.indices) {
            val cluster = clusters[i]
            result[cluster].add(points[i])
        }

        // Sort points within each cluster to create paths that can be drawn sequentially
        return result.filter { it.isNotEmpty() }.map { sortPointsIntoPath(it) }
    }

    /**
     * Sort points in a cluster into a sequential path using a nearest-neighbor approach
     */
    private fun sortPointsIntoPath(points: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        if (points.size <= 1) {
            return points
        }

        val result = mutableListOf<Pair<Int, Int>>()
        val unvisited = points.toMutableList()

        // Start with the first point
        var current = unvisited.removeAt(0)
        result.add(current)

        // Keep finding the nearest unvisited point
        while (unvisited.isNotEmpty()) {
            var minDist = Double.MAX_VALUE
            var minIdx = -1

            for (i in unvisited.indices) {
                val dist = squaredDistance(
                    current.first.toDouble(),
                    current.second.toDouble(),
                    unvisited[i].first.toDouble(),
                    unvisited[i].second.toDouble()
                )

                if (dist < minDist) {
                    minDist = dist
                    minIdx = i
                }
            }

            current = unvisited.removeAt(minIdx)
            result.add(current)
        }

        return result
    }

    /**
     * Calculate the squared Euclidean distance between two points
     */
    private fun squaredDistance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        return dx * dx + dy * dy
    }


    /**
     * Applies the Sobel operator to the given image data and returns new ImageData
     * containing the edge intensities.
     */
    private fun detectEdges(imageData: ImageData): ImageData {
        val width = imageData.width
        val height = imageData.height
        val src = imageData.data
        val dst = Uint8ClampedArray(src.length)

        // Sobel operator kernels.
        val kernelX = arrayOf(-1, 0, 1, -2, 0, 2, -1, 0, 1)
        val kernelY = arrayOf(-1, -2, -1, 0, 0, 0, 1, 2, 1)

        // Convert the image to grayscale.
        val gray = FloatArray(width * height)
        for (i in 0 until src.length step 4) {
            val r = src[i].toDouble()
            val g = src[i + 1].toDouble()
            val b = src[i + 2].toDouble()
            gray[i / 4] = ((r + g + b) / 3).toFloat()
        }

        // Apply the Sobel operator.
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var gx = 0.0
                var gy = 0.0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pos = ((y + ky) * width + (x + kx))
                        val pixel = gray[pos]
                        val kernelIndex = (ky + 1) * 3 + (kx + 1)
                        gx += kernelX[kernelIndex] * pixel
                        gy += kernelY[kernelIndex] * pixel
                    }
                }
                val magnitude = sqrt(gx * gx + gy * gy)
                val clamped = if (magnitude > 255) 255.0 else magnitude
                val idx = (y * width + x) * 4
                (dst.asDynamic())[idx] = clamped.toInt()
                (dst.asDynamic())[idx + 1] = clamped.toInt()
                (dst.asDynamic())[idx + 2] = clamped.toInt()
                (dst.asDynamic())[idx + 3] = 255
            }
        }
        return ImageData(dst, width, height)
    }

    /**
     * Extracts (x, y) points from the edge-detected image data where the intensity
     * exceeds the provided threshold.
     */
    private fun extractEdgePoints(edgeData: ImageData, threshold: Int): List<Pair<Int, Int>> {
        val width = edgeData.width
        val height = edgeData.height
        val data = edgeData.data
        val points = mutableListOf<Pair<Int, Int>>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = (y * width + x) * 4
                val intensity = data[idx].toInt()
                if (intensity >= threshold) {
                    points.add(Pair(x, y))
                }
            }
        }
        return points
    }
}
