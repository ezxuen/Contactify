package com.ezxuen.contactify;

import android.content.ClipData;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.helper.widget.Flow;

import com.ezxuen.contactify.utils.DatabaseHelper;
import com.google.android.material.chip.Chip;
import com.google.mlkit.nl.entityextraction.Entity;
import com.google.mlkit.nl.entityextraction.EntityAnnotation;
import com.google.mlkit.nl.entityextraction.EntityExtraction;
import com.google.mlkit.nl.entityextraction.EntityExtractionParams;
import com.google.mlkit.nl.entityextraction.EntityExtractor;
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions;

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

        View.OnDragListener dragListener = (v, event) -> {
            if (event.getAction() == DragEvent.ACTION_DROP && v instanceof EditText) {
                ClipData.Item item = event.getClipData().getItemAt(0);
                String droppedText = item.getText().toString();
                appendToEditText((EditText) v, droppedText);

                View draggedView = (View) event.getLocalState();
                if (draggedView instanceof Chip) {
                    ((ViewGroup) draggedView.getParent()).removeView(draggedView);
                    chipIds.remove((Integer) draggedView.getId());
                    updateFlowReferencedIds();
                }
            }
            return true;
        };

        for (EditText field : new EditText[]{editName, editPhones, editEmails, editJobTitle, editCompany, editAddress, editWebsite}) {
            field.setOnFocusChangeListener(focusListener);
            field.setOnDragListener(dragListener);
        }
    }

    private void loadIndustryAndFieldData() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        ArrayList<String> industryList = dbHelper.getAllIndustryNames();
        industryList.add(0, "Select Industry");

        industryNameToId = dbHelper.getIndustryNameToIdMap();
        fieldsByIndustry = dbHelper.getFieldsGroupedByIndustry();

        fieldDropdown.setEnabled(false);

        ArrayAdapter<String> industryAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_placeholder, industryList);
        industryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        industryDropdown.setAdapter(industryAdapter);
        industryDropdown.setSelection(0);

        industryDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos == 0) {
                    fieldDropdown.setAdapter(null);
                    fieldDropdown.setEnabled(false);
                    return;
                }

                String industry = parent.getItemAtPosition(pos).toString();
                Integer industryId = industryNameToId.get(industry);
                if (industryId == null) return;

                ArrayList<String> fields = new ArrayList<>(fieldsByIndustry.get(industryId));
                fields.add(0, "Select Field");

                fieldDropdown.setEnabled(true);
                ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(ReviewActivity.this, R.layout.spinner_item_placeholder, fields);
                fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                fieldDropdown.setAdapter(fieldAdapter);
                fieldDropdown.setSelection(0);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void parseAndDisplayFields(String rawText) {
        EntityExtractor extractor = EntityExtraction.getClient(
                new EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build());

        editEmails.setText(""); editPhones.setText(""); editAddress.setText("");
        ArrayList<String> usedLines = new ArrayList<>();
        unclassifiedData.clear();

        extractor.downloadModelIfNeeded()
                .addOnSuccessListener(unused -> {
                    EntityExtractionParams params = new EntityExtractionParams.Builder(rawText).build();
                    extractor.annotate(params).addOnSuccessListener(annotations -> {
                        for (EntityAnnotation annotation : annotations) {
                            String line = annotation.getAnnotatedText().trim();
                            if (usedLines.contains(line)) continue;

                            for (Entity entity : annotation.getEntities()) {
                                switch (entity.getType()) {
                                    case Entity.TYPE_EMAIL:
                                        appendToEditText(editEmails, line); usedLines.add(line); break;
                                    case Entity.TYPE_PHONE:
                                        if (isValidPhone(line) && !looksLikeAddress(line)) {
                                            appendToEditText(editPhones, line); usedLines.add(line);
                                        }
                                        break;
                                    case Entity.TYPE_ADDRESS:
                                        appendToEditText(editAddress, line); usedLines.add(line); break;
                                }
                            }
                        }

                        for (String line : rawText.split("\n")) {
                            String trimmed = line.trim();
                            if (!trimmed.isEmpty() && !usedLines.contains(trimmed)) {
                                unclassifiedData.add(trimmed);
                            }
                        }

                        ViewGroup chipHolder = findViewById(R.id.unclassifiedChipHolder);
                        chipHolder.removeAllViews();
                        chipIds.clear();

                        if (!unclassifiedData.isEmpty()) {
                            unclassifiedLabel.setVisibility(View.VISIBLE);
                            unclassifiedContainer.setVisibility(View.VISIBLE);
                            for (String item : unclassifiedData) addChip(item);
                            setFieldsEditable(true);
                        } else {
                            btnEdit.setVisibility(View.VISIBLE);
                            setFieldsEditable(false);
                        }
                    });
                });
    }

    private boolean isValidPhone(String value) {
        return value.matches("^(\\+?\\d{1,3}[-.\\s]?)?(\\(?\\d{2,4}\\)?[-.\\s]?){2,4}\\d{3,6}$") && !value.matches(".*[a-zA-Z].*");
    }

    private boolean looksLikeAddress(String value) {
        return value.matches(".*\\b(ST|STREET|RD|ROAD|AVE|AVENUE|BLVD|LANE|LN|DR|DRIVE|WAY|CT|COURT|PL|PLACE|APT|UNIT|FL|FLOOR|BUILDING|SUITE|CITY|STATE|MANKATO)\\b.*")
                || value.matches(".*\\d{1,5}\\s+\\p{L}+.*")
                || value.matches(".*\\d{5}(-\\d{4})?$");
    }

    private void appendToEditText(EditText editText, String value) {
        String current = editText.getText().toString();
        if (!TextUtils.isEmpty(current)) current += ", ";
        editText.setText(current + value);
    }

    private void addChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setClickable(true);
        chip.setCheckable(false);
        chip.setId(View.generateViewId());
        chip.setChipBackgroundColorResource(android.R.color.darker_gray);

        // Margin around chip
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int dp8 = (int) (8 * getResources().getDisplayMetrics().density);
        params.setMargins(dp8, dp8, dp8, dp8);
        chip.setLayoutParams(params);

        chip.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText("label", chip.getText());
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(chip);
            v.startDragAndDrop(data, shadowBuilder, chip, 0);
            return true;
        });

        chip.setOnClickListener(v -> {
            if (selectedTargetField != null) {
                appendToEditText(selectedTargetField, text);
                ((ViewGroup) chip.getParent()).removeView(chip);
                chipIds.remove((Integer) chip.getId());
                updateFlowReferencedIds();
            }
        });

        ViewGroup chipHolder = findViewById(R.id.unclassifiedChipHolder);
        chipHolder.setVisibility(View.VISIBLE);
        chipHolder.addView(chip);
        chipIds.add(chip.getId());
        updateFlowReferencedIds();
    }

    private void updateFlowReferencedIds() {
        Flow flow = findViewById(R.id.unclassifiedFlow);
        ViewGroup chipHolder = findViewById(R.id.unclassifiedChipHolder);
        if (flow != null && chipHolder != null) {
            int count = chipHolder.getChildCount();
            int[] ids = new int[count];
            for (int i = 0; i < count; i++) {
                ids[i] = chipHolder.getChildAt(i).getId();
            }
            flow.setReferencedIds(ids);
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