package helper;

import model.Categrory;
import model.Item;
import model.Order;
import model.OrderDetail;
import model.Status;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Saves and loads orders using one UTF-8 text file.
 * Each ORDER line is followed by the DETAIL lines belonging to that order.
 */
public class OrderFileManager {
    public static final String FILE_PATH = "data/orders.txt";
    private static final Path ORDER_FILE = Path.of(FILE_PATH);

    /**
     * Reads and validates the entire file before returning the loaded orders.
     * The caller's existing order list is not changed if reading fails.
     *
     * @return a new list containing all saved orders and their items
     * @throws IOException if the file cannot be read or contains invalid data
     */
    public static ArrayList<Order> load() throws IOException {
        ArrayList<Order> orders = new ArrayList<>();
        // DETAIL lines belong to the most recently read ORDER line.
        Order currentOrder = null;
        int orderLineNumber = 0;

        try (BufferedReader reader = Files.newBufferedReader(ORDER_FILE, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                // Escape | because split uses a regular expression; -1 keeps empty fields.
                String[] fields = line.split("\\|", -1);
                try {
                    if (fields.length != 6) {
                        throw new IllegalArgumentException("Expected 6 fields separated by |.");
                    }
                    if (fields[0].equals("ORDER")) {
                        // All details for the previous order have now been read.
                        checkFinishedOrder(currentOrder, orderLineNumber);
                        currentOrder = readOrder(fields, orders);
                        orders.add(currentOrder);
                        orderLineNumber = lineNumber;
                    } else if (fields[0].equals("DETAIL")) {
                        if (currentOrder == null) {
                            throw new IllegalArgumentException("DETAIL must follow an ORDER line.");
                        }
                        readDetail(fields, currentOrder);
                    } else {
                        throw new IllegalArgumentException("Expected ORDER or DETAIL.");
                    }
                } catch (IllegalArgumentException | DateTimeException e) {
                    throw new IOException("Invalid data at line " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        }
        // The final order has no following ORDER line to trigger its validation.
        checkFinishedOrder(currentOrder, orderLineNumber);
        return orders;
    }

    /**
     * Converts an ORDER line into an order with an empty detail list.
     * Format: ORDER|id|createdAt|completedAt|status|employeeName.
     * Checks the ID, duplicate orders, dates, status, and employee name.
     */
    private static Order readOrder(String[] fields, ArrayList<Order> orders) {
        String id = fields[1];
        if (!id.matches("ORD[0-9]+")) {
            throw new IllegalArgumentException("Invalid order ID.");
        }
        int number = Integer.parseInt(id.substring(3));
        // %03d pads small numbers with zeros: 1 becomes 001, while 1000 stays 1000.
        if (number <= 0 || !id.equals(String.format(Locale.ROOT, "ORD%03d", number))) {
            throw new IllegalArgumentException("Order ID must use the form ORD001, ORD002, etc.");
        }
        for (Order order : orders) {
            if (order.getId().equals(id)) {
                throw new IllegalArgumentException("Duplicate order ID: " + id);
            }
        }
        LocalDateTime createdAt = LocalDateTime.parse(fields[2]);
        // An empty completion-time field represents an order that was not completed.
        LocalDateTime completedAt = null;
        if (!fields[3].isEmpty()) {
            completedAt = LocalDateTime.parse(fields[3]);
        }
        Status status = Status.valueOf(fields[4]);
        String employeeName = readText(fields[5], "Employee name");
        if (status == Status.COMPLETED) {
            if (completedAt == null || completedAt.isBefore(createdAt)) {
                throw new IllegalArgumentException("A completed order needs a valid completion time.");
            }
        } else if (completedAt != null) {
            throw new IllegalArgumentException("Only completed orders can have a completion time.");
        }
        return new Order(id, new ArrayList<>(), createdAt, completedAt, status, employeeName);
    }

    /**
     * Converts a DETAIL line into an item and quantity, then adds it to the order.
     * Format: DETAIL|itemId|itemName|unitPrice|category|quantity.
     * Rejects duplicate items and invalid prices or quantities.
     */
    private static void readDetail(String[] fields, Order order) {
        String itemId = fields[1];
        // Keep accepting old I-prefixed IDs so previously saved orders can still load.
        if (!itemId.matches("[CMTJSOI][0-9]+")) {
            throw new IllegalArgumentException("Invalid item ID.");
        }
        for (OrderDetail detail : order.getListOrderDetails()) {
            if (detail.getItem().getId().equals(itemId)) {
                throw new IllegalArgumentException("Duplicate item in this order: " + itemId);
            }
        }
        String itemName = readText(fields[2], "Item name");
        double price = Double.parseDouble(fields[3]);
        Categrory category = Categrory.valueOf(fields[4]);
        int quantity = Integer.parseInt(fields[5]);
        if (!Double.isFinite(price) || price <= 0 || quantity <= 0
                || !Double.isFinite(price * quantity)) {
            throw new IllegalArgumentException("Price and quantity must be positive, finite numbers.");
        }
        // Use the saved item information to preserve the original name and price.
        Item item = new Item(itemId, itemName, price, category);
        order.getListOrderDetails().add(new OrderDetail(item, quantity));
    }

    /**
     * Checks a required text field and removes surrounding spaces.
     * Separators and line breaks are rejected because they would change the file structure.
     * fieldName identifies the invalid field in the error message.
     */
    private static String readText(String text, String fieldName) {
        if (text.isBlank() || text.contains("|") || text.contains("\r") || text.contains("\n")) {
            throw new IllegalArgumentException(fieldName + " cannot be blank or contain | or line breaks.");
        }
        return text.trim();
    }

    /**
     * Checks rules that need all of an order's DETAIL lines to have been read.
     * "Finished" refers to reading the order, not its PENDING/COMPLETED/CANCELED status.
     * lineNumber points to the order's header so errors identify the affected order.
     */
    private static void checkFinishedOrder(Order order, int lineNumber) throws IOException {
        if (order == null) {
            return;
        }
        if (order.getStatus() == Status.COMPLETED && order.getListOrderDetails().isEmpty()) {
            throw new IOException("Invalid data at line " + lineNumber + ": A completed order cannot be empty.");
        }
        if (!Double.isFinite(order.getTotal())) {
            throw new IOException("Invalid data at line " + lineNumber + ": Order total is too large.");
        }
    }

    /**
     * Saves the complete order list, replacing the previous file rather than appending.
     * Writes to a temporary file first so a failed write leaves the saved file unchanged.
     *
     * @param orders all orders to save, including completed and canceled orders
     * @throws IOException if the directory, temporary file, or saved file cannot be written
     */
    public static void save(ArrayList<Order> orders) throws IOException {
        Files.createDirectories(ORDER_FILE.getParent());
        Path temporaryFile = Files.createTempFile(ORDER_FILE.getParent(), "orders-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                for (Order order : orders) {
                    writeOrder(writer, order);
                }
            }
            // Do not replace the saved file until writing the complete snapshot succeeds.
            Files.move(temporaryFile, ORDER_FILE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            // Remove a leftover temporary file if writing or replacing the saved file failed.
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * Writes one ORDER header followed by a DETAIL line for each item.
     * A missing completion time is saved as an empty field.
     * Totals are not stored because they can be calculated from prices and quantities.
     */
    private static void writeOrder(BufferedWriter writer, Order order) throws IOException {
        String completedAt = "";
        if (order.getCompletedAt() != null) {
            completedAt = order.getCompletedAt().toString();
        }
        writer.write("ORDER|" + order.getId() + "|" + order.getCreatedAt() + "|"
                + completedAt + "|" + order.getStatus() + "|" + order.getEmployeeName());
        writer.newLine();
        for (OrderDetail detail : order.getListOrderDetails()) {
            Item item = detail.getItem();
            writer.write("DETAIL|" + item.getId() + "|" + item.getName() + "|"
                    + item.getPrice() + "|" + item.getCategrory() + "|" + detail.getQuantity());
            writer.newLine();
        }
    }
}
