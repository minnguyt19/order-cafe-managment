package controller;

import model.Item;
import model.Order;
import model.OrderDetail;
import model.Status;
import service.CafeService;
import helper.OrderFileManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Handles the console menu, reads user input, and displays orders.
 * Calls CafeService to change orders and apply the program's rules.
 */
public class CafeController {
    private final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    private final CafeService service = new CafeService();
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private boolean isRunning = true;

    /**
     * Loads saved orders once, then repeats the menu until the user exits.
     * A missing file starts a new session; an invalid existing file stops startup.
     */
    public void run() {
        try {
            service.loadOrders();
            System.out.println("Loaded " + service.getListOrders().size() + " order(s).");
        } catch (NoSuchFileException e) {
            System.out.println("No saved orders yet. Starting with an empty order list.");
        } catch (IOException e) {
            System.out.println("Could not read " + OrderFileManager.FILE_PATH + ": " + e.getMessage());
            System.out.println("Please fix the file or its permissions and restart the program.");
            return;
        }

        try {
            while (isRunning) {
                printMenu();
                choose(readInt("Choose an option (0-11): ", 0, 11));
            }
        } catch (NoSuchElementException e) {
            // Redirected input can end without an explicit Exit choice.
            System.out.println("\nInput ended.");
            if (service.hasUnsavedChanges()) {
                saveOrders();
            }
        }
    }

