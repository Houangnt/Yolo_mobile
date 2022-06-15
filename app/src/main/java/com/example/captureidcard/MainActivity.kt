package com.example.captureidcard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import com.example.autocapture.CaptureActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object{
        const val FRONT_CARD_IMAGE= "front_card.jpg"
        const val BACK_CARD_IMAGE= "back_card.jpg"
    }

    private var captureResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val frontCard = File(this.filesDir, FRONT_CARD_IMAGE )
            val backCard = File(this.filesDir, BACK_CARD_IMAGE )
            if(!frontCard.exists()){
                Toast.makeText(this,"Thiếu ảnh mặt trước",Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            if(!backCard.exists()){
                Toast.makeText(this,"Thiếu ảnh mặt sau",Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            try {
                findViewById<ImageView>(R.id.imgFrontCard).setImageURI(Uri.fromFile(frontCard))
                findViewById<ImageView>(R.id.imgBackCard).setImageURI(Uri.fromFile(backCard))
                frontCard.delete()
                backCard.delete()
            }catch (e:Exception){
                Toast.makeText(this,"Có lỗi xảy ra vui lòng thử lại \n$e",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<AppCompatButton>(R.id.actionOpenCamera).setOnClickListener {
            findViewById<ImageView>(R.id.imgFrontCard).setImageURI(null)
            findViewById<ImageView>(R.id.imgBackCard).setImageURI(null)
            captureResult.launch(Intent(this,CaptureActivity::class.java))
        }
    }

    override fun onBackPressed() {
        finish()
    }

}