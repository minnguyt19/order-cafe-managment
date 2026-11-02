package model;

/** Defines the ID getter that every identifiable model must provide. */
public interface Identifiable {
    /** Returns the ID used to recognize the object, such as C001 or ORD001. */
    String getId();
}
