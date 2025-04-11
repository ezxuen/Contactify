// Top of your file
package com.ezxuen.contactify;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.ezxuen.contactify.utils.DatabaseHelper;
import com.google.android.material.chip.Chip;
import com.google.mlkit.nl.entityextraction.*;

import java.util.ArrayList;
import java.util.HashMap;

public class ReviewActivity extends AppCompatActivity {

    EditText editName, editPhones, editEmails, editJobTitle, editCompany, editAddress, editWebsite;
    Spinner industryDropdown, fieldDropdown;
    ConstraintLayout unclassifiedContainer;
    TextView unclassifiedLabel;
    Button btnEdit, btnSave;

    EditText selectedTargetField = null;

    ArrayList<String> unclassifiedData = new ArrayList<>();
    ArrayList<Integer> chipIds = new ArrayList<>();
    boolean hasLowConfidenceData = true;

    HashMap<String, Integer> industryNameToId = new HashMap<>();
    HashMap<Integer, ArrayList<String>> fieldsByIndustry = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        initViews();
        loadIndustryAndFieldData();

        String ocrResult = getIntent().getStringExtra("ocr_result");
        Log.d("ReviewActivity", "OCR result: " + ocrResult);

        if (ocrResult != null && !ocrResult.isEmpty()) {
            parseAndDisplayFields(ocrResult);
        }

