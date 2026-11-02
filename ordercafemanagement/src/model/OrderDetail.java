package model;

/** Represents one row in an order: a drink, its quantity, and its calculated subtotal. */
public class OrderDetail {
    private Item item;
    private int quantity;

    /** Stores the ordered item and quantity; CafeService validates quantities before creating details. */
    public OrderDetail(Item item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    /** Returns the item containing the name and unit price recorded for this order. */
    public Item getItem() {
        return item;
    }

    /** Replaces the item referenced by this detail. */
    public void setItem(Item item) {
        this.item = item;
    }

    /** Returns how many units of this drink were ordered. */
    public int getQuantity() {
        return quantity;
    }

    /** Replaces the quantity after the service validates it. */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /** Calculates this row's subtotal as the recorded unit price multiplied by the quantity. */
    public double getSubTotal() {
        return this.item.getPrice() * this.quantity;
    }

}
