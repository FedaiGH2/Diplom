package com.example.diplom.logic;

import com.example.diplom.MainActivity;
import com.example.diplom.R;
import com.example.diplom.models.MealItem;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import java.util.Calendar;


public class TodayMealAdapter extends RecyclerView.Adapter<TodayMealAdapter.ViewHolder> {

    private Context context;
    private List<MealItem> meals;
    private Calendar currentCalendar; // выбранная дата
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public TodayMealAdapter(Context context, List<MealItem> meals, Calendar currentCalendar) {
        this.context = context;
        this.meals = meals;
        this.currentCalendar = currentCalendar;
    }


    public void setCurrentCalendar(Calendar calendar) {
        this.currentCalendar = calendar;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_today_meal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MealItem meal = meals.get(position);
        holder.nameText.setText(meal.getName());
        holder.gramsText.setText(meal.getGrams() + " г");

        holder.gramsText.setOnClickListener(v -> showEditMealDialog(meal, position));

        holder.deleteBtn.setOnClickListener(v -> {
            if (currentUser == null) return;
            String userId = currentUser.getUid();
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(currentCalendar.getTime()); // используем выбранную дату

            db.collection("dailyMeals")
                    .document(userId)
                    .collection(today)
                    .document(meal.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        meals.remove(position);
                        notifyItemRemoved(position);
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).loadDailyMeals();
                        }
                    });
        });
    }

    private void showEditMealDialog(MealItem meal, int position) {
        if (currentUser == null) return;

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_meal, null);

        TextView productNameText = view.findViewById(R.id.dialogProductName);
        TextView caloriesText = view.findViewById(R.id.dialogCaloriesText);
        TextView proteinText = view.findViewById(R.id.dialogProteinText);
        TextView fatText = view.findViewById(R.id.dialogFatText);
        TextView carbText = view.findViewById(R.id.dialogCarbText);
        TextView fiberText = view.findViewById(R.id.dialogFiberText);
        EditText gramsInput = view.findViewById(R.id.dialogGramsInput);
        Button editButton = view.findViewById(R.id.dialogEditButton);

        // Заполняем данные
        productNameText.setText(meal.getName());
        caloriesText.setText("Калории: " + Math.round(meal.getCalories()));
        proteinText.setText("Белки: " + meal.getProtein() + " г");
        fatText.setText("Жиры: " + meal.getFat() + " г");
        carbText.setText("Углеводы: " + meal.getCarb() + " г");
        fiberText.setText("Клетчатка: " + meal.getFiber() + " г");
        gramsInput.setText(String.valueOf(meal.getGrams()));

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        dialog.show();

        editButton.setOnClickListener(v -> {
            String gramsStr = gramsInput.getText().toString().trim();
            if (gramsStr.isEmpty()) return;

            float newGrams;
            try {
                newGrams = Float.parseFloat(gramsStr);
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Введите корректное число", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = currentUser.getUid();
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(currentCalendar.getTime());

            db.collection("dailyMeals").document(userId)
                    .collection(today)
                    .document(meal.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        float factor = newGrams / meal.getGrams();
                        Map<String, Object> newMeal = new HashMap<>();
                        newMeal.put("name", meal.getName());
                        newMeal.put("grams", newGrams);
                        newMeal.put("calories", meal.getCalories() * factor);
                        newMeal.put("protein", meal.getProtein() * factor);
                        newMeal.put("fat", meal.getFat() * factor);
                        newMeal.put("carb", meal.getCarb() * factor);
                        newMeal.put("fiber", meal.getFiber() * factor);
                        newMeal.put("timestamp", System.currentTimeMillis());

                        db.collection("dailyMeals").document(userId)
                                .collection(today)
                                .add(newMeal)
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(context, "Продукт обновлён", Toast.LENGTH_SHORT).show();

                                    MealItem updatedMeal = meals.get(position);
                                    updatedMeal.setGrams(newGrams);
                                    updatedMeal.setCalories(meal.getCalories() * factor);
                                    updatedMeal.setProtein(meal.getProtein() * factor);
                                    updatedMeal.setFat(meal.getFat() * factor);
                                    updatedMeal.setCarb(meal.getCarb() * factor);
                                    updatedMeal.setFiber(meal.getFiber() * factor);

                                    notifyItemChanged(position);

                                    if (context instanceof MainActivity) {
                                        ((MainActivity) context).loadDailyMeals();
                                    }
                                    dialog.dismiss();
                                });
                    });
        });
    }



    @Override
    public int getItemCount() {
        return meals.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, gramsText;
        Button deleteBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
            gramsText = itemView.findViewById(R.id.gramsText);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
        }
    }
}