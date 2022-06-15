package com.example.autocapture;

import android.content.res.AssetManager;
import android.util.Log;
import android.view.Surface;

class NcnnYolov5 {
    external fun loadModel(mgr: AssetManager?, modelid: Int, cpugpu: Int): Boolean
    external fun openCamera(facing: Int): Boolean
    external fun closeCamera(): Boolean
    external fun setOutputWindow(surface: Surface?): Boolean
    external fun hasFace(): Boolean
    external fun getCorners(corners: IntArray?): IntArray?

    companion object {
        var corners = intArrayOf(0, 0, 0, 0, 0, 0 , 0 ,0 , 0, 0, 0, 0)
        fun printCorners() {
            Log.d(
                "printCorners", corners[0].toString() + ", " + corners[1] + ", " + corners[2] +
                        ", " + corners[3] + ", " + corners[4] + ", " + corners[5] + ", " + corners[6]
                        + ", " + corners[7] + ", " + corners[8] + ", " + corners[9] + ", " + corners[10] + ", " + corners[11]
            )
        }

        init {
            System.loadLibrary("ncnnyolov5")
        }
    }
}
