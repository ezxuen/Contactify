package com.ezxuen.contactify.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;

public class OcrCaptureHandler {

    public static final int REQUEST_CODE_CAMERA = 201;
    public static final int REQUEST_CODE_GALLERY = 202;

    private static Uri cameraImageUri;
    private static OcrCallback currentCallback;

    public interface OcrCallback {
        void onResult(String rawText);
    }

    // ==== CAMERA CAPTURE METHODS ====

    public static void startCameraCapture(Activity activity) {
        cameraImageUri = createImageUri(activity);
        if (cameraImageUri == null) return;

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        activity.startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }

    public static void startCameraCapture(Fragment fragment) {
        Activity activity = fragment.requireActivity();
        cameraImageUri = createImageUri(activity);
        if (cameraImageUri == null) return;

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        fragment.startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }

    // ==== GALLERY SELECTION METHODS ====

    public static void startGallerySelection(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    public static void startGallerySelection(Fragment fragment) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        fragment.startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    // ==== HANDLE RESULT ====

    public static void handleResult(Activity activity, int requestCode, int resultCode, @Nullable Intent data, OcrCallback callback) {
        if (resultCode != Activity.RESULT_OK) return;

        currentCallback = callback;
        InputImage image = null;

        try {
            if (requestCode == REQUEST_CODE_CAMERA && cameraImageUri != null) {
                image = InputImage.fromFilePath(activity, cameraImageUri);
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

        } catch (IOException e) {
            Log.e("OCR_HANDLER", "Error loading image: ", e);
            if (currentCallback != null) currentCallback.onResult("");
        }
    }

    // ==== OCR PROCESSING ====

    private static void extractTextFromImage(InputImage image) {
        try {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener(result -> {
                        StringBuilder extracted = new StringBuilder();
                        for (Text.TextBlock block : result.getTextBlocks()) {
                            extracted.append(block.getText()).append("\n");
                        }

                        // Replace bullets with newlines
                        String formatted = extracted.toString().replace("•", "\n").trim();

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

    // ==== URI CREATION UTILITY ====

    private static Uri createImageUri(Activity activity) {
        try {
            File imageFile = new File(activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "contactify_temp.jpg");

            return FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".provider",
                    imageFile
            );
        } catch (Exception e) {
            Log.e("OCR_HANDLER", "Failed to create image URI", e);
            return null;
        }
    }
}