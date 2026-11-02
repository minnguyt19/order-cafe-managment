package service;

import model.Categrory;
import model.Item;
import model.Order;
import model.OrderDetail;
import model.Status;
import helper.OrderFileManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Holds the orders and fixed drinks catalog in memory.
 * Applies order rules independently of console input and delegates file access to OrderFileManager.
 */
public class CafeService {
    private final ArrayList<Item> listItems = new ArrayList<>();
    private ArrayList<Order> listOrders = new ArrayList<>();

    // True after a successful change; reset only after saving or loading succeeds.
    private boolean unsavedChanges;

    /**
     * Creates the fixed catalog once per service instance.
     * IDs use C/M/T/J/S/O for the category and a number starting at 001 within that category.
     */
    public CafeService() {
        // Coffee
        listItems.add(new Item("C001", "Black coffee", 25000, Categrory.COFFEE));
        listItems.add(new Item("C002", "Milk coffee", 30000, Categrory.COFFEE));
        listItems.add(new Item("C003", "Espresso", 30000, Categrory.COFFEE));
        listItems.add(new Item("C004", "Cappuccino", 40000, Categrory.COFFEE));
        listItems.add(new Item("C005", "Latte", 40000, Categrory.COFFEE));
        listItems.add(new Item("C006", "Macchiato", 35000, Categrory.COFFEE));

        // Milk tea
        listItems.add(new Item("M001", "Milk tea", 35000, Categrory.MILKTEA));

        // Tea
        listItems.add(new Item("T001", "Green tea", 30000, Categrory.TEA));
        listItems.add(new Item("T002", "Black tea", 30000, Categrory.TEA));
        listItems.add(new Item("T003", "Oolong tea", 35000, Categrory.TEA));
        listItems.add(new Item("T004", "White tea", 35000, Categrory.TEA));
        listItems.add(new Item("T005", "Chamomile tea", 30000, Categrory.TEA));
        listItems.add(new Item("T006", "Hibiscus tea", 30000, Categrory.TEA));
        listItems.add(new Item("T007", "Jasmine tea", 35000, Categrory.TEA));
        listItems.add(new Item("T008", "Peppermint tea", 30000, Categrory.TEA));

        // Juice
        listItems.add(new Item("J001", "Orange juice", 40000, Categrory.JUICE));
        listItems.add(new Item("J002", "Watermelon juice", 35000, Categrory.JUICE));
        listItems.add(new Item("J003", "Pineapple juice", 35000, Categrory.JUICE));
        listItems.add(new Item("J004", "Apple juice", 40000, Categrory.JUICE));

        // Soda
        listItems.add(new Item("S001", "Lemon soda", 30000, Categrory.SODA));
        listItems.add(new Item("S002", "Blueberry soda", 35000, Categrory.SODA));
        listItems.add(new Item("S003", "Strawberry soda", 35000, Categrory.SODA));

        // Others
        listItems.add(new Item("O001", "Hot chocolate", 40000, Categrory.OTHERS));
        listItems.add(new Item("O002", "Iced cocoa", 35000, Categrory.OTHERS));
        listItems.add(new Item("O003", "Fresh milk", 25000, Categrory.OTHERS));
        listItems.add(new Item("O004", "Aquaphina", 15000, Categrory.OTHERS));
    }

    /** Returns the current order list for displaying and searching. */
    public ArrayList<Order> getListOrders() {
        return listOrders;
    }

    /** Returns the fixed drinks catalog used when adding items to orders. */
    public ArrayList<Item> getListItems() {
        return listItems;
    }

    /** Indicates whether successful order changes have not yet been saved. */
    public boolean hasUnsavedChanges() {
        return unsavedChanges;
    }

    /** Finds an order without case sensitivity or surrounding spaces; returns null if not found. */
    public Order findOrderById(String id) {
        for (Order order : listOrders) {
            if (order.getId().equalsIgnoreCase(id.trim())) {
                return order;
            }
        }
        return null;
    }

