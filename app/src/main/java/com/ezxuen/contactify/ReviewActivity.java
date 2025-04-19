package com.ezxuen.contactify;

import android.app.AlertDialog;
import android.content.ClipData;
import android.database.Cursor;
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

        editingContactId = getIntent().getIntExtra("contact_id", -1);

        if (editingContactId != -1) {
            loadContactData(editingContactId);
            btnEdit.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.GONE);
            btnDelete.setVisibility(View.VISIBLE);
            setFieldsEditable(false);
        } else {
            String ocrResult = getIntent().getStringExtra("ocr_result");
            Log.d("ReviewActivity", "OCR result: " + ocrResult);

            if (ocrResult != null && !ocrResult.isEmpty()) {
                parseAndDisplayFields(ocrResult);
            }

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
        industryAdapter.setDropDownViewResource(R.layout.spinner_item_placeholder);
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

                fieldDropdown.setEnabled(editJobTitle.isEnabled());
                ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(ReviewActivity.this, R.layout.spinner_item_placeholder, fields);
                fieldAdapter.setDropDownViewResource(R.layout.spinner_item_placeholder);
                fieldDropdown.setAdapter(fieldAdapter);
                fieldDropdown.setSelection(0);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void parseAndDisplayFields(String rawText) {
        EntityExtractor extractor = EntityExtraction.getClient(new EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build());
        editEmails.setText(""); editPhones.setText(""); editAddress.setText("");
        ArrayList<String> usedLines = new ArrayList<>();
        unclassifiedData.clear();

        extractor.downloadModelIfNeeded().addOnSuccessListener(unused -> {
            EntityExtractionParams params = new EntityExtractionParams.Builder(rawText).build();
            extractor.annotate(params).addOnSuccessListener(annotations -> {
                for (EntityAnnotation annotation : annotations) {
                    String line = annotation.getAnnotatedText().trim();
                    if (usedLines.contains(line)) continue;

                    for (Entity entity : annotation.getEntities()) {
                        switch (entity.getType()) {
                            case Entity.TYPE_EMAIL: appendToEditText(editEmails, line); usedLines.add(line); break;
                            case Entity.TYPE_PHONE: if (isValidPhone(line) && !looksLikeAddress(line)) {
                                appendToEditText(editPhones, line); usedLines.add(line); }
                                break;
                            case Entity.TYPE_ADDRESS: appendToEditText(editAddress, line); usedLines.add(line); break;
                        }
                    }
                }

                for (String line : rawText.split("\n")) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !usedLines.contains(trimmed)) {
                        unclassifiedData.add(trimmed);
                    }
                }

                ViewGroup chipHolder = findViewById(R.id.chipContainer);
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

        // ✅ Set background and text color
        chip.setChipBackgroundColorResource(android.R.color.white);
        chip.setTextColor(getResources().getColor(android.R.color.black));


        // ✅ Drag and drop behavior
        chip.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText("label", chip.getText());
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(chip);
            v.startDragAndDrop(data, shadowBuilder, chip, 0);
            return true;
        });

        // ✅ Assign chip text to selected field on click
        chip.setOnClickListener(v -> {
            if (selectedTargetField != null) {
                appendToEditText(selectedTargetField, text);
                ((ViewGroup) chip.getParent()).removeView(chip);
                chipIds.remove((Integer) chip.getId());
                updateFlowReferencedIds();
            }
        });

        // ✅ Add chip to container and update flow
        ViewGroup chipHolder = findViewById(R.id.chipContainer);
        chipHolder.setVisibility(View.VISIBLE);
        chipHolder.addView(chip);
        chipIds.add(chip.getId());
        updateFlowReferencedIds();
    }
    private void updateFlowReferencedIds() {
        Flow flow = findViewById(R.id.unclassifiedFlow);
        ViewGroup chipHolder = findViewById(R.id.chipContainer);

        if (flow != null && chipHolder != null) {
            int count = chipHolder.getChildCount();
            int[] ids = new int[count];
            for (int i = 0; i < count; i++) {
                ids[i] = chipHolder.getChildAt(i).getId();
            }
            flow.setReferencedIds(ids);
        } else {
            Log.e("ReviewActivity", "Flow or chipHolder is null!");
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
                }, 100);
            });
        }
        if (cursor != null) cursor.close();
    }
}