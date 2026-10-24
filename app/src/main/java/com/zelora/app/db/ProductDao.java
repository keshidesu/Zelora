package com.zelora.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.zelora.app.model.Product;

import java.util.ArrayList;
import java.util.List;

// database
public class ProductDao {
    public static final int SORT_DEFAULT = 0, SORT_PRICE_LOW = 1, SORT_PRICE_HIGH = 2, SORT_RATING = 3;

    static final String COLUMNS = "p.id, p.name, p.category, p.description, p.price, p.original_price,"
            + " p.rating, p.review_count, p.badge, p.gallery, p.colors, p.default_color, p.stock,"
            + " p.seller_id, p.active";

    private final DatabaseHelper helper;

    public ProductDao(Context context) {
        helper = DatabaseHelper.get(context);
    }

    static Product fromCursor(Cursor c, int offset) {
        Product p = build(c, offset);
        p.sellerId = c.isNull(offset + 13) ? 0 : c.getInt(offset + 13);
        p.active = c.getInt(offset + 14) == 1;
        return p;
    }

    private static Product build(Cursor c, int offset) {
        return new Product(c.getInt(offset), c.getString(offset + 1), c.getString(offset + 2),
                c.getString(offset + 3), c.getInt(offset + 4), c.getInt(offset + 5),
                c.getDouble(offset + 6), c.getInt(offset + 7), c.getString(offset + 8),
                c.getString(offset + 9), c.getString(offset + 10), c.getString(offset + 11),
                c.getInt(offset + 12));
    }

    public List<Product> getBySeller(int sellerId) {
        List<Product> out = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT " + COLUMNS + " FROM products p WHERE p.seller_id = ? ORDER BY p.id DESC",
                new String[]{String.valueOf(sellerId)})) {
            while (c.moveToNext()) out.add(fromCursor(c, 0));
        }
        return out;
    }

    public long insert(int sellerId, String name, String category, String description, int price,
                       int originalPrice, String photo, String colors, int stock) {
        ContentValues v = new ContentValues();
        v.put("name", name.trim());
        v.put("category", category);
        v.put("description", description.trim());
        v.put("price", price);
        v.put("original_price", originalPrice);
        v.put("rating", 0);
        v.put("review_count", 0);
        v.put("badge", "NEW");
        String g = photo + "," + photo + "," + photo + "," + photo;
        v.put("gallery", g);
        v.put("colors", colors);
        v.put("default_color", colors.split(",")[0]);
        v.put("stock", stock);
        v.put("seller_id", sellerId);
        return helper.getWritableDatabase().insertOrThrow("products", null, v);
    }

    public boolean update(int sellerId, int productId, String name, String category, String description,
                          int price, int originalPrice, String photo, String colors, int stock) {
        ContentValues v = new ContentValues();
        v.put("name", name.trim());
        v.put("category", category);
        v.put("description", description.trim());
        v.put("price", price);
        v.put("original_price", originalPrice);
        v.put("gallery", photo + "," + photo + "," + photo + "," + photo);
        v.put("colors", colors);
        v.put("default_color", colors.split(",")[0]);
        v.put("stock", stock);
        return helper.getWritableDatabase().update("products", v, "id=? AND seller_id=?",
                new String[]{String.valueOf(productId), String.valueOf(sellerId)}) > 0;
    }

    public boolean setActive(int sellerId, int productId, boolean active) {
        ContentValues v = new ContentValues();
        v.put("active", active ? 1 : 0);
        return helper.getWritableDatabase().update("products", v, "id=? AND seller_id=?",
                new String[]{String.valueOf(productId), String.valueOf(sellerId)}) > 0;
    }

    public Product getById(int id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT " + COLUMNS + " FROM products p WHERE p.id = ?",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? fromCursor(c, 0) : null;
        }
    }

    public List<Product> query(String category, String text, int sort) {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM products p WHERE p.active = 1");
        List<String> args = new ArrayList<>();
        if (category != null && !category.equals("All")) {
            sql.append(" AND p.category = ?");
            args.add(category);
        }
        if (text != null && !text.trim().isEmpty()) {
            sql.append(" AND (p.name LIKE ? ESCAPE '\\' OR p.category LIKE ? ESCAPE '\\' OR p.description LIKE ? ESCAPE '\\')");
            String clean = text.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            String like = "%" + clean + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        switch (sort) {
            case SORT_PRICE_LOW:  sql.append(" ORDER BY p.price ASC"); break;
            case SORT_PRICE_HIGH: sql.append(" ORDER BY p.price DESC"); break;
            case SORT_RATING:     sql.append(" ORDER BY p.rating DESC"); break;
            default:              sql.append(" ORDER BY p.id ASC"); break;
        }
        List<Product> out = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (c.moveToNext()) out.add(fromCursor(c, 0));
        }
        return out;
    }
}
