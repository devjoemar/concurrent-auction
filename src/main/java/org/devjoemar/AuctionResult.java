package org.devjoemar;

/**
 * Represents the result of an auction.
 */
public class AuctionResult {
    private final String item;
    private final long closeTime;
    private final int userId;
    private final String status;
    private final double pricePaid;
    private final int totalBidCount;
    private final double highestBid;
    private final double lowestBid;

    public AuctionResult(String item, long closeTime, int userId, String status,
                         double pricePaid, int totalBidCount, double highestBid, double lowestBid) {
        this.item = item;
        this.closeTime = closeTime;
        this.userId = userId;
        this.status = status;
        this.pricePaid = pricePaid;
        this.totalBidCount = totalBidCount;
        this.highestBid = highestBid;
        this.lowestBid = lowestBid;
    }

    public String getItem() {
        return item;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public int getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }

    public double getPricePaid() {
        return pricePaid;
    }

    public int getTotalBidCount() {
        return totalBidCount;
    }

    public double getHighestBid() {
        return highestBid;
    }

    public double getLowestBid() {
        return lowestBid;
    }
}
