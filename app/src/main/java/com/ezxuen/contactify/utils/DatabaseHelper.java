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

        // ✅ Return all industry names
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

        // ✅ Map industry_name → industry_id
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

        // ✅ Group fields by industry_id
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

    }