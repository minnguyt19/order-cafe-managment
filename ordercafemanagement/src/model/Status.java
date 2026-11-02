package model;

/**
 * Lists the stages of an order.
 * Only PENDING orders are editable; COMPLETED and CANCELED orders remain in history.
 */
public enum Status {
    PENDING, COMPLETED, CANCELED
}
