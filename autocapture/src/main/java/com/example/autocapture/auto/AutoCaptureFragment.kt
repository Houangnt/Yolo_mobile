package com.example.autocapture.auto

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.example.autocapture.*
import com.example.autocapture.face.FaceDetectorProcessor

class AutoCaptureFragment : Fragment(), SurfaceHolder.Callback {

    companion object {
        private const val CAMERA_ID = "CAMERA_ID"
        fun newInstance(backCameraId: Int) = AutoCaptureFragment().apply {
            Log.i("AutoCaptureFragment", "backCameraId: $backCameraId")
            arguments = Bundle().apply {
                putInt(CAMERA_ID, backCameraId)
            }
        }
    }

    private val faceDetectorProcessor : FaceDetectorProcessor by lazy {
        FaceDetectorProcessor()
    }

    val cameraId:Int get() = arguments?.getInt("CAMERA_ID",1)?:1

    private var cameraView: SurfaceView?=null
    private var actionClose: ImageView?=null
    private var frontCardGuide: ImageView?=null
    private var backCardGuide: ImageView?=null
    private var imgCaptured: ImageView?=null
    private var msgCapture: TextView?=null
    private var msgStatus: TextView?=null
    private var mNcnnYolov5: NcnnYolov5? = null
    private var cameraViewContainer: ConstraintLayout? = null
    private var imgCapturedContainer: ConstraintLayout? = null

