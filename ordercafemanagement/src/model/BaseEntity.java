package model;

/**
 * Shares an ID between Item and Order so each class does not need its own ID field.
 * This abstract class is extended by concrete models rather than created directly.
 */
public abstract class BaseEntity implements Identifiable {
    // final keeps an entity's ID unchanged after construction.
    private final String id;

    /** Stores the ID supplied by a subclass constructor through super(id). */
    protected BaseEntity(String id) {
        this.id = id;
    }

    /** Returns the entity's permanent ID, as required by the Identifiable interface. */
    public String getId() {
        return id;
    }

    /** Requires each subclass to provide its own readable name. */
    public abstract String getDisplayName();

}
