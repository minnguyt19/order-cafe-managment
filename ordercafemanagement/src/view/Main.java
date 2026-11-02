package view;

import controller.CafeController;

/**
 * Entry point that connects the application to its console controller.
 *
 * @author Microsucks
 */
public class Main {
    /**
     * Creates the controller and starts its startup-loading and menu loop.
     *
     * @param args command-line arguments (unused by this console program)
     */
    public static void main(String[] args) {
        CafeController controller = new CafeController();
        controller.run();
    }
}
