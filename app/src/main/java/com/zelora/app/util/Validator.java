package com.zelora.app.util;

import android.util.Patterns;

import java.util.Calendar;

// validator
public final class Validator {
    private Validator() {}

    public static String name(String s) {
        if (s == null || s.trim().length() < 2) return "Please enter your full name";
        return null;
    }

    public static String email(String s) {
        if (s == null || s.trim().isEmpty()) return "Email is required";
        if (!Patterns.EMAIL_ADDRESS.matcher(s.trim()).matches()) return "Enter a valid email address";
        return null;
    }

    public static String password(String s) {
        if (s == null || s.length() < 8) return "Password must be at least 8 characters";
        boolean letter = false, digit = false;
        for (char c : s.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        if (!letter || !digit) return "Use both letters and numbers";
        return null;
    }

    public static String phone(String s, boolean required) {
        if (s == null || s.trim().isEmpty()) return required ? "Mobile number is required" : null;
        if (!s.trim().matches("^(09|\\+639)\\d{9}$")) return "Use format 09XXXXXXXXX";
        return null;
    }

    public static String address(String s) {
        if (s == null || s.trim().length() < 8) return "Enter a complete address";
        return null;
    }

    public static String cardNumber(String s) {
        String d = s == null ? "" : s.replaceAll("\\s", "");
        if (!d.matches("\\d{13,19}")) return "Enter a valid card number";
        int sum = 0;
        boolean alt = false;
        for (int i = d.length() - 1; i >= 0; i--) {
            int n = d.charAt(i) - '0';
            if (alt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0 ? null : "Card number is invalid";
    }

    public static String expiry(String s) {
        if (s == null || !s.matches("(0[1-9]|1[0-2])/\\d{2}")) return "Use MM/YY";
        int month = Integer.parseInt(s.substring(0, 2));
        int year = 2000 + Integer.parseInt(s.substring(3));
        Calendar now = Calendar.getInstance();
        int curYear = now.get(Calendar.YEAR);
        int curMonth = now.get(Calendar.MONTH) + 1;
        if (year < curYear || (year == curYear && month < curMonth)) return "Card has expired";
        return null;
    }

    public static String cvv(String s) {
        if (s == null || !s.matches("\\d{3,4}")) return "3-4 digits";
        return null;
    }

    public static String productName(String s) {
        if (s == null || s.trim().length() < 2) return "Enter a product name (2 characters or more)";
        if (s.trim().length() > 60) return "Product name is too long (60 characters max)";
        return null;
    }

    public static String price(String s) {
        Integer v = parseInt(s);
        if (v == null || v < 1) return "Enter a price in pesos (1 or more)";
        if (v > 1000000) return "Price is too high";
        return null;
    }

    public static String originalPrice(String s, int price) {
        if (s == null || s.trim().isEmpty()) return null;
        Integer v = parseInt(s);
        if (v == null || v <= price) return "Old price must be higher than the price";
        if (v > 1000000) return "Price is too high";
        return null;
    }

    public static String stock(String s) {
        Integer v = parseInt(s);
        if (v == null || v < 0) return "Enter the stock (0 or more)";
        if (v > 9999) return "Stock is too high (9999 max)";
        return null;
    }

    public static String productDescription(String s) {
        if (s == null || s.trim().length() < 10) return "Write a short description (10 characters or more)";
        if (s.trim().length() > 600) return "Description is too long (600 characters max)";
        return null;
    }

    public static String disputeDetails(String s) {
        if (s == null || s.trim().length() < 5) return "Please tell us what happened";
        return null;
    }

    private static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
