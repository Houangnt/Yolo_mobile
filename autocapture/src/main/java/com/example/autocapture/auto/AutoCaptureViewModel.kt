package com.example.autocapture.auto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.util.Log
import android.util.Size
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.example.autocapture.*
import com.example.autocapture.face.FaceDetectorProcessor
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.resume

class AutoCaptureViewModel : ViewModel() {
    companion object {
        const val TAG = "AutoCaptureViewModel"
    }

    var frontBitmap: Bitmap? = null
    var backBitmap: Bitmap? = null


    var captureJob: Job? = null
    val cardFaceLD = MutableLiveData(CardFace.FRONT)

    val captureStatusLD = MutableLiveData(CardCaptureStatus.START)
    val createBitmapLD = MutableLiveData<String>()

    fun changeCardFace(value: Int) {
        cardFaceLD.postValue(value)
    }

    fun changeCaptureStatus(value: Int) {
        captureStatusLD.postValue(value)
    }

    val runRepeatLD = MutableLiveData<Boolean>()
    fun startCapture(start: Boolean) {
        if (start) {
            captureJob?.cancel()
            captureJob = null
            captureJob = viewModelScope.launch(Dispatchers.IO) {
                while (isActive) {
                    Log.d(TAG, "startCapture: running")
                    delay(1200)
                    runRepeatLD.postValue(true)
                }
            }
        } else {
            captureJob?.cancel()
            captureJob = null
        }

    }

