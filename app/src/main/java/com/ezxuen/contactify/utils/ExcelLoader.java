package com.ezxuen.contactify.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;

public class ExcelLoader {

    public static void loadIndustriesAndFields(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            InputStream inputStream = context.getAssets().open("industry_fields.xlsx");
            Workbook workbook = new XSSFWorkbook(inputStream);

            // Load industries
            Sheet industriesSheet = workbook.getSheetAt(0); // Assuming first sheet = industries
            for (Row row : industriesSheet) {
                if (row.getRowNum() == 0) continue; // skip header
                int industryId = (int) row.getCell(0).getNumericCellValue();
                String industryName = row.getCell(1).getStringCellValue();

                ContentValues values = new ContentValues();
                values.put("industry_id", industryId);
                values.put("industry_name", industryName);
                db.insertWithOnConflict(DatabaseHelper.TABLE_INDUSTRIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }

            // Load fields
            Sheet fieldsSheet = workbook.getSheetAt(1); // Assuming second sheet = fields
            for (Row row : fieldsSheet) {
                if (row.getRowNum() == 0) continue; // skip header
                int fieldId = (int) row.getCell(0).getNumericCellValue();
                String fieldName = row.getCell(1).getStringCellValue();
                int industryId = (int) row.getCell(2).getNumericCellValue();

                ContentValues values = new ContentValues();
                values.put("field_id", fieldId);
                values.put("field_name", fieldName);
                values.put("industry_id", industryId);
                db.insertWithOnConflict(DatabaseHelper.TABLE_FIELDS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }

            workbook.close();
            inputStream.close();

            Log.d("ExcelLoader", "✅ Data loaded successfully.");

        } catch (Exception e) {
            Log.e("ExcelLoader", "❌ Failed to load Excel data", e);
        }
    }
}