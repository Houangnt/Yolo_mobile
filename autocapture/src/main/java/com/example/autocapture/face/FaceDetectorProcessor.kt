package com.example.autocapture.face

import android.graphics.Bitmap
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_FAST

class FaceDetectorProcessor() {

    private val detector: FaceDetector

    init {
        val options = FaceDetectorOptions.Builder()
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setPerformanceMode(PERFORMANCE_MODE_FAST)
            .build()

        detector = FaceDetection.getClient(options)
    }

    fun stop() {
        detector.close()
    }

    fun detectFaceOnImage(bitmap: Bitmap, cb: (result: Int) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        detector.process(image)
            .addOnCompleteListener {
                cb.invoke(it.result.size)
            }
    }

    fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

}
