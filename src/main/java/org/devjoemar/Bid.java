package org.devjoemar;

/**
 * Represents a bid in an auction.
 */
public class Bid {
    private final int userId;
    private final double amount;
    private final long timestamp;

    /**
     * Constructs a Bid instance.
     * @param userId The ID of the user who made the bid.
     * @param amount The amount of the bid.
     * @param timestamp The timestamp when the bid was placed.
     */
    public Bid(int userId, double amount, long timestamp) {
        this.userId = userId;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public int getUserId() { return userId; }
    public double getAmount() { return amount; }
    public long getTimestamp() { return timestamp; }
}
