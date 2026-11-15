package com.devdenisviana.gabaritoscanner.scan

import android.graphics.Bitmap
import com.devdenisviana.gabaritoscanner.domain.sheet.SheetLayout
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.roundToInt

/** Result of trying to straighten a photographed answer sheet. */
sealed interface AlignResult {
    /** [sheet] is the sheet seen from the front, OUTPUT_WIDTH x OUTPUT_HEIGHT pixels. */
    data class Success(val sheet: Bitmap) : AlignResult

    data class Failure(val reason: String) : AlignResult
}

/**
 * Finds the four black corner markers in a photo and warps the sheet so that
 * normalized sheet coordinates (see [SheetLayout]) map directly to pixels:
 * x_px = x * OUTPUT_WIDTH, y_px = y * OUTPUT_HEIGHT.
 */
object SheetAligner {

    const val OUTPUT_WIDTH = 1000
    val OUTPUT_HEIGHT: Int = (OUTPUT_WIDTH * SheetLayout.HEIGHT_TO_WIDTH_RATIO).roundToInt()

    private const val DETECTION_MAX_SIDE = 1200.0
    private const val MIN_AREA_RATIO = 0.0002
    private const val MAX_AREA_RATIO = 0.01
    private const val MIN_SQUARE_FILL = 0.88
    private const val MIN_ASPECT = 0.7
    private const val MAX_ASPECT = 1.4

    private val openCvReady: Boolean by lazy { OpenCVLoader.initLocal() }

    fun align(source: Bitmap): AlignResult {
        if (!openCvReady) return AlignResult.Failure("Não foi possível iniciar o OpenCV.")

        val rgba = Mat()
        val gray = Mat()
        val small = Mat()
        val binary = Mat()
        val warped = Mat()
        try {
            Utils.bitmapToMat(source, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)

            // Detect on a smaller copy (faster); warp later from the full-resolution image.
            val scale = DETECTION_MAX_SIDE / max(gray.cols(), gray.rows())
            Imgproc.resize(gray, small, Size(), scale, scale, Imgproc.INTER_AREA)
            Imgproc.GaussianBlur(small, small, Size(5.0, 5.0), 0.0)
            Imgproc.adaptiveThreshold(
                small,
                binary,
                255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                75,
                15.0,
            )

            val centers = findSquareCenters(binary)
            if (centers.size < 4) {
                return AlignResult.Failure(
                    "Marcadores dos cantos não encontrados (achei ${centers.size} de 4). " +
                            "Enquadre a folha inteira, com boa luz e sem sombras.",
                )
            }

            val topLeft = centers.minBy { it.x + it.y }
            val bottomRight = centers.maxBy { it.x + it.y }
            val topRight = centers.maxBy { it.x - it.y }
            val bottomLeft = centers.minBy { it.x - it.y }

            val ordered = listOf(topLeft, topRight, bottomRight, bottomLeft)
            if (ordered.distinct().size < 4) {
                return AlignResult.Failure("Não consegui identificar os 4 cantos da folha.")
            }

            // Back to full-resolution coordinates.
            val srcPoints = MatOfPoint2f(*ordered.map { Point(it.x / scale, it.y / scale) }.toTypedArray())
            val w = OUTPUT_WIDTH.toDouble()
            val h = OUTPUT_HEIGHT.toDouble()
            val dstPoints = MatOfPoint2f(
                Point(0.0, 0.0),
                Point(w, 0.0),
                Point(w, h),
                Point(0.0, h),
            )

            val transform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
            Imgproc.warpPerspective(rgba, warped, transform, Size(w, h), Imgproc.INTER_LINEAR)

            srcPoints.release()
            dstPoints.release()
            transform.release()

            val output = Bitmap.createBitmap(OUTPUT_WIDTH, OUTPUT_HEIGHT, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(warped, output)
            return AlignResult.Success(output)
        } finally {
            rgba.release()
            gray.release()
            small.release()
            binary.release()
            warped.release()
        }
    }

    /** Returns the centers of blobs that look like filled squares (the corner markers). */
    private fun findSquareCenters(binary: Mat): List<Point> {
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        hierarchy.release()

        val imageArea = binary.rows().toDouble() * binary.cols()
        val centers = mutableListOf<Point>()

        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area < imageArea * MIN_AREA_RATIO || area > imageArea * MAX_AREA_RATIO) continue

            val contour2f = MatOfPoint2f(*contour.toArray())
            val rect = Imgproc.minAreaRect(contour2f)
            contour2f.release()

            val width = rect.size.width
            val height = rect.size.height
            if (width <= 0.0 || height <= 0.0) continue

            val aspect = width / height
            val fill = area / (width * height)
            if (aspect in MIN_ASPECT..MAX_ASPECT && fill >= MIN_SQUARE_FILL) {
                centers.add(rect.center)
            }
        }

        contours.forEach { it.release() }
        return centers
    }
}
