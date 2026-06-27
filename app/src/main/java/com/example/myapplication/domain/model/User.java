package com.example.myapplication.domain.model;

import java.util.Objects;

public class User {

    private final long id;
    private final String name;
    private final int age;
    private final float weight;

    public User(long id, String name, int age, float weight) {
        this.id = id;
        this.name = name == null ? "" : name;
        this.age = age;
        this.weight = weight;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public float getWeight() {
        return weight;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User)) {
            return false;
        }
        User that = (User) other;
        return id == that.id
                && age == that.age
                && Float.compare(that.weight, weight) == 0
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, age, weight);
    }

    @Override
    public String toString() {
        return "User{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", age=" + age
                + ", weight=" + weight
                + '}';
    }
}
