package com.haibox.cropping.sample;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.haibox.cropimage.ClipView;
import com.haibox.cropimage.CroppingActivity;
import com.haibox.cropping.sample.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Des:
 * author: Bob
 * date: 2023/08/31
 */
public class MainActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();

    private ActivityMainBinding binding;
    private SelectionPopupWindow selectionPopup;
    private int selectType = -1;
    private final String TMP_FILENAME = "cropping-image.jpeg";
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.i(TAG, "VERSION SDK INT="+ Build.VERSION.SDK_INT);
        binding.llQQ.setOnClickListener(listener);
        binding.llWechat.setOnClickListener(listener);
        binding.llWechatGrid.setOnClickListener(listener);
    }

    private final View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == binding.llQQ) {
                selectType = ClipView.ClipType.CIRCLE;
            } else if (v == binding.llWechat) {
                selectType = ClipView.ClipType.RECTANGLE;
            } else if (v == binding.llWechatGrid) {
                selectType = ClipView.ClipType.PALACE;
            }
            selectPicture();
        }
    };

    private void selectPicture() {
        if (selectionPopup == null) {
            selectionPopup = new SelectionPopupWindow(binding.getRoot(), new SelectionPopupWindow.OnSelectionListener() {
                @Override
                public void onPhoto() {
                    selectionPopup.hide();
                    checkCameraPermission();
                }

                @Override
                public void onGallery() {
                    selectionPopup.hide();
                    checkStoragePermission();
                }

                @Override
                public void onDismiss() {

                }
            });
        }
        if (!selectionPopup.isShowing()) {
            selectionPopup.show();
        } else {
            selectionPopup.hide();
        }
    }
    private final ActivityResultLauncher<String> launchCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
                if (result) {
                    takePicture();
                } else {
                    Log.e(TAG, "No camera permission");
                }
            });
    private void checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePicture();
        } else {
            launchCameraPermission.launch(android.Manifest.permission.CAMERA);
        }
    }


    private File photoFile = null;
    private Uri photoUri = null;
    private final ActivityResultLauncher<Uri> launchTakePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result) {
                        binding.ivAvatar.setImageURI(null);
                        binding.ivAvatar.setImageURI(photoUri);

                        gotoClipActivity(photoFile.getAbsolutePath());// take picture
                    } else {
                        Log.e(TAG, "Fail to take picture");
                    }
                }
            });

    private void takePicture() {
        if (photoFile != null && photoFile.exists()) {
            if (!photoFile.delete()) {
                Log.e(TAG, "Delete failed");
            }
        }
        photoFile = new File(getExternalCacheDir(), TMP_FILENAME);
        photoUri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", photoFile);
        launchTakePicture.launch(photoUri);
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            launchPicturePicker();
        } else {
            Log.i(TAG, "Request storage permission");
            launchStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }
    private final ActivityResultLauncher<String> launchStoragePermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
                if (result) {
                    launchPicturePicker();
                } else {
                    Log.e(TAG, "No storage permission");
                }
            });
    private void launchPicturePicker() {
        if (photoFile != null && photoFile.exists()) {
            if (!photoFile.delete()) {
                Log.e(TAG, "Delete failed");
            }
        }
        picturePicker.launch("image/*");
    }

    private final ActivityResultLauncher<String> picturePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
                if (result == null) {
                    Log.e(TAG, "Picker uri is null");
                    return;
                }
                Log.i(TAG, "path=" + result.getPath());
                binding.ivAvatar.setImageURI(result);
                copyPicturePicker(result);// picture picker
            });

    private void copyPicturePicker(final Uri uri) {
        if (uri != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    InputStream inputStream;
                    try {
                        inputStream = getContentResolver().openInputStream(uri);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    photoFile = new File(getExternalCacheDir(), TMP_FILENAME);
                    OutputStream outputStream;
                    try {
                        outputStream = new FileOutputStream(photoFile);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 10, outputStream);
                    runOnUiThread(() -> gotoClipActivity(photoFile.getAbsolutePath()));
                    try {
                        inputStream.close();
                        outputStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

    private void gotoClipActivity(String path) {
        launcherCroppingActivity.launch(CroppingActivity.getClipIntent(MainActivity.this, path, selectType));
    }

    private final ActivityResultLauncher<Intent> launcherCroppingActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if(result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    if (intent == null) {
                        return;
                    }
                    int type = intent.getIntExtra(CroppingActivity.KEY_CLIP_TYPE, -1);
                    byte[] bytes  = intent.getByteArrayExtra(CroppingActivity.KEY_RESULT_DATA);
                    if (bytes != null) {
                        Log.i(TAG, "onActivityResult: bytes =" + bytes.length * 1.0f / 1024 + "KB");
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (type == ClipView.ClipType.CIRCLE) {
                            binding.civAvatar1.setImageBitmap(bitmap);
                        } else if (type == ClipView.ClipType.RECTANGLE) {
                            binding.civAvatar2.setImageBitmap(bitmap);
                        } else if (type == ClipView.ClipType.PALACE) {
                            binding.civAvatar3.setImageBitmap(bitmap);
                        }
                    } else {
                        Log.e(TAG, "bitmap is null");
                    }
                }
            });

    private void saveBitmap(Bitmap bitmap, @NonNull File file) {
        Log.i(TAG, "saveBitmapToSDCard: " + file.getAbsolutePath());
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}