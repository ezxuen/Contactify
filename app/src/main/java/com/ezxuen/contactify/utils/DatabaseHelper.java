package com.ezxuen.contactify.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "contactify.db";
    public static final int DB_VERSION = 1;

    public static final String TABLE_INDUSTRIES = "industries";
    public static final String TABLE_FIELDS = "fields";
    public static final String TABLE_CONTACTS = "contacts";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_INDUSTRIES + " (" +
                "industry_id INTEGER PRIMARY KEY, " +
                "industry_name TEXT NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FIELDS + " (" +
                "field_id INTEGER PRIMARY KEY, " +
                "field_name TEXT NOT NULL, " +
                "industry_id INTEGER NOT NULL, " +
                "FOREIGN KEY(industry_id) REFERENCES " + TABLE_INDUSTRIES + "(industry_id))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CONTACTS + " (" +
                "contact_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, phone TEXT, email TEXT, job_title TEXT, company TEXT, " +
                "address TEXT, website TEXT, " +
                "industry_id INTEGER, field_id INTEGER, " +
                "FOREIGN KEY(industry_id) REFERENCES " + TABLE_INDUSTRIES + "(industry_id), " +
                "FOREIGN KEY(field_id) REFERENCES " + TABLE_FIELDS + "(field_id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FIELDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INDUSTRIES);
        onCreate(db);
    }

    public ArrayList<String> getAllIndustryNames() {
        ArrayList<String> industries = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT industry_name FROM " + TABLE_INDUSTRIES + " ORDER BY industry_name", null);
        if (cursor.moveToFirst()) {
            do {
                industries.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return industries;
    }

    public HashMap<String, Integer> getIndustryNameToIdMap() {
        HashMap<String, Integer> map = new HashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT industry_id, industry_name FROM " + TABLE_INDUSTRIES, null);
        if (cursor.moveToFirst()) {
            do {
                map.put(cursor.getString(1), cursor.getInt(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return map;
    }

    public HashMap<Integer, ArrayList<String>> getFieldsGroupedByIndustry() {
        HashMap<Integer, ArrayList<String>> map = new HashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT field_name, industry_id FROM " + TABLE_FIELDS + " ORDER BY field_name", null);
        if (cursor.moveToFirst()) {
            do {
                String field = cursor.getString(0);
                int industryId = cursor.getInt(1);
                if (!map.containsKey(industryId)) {
                    map.put(industryId, new ArrayList<>());
                }
                map.get(industryId).add(field);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return map;
    }
    public int getFieldIdByNameAndIndustry(String fieldName, int industryId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT field_id FROM fields WHERE field_name = ? AND industry_id = ? LIMIT 1",
                new String[]{fieldName, String.valueOf(industryId)}
        );

        int fieldId = -1;
        if (cursor.moveToFirst()) {
            fieldId = cursor.getInt(0);
        }
        cursor.close();
        return fieldId;
    }
    public void insertContact(String name, String phone, String email, String job, String company,
                              String address, String website, int industryId, int fieldId) {

        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO contacts (name, phone, email, job_title, company, address, website, industry_id, field_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{name, phone, email, job, company, address, website, industryId, fieldId});
    }
    public ArrayList<String> getIndustriesWithContacts() {
        ArrayList<String> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT i.industry_name " +
                        "FROM industries i " +
                        "JOIN contacts c ON i.industry_id = c.industry_id", null);
        while (cursor.moveToNext()) {
            result.add(cursor.getString(0));
        }
        cursor.close();
        return result;
    }

    public ArrayList<String> getFieldsWithContacts(String industryName) {
        ArrayList<String> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT f.field_name " +
                        "FROM fields f " +
                        "JOIN contacts c ON f.field_id = c.field_id " +
                        "JOIN industries i ON f.industry_id = i.industry_id " +
                        "WHERE i.industry_name = ?", new String[]{industryName});
        while (cursor.moveToNext()) {
            result.add(cursor.getString(0));
        }
        cursor.close();
        return result;
    }

    public ArrayList<String> getContactsByField(String fieldName) {
        ArrayList<String> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT name, job_title FROM contacts c " +
                        "JOIN fields f ON c.field_id = f.field_id " +
                        "WHERE f.field_name = ? ORDER BY job_title ASC", new String[]{fieldName});
        while (cursor.moveToNext()) {
            String contact = cursor.getString(0) + " — " + cursor.getString(1);
            result.add(contact);
        }
        cursor.close();
        return result;
    }

    public LinkedHashMap<String, ArrayList<Pair<Integer, String>>> getGroupedContactsByField(String fieldName) {
        LinkedHashMap<String, ArrayList<Pair<Integer, String>>> result = new LinkedHashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT c.contact_id, c.name, c.job_title " +
                        "FROM contacts c " +
                        "JOIN fields f ON c.field_id = f.field_id " +
                        "WHERE f.field_name = ? " +
                        "ORDER BY c.job_title, c.name",
                new String[]{fieldName}
        );


        int count = 0;
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            String job = cursor.getString(2);

            android.util.Log.d("GroupedContacts", "Found: " + name + " - " + job);

            if (!result.containsKey(job)) {
                result.put(job, new ArrayList<>());
            }
            result.get(job).add(new Pair<>(id, name));
            count++;
        }

        android.util.Log.d("GroupedContacts", "Total contacts grouped for field '" + fieldName + "': " + count);
        cursor.close();
        return result;
    }
    public Cursor getContactById(int contactId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM contacts WHERE contact_id = ?", new String[]{String.valueOf(contactId)});
    }

    public String getIndustryNameById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT industry_name FROM industries WHERE industry_id = ?", new String[]{String.valueOf(id)});
        if (c.moveToFirst()) {
            String name = c.getString(0);
            c.close();
            return name;
        }
        c.close();
        return null;
    }

    public String getFieldNameById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT field_name FROM fields WHERE field_id = ?\n", new String[]{String.valueOf(id)});
        if (c.moveToFirst()) {
            String name = c.getString(0);
            c.close();
            return name;
        }
        c.close();
        return null;
    }
    public void updateContact(int contactId, String name, String phone, String email, String job,
                              String company, String address, String website,
                              int industryId, int fieldId) {

        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE contacts SET " +
                        "name = ?, phone = ?, email = ?, job_title = ?, company = ?, " +
                        "address = ?, website = ?, industry_id = ?, field_id = ? " +
                        "WHERE contact_id = ?",
                new Object[]{
                        name, phone, email, job, company, address, website,
                        industryId, fieldId, contactId
                });
    }
    public void deleteContact(int contactId) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM contacts WHERE contact_id = ?", new Object[]{contactId});
    }
    public void insertExample5Industries() {
        SQLiteDatabase db = getWritableDatabase();


        db.execSQL("INSERT INTO industries (industry_id, industry_name) VALUES (1, 'Information Technology')");
        db.execSQL("INSERT INTO industries (industry_id, industry_name) VALUES (2, 'Healthcare')");
        db.execSQL("INSERT INTO industries (industry_id, industry_name) VALUES (3, 'Education')");
        db.execSQL("INSERT INTO industries (industry_id, industry_name) VALUES (4, 'Finance')");
        db.execSQL("INSERT INTO industries (industry_id, industry_name) VALUES (5, 'Construction')");

        db.execSQL("INSERT INTO fields (field_id, field_name, industry_id) VALUES (1, 'Software Development', 1)");
        db.execSQL("INSERT INTO fields (field_id, field_name, industry_id) VALUES (2, 'Cybersecurity', 1)");
        db.execSQL("INSERT INTO fields (field_id, field_name, industry_id) VALUES (3, 'Nursing', 2)");
        db.execSQL("INSERT INTO fields (field_id, field_name, industry_id) VALUES (4, 'Pharmacy', 2)");
        db.execSQL("INSERT INTO fields (field_id, field_name, industry_id) VALUES (5, 'Primary Teaching', 3)");
        db.execSQL("INSERT INTO fields (field_id, field_name, industry_id) VALUES (6, 'University Lecturing', 3)");
        db.execSQL("INSERT INTO fields (field_id, field_name, industry_id) VALUES (7, 'Accounting', 4)");
        db.execSQL("INSERT INTO fields (field_id, field_name, industry_id) VALUES (8, 'Investment Banking', 4)");
        db.execSQL("INSERT INTO fields (field_id, field_name, industry_id) VALUES (9, 'Civil Engineering', 5)");
        db.execSQL("INSERT INTO fields (field_id, field_name, industry_id) VALUES (10, 'Architecture', 5)");

        // Information Technology
        insertContact("John Doe", "+1111111111", "john.doe@techsolutions.com", "Software Developer", "Tech Solutions", "123 Tech Street", "www.techsolutions.com", 1, 1);
        insertContact("Jane Smith", "+1111111112", "jane.smith@techsolutions.com", "Software Developer", "Tech Solutions", "123 Tech Street", "www.techsolutions.com", 1, 1);

        insertContact("Alice Carter", "+1111111123", "alice.carter@cybershield.com", "Cybersecurity Specialist", "CyberShield", "456 Secure Avenue", "www.cybershield.com", 1, 2);
        insertContact("Bob Knight", "+1111111124", "bob.knight@cybershield.com", "Cybersecurity Specialist", "CyberShield", "456 Secure Avenue", "www.cybershield.com", 1, 2);

        // Healthcare
        insertContact("Emily Johnson", "+2222222221", "emily.johnson@cityhospital.org", "Nurse", "City Hospital", "789 Health Blvd", "www.cityhospital.org", 2, 3);
        insertContact("Michael Brown", "+2222222222", "michael.brown@cityhospital.org", "Nurse", "City Hospital", "789 Health Blvd", "www.cityhospital.org", 2, 3);

        insertContact("Sarah Williams", "+2222222233", "sarah.williams@pharmalink.com", "Pharmacist", "PharmaLink", "654 Pharmacy Street", "www.pharmalink.com", 2, 4);
        insertContact("David Miller", "+2222222234", "david.miller@pharmalink.com", "Pharmacist", "PharmaLink", "654 Pharmacy Street", "www.pharmalink.com", 2, 4);

        // Education
        insertContact("Olivia Davis", "+3333333331", "olivia.davis@greenschool.edu", "Teacher", "Green School", "123 Education Road", "www.greenschool.edu", 3, 5);
        insertContact("James Wilson", "+3333333332", "james.wilson@greenschool.edu", "Teacher", "Green School", "123 Education Road", "www.greenschool.edu", 3, 5);

        insertContact("Sophia Martinez", "+3333333343", "sophia.martinez@university.edu", "Lecturer", "University of Knowledge", "321 Academic Blvd", "www.university.edu", 3, 6);
        insertContact("Daniel Anderson", "+3333333344", "daniel.anderson@university.edu", "Lecturer", "University of Knowledge", "321 Academic Blvd", "www.university.edu", 3, 6);

        // Finance
        insertContact("Emma Thomas", "+4444444441", "emma.thomas@accountpros.com", "Accountant", "AccountPros", "789 Finance Street", "www.accountpros.com", 4, 7);
        insertContact("Benjamin Lee", "+4444444442", "benjamin.lee@accountpros.com", "Accountant", "AccountPros", "789 Finance Street", "www.accountpros.com", 4, 7);

        insertContact("Ava Harris", "+4444444453", "ava.harris@bankinvest.com", "Investment Banker", "BankInvest", "159 Wall Street", "www.bankinvest.com", 4, 8);
        insertContact("Ethan Clark", "+4444444454", "ethan.clark@bankinvest.com", "Investment Banker", "BankInvest", "159 Wall Street", "www.bankinvest.com", 4, 8);

        // Construction
        insertContact("Mia Lewis", "+5555555551", "mia.lewis@buildcorp.com", "Civil Engineer", "BuildCorp", "951 Build Ave", "www.buildcorp.com", 5, 9);
        insertContact("Logan Young", "+5555555552", "logan.young@buildcorp.com", "Civil Engineer", "BuildCorp", "951 Build Ave", "www.buildcorp.com", 5, 9);

        insertContact("Isabella Walker", "+5555555563", "isabella.walker@archidesign.com", "Architect", "ArchiDesign", "654 Design Road", "www.archidesign.com", 5, 10);
        insertContact("Lucas Hall", "+5555555564", "lucas.hall@archidesign.com", "Architect", "ArchiDesign", "654 Design Road", "www.archidesign.com", 5, 10);
    }
}