package com.ezxuen.contactify;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ezxuen.contactify.utils.DatabaseHelper;

import java.util.ArrayList;

public class IndustryActivity extends AppCompatActivity implements IndustryAdapter.OnIndustryClickListener {

    RecyclerView recyclerView;
    IndustryAdapter adapter;
    ArrayList<String> industries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_industry);

        recyclerView = findViewById(R.id.industryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        industries = dbHelper.getIndustriesWithContacts();

        if (industries.isEmpty()) {
            Toast.makeText(this, "No contacts yet.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new IndustryAdapter(industries, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onIndustryClick(String industryName) {
        Intent intent = new Intent(this, FieldActivity.class);
        intent.putExtra("industry_name", industryName);
        startActivity(intent);
    }
}