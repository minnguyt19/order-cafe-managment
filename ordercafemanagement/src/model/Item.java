package model;

public class Item implements Identifiable {
    private String id;
    private String name;
    private double price;
    private Categrory categrory;

    public Item() {
    }

    public Item(String id, String name, double price, Categrory categrory) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.categrory = categrory;
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public String setId() {
        return "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Categrory getCategrory() {
        return categrory;
    }

    public void setCategrory(Categrory categrory) {
        this.categrory = categrory;
    }
}
