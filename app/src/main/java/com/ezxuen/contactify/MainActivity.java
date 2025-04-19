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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ezxuen.contactify.utils.DatabaseHelper;
import com.ezxuen.contactify.utils.ExcelLoader;
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
    private Uri imageUri;

    private enum Level { INDUSTRY, FIELD, CONTACT }
    private Level currentLevel = Level.INDUSTRY;
    private String selectedIndustry = null;
    private String selectedField = null;

    private DatabaseHelper dbHelper;

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

        dbHelper = new DatabaseHelper(this);
        fabMain = findViewById(R.id.fabMain);
        categoryRecyclerView = findViewById(R.id.categoryRecyclerView);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (!hasIndustries()) {
            Log.d(TAG, "Industries not found. Loading from Excel...");
            ExcelLoader.loadIndustriesAndFields(getApplicationContext());
        }

        loadIndustries();

        fabMain.setOnClickListener(view -> openImagePicker());
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
        ArrayList<String> industries = dbHelper.getIndustriesWithContacts();
        categoryRecyclerView.setAdapter(new IndustryAdapter(industries, industry -> {
            selectedIndustry = industry;
            loadFields(industry);
        }));
    }

    private void loadFields(String industry) {
        currentLevel = Level.FIELD;
        ArrayList<String> fields = dbHelper.getFieldsWithContacts(industry);
        categoryRecyclerView.setAdapter(new FieldAdapter(fields, field -> {
            selectedField = field;
            loadContacts(field);
        }));
    }

    private void loadContacts(String field) {
        currentLevel = Level.CONTACT;

        LinkedHashMap<String, ArrayList<Pair<Integer, String>>> groupedContacts =
                dbHelper.getGroupedContactsByField(field);

        categoryRecyclerView.setAdapter(new GroupedContactAdapter(groupedContacts));
    }

    @Override
    public void onBackPressed() {
        if (currentLevel == Level.CONTACT) {
            loadFields(selectedIndustry);
        } else if (currentLevel == Level.FIELD) {
            loadIndustries();
        } else {
            super.onBackPressed();
        }
    }
}