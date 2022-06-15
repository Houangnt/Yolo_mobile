package com.example.autocapture

import android.graphics.*
import android.media.FaceDetector
import android.util.Log

object BitmapUtils {
    const val TAG = "BitmapUtils"
    fun detectFaces(bmp: Bitmap): Boolean {
        return try {
            val bitmap565: Bitmap = bmp.copy(Bitmap.Config.RGB_565, true)
            FaceDetector(bitmap565.width, bitmap565.height, 1).findFaces(
                bitmap565,
                arrayOfNulls<FaceDetector.Face>(1)
            ).also {
                Log.d(TAG, "detectFaces: found $it face")
            } > 0
        } catch (e: Exception) {
            Log.e(TAG, "detectFaces error: $e ")
            false
        }
    }

    fun crop(
        bitmap: Bitmap,
        topLeft: Point,
        topRight: Point,
        bottomLeft: Point,
        bottomRight: Point
    ): Bitmap {

        val output =
            Bitmap.createBitmap(bitmap.width + 1, bitmap.height + 1, Bitmap.Config.ARGB_8888)
        Log.d("CaptureActivity", "width+height: len:${bitmap.width}  level ${bitmap.height}")
        val canvas = Canvas(output)
        val paint = Paint()
        // 1. draw path
        val path = Path()
        path.moveTo(topLeft.x.toFloat(), topLeft.y.toFloat())
        path.lineTo(topRight.x.toFloat(), topRight.y.toFloat())
        path.lineTo(bottomRight.x.toFloat(), bottomRight.y.toFloat())
        path.lineTo(bottomLeft.x.toFloat(), bottomLeft.y.toFloat())
        path.close()
        canvas.drawPath(path, paint)

        // 2. draw original bitmap
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        // 3. cut
        val cropRect = Rect(
            Math.min(topLeft.x, bottomLeft.x),
            Math.min(topLeft.y, topRight.y),
            Math.max(bottomRight.x, topRight.x),
            Math.max(bottomRight.y, bottomLeft.y)
        )
        val cut = Bitmap.createBitmap(
            output,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height()
        )
        val cutTopLeft = Point()
        val cutTopRight = Point()
        val cutBottomLeft = Point()
        val cutBottomRight = Point()
        cutTopLeft.x = if (topLeft.x > bottomLeft.x) topLeft.x - bottomLeft.x else 0
        cutTopLeft.y = if (topLeft.y > topRight.y) topLeft.y - topRight.y else 0
        cutTopRight.x =
            if (topRight.x > bottomRight.x) cropRect.width() else cropRect.width() - Math.abs(
                bottomRight.x - topRight.x
            )
        cutTopRight.y = if (topLeft.y > topRight.y) 0 else Math.abs(topLeft.y - topRight.y)
        cutBottomLeft.x = if (topLeft.x > bottomLeft.x) 0 else Math.abs(topLeft.x - bottomLeft.x)
        cutBottomLeft.y =
            if (bottomLeft.y > bottomRight.y) cropRect.height() else cropRect.height() - Math.abs(
                bottomRight.y - bottomLeft.y
            )
        cutBottomRight.x =
            if (topRight.x > bottomRight.x) cropRect.width() - Math.abs(bottomRight.x - topRight.x) else cropRect.width()
        cutBottomRight.y =
            if (bottomLeft.y > bottomRight.y) cropRect.height() - Math.abs(bottomRight.y - bottomLeft.y) else cropRect.height()
        val width = cut.width.toFloat()
        val height = cut.height.toFloat()
        Log.d("CaptureActivity", "getBackCamera2IDCUT: len:$width  level $height")
        val src = floatArrayOf(
            cutTopLeft.x.toFloat(),
            cutTopLeft.y.toFloat(),
            cutTopRight.x.toFloat(),
            cutTopRight.y.toFloat(),
            cutBottomRight.x.toFloat(),
            cutBottomRight.y.toFloat(),
            cutBottomLeft.x.toFloat(),
            cutBottomLeft.y.toFloat()
        )
        val dst = floatArrayOf(0f, 0f, width, 0f, width, height, 0f, height)
        val matrix = Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, 4)
        val stretch = Bitmap.createBitmap(cut.width, cut.height, Bitmap.Config.ARGB_8888)
        val stretchCanvas = Canvas(stretch)
        //            stretchCanvas.drawBitmap(cut, matrix, null);
        stretchCanvas.concat(matrix)

        //generateVertices
        val WIDTH_BLOCK = 40
        val HEIGHT_BLOCK = 40
        val vertices = FloatArray((WIDTH_BLOCK + 1) * (HEIGHT_BLOCK + 1) * 2)
        val widthBlock = cut.width.toFloat() / WIDTH_BLOCK
        val heightBlock = cut.height.toFloat() / HEIGHT_BLOCK
        Log.d("CaptureActivity", "width+height $widthBlock, $heightBlock")
        for (i in 0..HEIGHT_BLOCK) for (j in 0..WIDTH_BLOCK) {
            vertices[i * ((HEIGHT_BLOCK + 1) * 2) + j * 2] = j * widthBlock
            vertices[i * ((HEIGHT_BLOCK + 1) * 2) + j * 2 + 1] = i * heightBlock
        }
        stretchCanvas.drawBitmapMesh(
            cut,
            WIDTH_BLOCK,
            HEIGHT_BLOCK,
            vertices,
            0,
            null,
            0,
            null
        )
        return stretch
    }
}