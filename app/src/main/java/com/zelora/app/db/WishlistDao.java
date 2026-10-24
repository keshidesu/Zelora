package com.zelora.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.zelora.app.model.Product;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// wishlist
public class WishlistDao {
    private final DatabaseHelper helper;

    public WishlistDao(Context context) {
        helper = DatabaseHelper.get(context);
    }

    public boolean toggle(int userId, int productId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        String[] args = {String.valueOf(userId), String.valueOf(productId)};
        int deleted = db.delete("wishlist", "user_id=? AND product_id=?", args);
        if (deleted > 0) return false;
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("product_id", productId);
        db.insert("wishlist", null, v);
        return true;
    }

    public boolean contains(int userId, int productId) {
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT 1 FROM wishlist WHERE user_id=? AND product_id=?",
                new String[]{String.valueOf(userId), String.valueOf(productId)})) {
            return c.moveToFirst();
        }
    }

    public Set<Integer> getProductIds(int userId) {
        Set<Integer> ids = new HashSet<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT product_id FROM wishlist WHERE user_id=?", new String[]{String.valueOf(userId)})) {
            while (c.moveToNext()) ids.add(c.getInt(0));
        }
        return ids;
    }

    public List<Product> getProducts(int userId) {
        List<Product> out = new ArrayList<>();
        String sql = "SELECT " + ProductDao.COLUMNS + " FROM wishlist w JOIN products p ON p.id = w.product_id"
                + " WHERE w.user_id = ? ORDER BY p.id";
        try (Cursor c = helper.getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(userId)})) {
            while (c.moveToNext()) out.add(ProductDao.fromCursor(c, 0));
        }
        return out;
    }
}
