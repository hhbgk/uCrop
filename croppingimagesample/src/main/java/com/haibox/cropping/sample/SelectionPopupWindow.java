package com.haibox.cropping.sample;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;

import com.haibox.cropping.sample.databinding.ViewSelectionPopupBinding;

/**
 * Des:
 * author: Bob
 * date: 2023/08/31
 */
public class SelectionPopupWindow {
    private String TAG = getClass().getSimpleName();
    private PopupWindow popupWindow;
    private ViewSelectionPopupBinding binding;
    private View parentView;
    private OnSelectionListener onSelectionListener;

    public SelectionPopupWindow(View parentView, OnSelectionListener listener) {
        this.parentView = parentView;
        this.onSelectionListener = listener;
        View root = LayoutInflater.from(parentView.getContext()).inflate(R.layout.view_selection_popup, null);
        root.setFocusable(true);
        root.setFocusableInTouchMode(true);
        root.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                hide();
                return true;
            }
            return false;
        });

        View.OnAttachStateChangeListener l = new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                binding = ViewSelectionPopupBinding.bind(v);
                initUI();
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                v.removeOnAttachStateChangeListener(this);
            }
        };
        root.addOnAttachStateChangeListener(l);

        popupWindow = new PopupWindow(root, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        popupWindow.setClippingEnabled(false);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOnDismissListener(this::hide);
    }

    public Boolean isShowing() {
        return popupWindow.isShowing();
    }

    public void show() {
        if (!popupWindow.isShowing() && parentView.getWindowToken() != null) {
            int[] location = new int[2];
            parentView.getLocationOnScreen(location);

//            popupWindow.showAtLocation(parentView, Gravity.NO_GRAVITY, 0, location[1])
            popupWindow.showAsDropDown(parentView, 0, location[1], Gravity.NO_GRAVITY);
            popupWindow.update();
        }
    }

    public void hide() {
        if (popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
        onSelectionListener.onDismiss();
    }

    private void initUI() {
        binding.tvCancel.setOnClickListener(v -> hide());
        binding.viewOutside.setOnClickListener(v -> hide() );

        binding.tvPhoto.setOnClickListener(v -> onSelectionListener.onPhoto() );
        binding.tvMobileAlbum.setOnClickListener(v -> onSelectionListener.onGallery() );
    }

    public interface OnSelectionListener {
        void onPhoto();
        void onGallery();
        void onDismiss();
    }
}
