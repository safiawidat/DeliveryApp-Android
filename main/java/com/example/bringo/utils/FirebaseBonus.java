package com.example.bringo.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public final class FirebaseBonus {
    private FirebaseBonus() {}

    private static FirebaseAuth auth() {
        return FirebaseAuth.getInstance();
    }

    private static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    // Best-effort auth: app must keep working even if Firebase fails
    public static void ensureSignedIn(String email, String password) {
        if (auth().getCurrentUser() != null) return;

        auth().signInWithEmailAndPassword(email, password)
                .addOnFailureListener(e ->
                        auth().createUserWithEmailAndPassword(email, password)
                                .addOnFailureListener(ignore -> {})
                );
    }

    public static void signOut() {
        auth().signOut();
    }

    // Save the user's profile to Firestore (users/{uid})
    public static void upsertUserProfile(long sqliteUserId, String role, String email, long sqliteCarrierId) {
        if (auth().getCurrentUser() == null) return;

        String uid = auth().getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("role", role);
        data.put("sqliteUserId", sqliteUserId);
        data.put("sqliteCarrierId", sqliteCarrierId);
        data.put("updatedAt", FieldValue.serverTimestamp());

        db().collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnFailureListener(ignore -> {});
    }

    // (We’ll use these later when you paste the remaining screens)
    public static void upsertShipment(Map<String, Object> shipmentDoc, String shipmentId) {
        if (auth().getCurrentUser() == null) return;

        shipmentDoc.put("updatedAt", FieldValue.serverTimestamp());

        db().collection("shipments").document(shipmentId)
                .set(shipmentDoc, SetOptions.merge())
                .addOnFailureListener(ignore -> {});
    }

    public static void addShipmentEvent(String shipmentId, Map<String, Object> eventDoc) {
        if (auth().getCurrentUser() == null) return;

        eventDoc.put("occurredAt", FieldValue.serverTimestamp());

        db().collection("shipments").document(shipmentId)
                .collection("events")
                .add(eventDoc)
                .addOnFailureListener(ignore -> {});
    }
}