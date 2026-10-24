package com.zelora.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.zelora.app.model.CartItem;
import com.zelora.app.model.Order;
import com.zelora.app.model.Voucher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// order
public class OrderDao {
    private final DatabaseHelper helper;
    private final Context appContext;

    public OrderDao(Context context) {
        appContext = context.getApplicationContext();
        helper = DatabaseHelper.get(appContext);
    }

    // order
    public String placeOrder(int userId, List<CartItem> items, String recipient, String address,
                             String paymentMethod, String paymentStatus, int subtotal, int shipping,
                             String voucherCode) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            java.util.Map<Integer, Integer> livePrice = new java.util.HashMap<>();
            subtotal = 0;
            for (CartItem item : items) {
                try (Cursor c = db.rawQuery("SELECT price FROM products WHERE id = ? AND active = 1",
                        new String[]{String.valueOf(item.product.id)})) {
                    if (!c.moveToFirst()) {
                        throw new IllegalStateException(item.product.name + " is no longer available.");
                    }
                    livePrice.put(item.product.id, c.getInt(0));
                    subtotal += c.getInt(0) * item.quantity;
                }
            }
            shipping = com.zelora.app.util.Money.shippingFor(subtotal);

            int discount = 0;
            Voucher voucher = null;
            if (voucherCode != null && !voucherCode.trim().isEmpty()) {
                VoucherDao.Result r = new VoucherDao(appContext).check(userId, voucherCode, subtotal, shipping);
                if (r.voucher == null) throw new IllegalStateException(r.error);
                voucher = r.voucher;
                discount = r.discount;
            }
            int total = subtotal + shipping - discount;

            ContentValues o = new ContentValues();
            o.put("user_id", userId);
            o.put("subtotal", subtotal);
            o.put("shipping", shipping);
            o.put("discount", discount);
            if (voucher != null) o.put("voucher_code", voucher.code);
            o.put("total", total);
            o.put("recipient", recipient);
            o.put("address", address);
            long orderId = db.insertOrThrow("orders", null, o);
            String code = String.format(Locale.US, "ZEL-%06d", orderId);
            ContentValues codeValue = new ContentValues();
            codeValue.put("order_code", code);
            db.update("orders", codeValue, "id=?", new String[]{String.valueOf(orderId)});

            for (CartItem item : items) {
                SQLiteStatement stock = db.compileStatement(
                        "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ? AND active = 1");
                stock.bindLong(1, item.quantity);
                stock.bindLong(2, item.product.id);
                stock.bindLong(3, item.quantity);
                if (stock.executeUpdateDelete() == 0) {
                    throw new IllegalStateException("Sorry, " + item.product.name + " is out of stock.");
                }
                int sellerId = 0;
                try (Cursor c = db.rawQuery("SELECT seller_id FROM products WHERE id=?",
                        new String[]{String.valueOf(item.product.id)})) {
                    if (c.moveToFirst() && !c.isNull(0)) sellerId = c.getInt(0);
                }
                ContentValues l = new ContentValues();
                l.put("order_id", orderId);
                l.put("product_id", item.product.id);
                l.put("product_name", item.product.name);
                l.put("color", item.color);
                l.put("quantity", item.quantity);
                l.put("unit_price", livePrice.get(item.product.id));
                if (sellerId > 0) l.put("seller_id", sellerId);
                db.insertOrThrow("order_items", null, l);
                if (item.cartId > 0) {
                    db.delete("cart_items", "id=?", new String[]{String.valueOf(item.cartId)});
                }
            }

            if (voucher != null) {
                ContentValues rv = new ContentValues();
                rv.put("user_id", userId);
                rv.put("voucher_id", voucher.id);
                rv.put("order_id", orderId);
                db.insertOrThrow("voucher_redemptions", null, rv);
            }

            ContentValues p = new ContentValues();
            p.put("order_id", orderId);
            p.put("method", paymentMethod);
            p.put("amount", total);
            p.put("status", paymentStatus);
            db.insertOrThrow("payments", null, p);

