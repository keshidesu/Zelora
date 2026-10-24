package com.zelora.app.model;

// auth
public class User {
    public static final String CUSTOMER = "CUSTOMER", SELLER = "SELLER", ADMIN = "ADMIN", RIDER = "RIDER";

    public final int id;
    public final String fullName;
    public final String email;
    public final String phone;
    public final String role;
    public final boolean active;

    public User(int id, String fullName, String email, String phone) {
        this(id, fullName, email, phone, CUSTOMER, true);
    }

    public User(int id, String fullName, String email, String phone, String role, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone == null ? "" : phone;
        this.role = role == null ? CUSTOMER : role;
        this.active = active;
    }

    public String initials() {
        String[] parts = fullName.trim().split("\\s+");
        String s = parts[0].substring(0, 1);
        if (parts.length > 1) s += parts[parts.length - 1].substring(0, 1);
        return s.toUpperCase();
    }

    public static String roleLabel(String role) {
        if (role == null || role.isEmpty()) return "Customer";
        return role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();
    }
}
