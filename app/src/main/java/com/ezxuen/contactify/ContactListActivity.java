package com.ezxuen.contactify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ezxuen.contactify.utils.DatabaseHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ContactListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GroupedContactAdapter adapter;
    private DatabaseHelper dbHelper;
    private String fieldName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get selected field
        fieldName = getIntent().getStringExtra("field_name");

        if (fieldName == null) {
            Log.e("ContactListActivity", "Missing field_name intent extra");
            finish();
            return;
        }

        getSupportActionBar().setTitle(fieldName);

        recyclerView = findViewById(R.id.contactRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dbHelper = new DatabaseHelper(this);

        LinkedHashMap<String, ArrayList<Pair<Integer, String>>> groupedData =
                dbHelper.getGroupedContactsByField(fieldName);

        adapter = new GroupedContactAdapter(groupedData, contactId -> {
            Intent intent = new Intent(ContactListActivity.this, ReviewActivity.class);
            intent.putExtra("contact_id", contactId);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}