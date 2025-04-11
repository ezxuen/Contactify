package com.ezxuen.contactify;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ezxuen.contactify.utils.DatabaseHelper;

import java.util.ArrayList;

public class ContactListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private String fieldName;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        recyclerView = findViewById(R.id.contactRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dbHelper = new DatabaseHelper(this);

        fieldName = getIntent().getStringExtra("field_name");

        if (fieldName != null) {
            Log.d("ContactListActivity", "Field: " + fieldName);
            ArrayList<String> contacts = dbHelper.getContactsByField(fieldName);
            adapter = new ContactAdapter(contacts);
            recyclerView.setAdapter(adapter);
        } else {
            Log.e("ContactListActivity", "Missing field_name intent extra");
            finish();
        }
    }
}