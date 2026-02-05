package com.example.diplom.logic;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileLogic {

    private final Context context;
    private final FirebaseUser currentUser;
    private final FirebaseFirestore db;

    public ProfileLogic(Context context, FirebaseUser user) {
        this.context = context;
        this.currentUser = user;
        this.db = FirebaseFirestore.getInstance();
    }

    public void logout() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
        Toast.makeText(context, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
        context.startActivity(new android.content.Intent(context, com.example.diplom.auth.LoginActivity.class)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    public void showChangeValueDialog(String field, TextView textView) {
        if (currentUser == null) return;

        EditText input = new EditText(context);

        switch (field) {
            case "weight":
            case "goal":
            case "height":
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                break;
            case "age":
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            case "gender":
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                break;
        }

        new AlertDialog.Builder(context)
                .setTitle("Изменить " + field)
                .setView(input)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String valueStr = input.getText().toString().trim();
                    if (valueStr.isEmpty()) {
                        Toast.makeText(context, "Введите значение", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> update = new HashMap<>();
                    if (field.equals("weight") || field.equals("goal") || field.equals("height")) {
                        try {
                            update.put(field, Double.parseDouble(valueStr));
                        } catch (NumberFormatException e) {
                            Toast.makeText(context, "Введите корректное число", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else if (field.equals("age")) {
                        try {
                            update.put(field, Integer.parseInt(valueStr));
                        } catch (NumberFormatException e) {
                            Toast.makeText(context, "Введите корректный возраст", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        update.put(field, valueStr);
                    }

                    db.collection("users")
                            .document(currentUser.getUid())
                            .update(update)
                            .addOnSuccessListener(aVoid -> textView.setText(field + ": " + valueStr))
                            .addOnFailureListener(e -> Toast.makeText(context, "Ошибка обновления: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}
