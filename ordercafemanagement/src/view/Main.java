/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view;

import model.Order;

import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author Microsucks
 */
public class Main {

    static ArrayList<Order> listOrders = new ArrayList<>();
    static boolean isRunning = true;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        while (isRunning) {
            printMenu();
            int option = scanner.nextInt();
            scanner.nextLine();
            chooseOption(option);
        }

    }

    public static void chooseOption(int option) {
        switch (option) {
            case 1:
                // Handle create new order
                break;
            case 2:
                // Handle add item to order
                break;
            case 3:
                // Handle update item quantity
                break;
            case 4:
                // Handle remove item from order
                break;
            case 5:
                // Handle view order details
                break;
            case 6:
                // Handle search order by ID
                break;
            case 7:
                // Handle list all orders
                break;
            case 8:
                // Handle update order status
                break;
            case 9:
                // Handle cancel order
                break;
            case 0:
                System.out.println("Exiting the application. Goodbye!");
                isRunning = false;
            default:
                System.out.println("Invalid option. Please try again.");
        }
    }

    public static void printMenu() {
        System.out.print("""
                \n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n
                🌹========== ORDER MANAGEMENT ==========🌹
                        1. Create New Order
                        2. Add Item to Order
                        3. Update Item Quantity
                        4. Remove Item from Order
                        5. View Order Details
                        6. Search Order by ID
                        7. List All Orders
                        8. Update Order Status
                        9. Cancel Order
                        0. Exit
                🌹======================================🌹
                Choose an option (0-9):
                """
        );
    }

}
