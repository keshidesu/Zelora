package com.zelora.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.zelora.app.model.Address;

// address
public class AddressDao {
    private final DatabaseHelper helper;

    public AddressDao(Context context) {
        helper = DatabaseHelper.get(context);
    }

    public Address get(int userId) {
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT id, recipient, address_line FROM addresses WHERE user_id=?",
                new String[]{String.valueOf(userId)})) {
            return c.moveToFirst() ? new Address(c.getInt(0), c.getString(1), c.getString(2)) : null;
        }
    }

    public void save(int userId, String recipient, String line) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("recipient", recipient.trim());
        v.put("address_line", line.trim());
        int rows = db.update("addresses", v, "user_id=?", new String[]{String.valueOf(userId)});
        if (rows == 0) {
            v.put("user_id", userId);
            db.insert("addresses", null, v);
        }
    }
}
