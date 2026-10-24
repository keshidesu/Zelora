package com.zelora.app.model;

import android.content.Context;

import java.io.Serializable;

// product
public class Product implements Serializable {
    public final int id;
    public final String name;
    public final String category;
    public final String description;
    public final int price;
    public final int originalPrice;
    public final double rating;
    public final int reviewCount;
    public final String badge;
    public final String[] gallery;
    public final String[] colors;
    public final String defaultColor;
    public final int stock;
    public int sellerId;
    public boolean active = true;

    public Product(int id, String name, String category, String description, int price,
                   int originalPrice, double rating, int reviewCount, String badge,
                   String gallery, String colors, String defaultColor, int stock) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.originalPrice = originalPrice;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.badge = badge == null ? "" : badge;
        this.gallery = gallery.split(",");
        this.colors = colors.split(",");
        this.defaultColor = defaultColor;
        this.stock = stock;
    }

    public boolean hasDiscount() {
        return originalPrice > price;
    }

    public int discountPercent() {
        if (!hasDiscount()) return 0;
        return Math.round((originalPrice - price) * 100f / originalPrice);
    }

    public int thumbRes(Context c) {
        return drawableRes(c, gallery[0]);
    }

    public static int drawableRes(Context c, String drawableName) {
        return c.getResources().getIdentifier(drawableName, "drawable", c.getPackageName());
    }
}
