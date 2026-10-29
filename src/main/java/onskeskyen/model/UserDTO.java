package onskeskyen.model;

import java.util.List;

public class UserDTO {

    private int userID;
    private String email;
    private int wishID;
    private String name;
    private float price;
    private List<String> tags;

    public UserDTO(int userID, String email, int wishID, String name, float price, List<String> tags) {
        this.userID = userID;
        this.email = email;
        this.wishID = wishID;
        this.name = name;
        this.price = price;
        this.tags = tags;
    }

    public UserDTO() {

    }

    public int getUserID() {
        return userID;
    }

    public String getEmail() {
        return email;
    }

    public int getWishID() {
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

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setWishID(int wishID) {
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
