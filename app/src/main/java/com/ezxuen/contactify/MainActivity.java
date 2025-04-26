package com.ezxuen.contactify;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ezxuen.contactify.utils.DatabaseHelper;
import com.ezxuen.contactify.utils.ExcelLoader;
import com.ezxuen.contactify.utils.OcrCaptureHandler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FloatingActionButton fabMain;
    private RecyclerView categoryRecyclerView;
    private TextView headerText;
    private ImageView backArrow;
    private Uri imageUri;
    private TextView emptyMessage;

    private enum Level { INDUSTRY, FIELD, CONTACT }
    private Level currentLevel = Level.INDUSTRY;
    private String selectedIndustry = null;
    private String selectedField = null;

    private DatabaseHelper dbHelper;
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean cameraGranted = Boolean.TRUE.equals(result.getOrDefault(android.Manifest.permission.CAMERA, false));
                boolean readGranted = Boolean.TRUE.equals(result.getOrDefault(android.Manifest.permission.READ_EXTERNAL_STORAGE, false));

                if (cameraGranted && readGranted) {
                    showImageSourceDialog();
                }
            });
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    Log.d(TAG, "Selected image URI: " + imageUri);

                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        inputStream.close();

                        Toast.makeText(this, "Image selected. Running OCR...", Toast.LENGTH_SHORT).show();

                        InputImage image = InputImage.fromBitmap(bitmap, 0);
                        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                        recognizer.process(image)
                                .addOnSuccessListener(visionText -> {
                                    String extractedText = visionText.getText();
                                    Log.d("OCR", "Extracted text:\n" + extractedText);

                                    if (extractedText.trim().isEmpty()) {
                                        Toast.makeText(this, "No text recognized.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    Intent intent = new Intent(MainActivity.this, ReviewActivity.class);
                                    intent.putExtra("ocr_result", extractedText);
                                    startActivity(intent);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("OCR", "Failed to process image for text", e);
                                    Toast.makeText(this, "OCR failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });

                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        emptyMessage = findViewById(R.id.emptyMessage);
        dbHelper = new DatabaseHelper(this);
        fabMain = findViewById(R.id.fabMain);
        categoryRecyclerView = findViewById(R.id.categoryRecyclerView);
        headerText = findViewById(R.id.headerText);
        backArrow = findViewById(R.id.backArrow);
        dbHelper.insertExample5Industries(); // remove
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        backArrow.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        if (!hasIndustries()) {
            Log.d(TAG, "Industries not found. Loading from Excel...");
            ExcelLoader.loadIndustriesAndFields(getApplicationContext());
        }

        loadIndustries();

        fabMain.setOnClickListener(view -> {
            showImageSourceDialog();
            requestPermissionsAndShowDialog();
        });



        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentLevel == Level.CONTACT) {
                    loadFields(selectedIndustry);
                } else if (currentLevel == Level.FIELD) {
                    loadIndustries();
                } else {
                    finish();
                }
            }
        });
    }
    private void requestPermissionsAndShowDialog() {
        boolean cameraGranted = checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        boolean readGranted = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;

        if (cameraGranted && readGranted) {
            showImageSourceDialog();
        } else {
            String[] permissions = {
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            };
            permissionLauncher.launch(permissions);
        }
    }
    private void showImageSourceDialog() {
        String[] options = {"Camera", "Gallery"};

        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else if (which == 1) {
                        openGallery();
                    }
                })
                .show();
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }
    private void openCamera() {
        OcrCaptureHandler.startCameraCapture(this);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        OcrCaptureHandler.handleResult(this, requestCode, resultCode, data, rawText -> {
            if (rawText != null && !rawText.trim().isEmpty()) {
                Intent intent = new Intent(MainActivity.this, ReviewActivity.class);
                intent.putExtra("ocr_result", rawText);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No text recognized.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (currentLevel == Level.INDUSTRY) {
            loadIndustries();
        } else if (currentLevel == Level.FIELD && selectedIndustry != null) {
            loadFields(selectedIndustry);
        } else if (currentLevel == Level.CONTACT && selectedField != null) {
            loadContacts(selectedField);
        }
    }

    private boolean hasIndustries() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_INDUSTRIES, null);
        boolean result = false;
        if (cursor.moveToFirst()) result = cursor.getInt(0) > 0;
        cursor.close();
        db.close();
        return result;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickImageLauncher.launch(Intent.createChooser(intent, "Select a business card image"));
    }

    private void loadIndustries() {
        currentLevel = Level.INDUSTRY;
        headerText.setText("Industries");
        backArrow.setVisibility(View.GONE);

        ArrayList<String> industries = dbHelper.getIndustriesWithContacts();
        if (industries.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
            categoryRecyclerView.setVisibility(View.GONE);
        } else {
            emptyMessage.setVisibility(View.GONE);
            categoryRecyclerView.setVisibility(View.VISIBLE);
            categoryRecyclerView.setAdapter(new IndustryAdapter(industries, industry -> {
                selectedIndustry = industry;
                loadFields(industry);
            }));
        }
    }

    private void loadFields(String industry) {
        currentLevel = Level.FIELD;
        headerText.setText(industry);
        backArrow.setVisibility(View.VISIBLE);

        ArrayList<String> fields = dbHelper.getFieldsWithContacts(industry);
        categoryRecyclerView.setAdapter(new FieldAdapter(fields, field -> {
            selectedField = field;
            loadContacts(field);
        }));
    }

    private void loadContacts(String field) {
        currentLevel = Level.CONTACT;
        headerText.setText(field);
        backArrow.setVisibility(View.VISIBLE);

        LinkedHashMap<String, ArrayList<Pair<Integer, String>>> groupedContacts =
                dbHelper.getGroupedContactsByField(field);

        categoryRecyclerView.setAdapter(new GroupedContactAdapter(groupedContacts, contactId -> {
            Intent intent = new Intent(MainActivity.this, ReviewActivity.class);
            intent.putExtra("contact_id", contactId);
            startActivity(intent);
        }));
    }
}