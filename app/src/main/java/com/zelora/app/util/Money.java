package com.zelora.app.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

// money
public final class Money {
    public static final int FREE_SHIPPING_MIN = 800;
    public static final int SHIPPING_FEE = 50;
    public static final int RIDER_FEE = 40;

    private Money() {}

    public static String peso(int amount) {
        return "₱" + new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US)).format(amount);
    }

    public static int shippingFor(int subtotal) {
        if (subtotal <= 0) return 0;
        return subtotal >= FREE_SHIPPING_MIN ? 0 : SHIPPING_FEE;
    }
}