    /**
     * Runs the action matching a menu number.
     * Displays validation errors from the service so the user can continue using the menu.
     */
    public void choose(int option) {
        try {
            switch (option) {
                case 1:
                    createOrder();
                    break;
                case 2:
                    addItemToOrder();
                    break;
                case 3:
                    updateItemQuantity();
                    break;
                case 4:
                    removeItemFromOrder();
                    break;
                case 5:
                    viewOrderDetails();
                    break;
                case 6:
                    searchOrderById();
                    break;
                case 7:
                    printOrderSummaries();
                    break;
                case 8:
                    updateOrderStatus();
                    break;
                case 9:
                    cancelOrder();
                    break;
                case 10:
                    readOrders();
                    break;
                case 11:
                    saveOrders();
                    break;
                case 0:
                    exit();
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    /** Asks for an employee name and creates an empty pending order. */
    private void createOrder() {
        String employeeName = readNonEmptyText("Employee name: ");
        Order order = service.createOrder(employeeName);
        System.out.println("Created order " + order.getId() + ". Use option 2 to add drinks.");
    }

    /** Shows the catalog and adds the selected drink and quantity to a pending order. */
    private void addItemToOrder() {
        Order order = readPendingOrder();
        if (order == null) {
            return;
        }
        printCatalog();
        String itemId = readNonEmptyText("Item ID: ");
        int quantity = readInt("Quantity: ", 1, Integer.MAX_VALUE);
        service.addItemToOrder(order.getId(), itemId, quantity);
        System.out.println("Item added.");
        printOrder(order);
    }

    /** Replaces an item's quantity with the entered value, then displays the updated receipt. */
    private void updateItemQuantity() {
        Order order = readPendingOrder();
        if (order == null) {
            return;
        }
        printOrder(order);
        if (order.getListOrderDetails().isEmpty()) {
            return;
        }
        String itemId = readNonEmptyText("Item ID to update: ");
        int quantity = readInt("New quantity: ", 1, Integer.MAX_VALUE);
        service.updateItemQuantity(order.getId(), itemId, quantity);
        System.out.println("Quantity updated.");
        printOrder(order);
    }

    /** Removes the selected item's entire row from a pending order. */
    private void removeItemFromOrder() {
        Order order = readPendingOrder();
        if (order == null) {
            return;
        }
        printOrder(order);
        if (order.getListOrderDetails().isEmpty()) {
            return;
        }
        String itemId = readNonEmptyText("Item ID to remove: ");
        service.removeItemFromOrder(order.getId(), itemId);
        System.out.println("Item removed.");
        printOrder(order);
    }

    /** Lists order summaries first so the user can choose an order to view in full. */
    private void viewOrderDetails() {
        printOrderSummaries();
        if (!service.getListOrders().isEmpty()) {
            searchOrderById();
        }
    }

    /** Asks for an order ID directly and prints its receipt when the order exists. */
    private void searchOrderById() {
        Order order = readOrder();
        if (order != null) {
            printOrder(order);
        }
    }

    /**
     * Asks for an ID and finds the matching order.
     * Returns null after showing a message if the ID does not exist.
     */
    private Order readOrder() {
        String orderId = readNonEmptyText("Order ID: ");
        Order order = service.findOrderById(orderId);
        if (order == null) {
            System.out.println("Order not found.");
        }
        return order;
    }

    /** Returns the selected order only if it exists and is still editable; otherwise returns null. */
    private Order readPendingOrder() {
        Order order = readOrder();
        if (order != null && order.getStatus() != Status.PENDING) {
            System.out.println("Only PENDING orders can be changed.");
            return null;
        }
        return order;
    }

    /** Shows the submenu for completing or canceling a pending order, or going back. */
    private void updateOrderStatus() {
        Order order = readPendingOrder();
        if (order == null) {
            return;
        }
        System.out.println("1. Complete order\n2. Cancel order\n0. Back");
        int choice = readInt("Choose status (0-2): ", 0, 2);
        if (choice == 1) {
            service.updateOrderStatus(order.getId(), Status.COMPLETED);
        } else if (choice == 2) {
            service.cancelOrder(order.getId());
        } else {
            return;
        }
        System.out.println("Order " + order.getId() + " is now " + order.getStatus() + ".");
    }

    /** Provides the main menu's shortcut for canceling a pending order. */
    private void cancelOrder() {
        Order order = readPendingOrder();
        if (order != null) {
            service.cancelOrder(order.getId());
            System.out.println("Order " + order.getId() + " is now CANCELED.");
        }
    }

    /** Displays available drinks with their IDs, categories, and unit prices in VND. */
    private void printCatalog() {
        System.out.println("\nDRINKS CATALOG");
        // Negative widths left-align text; commas group price digits and .2 shows two decimal places.
        System.out.printf("%-7s %-22s %-10s %15s%n", "ID", "Name", "Category", "Price (VND)");
        for (Item item : service.getListItems()) {
            System.out.printf(Locale.US, "%-7s %-22s %-10s %,15.2f%n",
                    item.getId(), item.getName(), item.getCategrory(), item.getPrice());
        }
    }

    /** Prints one summary row per order, or an empty-list message when no orders exist. */
    private void printOrderSummaries() {
        if (service.getListOrders().isEmpty()) {
            System.out.println("No orders yet.");
            return;
        }
        System.out.printf("%n%-10s %-20s %-10s %-19s %15s%n",
                "Order ID", "Employee", "Status", "Created at", "Total (VND)");
        for (Order order : service.getListOrders()) {
            System.out.printf(Locale.US, "%-10s %-20s %-10s %-19s %,15.2f%n",
                    order.getId(), order.getEmployeeName(), order.getStatus(),
                    formatDate(order.getCreatedAt()), order.getTotal());
        }
    }

    /** Displays order information, item subtotals, and the calculated total as a receipt. */
    private void printOrder(Order order) {
        System.out.println("\n========== ORDER " + order.getId() + " ==========");
        System.out.println("Employee: " + order.getEmployeeName());
        System.out.println("Status: " + order.getStatus());
        System.out.println("Created at: " + formatDate(order.getCreatedAt()));
        System.out.println("Completed at: " + formatDate(order.getCompletedAt()));
        if (order.getListOrderDetails().isEmpty()) {
            System.out.println("This order has no items.");
        } else {
            System.out.printf("%-7s %-22s %15s %10s %18s%n",
                    "Item ID", "Name", "Price (VND)", "Quantity", "Subtotal (VND)");
            for (OrderDetail detail : order.getListOrderDetails()) {
                Item item = detail.getItem();
                System.out.printf(Locale.US, "%-7s %-22s %,15.2f %10d %,18.2f%n",
                        item.getId(), item.getName(), item.getPrice(),
                        detail.getQuantity(), detail.getSubTotal());
            }
        }
        System.out.printf(Locale.US, "Total: %,.2f VND%n", order.getTotal());
    }

    /** Formats a date as yyyy-MM-dd HH:mm:ss; a missing completion time is displayed as a dash. */
    private String formatDate(LocalDateTime date) {
        if (date == null) {
            return "-";
        }
        return date.format(dateFormat);
    }

    /**
     * Reloads orders from the file after asking before discarding unsaved changes.
     * If reading fails, reports the error and keeps the current orders.
     */
    private void readOrders() {
        if (service.hasUnsavedChanges()
                && !confirm("Discard unsaved changes and read orders from file? (y/n): ")) {
            System.out.println("Read canceled. Your current orders are unchanged.");
            return;
        }
        try {
            service.loadOrders();
            System.out.println("Loaded " + service.getListOrders().size() + " order(s).");
        } catch (IOException e) {
            System.out.println("Could not read orders: " + e.getMessage());
            System.out.println("Your current orders are unchanged.");
        }
    }

    /** Attempts to save all orders and returns true on success, or displays the error and returns false. */
    private boolean saveOrders() {
        try {
            service.saveOrders();
            System.out.println("Orders saved to " + OrderFileManager.FILE_PATH + ".");
            return true;
        } catch (IOException e) {
            System.out.println("Could not save orders: " + e.getMessage());
            return false;
        }
    }

    /**
     * Saves outstanding changes before stopping the menu loop.
     * If saving fails, the user can stay in the menu or explicitly exit without saving.
     */
    private void exit() {
        if (service.hasUnsavedChanges() && !saveOrders()
                && !confirm("Exit without saving? (y/n): ")) {
            return;
        }
        isRunning = false;
        System.out.println("Exiting the application. Goodbye!");
    }

    /**
     * Reads a whole line and retries until it contains an integer within the inclusive bounds.
     * Using nextLine for every prompt consumes Enter before the next question is asked.
     */
    private int readInt(String prompt, int minimum, int maximum) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int number = Integer.parseInt(input);
                if (number >= minimum && number <= maximum) {
                    return number;
                }
            } catch (NumberFormatException e) {
                // Print the same friendly message for non-numbers and out-of-range values.
            }
            System.out.println("Please enter a whole number from " + minimum + " to " + maximum + ".");
        }
    }

    /** Reads trimmed, non-empty text without the | separator used by the data file. */
    private String readNonEmptyText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String text = scanner.nextLine().trim();
            if (!text.isEmpty() && !text.contains("|")) {
                return text;
            }
            System.out.println("Please enter non-empty text without |.");
        }
    }

    /** Returns true for y/yes and false for n/no, asking again for any other answer. */
    private boolean confirm(String prompt) {
        while (true) {
            System.out.print(prompt);
            String answer = scanner.nextLine().trim();
            if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
                return true;
            }
            if (answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase("no")) {
                return false;
            }
            System.out.println("Please enter y or n.");
        }
    }

    /** Displays all main menu options; run() asks for the choice separately. */
    public static void printMenu() {
        System.out.println("""

                ========== ORDER MANAGEMENT ==========
                1. Create New Order
                2. Add Item to Order
                3. Update Item Quantity
                4. Remove Item from Order
                5. View Order Details
                6. Search Order by ID
                7. List All Orders
                8. Update Order Status
                9. Cancel Order
                10. Read orders from file
                11. Save orders to file
                0. Exit
                ======================================
                """);
    }
}
