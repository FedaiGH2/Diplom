package com.example.diplom.models;



public class ProductItem {

    private String id;
    private String name;

    private float calories;
    private float protein;
    private float fat;
    private float carb;
    private float fiber;


    public ProductItem() {}


    public ProductItem(String id, String name) {
        this.id = id;
        this.name = name;
    }


    public ProductItem(String id, String name,
                       float calories, float protein,
                       float fat, float carb, float fiber) {
        this.id = id;
        this.name = name;
        this.calories = calories;
        this.protein = protein;
        this.fat = fat;
        this.carb = carb;
        this.fiber = fiber;
    }


    public String getId() { return id; }
    public String getName() { return name; }

    public float getCalories() { return calories; }
    public float getProtein() { return protein; }
    public float getFat() { return fat; }
    public float getCarb() { return carb; }
    public float getFiber() { return fiber; }


    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }

    public void setCalories(float calories) { this.calories = calories; }
    public void setProtein(float protein) { this.protein = protein; }
    public void setFat(float fat) { this.fat = fat; }
    public void setCarb(float carb) { this.carb = carb; }
    public void setFiber(float fiber) { this.fiber = fiber; }
}