    private val  viewModel: AutoCaptureViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.auto_capture_fragment, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mNcnnYolov5 = NcnnYolov5().apply { loadModel(resources.assets, 0, 0) }
    }

    fun test(){

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        cameraView = view.findViewById(R.id.cameraView)
        actionClose = view.findViewById(R.id.actionClose)
        msgCapture = view.findViewById(R.id.msgCapture)
        msgStatus = view.findViewById(R.id.msgStatus)
        frontCardGuide = view.findViewById(R.id.frontCardGuide)
        backCardGuide = view.findViewById(R.id.backCardGuide)
        imgCaptured = view.findViewById(R.id.imgCaptured)
        cameraViewContainer = view.findViewById(R.id.cameraViewContainer)
        imgCapturedContainer = view.findViewById(R.id.imgCapturedContainer)

        cameraView?.holder?.let {
            it.setFormat(PixelFormat.RGBA_8888)
            it.addCallback(this)
        }
        actionClose?.setOnClickListener { v: View? -> activity?.finishAndRemoveTask() }
        handleDataResponse()
    }
    private fun handleDataResponse() {
        viewModel.runRepeatLD.observe(viewLifecycleOwner){
            cameraView?.let {surfaceView ->
                Bitmap.createBitmap(surfaceView.width, surfaceView.height, Bitmap.Config.ARGB_8888)?.let { bmp->
                    PixelCopy.request(surfaceView,bmp,{
                        mNcnnYolov5?.getCorners(NcnnYolov5.corners)?.let { corners->

                            val sum = corners.toList().subList(0,7).sum()
                            val hasInValidPoint =   corners.toList().subList(0,7).firstOrNull { it<=0 } != null
                            val wRgb = if(corners[8]>corners[9]) corners[8] else corners[9]
                            val AB = if(corners[0]*corners[1]*corners[2]*corners[3] == 0 || wRgb==0) 0 else Math.sqrt(Math.pow((corners[0]-corners[2]).toDouble(),2.0) + Math.pow((corners[1]-corners[3]).toDouble(),2.0)).toInt()
                            val ratio = if(wRgb == 0) 0.0 else AB*1.0/wRgb
                            Log.d("AutoCaptureFragment", "printCorners: ${corners.map { it }.joinToString(", ")} sum: $sum prod: $hasInValidPoint ratio $ratio surfaceView.width ${surfaceView.width} rgb width ${wRgb}")
                            imgCapturedContainer?.isVisible = false
                            faceDetectorProcessor.detectFaceOnImage(bmp){
                                when {
                                    sum == 0 -> {
                                        viewModel.changeCaptureStatus(CardCaptureStatus.NO_CARD)
                                    }
                                    viewModel.cardFaceLD.value == CardFace.FRONT && it !=1 ->{
                                        viewModel.changeCaptureStatus(CardCaptureStatus.CARD_NO_FACE)
                                    }
                                    viewModel.cardFaceLD.value == CardFace.BACK && it != 0 ->{
                                        viewModel.changeCaptureStatus(CardCaptureStatus.CARD_BACK_HAS_FACE)
                                    }
                                    hasInValidPoint -> {
                                        viewModel.changeCaptureStatus(CardCaptureStatus.CARD_NOT_FIT)
                                    }
                                    ratio <0.6->{
                                        viewModel.changeCaptureStatus(CardCaptureStatus.CARD_TOO_FAR)
                                    }

                                    ratio >0.8->{
                                        viewModel.changeCaptureStatus(CardCaptureStatus.CARD_TOO_NEAR)
                                    }
                                    viewModel.captureBitmap(requireContext().applicationContext,bmp,corners)!=null->{
                                        if (viewModel.captureStatusLD.value != CardCaptureStatus.CARD_OK) {
                                            viewModel.changeCaptureStatus(CardCaptureStatus.CARD_OK)
                                        }else{
                                            viewModel.saveBitmap(requireContext().applicationContext,bmp,corners)
                                            viewModel.backBitmap?.let {
                                                imgCapturedContainer?.isVisible = true
                                                imgCaptured?.setImageBitmap(it)
                                            }?: kotlin.run {
                                                viewModel.frontBitmap?.let {
                                                    imgCapturedContainer?.isVisible = true
                                                    imgCaptured?.setImageBitmap(it)
                                                }
                                            }
                                        }
                                    }
                                    else -> {
                                        viewModel.changeCaptureStatus(CardCaptureStatus.CARD_NOT_FIT)
                                    }
                                }
                            }


                        }

                    }, Handler(Looper.getMainLooper()))
                }
            }
        }

        viewModel.cardFaceLD.observe(viewLifecycleOwner) {
            if(it == CardFace.FRONT){
                msgCapture?.text = getString(R.string.capture_front_of_card_title)
                frontCardGuide?.setImageResource(R.drawable.ic_front_card_guide_active)
                backCardGuide?.setImageResource(R.drawable.ic_back_card_guide_deactivate)
            }else{
                msgCapture?.text = getString(R.string.capture_back_of_card_title)
                frontCardGuide?.setImageResource(R.drawable.ic_front_card_guide_deactivate)
                backCardGuide?.setImageResource(R.drawable.ic_back_card_guide_active)
            }
        }
        viewModel.createBitmapLD.observe(viewLifecycleOwner){
            if(it  == "ok"){
                activity?.setResult(Activity.RESULT_OK)
                activity?.finishAndRemoveTask()
            }else{
                Toast.makeText(requireContext(),"Có lỗi xảy ra vui lòng thử lại. \n$it", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.captureStatusLD.observe(viewLifecycleOwner) {
            when(it){
                CardCaptureStatus.START->{
                    msgStatus?.isVisible = false
                }
                CardCaptureStatus.CARD_NOT_FIT->{
                    msgStatus?.let {
                        it.isVisible = true
                        it.text = getString(R.string.card_not_fit)
                    }
                }
                CardCaptureStatus.NO_CARD->{
                    msgStatus?.let {
                        it.isVisible = true
                        it.text = if(viewModel.cardFaceLD.value == CardFace.FRONT) getString(R.string.no_card) else getString(R.string.no_card_back)
                    }
                }
                CardCaptureStatus.CARD_NO_FACE->{
                    msgStatus?.let {
                        it.isVisible = true
                        it.text = getString(R.string.card_no_face)
                    }
                }
                CardCaptureStatus.CARD_BACK_HAS_FACE->{
                    msgStatus?.let {
                        it.isVisible = true
                        it.text = getString(R.string.card_back_has_face)
                    }
                }
                CardCaptureStatus.CARD_OK->{
                    msgStatus?.let {
                        it.isVisible = true
                        it.text = getString(R.string.card_ok)
                    }
                }
                CardCaptureStatus.CARD_TOO_FAR->{
                    msgStatus?.let {
                        it.isVisible = true
                        it.text = getString(R.string.card_too_far)
                    }
                }
                CardCaptureStatus.CARD_TOO_NEAR->{
                    msgStatus?.let {
                        it.isVisible = true
                        it.text = getString(R.string.card_too_near)
                    }
                }
                CardCaptureStatus.CARD_DONE->{
                    viewModel.startCapture(false)
                    viewModel.createResult(requireContext().applicationContext)
                }
            }
        }
    }
    override fun surfaceCreated(holder: SurfaceHolder) {
        mNcnnYolov5?.setOutputWindow(holder.surface)
    }

    override fun onResume() {
        super.onResume()
        mNcnnYolov5?.openCamera(cameraId)
        viewModel.startCapture(true)
    }

    override fun onPause() {
        super.onPause()
        viewModel.startCapture(false)
        mNcnnYolov5?.closeCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        faceDetectorProcessor.stop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

}