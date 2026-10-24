package com.zelora.app.db;

import android.content.ContentValues;
import android.database.Cursor;

import com.zelora.app.util.PasswordUtil;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// database
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "zelora.db";
    public static final int DB_VERSION = 3;

    // staff
    public static final String ADMIN_EMAIL = "admin@zelora.com", ADMIN_PASSWORD = "Admin1234";
    public static final String SELLER_EMAIL = "seller@zelora.com", SELLER_PASSWORD = "Seller1234";
    public static final String RIDER_EMAIL = "rider@zelora.com", RIDER_PASSWORD = "Rider1234";

    private static DatabaseHelper instance;

    // database
    public static synchronized DatabaseHelper get(Context context) {
        if (instance == null) instance = new DatabaseHelper(context.getApplicationContext());
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "full_name TEXT NOT NULL,"
                + "email TEXT NOT NULL UNIQUE,"
                + "password_hash TEXT NOT NULL,"
                + "salt TEXT NOT NULL,"
                + "phone TEXT DEFAULT '',"
                + "role TEXT NOT NULL DEFAULT 'CUSTOMER',"
                + "active INTEGER NOT NULL DEFAULT 1,"
                + "created_at TEXT DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE products ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "category TEXT NOT NULL,"
                + "description TEXT NOT NULL,"
                + "price INTEGER NOT NULL,"
                + "original_price INTEGER NOT NULL DEFAULT 0,"
                + "rating REAL NOT NULL,"
                + "review_count INTEGER NOT NULL,"
                + "badge TEXT DEFAULT '',"
                + "gallery TEXT NOT NULL,"
                + "colors TEXT NOT NULL,"
                + "default_color TEXT NOT NULL,"
                + "stock INTEGER NOT NULL,"
                + "seller_id INTEGER,"
                + "active INTEGER NOT NULL DEFAULT 1)");

        db.execSQL("CREATE TABLE cart_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                + "product_id INTEGER NOT NULL REFERENCES products(id),"
                + "color TEXT NOT NULL,"
                + "quantity INTEGER NOT NULL,"
                + "UNIQUE(user_id, product_id, color))");

        db.execSQL("CREATE TABLE wishlist ("
                + "user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                + "product_id INTEGER NOT NULL REFERENCES products(id),"
                + "PRIMARY KEY(user_id, product_id))");

        db.execSQL("CREATE TABLE addresses ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,"
                + "recipient TEXT NOT NULL,"
                + "address_line TEXT NOT NULL)");

        db.execSQL("CREATE TABLE orders ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "order_code TEXT,"
                + "user_id INTEGER NOT NULL REFERENCES users(id),"
                + "subtotal INTEGER NOT NULL,"
                + "shipping INTEGER NOT NULL,"
                + "total INTEGER NOT NULL,"
                + "status TEXT NOT NULL DEFAULT 'Processing',"
                + "recipient TEXT NOT NULL,"
                + "address TEXT NOT NULL,"
                + "voucher_code TEXT,"
                + "discount INTEGER NOT NULL DEFAULT 0,"
                + "rider_id INTEGER,"
                + "picked_up INTEGER NOT NULL DEFAULT 0,"
                + "delivered_at TEXT,"
                + "created_at TEXT DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE order_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,"
                + "product_id INTEGER NOT NULL REFERENCES products(id),"
                + "product_name TEXT NOT NULL,"
                + "color TEXT NOT NULL,"
                + "quantity INTEGER NOT NULL,"
                + "unit_price INTEGER NOT NULL,"
                + "seller_id INTEGER)");

        db.execSQL("CREATE TABLE payments ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,"
                + "method TEXT NOT NULL,"
                + "amount INTEGER NOT NULL,"
                + "status TEXT NOT NULL)");

        createV2Tables(db);
        seedStaffAndProducts(db);
    }

    // table
    private void createV2Tables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS vouchers ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "code TEXT NOT NULL UNIQUE,"
                + "description TEXT NOT NULL,"
                + "kind TEXT NOT NULL,"
                + "value INTEGER NOT NULL DEFAULT 0,"
                + "min_spend INTEGER NOT NULL DEFAULT 0,"
                + "active INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE IF NOT EXISTS voucher_redemptions ("
                + "user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                + "voucher_id INTEGER NOT NULL REFERENCES vouchers(id) ON DELETE CASCADE,"
                + "order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,"
                + "PRIMARY KEY(user_id, voucher_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS reviews ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "product_id INTEGER NOT NULL REFERENCES products(id),"
                + "user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                + "order_id INTEGER NOT NULL REFERENCES orders(id),"
                + "rating INTEGER NOT NULL,"
                + "comment TEXT DEFAULT '',"
                + "created_at TEXT DEFAULT CURRENT_TIMESTAMP,"
                + "UNIQUE(user_id, product_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS disputes ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,"
                + "user_id INTEGER NOT NULL REFERENCES users(id),"
                + "reason TEXT NOT NULL,"
                + "details TEXT DEFAULT '',"
                + "status TEXT NOT NULL DEFAULT 'Open',"
                + "admin_note TEXT DEFAULT '',"
                + "created_at TEXT DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE IF NOT EXISTS job_rejections ("
                + "order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,"
                + "rider_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,"
                + "PRIMARY KEY(order_id, rider_id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'CUSTOMER'");
            db.execSQL("ALTER TABLE users ADD COLUMN active INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE products ADD COLUMN seller_id INTEGER");
            db.execSQL("ALTER TABLE products ADD COLUMN active INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE orders ADD COLUMN voucher_code TEXT");
            db.execSQL("ALTER TABLE orders ADD COLUMN discount INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE orders ADD COLUMN rider_id INTEGER");
            db.execSQL("ALTER TABLE order_items ADD COLUMN seller_id INTEGER");
            createV2Tables(db);
            int sellerId = ensureStaff(db);
            db.execSQL("UPDATE products SET seller_id = ? WHERE seller_id IS NULL", new Object[]{sellerId});
            db.execSQL("UPDATE order_items SET seller_id = ? WHERE seller_id IS NULL", new Object[]{sellerId});
            seedVouchers(db);
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE orders ADD COLUMN picked_up INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE orders ADD COLUMN delivered_at TEXT");
            db.execSQL("UPDATE orders SET picked_up = 1 WHERE status IN ('Out for Delivery','Delivered')");
            db.execSQL("UPDATE orders SET delivered_at = created_at WHERE status = 'Delivered'");
        }
    }

    // staff
    private int ensureStaff(SQLiteDatabase db) {
        addUser(db, "Zelora Admin", ADMIN_EMAIL, ADMIN_PASSWORD, "ADMIN");
        int seller = addUser(db, "Zelora Official Store", SELLER_EMAIL, SELLER_PASSWORD, "SELLER");
        addUser(db, "Rico Rider", RIDER_EMAIL, RIDER_PASSWORD, "RIDER");
        return seller;
    }

    private int addUser(SQLiteDatabase db, String name, String email, String password, String role) {
        try (Cursor c = db.rawQuery("SELECT id FROM users WHERE email = ?", new String[]{email})) {
            if (c.moveToFirst()) return c.getInt(0);
        }
        String salt = PasswordUtil.newSalt();
        ContentValues v = new ContentValues();
        v.put("full_name", name);
        v.put("email", email);
        v.put("salt", salt);
        v.put("password_hash", PasswordUtil.hash(password, salt));
        v.put("role", role);
        return (int) db.insert("users", null, v);
    }

    private void seedVouchers(SQLiteDatabase db) {
        addVoucher(db, "WELCOME10", "10% off your order", "PERCENT", 10, 0);
        addVoucher(db, "SAVE100", "P100 off orders of P1,000 or more", "FIXED", 100, 1000);
        addVoucher(db, "FREESHIP", "Free shipping on any order", "SHIPPING", 0, 0);
    }

    private void addVoucher(SQLiteDatabase db, String code, String description, String kind, int value, int minSpend) {
        ContentValues v = new ContentValues();
        v.put("code", code);
        v.put("description", description);
        v.put("kind", kind);
        v.put("value", value);
        v.put("min_spend", minSpend);
        db.insertWithOnConflict("vouchers", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void seedStaffAndProducts(SQLiteDatabase db) {
        int sellerId = ensureStaff(db);
        seedProducts(db, sellerId);
        seedVouchers(db);
    }

    // seed
    private int currentSeller;

    private void seedProducts(SQLiteDatabase db, int sellerId) {
        currentSeller = sellerId;
        addProduct(db, "Ceramic Mug", "Accessories",
                "Enjoy your favorite hot drinks every morning. This nice ceramic mug holds your hot coffee or tea very well. It is a wonderful item to use for your morning routine or when you just want to relax at home.",
                200, 0, 4.7, 98, "NEW", gallery("img_mug"), "Blue,Cream,Brown,Yellow", "Blue", 40);

        addProduct(db, "Casual Sneakers", "Apparel",
                "Walk in comfort and style all day. These light and trendy shoes go very well with almost any outfit you wear. They are a wonderful choice for going to school, going to work, or hanging out with your friends.",
                1000, 0, 4.8, 72, "TRENDING", gallery("img_sneakers"), "Blue,Cream,Brown,Yellow", "Blue", 25);

        addProduct(db, "Ribbed Knit Sweater", "Apparel",
                "Stay warm and cozy all day long. This soft knit sweater feels very nice on your skin and keeps you warm when the weather is cold. It is a great choice to wear every day and makes your outfit look very nice.",
                800, 0, 4.9, 52, "BEST SELLER", gallery("img_sweater"), "Red,Blue,Brown,Yellow", "Red", 30);

        addProduct(db, "Canvas Tote Bag", "Bags",
                "Carry your daily essentials in style with this cute floral canvas tote bag. Crafted from durable canvas with a classic vintage design, it is perfect for work, school, or daily shopping.",
                300, 400, 4.8, 124, "BEST SELLER", gallery("img_tote"), "Cream,Brown,Yellow,Blue", "Blue", 50);
    }

    private String gallery(String main) {
        return main + "," + main + "," + main + "," + main;
    }

    private void addProduct(SQLiteDatabase db, String name, String category, String description,
                            int price, int originalPrice, double rating, int reviews, String badge,
                            String gallery, String colors, String defaultColor, int stock) {
        ContentValues v = new ContentValues();
        v.put("seller_id", currentSeller);
        v.put("name", name);
        v.put("category", category);
        v.put("description", description);
        v.put("price", price);
        v.put("original_price", originalPrice);
        v.put("rating", rating);
        v.put("review_count", reviews);
        v.put("badge", badge);
        v.put("gallery", gallery);
        v.put("colors", colors);
        v.put("default_color", defaultColor);
        v.put("stock", stock);
        db.insert("products", null, v);
    }
}
