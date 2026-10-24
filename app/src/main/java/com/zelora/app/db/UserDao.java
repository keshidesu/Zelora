package com.zelora.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.zelora.app.model.User;
import com.zelora.app.util.PasswordUtil;

// auth
public class UserDao {
    private final DatabaseHelper helper;

    public UserDao(Context context) {
        helper = DatabaseHelper.get(context);
    }

    public long register(String fullName, String email, String password) {
        return register(fullName, email, password, User.CUSTOMER);
    }

    public long register(String fullName, String email, String password, String role) {
        if (!User.CUSTOMER.equals(role) && !User.SELLER.equals(role) && !User.RIDER.equals(role)) {
            role = User.CUSTOMER;
        }
        String salt = PasswordUtil.newSalt();
        ContentValues v = new ContentValues();
        v.put("full_name", fullName.trim());
        v.put("email", email.trim().toLowerCase(java.util.Locale.ROOT));
        v.put("salt", salt);
        v.put("password_hash", PasswordUtil.hash(password, salt));
        v.put("role", role);
        try {
            return helper.getWritableDatabase().insertOrThrow("users", null, v);
        } catch (SQLiteConstraintException duplicateEmail) {
            return -1;
        }
    }

    public User login(String email, String password) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, full_name, email, phone, password_hash, salt, role, active FROM users WHERE email = ?",
                new String[]{email.trim().toLowerCase(java.util.Locale.ROOT)})) {
            if (!c.moveToFirst()) return null;
            String hash = c.getString(4);
            String salt = c.getString(5);
            if (!PasswordUtil.matches(password, salt, hash)) return null;
            return new User(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), c.getString(6), c.getInt(7) == 1);
        }
    }

    public User getById(int id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id, full_name, email, phone, role, active FROM users WHERE id = ?",
                new String[]{String.valueOf(id)})) {
            if (!c.moveToFirst()) return null;
            return new User(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getInt(5) == 1);
        }
    }

    public void updateProfile(int id, String fullName, String phone) {
        ContentValues v = new ContentValues();
        v.put("full_name", fullName.trim());
        v.put("phone", phone == null ? "" : phone.trim());
        helper.getWritableDatabase().update("users", v, "id = ?", new String[]{String.valueOf(id)});
    }

    public boolean changePassword(int id, String currentPassword, String newPassword) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try (Cursor c = db.rawQuery("SELECT password_hash, salt FROM users WHERE id = ?",
                new String[]{String.valueOf(id)})) {
            if (!c.moveToFirst() || !PasswordUtil.matches(currentPassword, c.getString(1), c.getString(0)))
                return false;
        }
        setPassword(db, "id = ?", String.valueOf(id), newPassword);
        return true;
    }

    public boolean resetPassword(String email, String newPassword) {
        SQLiteDatabase db = helper.getWritableDatabase();
        String e = email.trim().toLowerCase(java.util.Locale.ROOT);
        try (Cursor c = db.rawQuery("SELECT id FROM users WHERE email = ? AND role = 'CUSTOMER'", new String[]{e})) {
            if (!c.moveToFirst()) return false;
        }
        setPassword(db, "email = ? AND role = 'CUSTOMER'", e, newPassword);
        return true;
    }

    private void setPassword(SQLiteDatabase db, String where, String arg, String newPassword) {
        String salt = PasswordUtil.newSalt();
        ContentValues v = new ContentValues();
        v.put("salt", salt);
        v.put("password_hash", PasswordUtil.hash(newPassword, salt));
        db.update("users", v, where, new String[]{arg});
    }

    // staff
    public java.util.List<User> getAll() {
        java.util.List<User> out = new java.util.ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT id, full_name, email, phone, role, active FROM users ORDER BY id DESC", null)) {
            while (c.moveToNext()) {
                out.add(new User(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getInt(5) == 1));
            }
        }
        return out;
    }

    public boolean setActive(int userId, boolean active) {
        ContentValues v = new ContentValues();
        v.put("active", active ? 1 : 0);
        return helper.getWritableDatabase().update("users", v, "id=? AND role<>'ADMIN'",
                new String[]{String.valueOf(userId)}) > 0;
    }
}
