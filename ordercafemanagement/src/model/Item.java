package model;

/**
 * Holds a drink's ID, name, unit price, and category.
 * Used for both catalog drinks and separate copies stored inside orders.
 */
public class Item extends BaseEntity {
    private String name;
    private double price;
    private Categrory categrory;

    /** Creates a drink with a unit price in VND and passes its ID to BaseEntity. */
    public Item(String id, String name, double price, Categrory categrory) {
        super(id);
        this.name = name;
        this.price = price;
        this.categrory = categrory;
    }

    /** Returns the drink's name. */
    public String getName() {
        return name;
    }

    /** Returns the price of one drink in VND. */
    public double getPrice() {
        return price;
    }

    /** Returns the drink category, such as COFFEE or TEA. */
    public Categrory getCategrory() {
        return categrory;
    }

    /** Replaces this item's name. */
    public void setName(String name) {
        this.name = name;
    }

    /** Replaces this item's unit price; separate item copies in existing orders are unaffected. */
    public void setPrice(double price) {
        this.price = price;
    }

    /** Replaces this item's category. */
    public void setCategrory(Categrory categrory) {
        this.categrory = categrory;
    }

    /** Uses the drink name as the readable name required by BaseEntity. */
    @Override
    public String getDisplayName() {
        return name;
    }
}
