package com.zelora.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.zelora.app.model.Review;

import java.util.ArrayList;
import java.util.List;

// review
public class ReviewDao {
    public static final int OK = 0, NOT_ELIGIBLE = 1, ALREADY_REVIEWED = 2;
    private final DatabaseHelper helper;

    public ReviewDao(Context context) {
        helper = DatabaseHelper.get(context);
    }

    public boolean hasReviewed(int userId, int productId) {
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT 1 FROM reviews WHERE user_id=? AND product_id=?",
                new String[]{String.valueOf(userId), String.valueOf(productId)})) {
            return c.moveToFirst();
        }
    }

    public int add(int userId, int productId, int orderId, int rating, String comment) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            try (Cursor c = db.rawQuery("SELECT 1 FROM orders o JOIN order_items i ON i.order_id = o.id"
                            + " WHERE o.id=? AND o.user_id=? AND o.status='Delivered' AND i.product_id=?",
                    new String[]{String.valueOf(orderId), String.valueOf(userId), String.valueOf(productId)})) {
                if (!c.moveToFirst()) return NOT_ELIGIBLE;
            }
            if (hasReviewed(userId, productId)) return ALREADY_REVIEWED;

            rating = Math.max(1, Math.min(5, rating));
            ContentValues v = new ContentValues();
            v.put("product_id", productId);
            v.put("user_id", userId);
            v.put("order_id", orderId);
            v.put("rating", Math.max(1, Math.min(5, rating)));
            v.put("comment", comment == null ? "" : comment.trim());
            db.insertOrThrow("reviews", null, v);

            double oldRating = 0;
            int oldCount = 0;
            try (Cursor c = db.rawQuery("SELECT rating, review_count FROM products WHERE id=?",
                    new String[]{String.valueOf(productId)})) {
                if (c.moveToFirst()) {
                    oldRating = c.getDouble(0);
                    oldCount = c.getInt(1);
                }
            }
            double avg = (oldRating * oldCount + rating) / (oldCount + 1);
            ContentValues p = new ContentValues();
            p.put("rating", Math.round(avg * 10) / 10.0);
            p.put("review_count", oldCount + 1);
            db.update("products", p, "id=?", new String[]{String.valueOf(productId)});

            db.setTransactionSuccessful();
            return OK;
        } finally {
            db.endTransaction();
        }
    }

    public List<Review> forProduct(int productId, int limit) {
        List<Review> out = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT r.id, r.rating, r.comment, r.created_at, u.full_name FROM reviews r"
                        + " JOIN users u ON u.id = r.user_id WHERE r.product_id=? ORDER BY r.id DESC LIMIT " + limit,
                new String[]{String.valueOf(productId)})) {
            while (c.moveToNext()) {
                Review r = new Review();
                r.id = c.getInt(0);
                r.productId = productId;
                r.rating = c.getInt(1);
                r.comment = c.getString(2);
                r.createdAt = c.getString(3);
                r.reviewer = c.getString(4);
                out.add(r);
            }
        }
        return out;
    }
}