    /** Finds a catalog item without case sensitivity or surrounding spaces; returns null if not found. */
    public Item findItemById(String id) {
        for (Item item : listItems) {
            if (item.getId().equalsIgnoreCase(id.trim())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Validates the employee name, generates an ID, and adds a new empty pending order.
     * Rejects separators and line breaks so the name can be stored safely in the text file.
     */
    public Order createOrder(String employeeName) {
        employeeName = employeeName.trim();
        if (employeeName.isEmpty() || employeeName.contains("|")
                || employeeName.contains("\n") || employeeName.contains("\r")) {
            throw new IllegalArgumentException("Employee name cannot be blank or contain | or line breaks.");
        }
        Order order = new Order(generateOrderId(), employeeName);
        listOrders.add(order);
        unsavedChanges = true;
        return order;
    }

    /**
     * Uses the highest existing order number plus one, including canceled orders.
     * Scanning the loaded orders prevents duplicate IDs after restarting the program.
     */
    private String generateOrderId() {
        int highestId = 0;
        for (Order order : listOrders) {
            // Skip the three letters ORD to read the numeric part of the ID.
            int number = Integer.parseInt(order.getId().substring(3));
            if (number > highestId) {
                highestId = number;
            }
        }
        if (highestId == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("No more order IDs are available.");
        }
        // %03d adds leading zeros, and Locale.ROOT keeps the ID format consistent across computers.
        return String.format(Locale.ROOT, "ORD%03d", highestId + 1);
    }

    /**
     * Adds a catalog drink to a pending order, or increases its existing quantity.
     * A new detail receives its own Item object to preserve the ordered name and price.
     */
    public void addItemToOrder(String orderId, String itemId, int quantity) {
        Order order = getPendingOrder(orderId);
        checkQuantity(quantity);
        Item item = findItemById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found in the drinks catalog.");
        }
        OrderDetail detail = findOrderDetail(order, itemId);
        if (detail == null) {
            // Keep the ordered price even if the catalog changes later.
            Item orderedItem = new Item(item.getId(), item.getName(), item.getPrice(), item.getCategrory());
            order.getListOrderDetails().add(new OrderDetail(orderedItem, quantity));
        } else {
            // Check before addition: an int that exceeds its limit can wrap to a negative value.
            if (quantity > Integer.MAX_VALUE - detail.getQuantity()) {
                throw new IllegalArgumentException("The combined quantity is too large.");
            }
            detail.setQuantity(detail.getQuantity() + quantity);
        }
        unsavedChanges = true;
    }

    /** Replaces the quantity of an existing detail; both the order and quantity must be valid. */
    public void updateItemQuantity(String orderId, String itemId, int quantity) {
        Order order = getPendingOrder(orderId);
        checkQuantity(quantity);
        OrderDetail detail = getOrderDetail(order, itemId);
        detail.setQuantity(quantity);
        unsavedChanges = true;
    }

    /** Removes a matching detail from a pending order; the order may become empty. */
    public void removeItemFromOrder(String orderId, String itemId) {
        Order order = getPendingOrder(orderId);
        OrderDetail detail = getOrderDetail(order, itemId);
        order.getListOrderDetails().remove(detail);
        unsavedChanges = true;
    }

    /**
     * Changes a pending order to COMPLETED or CANCELED; final orders cannot be changed again.
     * Completion requires at least one item and records the completion time.
     */
    public void updateOrderStatus(String orderId, Status status) {
        Order order = getPendingOrder(orderId);
        if (status == Status.COMPLETED) {
            if (order.getListOrderDetails().isEmpty()) {
                throw new IllegalArgumentException("An empty order cannot be completed.");
            }
            order.setCompletedAt(LocalDateTime.now());
        } else if (status != Status.CANCELED) {
            throw new IllegalArgumentException("Choose COMPLETED or CANCELED.");
        }
        order.setStatus(status);
        unsavedChanges = true;
    }

    /** Reuses the status-changing rules to cancel an order without deleting its history. */
    public void cancelOrder(String orderId) {
        updateOrderStatus(orderId, Status.CANCELED);
    }

    /** Returns an editable order, or throws a validation error if it is missing or no longer pending. */
    private Order getPendingOrder(String orderId) {
        Order order = findOrderById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found.");
        }
        if (order.getStatus() != Status.PENDING) {
            throw new IllegalArgumentException("Only PENDING orders can be changed.");
        }
        return order;
    }

    /** Finds an item inside one order, returning null if that item has not been added. */
    private OrderDetail findOrderDetail(Order order, String itemId) {
        for (OrderDetail detail : order.getListOrderDetails()) {
            if (detail.getItem().getId().equalsIgnoreCase(itemId.trim())) {
                return detail;
            }
        }
        return null;
    }

    /** Requires an existing detail and reports an error instead of returning null when it is missing. */
    private OrderDetail getOrderDetail(Order order, String itemId) {
        OrderDetail detail = findOrderDetail(order, itemId);
        if (detail == null) {
            throw new IllegalArgumentException("This item is not in the order.");
        }
        return detail;
    }

    /** Rejects zero and negative quantities; removing items is a separate operation. */
    private void checkQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
    }

    /**
     * Replaces the in-memory orders with the fully validated saved list.
     * If reading throws IOException, the existing orders and unsaved-change flag remain unchanged.
     */
    public void loadOrders() throws IOException {
        // Replace the current list only after the whole file has been read successfully.
        ArrayList<Order> loadedOrders = OrderFileManager.load();
        listOrders = loadedOrders;
        unsavedChanges = false;
    }

    /** Saves all orders and clears the unsaved-change flag only after writing succeeds. */
    public void saveOrders() throws IOException {
        OrderFileManager.save(listOrders);
        unsavedChanges = false;
    }
}
