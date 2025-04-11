package com.ezxuen.contactify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ezxuen.contactify.utils.DatabaseHelper;

import java.util.ArrayList;

public class FieldActivity extends AppCompatActivity implements FieldAdapter.OnFieldClickListener {

    private RecyclerView recyclerView;
    private FieldAdapter adapter;
    private String industryName;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field);

        recyclerView = findViewById(R.id.fieldRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dbHelper = new DatabaseHelper(this);

        industryName = getIntent().getStringExtra("industry_name");

        if (industryName != null) {
            Log.d("FieldActivity", "Industry: " + industryName);
            ArrayList<String> fields = dbHelper.getFieldsWithContacts(industryName);
            adapter = new FieldAdapter(fields, this);
            recyclerView.setAdapter(adapter);
        } else {
            Log.e("FieldActivity", "Missing industry_name intent extra");
            finish();
        }
    }

    @Override
    public void onFieldClick(String fieldName) {
        Intent intent = new Intent(this, ContactListActivity.class);
        intent.putExtra("field_name", fieldName);
        startActivity(intent);
    }
}