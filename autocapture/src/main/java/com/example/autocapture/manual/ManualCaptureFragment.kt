package com.example.autocapture.manual

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.PictureFormat
import com.example.autocapture.ManualCaptureStatus
import com.example.autocapture.R

class ManualCaptureFragment : Fragment() {

    companion object {
        fun newInstance() = ManualCaptureFragment()
    }

    private var cameraView: CameraView?=null
    private var actionClose:ImageView?=null
    private var ivResult:ImageView?=null
    private var actionCapture:ImageView?=null
    private var titleCapture:TextView?=null
    private var captureGuide:TextView?=null
    private var actionCancel:TextView?=null
    private var actionConfirm:TextView?=null
    private var captureResultContainer: ConstraintLayout?=null

    private val viewModel: ManualCaptureViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.manual_capture_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraView = view.findViewById(R.id.cameraView)
        actionClose = view.findViewById(R.id.actionClose)
        ivResult = view.findViewById(R.id.ivResult)
        actionCapture = view.findViewById(R.id.actionCapture)
        titleCapture = view.findViewById(R.id.titleCapture)
        captureGuide = view.findViewById(R.id.captureGuide)
        actionCancel = view.findViewById(R.id.actionCancel)
        actionConfirm = view.findViewById(R.id.actionConfirm)
        captureResultContainer = view.findViewById(R.id.captureResultContainer)
        setupCamera()
        dataReady()
        userActions()
    }

    private fun userActions() {
        actionClose?.setOnClickListener {
            activity?.finishAndRemoveTask()
        }

        actionConfirm?.setOnClickListener {
            if(viewModel.manualCaptureStatus.value == ManualCaptureStatus.FRONT_CAPTURE_DONE){
                viewModel.changeCaptureStatus(ManualCaptureStatus.BACK_CAPTURE)
            }else{
                viewModel.createResult(requireContext())
            }
        }
        actionCapture?.setOnClickListener {
            if(cameraView?.isTakingPicture == false){
                cameraView?.takePictureSnapshot()
            }
        }

        actionCancel?.setOnClickListener {
            if(viewModel.manualCaptureStatus.value == ManualCaptureStatus.FRONT_CAPTURE_DONE){
                viewModel.clearBitmap(true)
                viewModel.changeCaptureStatus(ManualCaptureStatus.FRONT_CAPTURE)
            }else{
                viewModel.clearBitmap(false)
                viewModel.changeCaptureStatus(ManualCaptureStatus.BACK_CAPTURE)
            }
        }


    }

    private fun dataReady() {
        viewModel.createBitmapLD.observe(viewLifecycleOwner){
            if(it  == "ok"){
                activity?.setResult(Activity.RESULT_OK)
                activity?.finishAndRemoveTask()
            }else{
                Toast.makeText(requireContext(),"Có lỗi xảy ra vui lòng thử lại. \n$it", Toast.LENGTH_SHORT).show()
            }

        }
        viewModel.manualCaptureStatus.observe(viewLifecycleOwner){
            val captured = it == ManualCaptureStatus.FRONT_CAPTURE_DONE || it == ManualCaptureStatus.BACK_CAPTURE_DONE

            captureResultContainer?.isVisible = captured
            actionCapture?.isVisible = !captured
            when(it){
                ManualCaptureStatus.FRONT_CAPTURE->{
                    cameraView?.open()
                    captureGuide?.text = getString(R.string.capture_front_of_card_title)
                }
                ManualCaptureStatus.FRONT_CAPTURE_DONE->{
                    captureGuide?.text = getString(R.string.capture_front_of_card_title)
                    actionConfirm?.text = getString(R.string.capture_back)
                    cameraView?.close()
                }
                ManualCaptureStatus.BACK_CAPTURE->{
                    cameraView?.open()
                    captureGuide?.text = getString(R.string.capture_back_of_card_title)
                }
                ManualCaptureStatus.BACK_CAPTURE_DONE->{
                    captureGuide?.text = getString(R.string.capture_back_of_card_title)
                    actionConfirm?.text = getString(R.string.confirm_info)
                    cameraView?.close()
                }
            }
        }

    }

    private fun setupCamera() {
        cameraView?.run {
            setLifecycleOwner(viewLifecycleOwner)
            pictureFormat = PictureFormat.JPEG
            useDeviceOrientation = false
            addCameraListener( object :CameraListener(){

                override fun onPictureTaken(result: PictureResult) {
                    super.onPictureTaken(result)

                    result.toBitmap{
                        it?.let {
                            if(viewModel.manualCaptureStatus.value == ManualCaptureStatus.FRONT_CAPTURE){
                                viewModel.saveBitmap(true,it)
                                viewModel.changeCaptureStatus(ManualCaptureStatus.FRONT_CAPTURE_DONE)
                            }else{
                                viewModel.saveBitmap(false,it)
                                viewModel.changeCaptureStatus(ManualCaptureStatus.BACK_CAPTURE_DONE)
                            }
                            //ivResult?.setImageBitmap(it)
                        }?: kotlin.run {
                            Toast.makeText(requireContext(),"Có lỗi xảy ra vui lòng thử lại.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }

    }

    override fun onResume() {
        super.onResume()
        if(viewModel.manualCaptureStatus.value == ManualCaptureStatus.FRONT_CAPTURE || viewModel.manualCaptureStatus.value == ManualCaptureStatus.BACK_CAPTURE){
            cameraView?.open()
        }
    }

    override fun onPause() {
        super.onPause()
        cameraView?.close()
    }

    override fun onDestroy() {
        cameraView?.destroy()
        super.onDestroy()
    }

}