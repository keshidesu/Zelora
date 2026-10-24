package com.zelora.app.model;

// voucher
public class Voucher {
    public static final String PERCENT = "PERCENT", FIXED = "FIXED", SHIPPING = "SHIPPING";

    public int id;
    public String code;
    public String description;
    public String kind;
    public int value;
    public int minSpend;
    public boolean active;

    public int discountFor(int subtotal, int shipping) {
        if (subtotal < minSpend) return 0;
        switch (kind) {
            case PERCENT:  return subtotal * value / 100;
            case FIXED:    return Math.min(value, subtotal);
            default:       return shipping;
        }
    }
}