        btnEdit.setOnClickListener(v -> setFieldsEditable(true));
        btnSave.setOnClickListener(v -> saveContact());
    }

    private void initViews() {
        editName = findViewById(R.id.editName);
        editPhones = findViewById(R.id.editPhones);
        editEmails = findViewById(R.id.editEmails);
        editJobTitle = findViewById(R.id.editJobTitle);
        editCompany = findViewById(R.id.editCompany);
        editAddress = findViewById(R.id.editAddress);
        editWebsite = findViewById(R.id.editWebsite);
        industryDropdown = findViewById(R.id.industryDropdown);
        fieldDropdown = findViewById(R.id.fieldDropdown);

        unclassifiedContainer = findViewById(R.id.unclassifiedContainer);
        unclassifiedLabel = findViewById(R.id.unclassifiedLabel);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);

        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (hasFocus && v instanceof EditText) {
                selectedTargetField = (EditText) v;
            }
        };

        editName.setOnFocusChangeListener(focusListener);
        editPhones.setOnFocusChangeListener(focusListener);
        editEmails.setOnFocusChangeListener(focusListener);
        editJobTitle.setOnFocusChangeListener(focusListener);
        editCompany.setOnFocusChangeListener(focusListener);
        editAddress.setOnFocusChangeListener(focusListener);
        editWebsite.setOnFocusChangeListener(focusListener);
    }

    private void loadIndustryAndFieldData() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        ArrayList<String> industryList = dbHelper.getAllIndustryNames();
        industryNameToId = dbHelper.getIndustryNameToIdMap();
        fieldsByIndustry = dbHelper.getFieldsGroupedByIndustry();

        ArrayAdapter<String> industryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, industryList);
        industryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        industryDropdown.setAdapter(industryAdapter);

        industryDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedIndustry = parent.getItemAtPosition(position).toString();
                Integer industryId = industryNameToId.get(selectedIndustry);
                if (industryId != null) {
                    ArrayList<String> fields = fieldsByIndustry.get(industryId);
                    if (fields != null) {
                        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(ReviewActivity.this, android.R.layout.simple_spinner_item, fields);
                        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        fieldDropdown.setAdapter(fieldAdapter);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void parseAndDisplayFields(String rawText) {
        EntityExtractorOptions options = new EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build();
        EntityExtractor extractor = EntityExtraction.getClient(options);

        editEmails.setText("");
        editPhones.setText("");
        editAddress.setText("");

        ArrayList<String> alreadyUsedLines = new ArrayList<>();
        unclassifiedData.clear();

        extractor.downloadModelIfNeeded()
                .addOnSuccessListener(unused -> {
                    EntityExtractionParams params = new EntityExtractionParams.Builder(rawText).build();
                    extractor.annotate(params)
                            .addOnSuccessListener(annotations -> {
                                for (EntityAnnotation annotation : annotations) {
                                    String line = annotation.getAnnotatedText().trim();
                                    if (alreadyUsedLines.contains(line)) continue;

                                    for (Entity entity : annotation.getEntities()) {
                                        switch (entity.getType()) {
                                            case Entity.TYPE_EMAIL:
                                                appendToEditText(editEmails, line);
                                                alreadyUsedLines.add(line);
                                                break;
                                            case Entity.TYPE_PHONE:
                                                if (isValidPhone(line) && !looksLikeAddress(line)) {
                                                    appendToEditText(editPhones, line);
                                                    alreadyUsedLines.add(line);
                                                } else {
                                                    Log.d("MLKit", "❌ Rejected as phone: " + line);
                                                }
                                                break;
                                            case Entity.TYPE_ADDRESS:
                                                appendToEditText(editAddress, line);
                                                alreadyUsedLines.add(line);
                                                break;
                                        }
                                    }
                                }

                                for (String line : rawText.split("\n")) {
                                    String trimmed = line.trim();
                                    if (!trimmed.isEmpty() && !alreadyUsedLines.contains(trimmed)) {
                                        unclassifiedData.add(trimmed);
                                    }
                                }

                                if (!unclassifiedData.isEmpty()) {
                                    if (unclassifiedLabel != null) unclassifiedLabel.setVisibility(View.VISIBLE);
                                    if (unclassifiedContainer != null) {
                                        unclassifiedContainer.setVisibility(View.VISIBLE);
                                        for (String item : unclassifiedData) addChip(item);
                                    }
                                    setFieldsEditable(true);
                                } else {
                                    hasLowConfidenceData = false;
                                    btnEdit.setVisibility(View.VISIBLE);
                                    setFieldsEditable(false);
                                }
                            })
                            .addOnFailureListener(e -> Log.e("ReviewActivity", "Entity extraction failed", e));
                })
                .addOnFailureListener(e -> Log.e("ReviewActivity", "Model download failed", e));
    }

    private boolean isValidPhone(String value) {
        return value.matches("^(\\+?\\d{1,3}[-.\\s]?)?(\\(?\\d{2,4}\\)?[-.\\s]?){2,4}\\d{3,6}$")
                && !value.matches(".*[a-zA-Z].*");
    }

    private boolean looksLikeAddress(String value) {
        return value.matches(".*\\b(ST|STREET|RD|ROAD|AVE|AVENUE|BLVD|LANE|LN|DR|DRIVE|WAY|CT|COURT|PL|PLACE|APT|UNIT|FL|FLOOR|BUILDING|SUITE|CITY|STATE|MANKATO)\\b.*")
                || value.matches(".*\\d{1,5}\\s+\\p{L}+.*")
                || value.matches(".*\\d{5}(-\\d{4})?$");
    }

    private void appendToEditText(EditText editText, String value) {
        String current = editText.getText().toString();
        if (!TextUtils.isEmpty(current)) {
            current += ", ";
        }
        editText.setText(current + value);
    }

    private void addChip(String text) {
        if (unclassifiedContainer == null) return;
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setClickable(true);
        chip.setCheckable(false);
        chip.setId(View.generateViewId());
        chip.setChipBackgroundColorResource(android.R.color.darker_gray);
        chip.setOnClickListener(v -> {
            if (selectedTargetField != null) {
                appendToEditText(selectedTargetField, text);
                unclassifiedContainer.removeView(chip);
                chipIds.remove((Integer) chip.getId());
                updateFlowReferencedIds();
            }
        });
        unclassifiedContainer.addView(chip);
        chipIds.add(chip.getId());
        updateFlowReferencedIds();
    }

    private void updateFlowReferencedIds() {
        if (unclassifiedContainer != null) {
            androidx.constraintlayout.helper.widget.Flow flow = unclassifiedContainer.findViewById(R.id.unclassifiedFlow);
            if (flow != null) {
                flow.setReferencedIds(chipIds.stream().mapToInt(Integer::intValue).toArray());
            }
        }
    }

    private void setFieldsEditable(boolean editable) {
        editName.setEnabled(editable);
        editPhones.setEnabled(editable);
        editEmails.setEnabled(editable);
        editJobTitle.setEnabled(editable);
        editCompany.setEnabled(editable);
        editAddress.setEnabled(editable);
        editWebsite.setEnabled(editable);
        industryDropdown.setEnabled(editable);
        fieldDropdown.setEnabled(editable);
    }

    private void saveContact() {
        String name = editName.getText().toString();
        String phones = editPhones.getText().toString();
        String emails = editEmails.getText().toString();
        String job = editJobTitle.getText().toString();
        String company = editCompany.getText().toString();
        String address = editAddress.getText().toString();
        String website = editWebsite.getText().toString();
        String industryName = industryDropdown.getSelectedItem().toString();
        String fieldName = fieldDropdown.getSelectedItem().toString();

        Integer industryId = industryNameToId.get(industryName);
        Integer fieldId = null;

        if (industryId != null && fieldsByIndustry.containsKey(industryId)) {
            ArrayList<String> validFields = fieldsByIndustry.get(industryId);
            if (validFields.contains(fieldName)) {
                DatabaseHelper dbHelper = new DatabaseHelper(this);
                fieldId = dbHelper.getFieldIdByNameAndIndustry(fieldName, industryId);
            }
        }

        if (industryId == null || fieldId == null) {
            Log.w("SaveContact", "Industry or Field not selected or invalid.");
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.insertContact(name, phones, emails, job, company, address, website, industryId, fieldId);

        Toast.makeText(this, "Contact saved!", Toast.LENGTH_SHORT).show();
        finish();
    }
}