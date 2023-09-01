package com.haibox.cropimage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.haibox.cropimage.databinding.ActivityClipImageBinding;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 头像裁剪Activity
 */
public class ClipImageActivity extends AppCompatActivity {
    private static final String TAG = "ClipImageActivity";
    private ActivityClipImageBinding binding;
    public static final String KEY_CLIP_TYPE = "clip_type";
    private int mType;

    @NonNull
    public static Intent getClipIntent(Activity context, Uri uri, int type) {
        Intent intent = new Intent();
        intent.setClass(context, ClipImageActivity.class);
        intent.putExtra(KEY_CLIP_TYPE, type);
        intent.setData(uri);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClipImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.clipViewLayout.setVisibility(View.VISIBLE);
        Uri uri = getIntent().getData();
        //设置图片资源
        binding.clipViewLayout.setImageSrc(uri);
        mType = getIntent().getIntExtra(KEY_CLIP_TYPE, ClipView.ClipType.CIRCLE);
        Log.i(TAG, "onCreate: Type =" + mType + ", " + uri);

        //设置点击事件监听器
        binding.ivBack.setOnClickListener(v -> finish());
        binding.btnCancel.setOnClickListener(v -> finish());
        binding.btnOk.setOnClickListener(v -> generateUriAndReturn());
    }

    /**
     * 生成Uri并且通过setResult返回给打开的activity
     */
    private void generateUriAndReturn() {
        //调用返回剪切图
        Bitmap zoomedCropBitmap = binding.clipViewLayout.clip();
        if (zoomedCropBitmap == null) {
            Log.e(TAG, "Zoomed Crop Bitmap is null");
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(KEY_CLIP_TYPE, mType);

        Log.i(TAG, "zoomed CropBitmap=" + zoomedCropBitmap.getWidth() + ", " + zoomedCropBitmap.getHeight());
        Uri saveUri = Uri.fromFile(new File(getExternalCacheDir(), "tmp_img_cropped_" + System.currentTimeMillis() + ".jpg"));
        if (saveUri != null) {
            OutputStream outputStream = null;
            try {
                outputStream = getContentResolver().openOutputStream(saveUri);
                if (outputStream != null) {
                    zoomedCropBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                }
            } catch (IOException ex) {
                Log.e(TAG, "Cannot open file: " + saveUri, ex);
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            intent.setData(saveUri);
        } else {
            Log.e(TAG, "Cannot generate uri");
        }
        setResult(RESULT_OK, intent);
        finish();
    }
}