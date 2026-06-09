package com.example.bringo.utils;

import android.util.Patterns;

public final class Validation {
    private Validation() {}

    // Email format
    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    // Israeli phone: exactly 10 digits and starts with 0 (e.g., 05xxxxxxxx)
    public static boolean isValidPhoneIL(String phone) {
        if (phone == null) return false;
        String p = phone.trim();
        return p.matches("^0\\d{9}$");
    }

    // Name: at least 2 chars and no digits
    public static boolean isValidName(String name) {
        if (name == null) return false;
        String n = name.trim();
        return n.length() >= 2 && !n.matches(".*\\d.*");
    }

    // Car plate: 7 or 8 digits only
    public static boolean isValidCarPlate(String plate) {
        if (plate == null) return false;
        String p = plate.trim();
        return p.matches("^\\d{7,8}$");
    }

    // Israeli ID check (Teudat Zehut) – includes checksum
    public static boolean isValidIsraeliId(String id) {
        if (id == null) return false;
        String s = id.trim();
        if (!s.matches("^\\d{5,9}$")) return false;

        s = String.format("%09d", Integer.parseInt(s));

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int digit = s.charAt(i) - '0';
            int mult = (i % 2) + 1;          // 1,2,1,2...
            int inc = digit * mult;
            if (inc > 9) inc -= 9;
            sum += inc;
        }
        return sum % 10 == 0;
    }
}