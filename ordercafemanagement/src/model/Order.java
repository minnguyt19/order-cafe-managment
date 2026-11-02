package model;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Represents one order with its employee, item details, status, and timestamps.
 * CafeService applies the editing rules; this model stores data and calculates the total.
 */
public class Order extends BaseEntity {
    private ArrayList<OrderDetail> listOrderDetails;
    private LocalDateTime createdAt;
    // null means the order has not been completed, including when it is canceled.
    private LocalDateTime completedAt;
    private Status status = Status.PENDING;
    private String employeeName;

    /** Creates a new pending order with no items, the current creation time, and no completion time. */
    public Order(String id, String employeeName) {
        // Reuse the full constructor with the default values for a new order.
        this(id, new ArrayList<>(), LocalDateTime.now(), null, Status.PENDING, employeeName);
    }

    /** Restores all order fields, for example when loading a saved order from the text file. */
    public Order(String id, ArrayList<OrderDetail> listOrderDetails, LocalDateTime createdAt, LocalDateTime completedAt, Status status, String employeeName) {
        super(id);
        this.listOrderDetails = listOrderDetails;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.status = status;
        this.employeeName = employeeName;
    }

    /** Returns the order's detail list, with one row for each ordered item ID. */
    public ArrayList<OrderDetail> getListOrderDetails() {
        return listOrderDetails;
    }

    /** Replaces the entire detail list. */
    public void setListOrderDetails(ArrayList<OrderDetail> listOrderDetails) {
        this.listOrderDetails = listOrderDetails;
    }

    /** Returns when the order was created. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Replaces the stored creation time. */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** Returns the completion time, or null if the order has not been completed. */
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    /** Stores the completion time when the service completes an order. */
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    /** Returns the current order status. */
    public Status getStatus() {
        return status;
    }

    /** Stores a new status; CafeService checks whether the transition is allowed before calling this. */
    public void setStatus(Status status) {
        this.status = status;
    }

    /** Returns the employee name recorded for this order. */
    public String getEmployeeName() {
        return employeeName;
    }

    /** Replaces the employee name stored in the order. */
    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    /**
     * Adds the subtotal of every detail to calculate the current order total.
     * Calculating it on demand keeps the total consistent after quantities or items change.
     */
    public double getTotal() {
        double total = 0;
        for (OrderDetail detail : listOrderDetails) {
            total += detail.getSubTotal();
        }
        return total;
    }

    /** Combines the order ID and employee name into a readable label. */
    @Override
    public String getDisplayName() {
        return getId() + " - " + employeeName;
    }
}
