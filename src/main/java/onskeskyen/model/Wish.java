package onskeskyen.model;

import java.util.List;

public class Wish {
    private int wishID;
    private String name;
    private float price;
    private List<String> tags;

    public Wish(int wishID, String name, float price, List<String> tags) {
        this.wishID = wishID;
        this.name = name;
        this.price = price;
        this.tags = tags;
    }

    public int getID() {
        return wishID;
    }

    public String getName() {
        return name;
    }

    public float getPrice() {
        return price;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setID(int wishID) {
        this.wishID = wishID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

}
