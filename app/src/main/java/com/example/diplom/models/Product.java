package com.example.diplom.models;


public class Product {
    private String name;
    private double protein;
    private double fat;
    private double carbs;
    private double fiber;
    private double calories;

    public Product() {}

    public Product(String name, double protein, double fat, double carbs, double fiber, double calories) {
        this.name = name;
        this.protein = protein;
        this.fat = fat;
        this.carbs = carbs;
        this.fiber = fiber;
        this.calories = calories;
    }

    public String getName() { return name; }
    public double getProtein() { return protein; }
    public double getFat() { return fat; }
    public double getCarbs() { return carbs; }
    public double getFiber() { return fiber; }
    public double getCalories() { return calories; }

    public void setName(String name) { this.name = name; }
    public void setProtein(double protein) { this.protein = protein; }
    public void setFat(double fat) { this.fat = fat; }
    public void setCarbs(double carbs) { this.carbs = carbs; }
    public void setFiber(double fiber) { this.fiber = fiber; }
    public void setCalories(double calories) { this.calories = calories; }
}
