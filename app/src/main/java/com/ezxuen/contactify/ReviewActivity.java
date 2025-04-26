package com.ezxuen.contactify;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.ezxuen.contactify.utils.DatabaseHelper;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.chip.Chip;
import com.google.mlkit.nl.entityextraction.*;

import java.util.ArrayList;
import java.util.HashMap;

public class ReviewActivity extends AppCompatActivity {

    EditText editName, editPhones, editEmails, editJobTitle, editCompany, editAddress, editWebsite;
    Spinner industryDropdown, fieldDropdown;
    ConstraintLayout unclassifiedContainer;
    TextView unclassifiedLabel;
    Button btnEdit, btnSave, btnDelete;
    private int editingContactId = -1;

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
        monitorRequiredFields();
        editingContactId = getIntent().getIntExtra("contact_id", -1);

        if (editingContactId != -1) {
            setFieldsEditable(false);
            loadContactData(editingContactId);
            btnEdit.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.GONE);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            String ocrResult = getIntent().getStringExtra("ocr_result");
            Log.d("ReviewActivity", "OCR result: " + ocrResult);

            if (ocrResult != null && !ocrResult.isEmpty()) {
                parseAndDisplayFields(ocrResult);
            }
            validateSaveButton();

            btnEdit.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.GONE);
            setFieldsEditable(true);
        }

        btnEdit.setOnClickListener(v -> {
            setFieldsEditable(true);
            btnEdit.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
        });

        btnSave.setOnClickListener(v -> saveContact());

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Contact")
                    .setMessage("Are you sure you want to delete this contact?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        new DatabaseHelper(this).deleteContact(editingContactId);
                        Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
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
        btnDelete = findViewById(R.id.btnDelete);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("View Contact");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            toolbar.setTitleTextAppearance(this, R.style.ToolbarTitleBold);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

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
        industryAdapter.setDropDownViewResource(R.layout.spinner_item_placeholder);
        industryDropdown.setAdapter(industryAdapter);
        industryDropdown.setSelection(0);

        industryDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos == 0) {
                    fieldDropdown.setAdapter(null);
                    fieldDropdown.setEnabled(false);
                } else {
                    String industry = parent.getItemAtPosition(pos).toString();
                    Integer industryId = industryNameToId.get(industry);
                    if (industryId == null) return;

                    ArrayList<String> fields = new ArrayList<>(fieldsByIndustry.get(industryId));
                    fields.add(0, "Select Field");

                    ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(ReviewActivity.this, R.layout.spinner_item_placeholder, fields);
                    fieldAdapter.setDropDownViewResource(R.layout.spinner_item_placeholder);
                    fieldDropdown.setAdapter(fieldAdapter);
                    fieldDropdown.setSelection(0);
                    fieldDropdown.setEnabled(true);
                }
                validateSaveButton();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                validateSaveButton();
            }
        });

        fieldDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                validateSaveButton();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                validateSaveButton();
            }
        });
    }

    private void parseAndDisplayFields(String rawText) {
        EntityExtractor extractor = EntityExtraction.getClient(
                new EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
        );

        // Clear fields
        editEmails.setText("");
        editPhones.setText("");
        editAddress.setText("");
        unclassifiedData.clear();
        chipIds.clear();

        // Clean the OCR input
        String cleanedText = rawText.replace("\n", " ").replaceAll("\\s+", " ").trim();
        ArrayList<String> usedEntities = new ArrayList<>();

        extractor.downloadModelIfNeeded().addOnSuccessListener(unused -> {
            EntityExtractionParams params = new EntityExtractionParams.Builder(cleanedText).build();
            extractor.annotate(params).addOnSuccessListener(annotations -> {
                for (EntityAnnotation annotation : annotations) {
                    String text = annotation.getAnnotatedText().trim();
                    if (usedEntities.contains(text)) continue;

                    for (Entity entity : annotation.getEntities()) {
                        switch (entity.getType()) {
                            case Entity.TYPE_EMAIL:
                                appendToEditText(editEmails, text);
                                usedEntities.add(text);
                                break;
                            case Entity.TYPE_PHONE:
                                if (isValidPhone(text) && !looksLikeAddress(text)) {
                                    appendToEditText(editPhones, text);
                                    usedEntities.add(text);
                                }
                                break;
                            case Entity.TYPE_ADDRESS:
                                appendToEditText(editAddress, text);
                                usedEntities.add(text);
                                break;
                        }
                    }
                }

                populateUnclassifiedChips(rawText, usedEntities);
            }).addOnFailureListener(e -> {
                Log.e("EntityExtraction", "Annotation failed: " + e.getMessage());
                populateUnclassifiedChips(rawText, usedEntities); // fallback
            });
        }).addOnFailureListener(e -> {
            Log.e("EntityExtraction", "Model download failed: " + e.getMessage());
            populateUnclassifiedChips(rawText, usedEntities); // fallback
        });
    }
    private abstract static class TextWatcherAdapter implements android.text.TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }

    private void monitorRequiredFields() {
        TextWatcher watcher = new ReviewActivity.TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                validateSaveButton();
            }
        };

        editName.addTextChangedListener(watcher);
        editJobTitle.addTextChangedListener(watcher);
    }
    private void validateSaveButton() {
        boolean nameFilled = !TextUtils.isEmpty(editName.getText().toString().trim());
        boolean jobFilled = !TextUtils.isEmpty(editJobTitle.getText().toString().trim());

        String selectedIndustry = industryDropdown.getSelectedItem() != null ? industryDropdown.getSelectedItem().toString() : "";
        String selectedField = fieldDropdown.getSelectedItem() != null ? fieldDropdown.getSelectedItem().toString() : "";

        boolean industrySelected = !selectedIndustry.equals("Select Industry");
        boolean fieldSelected = fieldDropdown.isEnabled() && !selectedField.equals("Select Field");

        boolean enable = nameFilled && jobFilled && industrySelected && fieldSelected;
        btnSave.setEnabled(enable);
        btnSave.setAlpha(enable ? 1.0f : 0.5f);
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

    private void populateUnclassifiedChips(String rawText, ArrayList<String> usedEntities) {
        FlexboxLayout chipHolder = findViewById(R.id.chipContainer);
        chipHolder.removeAllViews();

        unclassifiedData.clear();

        for (String line : rawText.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !usedEntities.contains(trimmed)) {
                unclassifiedData.add(trimmed);
            }
        }

        if (!unclassifiedData.isEmpty()) {
            unclassifiedLabel.setVisibility(View.VISIBLE);
            unclassifiedContainer.setVisibility(View.VISIBLE);
            for (String item : unclassifiedData) {
                addChip(item);
            }
        } else {
            btnEdit.setVisibility(View.VISIBLE);
            setFieldsEditable(false);
        }
    }


    private void addChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setClickable(true);
        chip.setCheckable(false);
        chip.setId(View.generateViewId());

        chip.setChipBackgroundColorResource(android.R.color.white);
        chip.setTextColor(getResources().getColor(android.R.color.black));

        // ✅ Drag immediately when touching
        chip.setOnTouchListener((v, event) -> {
            ClipData data = ClipData.newPlainText("label", chip.getText());
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(chip);
            v.startDragAndDrop(data, shadowBuilder, chip, 0);
            return false;
        });

        chip.setOnClickListener(v -> {
            if (selectedTargetField != null) {
                appendToEditText(selectedTargetField, text);
                ((ViewGroup) chip.getParent()).removeView(chip);
                chipIds.remove((Integer) chip.getId());
            }
        });

        FlexboxLayout chipHolder = findViewById(R.id.chipContainer);
        chipHolder.addView(chip);
        chipIds.add(chip.getId());
    }

    private void setFieldsEditable(boolean editable) {
        float alpha = editable ? 1.0f : 0.5f;

        for (EditText field : new EditText[]{editName, editPhones, editEmails, editJobTitle, editCompany, editAddress, editWebsite}) {
            field.setEnabled(editable);
            field.setAlpha(alpha);
        }
        for (Spinner spinner : new Spinner[]{industryDropdown, fieldDropdown}) {
            spinner.setEnabled(editable);
            if (editable) {
                spinner.setBackgroundColor(getResources().getColor(android.R.color.white));
            } else {
                spinner.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            }
         }
    }


    private void saveContact() {
        String name = editName.getText().toString().trim();
        String phones = editPhones.getText().toString();
        String emails = editEmails.getText().toString();
        String job = editJobTitle.getText().toString().trim();
        String company = editCompany.getText().toString();
        String address = editAddress.getText().toString();
        String website = editWebsite.getText().toString();
        String industryName = industryDropdown.getSelectedItem().toString();
        String fieldName = fieldDropdown.getSelectedItem().toString();

        if (name.isEmpty()) {
            editName.setError("Name is required"); editName.requestFocus(); return;
        }
        if (job.isEmpty()) {
            editJobTitle.setError("Job Title is required"); editJobTitle.requestFocus(); return;
        }
        if (industryName.equals("Select Industry") || fieldName.equals("Select Field")) {
            Toast.makeText(this, "Please select valid industry and field", Toast.LENGTH_SHORT).show(); return;
        }

        Integer industryId = industryNameToId.get(industryName);
        Integer fieldId = null;

        if (industryId != null && fieldsByIndustry.containsKey(industryId)) {
            ArrayList<String> validFields = fieldsByIndustry.get(industryId);
            if (validFields.contains(fieldName)) {
                fieldId = new DatabaseHelper(this).getFieldIdByNameAndIndustry(fieldName, industryId);
            }
        }

        if (industryId == null || fieldId == null) {
            Toast.makeText(this, "Invalid industry or field selected", Toast.LENGTH_SHORT).show(); return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        if (editingContactId != -1) {
            dbHelper.updateContact(editingContactId, name, phones, emails, job, company, address, website, industryId, fieldId);
            Toast.makeText(this, "Contact updated!", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.insertContact(name, phones, emails, job, company, address, website, industryId, fieldId);
            Toast.makeText(this, "Contact saved!", Toast.LENGTH_SHORT).show();
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void loadContactData(int contactId) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        Cursor cursor = dbHelper.getContactById(contactId);
        if (cursor != null && cursor.moveToFirst()) {
            editName.setText(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            editPhones.setText(cursor.getString(cursor.getColumnIndexOrThrow("phone")));
            editEmails.setText(cursor.getString(cursor.getColumnIndexOrThrow("email")));
            editJobTitle.setText(cursor.getString(cursor.getColumnIndexOrThrow("job_title")));
            editCompany.setText(cursor.getString(cursor.getColumnIndexOrThrow("company")));
            editAddress.setText(cursor.getString(cursor.getColumnIndexOrThrow("address")));
            editWebsite.setText(cursor.getString(cursor.getColumnIndexOrThrow("website")));

            int industryId = cursor.getInt(cursor.getColumnIndexOrThrow("industry_id"));
            int fieldId = cursor.getInt(cursor.getColumnIndexOrThrow("field_id"));

            industryDropdown.post(() -> {
                String industryName = dbHelper.getIndustryNameById(industryId);
                if (industryName != null) {
                    ArrayAdapter adapter = (ArrayAdapter) industryDropdown.getAdapter();
                    if (adapter != null) {
                        int index = adapter.getPosition(industryName);
                        if (index >= 0) industryDropdown.setSelection(index);
                    }
                }

                fieldDropdown.postDelayed(() -> {
                    String fieldName = dbHelper.getFieldNameById(fieldId);
                    if (fieldName != null) {
                        ArrayAdapter fieldAdapter = (ArrayAdapter) fieldDropdown.getAdapter();
                        if (fieldAdapter != null) {
                            int index = fieldAdapter.getPosition(fieldName);
                            if (index >= 0) fieldDropdown.setSelection(index);
                        }
                    }
                    setFieldsEditable(false);
                }, 100);
            });
        }
        if (cursor != null) cursor.close();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}