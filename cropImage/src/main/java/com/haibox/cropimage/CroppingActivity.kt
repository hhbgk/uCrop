package com.haibox.cropimage

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.haibox.cropimage.databinding.ActivityClipImageBinding
import java.io.ByteArrayOutputStream


class CroppingActivity: AppCompatActivity() {
    companion object {
        private const val TAG = "ClipImageActivity"
        const val KEY_CLIP_TYPE = "clip_type"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_RESULT_DATA = "result_data"

        @JvmStatic
        fun getClipIntent(context: Activity, filePath: String, type: Int): Intent {
            val intent = Intent()
            intent.setClass(context, CroppingActivity::class.java)
            intent.putExtra(KEY_CLIP_TYPE, type)
            intent.putExtra(KEY_FILE_PATH, filePath)
            return intent
        }
    }

    private lateinit var binding: ActivityClipImageBinding
    private var mType = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClipImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.clipViewLayout.visibility = View.VISIBLE
        mType = intent.getIntExtra(KEY_CLIP_TYPE, ClipView.ClipType.CIRCLE)
        initData()

        //设置点击事件监听器
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnOk.setOnClickListener { generateUriAndReturn() }
    }

    private fun initData() {
        val path = intent.getStringExtra(KEY_FILE_PATH)
        Log.i(TAG, "Type =$mType, $path")
        //设置图片资源
        binding.clipViewLayout.setImageSrc(path)
    }

    /**
     * 生成Uri并且通过setResult返回给打开的activity
     */
    private fun generateUriAndReturn() {
        //调用返回剪切图
        val zoomedCropBitmap = binding.clipViewLayout.clip()
        if (zoomedCropBitmap == null) {
            Log.e(TAG, "Zoomed Crop Bitmap is null")
            return
        }
        val intent = Intent()
        intent.putExtra(KEY_CLIP_TYPE, mType)
//        Log.i(TAG, "zoomed CropBitmap=" + zoomedCropBitmap.width + ", " + zoomedCropBitmap.height)
        val byteStream = ByteArrayOutputStream()
        zoomedCropBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteStream)
        val data = byteStream.toByteArray()
        intent.putExtra(KEY_RESULT_DATA, data)
        byteStream.close()

        setResult(RESULT_OK, intent)
        finish()
    }
}