package com.example.autocapture

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.autocapture.auto.AutoCaptureFragment
import com.example.autocapture.manual.ManualCaptureFragment


class CaptureActivity : AppCompatActivity() {
    private fun getBackCamera2ID(): Int {
        var value = -1
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            manager.cameraIdList.firstOrNull {
                manager.getCameraCharacteristics(it)
                val len = manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING)
                val level = manager.getCameraCharacteristics(it).get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                Log.d("CaptureActivity", "getBackCamera2ID: len:$len  level $level")
                len == CameraMetadata.LENS_FACING_BACK && level != CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
            }?.let {
                value = CameraMetadata.LENS_FACING_BACK
            }
        } catch (e: Exception) {
            Log.e("CaptureActivity", "getBackCamera2ID: ${e.toString()}" )
        }
        return value
    }

    private val requestPermissionListener = registerForActivityResult(ActivityResultContracts.RequestPermission()){
        if(!it){
            finishAndRemoveTask()
        }else{
            val backCameraId = getBackCamera2ID()
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && backCameraId>=0){
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, AutoCaptureFragment.newInstance(backCameraId))
                    .commitNow()
            }else{
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, ManualCaptureFragment.newInstance())
                    .commitNow()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestPermissionListener.launch(Manifest.permission.CAMERA)
    }
}