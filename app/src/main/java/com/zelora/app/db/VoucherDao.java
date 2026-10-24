package com.zelora.app.db;

import android.content.Context;
import android.database.Cursor;

import com.zelora.app.model.Voucher;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// voucher
public class VoucherDao {
    public static class Result {
        public Voucher voucher;
        public int discount;
        public String error;
    }

    private final DatabaseHelper helper;

    public VoucherDao(Context context) {
        helper = DatabaseHelper.get(context);
    }

    static Voucher fromCursor(Cursor c) {
        Voucher v = new Voucher();
        v.id = c.getInt(0);
        v.code = c.getString(1);
        v.description = c.getString(2);
        v.kind = c.getString(3);
        v.value = c.getInt(4);
        v.minSpend = c.getInt(5);
        v.active = c.getInt(6) == 1;
        return v;
    }

    private static final String COLS = "id, code, description, kind, value, min_spend, active";

    public Voucher findByCode(String code) {
        if (code == null) return null;
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT " + COLS + " FROM vouchers WHERE code = ?",
                new String[]{code.trim().toUpperCase(Locale.ROOT)})) {
            return c.moveToFirst() ? fromCursor(c) : null;
        }
    }

    public List<Voucher> getActive() {
        List<Voucher> out = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT " + COLS + " FROM vouchers WHERE active = 1 ORDER BY id", null)) {
            while (c.moveToNext()) out.add(fromCursor(c));
        }
        return out;
    }

    public boolean alreadyUsed(int userId, int voucherId) {
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT 1 FROM voucher_redemptions WHERE user_id=? AND voucher_id=?",
                new String[]{String.valueOf(userId), String.valueOf(voucherId)})) {
            return c.moveToFirst();
        }
    }

    public Result check(int userId, String code, int subtotal, int shipping) {
        Result r = new Result();
        Voucher v = findByCode(code);
        if (v == null || !v.active) {
            r.error = "This voucher code is not valid.";
        } else if (alreadyUsed(userId, v.id)) {
            r.error = "You already used this voucher.";
        } else if (subtotal < v.minSpend) {
            r.error = "Spend at least ₱" + v.minSpend + " to use " + v.code + ".";
        } else {
            r.voucher = v;
            r.discount = Math.min(v.discountFor(subtotal, shipping), subtotal + shipping);
            if (r.discount <= 0) {
                r.voucher = null;
                r.error = v.code.equals("FREESHIP") ? "Your order already has free shipping." : "This voucher gives no discount here.";
            }
        }
        return r;
    }
}