    suspend  fun  getFaceOnImage(detectorProcessor: FaceDetectorProcessor,bitmap:Bitmap) : Int{
        return suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            detectorProcessor.detectInImage(image).addOnCompleteListener {
                cont.resume(it.result.size)
            }
        }
    }


    private fun getAbsoluteCoordinate(point: Point, rgb: Size, screen: Size): Point {
        if (rgb.width > 0 && rgb.height > 0) {
            point.x = point.x * screen.width / rgb.width
            point.y = point.y * screen.height / rgb.height
        } else {
            point.x = 0
            point.y = 0
        }
        //Log.d(TAG, "Rect Absolute x: " + point.x + " y: " + point.y)
        return point
    }

    fun captureBitmap(context: Context, bmp: Bitmap, originConers: IntArray): Bitmap? {
        try {
            val rgbSize = Size(originConers[8], originConers[9])
            val screenSize = Size(bmp.width, bmp.height)
            Log.d(
                TAG,
                "captureBitmap originBitmap tl: [${originConers[0]}-${originConers[1]}]  tr: [${originConers[2]}-${originConers[3]}]  bl: [${originConers[4]}-${originConers[5]}]  br: [${originConers[6]}-${originConers[7]}]  rgb: [${originConers[8]}-${originConers[9]}]  bitmap: [${bmp.width}-${bmp.height}] "
            )
            // chuyển từ tọa độ frame sang tạo độ hiển thị (surface)
            val corners = correctCoordinateCorners(originConers, rgbSize, screenSize)
            val A = Point(corners[0], corners[1])
            val B = Point(corners[2], corners[3])
            val D = Point(corners[4], corners[5])
            val C = Point(corners[6], corners[7])
            // tìm tọa độ hình chữ nhật A1,B1,C1,D1 bao quanh cách ABCD 1 khoảng padding
            val k1 = Math.abs((A.y - C.y) * 1.0 / (A.x - C.x))
            val padding = context.resources.getDimensionPixelSize(R.dimen.card_padding)
            val AC = Math.sqrt(
                Math.pow((A.x - C.x).toDouble(), 2.0) + Math.pow(
                    (A.y - C.y).toDouble(),
                    2.0
                )
            )


            val AB = Math.sqrt(
                Math.pow((A.x - B.x).toDouble(), 2.0) + Math.pow(
                    (A.y - B.y).toDouble(),
                    2.0
                )
            )

            val AD = Math.sqrt(
                Math.pow((A.x - D.x).toDouble(), 2.0) + Math.pow(
                    (A.y - D.y).toDouble(),
                    2.0
                )
            )
            // tim E tam giác ADE vuông cân
            val kDC = Math.abs((D.y - C.y) * 1.0 / (D.x - C.x))
            val Ex = D.x+ (AD / Math.sqrt((kDC * kDC + 1))).toInt()
            val Ey = if(D.y<C.y) D.y+ (kDC*Ex).toInt() else D.y- (kDC*Ex).toInt()
            val E =  Point(Ex, Ey)
            val KAE = Math.abs((A.y - E.y) * 1.0 / (A.x - E.x))
            val deltaX1 = (padding*Math.sqrt(2.0) / Math.sqrt((KAE * KAE + 1))).toInt()
            val deltaY1 = (deltaX1*KAE).toInt()
            Log.d(
                TAG,
                "captureBitmap absoluteBitmap tl: [${A.x}-${A.y}]  tr: [${B.x}-${B.y}] bl: [${D.x}-${D.y}] br: [${C.x}-${C.y}]  ratio ${AB / bmp.width}"
            )
            // tim F tam giác BCF vuông cân
            val Fx = C.x+ (AD / Math.sqrt((kDC * kDC + 1))).toInt()
            val Fy = if(D.y<C.y) C.y- (kDC*Ex).toInt() else C.y+ (kDC*Ex).toInt()
            val F =  Point(Fx, Fy)
            val KFB = Math.abs((B.y - F.y) * 1.0 / (B.x - F.x))
            val deltaX2 = (padding*Math.sqrt(2.0) / Math.sqrt((KFB * KFB + 1))).toInt()
            val deltaY2 = (deltaX1*KFB).toInt()
/*
            Log.d(
                TAG,
                "captureBitmap absoluteBitmap tl: [${A.x}-${A.y}]  tr: [${B.x}-${B.y}] bl: [${D.x}-${D.y}] br: [${C.x}-${C.y}]  ratio ${AB / bmp.width}"
            )
            val A1A = (AC * padding) / AB

            val deltaX1 = (A1A / Math.sqrt((k1 * k1 + 1))).toInt()
            val deltaY1 = (k1 * deltaX1).toInt()

            val k2 = Math.abs((D.y - B.y) * 1.0 / (D.x - B.x))

            // tính thêm BD, DC chứ ko lấy AC,AB để tăng độ chính xác do tính gần đúng của tọa độ
            val BD = Math.sqrt(
                Math.pow((B.x - D.x).toDouble(), 2.0) + Math.pow(
                    (B.y - D.y).toDouble(),
                    2.0
                )
            )
            val DC = Math.sqrt(
                Math.pow((D.x - C.x).toDouble(), 2.0) + Math.pow(
                    (D.y - C.y).toDouble(),
                    2.0
                )
            )
            val DD1 = (BD * padding) / DC
            val deltaX2 = (DD1 / Math.sqrt(k2 * k2 + 1)).toInt()
            val deltaY2 = (k2 * deltaX2).toInt()

            Log.d(TAG, "captureBitmap absoluteBitmap AB:$AB AC:$AC BD:$BD DC:$DC deltaX2:$deltaX2 deltaY2:$deltaY2  deltaX1:$deltaX1  deltaY1:$deltaY1")*/

            val A1 = Point(A.x - deltaX1, A.y - deltaY1)
            val B1 = Point(B.x + deltaX2, B.y - deltaY2)
            val C1 = Point(C.x + deltaX1, C.y + deltaY1)
            val D1 = Point(D.x - deltaX2, D.y + deltaY2)
            Log.d(
                TAG,
                "captureBitmap padding tl: [${A1.x}-${A1.y}]  tr: [${B1.x}-${B1.y}] bl: [${D1.x}-${D1.y}] br: [${C1.x}-${C1.y}] E [${E.x}-${E.y}]deltaX1 $deltaX1 deltaY1 $deltaY1"
            )
            //cắt CCCD với viền có độ dày padding

            Log.d("CaptureActivity", "getBackCamera2ID: len:${bmp.width}  level ${bmp.height}")
            val test = BitmapUtils.crop(bmp, A1, B1, D1, C1)
            Log.d("CaptureActivity", "getBack: len:${test.width}  level ${test.height}")
            return BitmapUtils.crop(bmp, A1, B1, D1, C1)

        } catch (e: Exception) {
            Log.e(TAG, "captureBitmap: error $e")
            return null
        }

    }

    private fun correctCoordinateCorners(
        corners: IntArray,
        rgbSize: Size,
        screenSize: Size
    ): IntArray {
        val newCorners = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        if (rgbSize.width * rgbSize.height != 0) {
            for (i in 0..7) {
                if (i % 2 == 0) {
                    newCorners[i] = corners[i] * screenSize.width / rgbSize.width
                } else {
                    newCorners[i] = corners[i] * screenSize.height / rgbSize.height
                }
            }
        }
        return newCorners
    }

    fun saveBitmap(context: Context, bmp: Bitmap, corners: IntArray) {
        if (frontBitmap == null) {
            frontBitmap = captureBitmap(context, bmp, corners)
            changeCardFace(CardFace.BACK)
            changeCaptureStatus(CardCaptureStatus.START)
        } else {
            backBitmap = captureBitmap(context, bmp, corners)
            changeCaptureStatus(CardCaptureStatus.CARD_DONE)
        }
    }

    fun createResult(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val front = File(context.filesDir, CardImage.FRONT_CARD_IMAGE)
                if (front.exists()) {
                    front.delete()
                }
                context.openFileOutput(front.name, Context.MODE_PRIVATE).use {
                    frontBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    it.close()
                }
                val back = File(context.filesDir, CardImage.BACK_CARD_IMAGE)
                if (back.exists()) {
                    back.delete()
                }
                context.openFileOutput(back.name, Context.MODE_PRIVATE).use {
                    backBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    it.close()
                }
                "ok"
            } catch (e: Exception) {
                Log.e(TAG, "createResult error: ", e)
                e.toString()
            }
            createBitmapLD.postValue(result)
        }

    }
}