            db.setTransactionSuccessful();
            return code;
        } finally {
            db.endTransaction();
        }
    }

    public boolean cancelOrder(int userId, int orderId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues status = new ContentValues();
            status.put("status", Order.CANCELLED);
            int changed = db.update("orders", status, "id=? AND user_id=? AND status='Processing'",
                    new String[]{String.valueOf(orderId), String.valueOf(userId)});
            if (changed == 0) return false;

            db.execSQL("UPDATE products SET stock = stock + COALESCE((SELECT SUM(quantity) FROM order_items"
                    + " WHERE order_id = ? AND product_id = products.id), 0)"
                    + " WHERE id IN (SELECT product_id FROM order_items WHERE order_id = ?)",
                    new Object[]{orderId, orderId});
            db.execSQL("UPDATE payments SET status = CASE WHEN status = 'PAID' THEN 'REFUNDED' ELSE 'CANCELLED' END"
                    + " WHERE order_id = ?", new Object[]{orderId});
            db.execSQL("DELETE FROM voucher_redemptions WHERE order_id = ?", new Object[]{orderId});

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    // staff
    public boolean acceptOrder(int sellerId, int orderId) {
        return exec(helper.getWritableDatabase(),
                "UPDATE orders SET status='Preparing' WHERE id=? AND status='Processing'"
                        + " AND id IN (SELECT order_id FROM order_items WHERE seller_id=?)",
                orderId, sellerId) > 0;
    }

    public boolean markReady(int sellerId, int orderId) {
        return exec(helper.getWritableDatabase(),
                "UPDATE orders SET status='Packed' WHERE id=? AND status='Preparing'"
                        + " AND id IN (SELECT order_id FROM order_items WHERE seller_id=?)",
                orderId, sellerId) > 0;
    }

    public boolean acceptJob(int riderId, int orderId) {
        return exec(helper.getWritableDatabase(),
                "UPDATE orders SET status='Out for Delivery', picked_up=0, rider_id=? WHERE id=? AND status='Packed' AND rider_id IS NULL",
                riderId, orderId) > 0;
    }

    public void rejectJob(int riderId, int orderId) {
        helper.getWritableDatabase().execSQL(
                "INSERT OR IGNORE INTO job_rejections(order_id, rider_id) VALUES(?, ?)", new Object[]{orderId, riderId});
    }

    public boolean confirmPickup(int riderId, int orderId) {
        return exec(helper.getWritableDatabase(),
                "UPDATE orders SET picked_up=1 WHERE id=? AND rider_id=? AND status='Out for Delivery' AND picked_up=0",
                orderId, riderId) > 0;
    }

    public boolean markDelivered(int riderId, int orderId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            if (exec(db, "UPDATE orders SET status='Delivered', delivered_at=CURRENT_TIMESTAMP"
                    + " WHERE id=? AND rider_id=? AND status='Out for Delivery' AND picked_up=1",
                    orderId, riderId) == 0) return false;
            db.execSQL("UPDATE payments SET status='PAID' WHERE order_id=? AND status='PENDING'", new Object[]{orderId});
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    private int exec(SQLiteDatabase db, String sql, Object... args) {
        SQLiteStatement st = db.compileStatement(sql);
        try {
            for (int i = 0; i < args.length; i++) {
                Object a = args[i];
                if (a instanceof Number) st.bindLong(i + 1, ((Number) a).longValue());
                else st.bindString(i + 1, String.valueOf(a));
            }
            return st.executeUpdateDelete();
        } finally {
            st.close();
        }
    }

    // order
    public List<Order> getOrders(int userId) {
        return query("o.user_id = ?", new String[]{String.valueOf(userId)}, 0);
    }

    public List<Order> getAllOrders() {
        return query("1=1", new String[0], 0);
    }

    public List<Order> getForSeller(int sellerId) {
        return query("o.id IN (SELECT order_id FROM order_items WHERE seller_id = ?)",
                new String[]{String.valueOf(sellerId)}, sellerId);
    }

    public List<Order> getAvailableJobs(int riderId) {
        return query("o.status = 'Packed' AND o.rider_id IS NULL AND o.id NOT IN"
                + " (SELECT order_id FROM job_rejections WHERE rider_id = ?)", new String[]{String.valueOf(riderId)}, 0);
    }

    public Order getRiderOrder(int riderId, int orderId) {
        List<Order> l = query("o.rider_id = ? AND o.id = ?",
                new String[]{String.valueOf(riderId), String.valueOf(orderId)}, 0);
        return l.isEmpty() ? null : l.get(0);
    }

    public int firstSellerId(int orderId) {
        return (int) scalar("SELECT seller_id FROM order_items WHERE order_id = ? ORDER BY id LIMIT 1",
                String.valueOf(orderId));
    }

    public List<Order> getRiderDeliveries(int riderId) {
        return query("o.rider_id = ?", new String[]{String.valueOf(riderId)}, 0);
    }

    private List<Order> query(String where, String[] args, int sellerFilter) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.id, o.order_code, o.subtotal, o.shipping, o.discount, o.voucher_code, o.total, o.status,"
                + " o.created_at, o.recipient, o.address, p.method, p.status, o.user_id, cu.full_name,"
                + " o.rider_id, ri.full_name, d.id, d.status, d.reason, d.admin_note, o.picked_up, o.delivered_at"
                + " FROM orders o"
                + " LEFT JOIN payments p ON p.order_id = o.id"
                + " LEFT JOIN users cu ON cu.id = o.user_id"
                + " LEFT JOIN users ri ON ri.id = o.rider_id"
                + " LEFT JOIN disputes d ON d.id = (SELECT MAX(id) FROM disputes WHERE order_id = o.id)"
                + " WHERE " + where + " ORDER BY o.id DESC";
        try (Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) {
                Order o = new Order();
                o.id = c.getInt(0);
                o.code = c.getString(1);
                o.subtotal = c.getInt(2);
                o.shipping = c.getInt(3);
                o.discount = c.getInt(4);
                o.voucherCode = c.getString(5);
                o.total = c.getInt(6);
                o.status = c.getString(7);
                o.createdAt = c.getString(8);
                o.recipient = c.getString(9);
                o.address = c.getString(10);
                o.paymentMethod = c.getString(11);
                o.paymentStatus = c.getString(12);
                o.customerId = c.getInt(13);
                o.customerName = c.getString(14);
                o.riderId = c.isNull(15) ? 0 : c.getInt(15);
                o.riderName = c.getString(16);
                o.disputeId = c.isNull(17) ? 0 : c.getInt(17);
                o.disputeStatus = c.getString(18);
                o.disputeReason = c.getString(19);
                o.disputeNote = c.getString(20);
                o.pickedUp = c.getInt(21) == 1;
                o.deliveredAt = c.getString(22);
                orders.add(o);
            }
        }
        for (Order o : orders) {
            String linesSql = "SELECT product_id, product_name, color, quantity, unit_price FROM order_items WHERE order_id=?";
            String[] lineArgs = {String.valueOf(o.id)};
            if (sellerFilter > 0) {
                linesSql += " AND seller_id=?";
                lineArgs = new String[]{String.valueOf(o.id), String.valueOf(sellerFilter)};
            }
            try (Cursor c = db.rawQuery(linesSql, lineArgs)) {
                while (c.moveToNext()) {
                    o.lines.add(new Order.Line(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4)));
                }
            }
        }
        return orders;
    }

    private long scalar(String sql, String... args) {
        try (Cursor c = helper.getReadableDatabase().rawQuery(sql, args)) {
            return c.moveToFirst() && !c.isNull(0) ? c.getLong(0) : 0;
        }
    }

    public Map<String, String> sellerStats(int sellerId) {
        String id = String.valueOf(sellerId);
        Map<String, String> m = new LinkedHashMap<>();
        long revenue = scalar("SELECT SUM(i.quantity * i.unit_price) FROM order_items i JOIN orders o ON o.id = i.order_id"
                + " WHERE i.seller_id = ? AND o.status <> 'Cancelled'", id);
        long units = scalar("SELECT SUM(i.quantity) FROM order_items i JOIN orders o ON o.id = i.order_id"
                + " WHERE i.seller_id = ? AND o.status <> 'Cancelled'", id);
        long orders = scalar("SELECT COUNT(DISTINCT i.order_id) FROM order_items i JOIN orders o ON o.id = i.order_id"
                + " WHERE i.seller_id = ? AND o.status <> 'Cancelled'", id);
        long delivered = scalar("SELECT COUNT(DISTINCT i.order_id) FROM order_items i JOIN orders o ON o.id = i.order_id"
                + " WHERE i.seller_id = ? AND o.status = 'Delivered'", id);
        long cancelled = scalar("SELECT COUNT(DISTINCT i.order_id) FROM order_items i JOIN orders o ON o.id = i.order_id"
                + " WHERE i.seller_id = ? AND o.status = 'Cancelled'", id);
        long lowStock = scalar("SELECT COUNT(*) FROM products WHERE seller_id = ? AND active = 1 AND stock <= 5", id);
        String top = "-";
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT i.product_name, SUM(i.quantity) q FROM order_items i JOIN orders o ON o.id = i.order_id"
                        + " WHERE i.seller_id = ? AND o.status <> 'Cancelled' GROUP BY i.product_id ORDER BY q DESC LIMIT 1",
                new String[]{id})) {
            if (c.moveToFirst()) top = c.getString(0) + " (" + c.getInt(1) + " sold)";
        }
        m.put("Total sales", com.zelora.app.util.Money.peso((int) revenue));
        m.put("Units sold", String.valueOf(units));
        m.put("Orders received", String.valueOf(orders));
        m.put("Delivered orders", String.valueOf(delivered));
        m.put("Cancelled orders", String.valueOf(cancelled));
        m.put("Products low on stock (5 or less)", String.valueOf(lowStock));
        m.put("Best seller", top);
        return m;
    }

    public int countForSeller(int sellerId, String status) {
        return (int) scalar("SELECT COUNT(DISTINCT i.order_id) FROM order_items i JOIN orders o ON o.id = i.order_id"
                + " WHERE i.seller_id = ? AND o.status = ?", String.valueOf(sellerId), status);
    }

    public int[] dailySales(int sellerId, int days, int endOffset) {
        int[] out = new int[days];
        for (int i = 0; i < days; i++) {
            int ago = endOffset + (days - 1 - i);
            out[i] = (int) scalar("SELECT SUM(i.quantity * i.unit_price) FROM order_items i JOIN orders o ON o.id = i.order_id"
                    + " WHERE i.seller_id = ? AND o.status <> 'Cancelled'"
                    + " AND date(o.created_at, 'localtime') = date('now', 'localtime', ?)",
                    String.valueOf(sellerId), "-" + ago + " days");
        }
        return out;
    }

    public int deliveriesToday(int riderId) {
        return (int) scalar("SELECT COUNT(*) FROM orders WHERE rider_id = ? AND status = 'Delivered'"
                + " AND date(delivered_at, 'localtime') = date('now', 'localtime')", String.valueOf(riderId));
    }

    public int deliveriesTotal(int riderId) {
        return (int) scalar("SELECT COUNT(*) FROM orders WHERE rider_id = ? AND status = 'Delivered'",
                String.valueOf(riderId));
    }

    public int activeDeliveries(int riderId) {
        return (int) scalar("SELECT COUNT(*) FROM orders WHERE rider_id = ? AND status = 'Out for Delivery'",
                String.valueOf(riderId));
    }

    public Map<String, String> adminStats() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Customers", String.valueOf(scalar("SELECT COUNT(*) FROM users WHERE role='CUSTOMER'")));
        m.put("Sellers", String.valueOf(scalar("SELECT COUNT(*) FROM users WHERE role='SELLER'")));
        m.put("Riders", String.valueOf(scalar("SELECT COUNT(*) FROM users WHERE role='RIDER'")));
        m.put("Orders (all)", String.valueOf(scalar("SELECT COUNT(*) FROM orders")));
        m.put("Orders in progress", String.valueOf(scalar("SELECT COUNT(*) FROM orders WHERE status IN ('Processing','Packed','Out for Delivery')")));
        m.put("Sales (not cancelled)", com.zelora.app.util.Money.peso((int) scalar("SELECT SUM(total) FROM orders WHERE status <> 'Cancelled'")));
        m.put("Open disputes", String.valueOf(scalar("SELECT COUNT(*) FROM disputes WHERE status='Open'")));
        return m;
    }
}
