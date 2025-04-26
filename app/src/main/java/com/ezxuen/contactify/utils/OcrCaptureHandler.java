package com.ezxuen.contactify.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class OcrCaptureHandler {

    public static final int REQUEST_CODE_CAMERA = 201;
    public static final int REQUEST_CODE_GALLERY = 202;

    private static OcrCallback currentCallback;

    public interface OcrCallback {
        void onResult(String rawText);
    }

    public static void startCameraCapture(Activity activity) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        activity.startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }

    public static void startCameraCapture(Fragment fragment) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        fragment.startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }


    public static void startGallerySelection(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    public static void startGallerySelection(Fragment fragment) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        fragment.startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }


    public static void handleResult(Activity activity, int requestCode, int resultCode, @Nullable Intent data, OcrCallback callback) {
        if (resultCode != Activity.RESULT_OK) return;

        currentCallback = callback;
        InputImage image = null;

        try {
            if (requestCode == REQUEST_CODE_CAMERA && data != null && data.getExtras() != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data"); // Get compressed Bitmap from camera
                if (photo != null) {
                    image = InputImage.fromBitmap(photo, 0);
                }
            } else if (requestCode == REQUEST_CODE_GALLERY && data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                image = InputImage.fromFilePath(activity, imageUri);
            }

            if (image != null) {
                InputImage finalImage = image;
                new Thread(() -> extractTextFromImage(finalImage)).start();
            } else {
                if (currentCallback != null) currentCallback.onResult("");
            }

        } catch (Exception e) {
            Log.e("OCR_HANDLER", "Error processing image", e);
            if (currentCallback != null) currentCallback.onResult("");
        }
    }


    private static void extractTextFromImage(InputImage image) {
        try {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener(result -> {
                        StringBuilder extracted = new StringBuilder();
                        for (Text.TextBlock block : result.getTextBlocks()) {
                            extracted.append(block.getText()).append("\n");
                        }

                        String formatted = extracted.toString()
                                .replace("•", "\n") // Optional: replace bullet points
                                .trim();

                        if (currentCallback != null) {
                            currentCallback.onResult(formatted);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("OCR_HANDLER", "OCR failed", e);
                        if (currentCallback != null) currentCallback.onResult("");
                    });
        } catch (Exception e) {
            Log.e("OCR_HANDLER", "Exception in OCR engine", e);
            if (currentCallback != null) currentCallback.onResult("");
        }
    }
}