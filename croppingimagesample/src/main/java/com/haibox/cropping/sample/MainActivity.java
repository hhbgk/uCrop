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
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.haibox.cropimage.ClipImageActivity;
import com.haibox.cropimage.ClipView;
import com.haibox.cropimage.util.FileUtils;
import com.haibox.cropping.sample.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
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
    private final ActivityResultLauncher<Intent> launchTakePhoto = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
//                binding.ivAvatar.setImageURI(photoUri);
                Bitmap bitMap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                binding.ivAvatar.setImageBitmap(bitMap);
                gotoClipActivity(Uri.fromFile(photoFile));
            }
        }
    });
    private void takePicture() {
        String filename = "img-" + simpleDateFormat.format(System.currentTimeMillis()) + ".jpg";
        photoFile = new File(getExternalCacheDir(), filename);
        Log.i(TAG, "file Path=" + photoFile.getAbsolutePath());

//        String path = getExternalFilesDir(null).getAbsolutePath() + "/"+ filename;
//        uri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", file);

       Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT >= 24) {
            // 设置7.0中共享文件，分享路径定义在xml/file_paths.xml
            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            Uri photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            grantUriPermission(getPackageName(), photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Log.i(TAG, "getPath=" + photoUri.getPath());
            Log.i(TAG, "getScheme=" + photoUri.getScheme());
            Log.i(TAG, "getAuthority=" + photoUri.getAuthority());
            Log.i(TAG, "uri=" + photoUri);
        } else {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
        }
        launchTakePhoto.launch(intent);
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
        picturePicker.launch("image/*");
    }

    private final ActivityResultLauncher<String> picturePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
                if (result == null) {
                    Log.e(TAG, "Picker uri is null");
                    return;
                }
                Log.i(TAG, "path=" + result.getPath());
                gotoClipActivity(result);
            });

    private final ActivityResultLauncher<Intent> launcherCroppingActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if(result.getResultCode() == Activity.RESULT_OK) {
                    final Uri uri = result.getData().getData();
                    if (uri == null) {
                        return;
                    }
                    int type = result.getData().getIntExtra(ClipImageActivity.KEY_CLIP_TYPE, -1);
                    String cropImagePath = FileUtils.getPath(getApplicationContext(), uri);
                    long fileSize = getFileSize(cropImagePath);
                    Log.i(TAG, "onActivityResult: fileSize =" + fileSize * 1.0f / 1024 + "KB");
                    Log.i(TAG, "type=" + type + ", uri=" + uri);
                    Bitmap bitMap = BitmapFactory.decodeFile(cropImagePath);
//                    binding.ivAvatar.setImageBitmap(bitMap);

                    String filename = "img-" + simpleDateFormat.format(System.currentTimeMillis()) + ".jpg";
                    File file = new File(getExternalCacheDir(), filename);
                    saveBitmap(bitMap, file);
                    if (type == ClipView.ClipType.CIRCLE) {
                        binding.civAvatar1.setImageBitmap(bitMap);
                    } else if (type == ClipView.ClipType.RECTANGLE) {
                        binding.civAvatar2.setImageBitmap(bitMap);
                    } else if (type == ClipView.ClipType.PALACE) {
                        binding.civAvatar3.setImageBitmap(bitMap);
                    }
                }
            });

    private void gotoClipActivity(Uri uri) {
        launcherCroppingActivity.launch(ClipImageActivity.getClipIntent(this, uri, selectType));
    }

    private static long getFileSize(String path) {
        if (TextUtils.isEmpty(path) || !new File(path).exists()) {
            return 0L;
        }

        return new File(path).length();
    }

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