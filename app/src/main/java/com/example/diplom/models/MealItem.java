package com.example.diplom.models;

public class MealItem {
    private String id;
    private String name;
    private float grams;
    private float calories;
    private float protein;
    private float fat;
    private float carb;
    private float fiber;

    public MealItem() {}  // нужен для Firebase


    public MealItem(String id, String name, float grams, float calories, float protein, float fat, float carb, float fiber) {
        this.id = id;
        this.name = name;
        this.grams = grams;
        this.calories = calories;
        this.protein = protein;
        this.fat = fat;
        this.carb = carb;
        this.fiber = fiber;
    }


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public float getGrams() { return grams; }
    public void setGrams(float grams) { this.grams = grams; }

    public float getCalories() { return calories; }
    public void setCalories(float calories) { this.calories = calories; }

    public float getProtein() { return protein; }
    public void setProtein(float protein) { this.protein = protein; }

    public float getFat() { return fat; }
    public void setFat(float fat) { this.fat = fat; }

    public float getCarb() { return carb; }
    public void setCarb(float carb) { this.carb = carb; }

    public float getFiber() { return fiber; }
    public void setFiber(float fiber) { this.fiber = fiber; }
}
