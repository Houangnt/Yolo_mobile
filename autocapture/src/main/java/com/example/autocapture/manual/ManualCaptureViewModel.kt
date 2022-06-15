package com.example.autocapture.manual

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autocapture.CardFace
import com.example.autocapture.CardImage
import com.example.autocapture.ManualCaptureStatus
import com.example.autocapture.auto.AutoCaptureViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ManualCaptureViewModel : ViewModel() {
    val TAG = "ManualCaptureViewModel"
    val manualCaptureStatus = MutableLiveData(ManualCaptureStatus.FRONT_CAPTURE)

    val createBitmapLD = MutableLiveData<String>()

    var frontBitmap : Bitmap?=null
    var backBitmap : Bitmap?=null



    fun changeCaptureStatus(value:Int){
        manualCaptureStatus.postValue(value)
    }
    fun saveBitmap(front:Boolean, bmp: Bitmap){
        if(front){
            frontBitmap = bmp
        }else{
            backBitmap = bmp
        }
    }
    fun clearBitmap(front:Boolean){
        if(front){
            frontBitmap = null
        }else{
            backBitmap = null
        }
    }

    fun createResult(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val front = File(context.filesDir, CardImage.FRONT_CARD_IMAGE)
                if(front.exists()){
                    front.delete()
                }
                context.openFileOutput(front.name, Context.MODE_PRIVATE).use {
                    frontBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    it.close()
                }
                val back = File(context.filesDir, CardImage.BACK_CARD_IMAGE)
                if(back.exists()){
                    back.delete()
                }
                context.openFileOutput(back.name, Context.MODE_PRIVATE).use {
                    backBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    it.close()
                }
                "ok"
            }catch (e: Exception){
                Log.e(TAG, "createResult error: ",e )
                e.toString()
            }
            createBitmapLD.postValue(result)
        }

    }

}