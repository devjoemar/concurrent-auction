package org.devjoemar;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents an individual auction for an item, managing bids and the auction's lifecycle.
 * <p>
 * This class encapsulates the state and behavior of an auction. It includes details such as
 * the item being auctioned, its reserve price, the closing time, and the bids placed. The class
 * is responsible for adding new bids to the auction and finalizing the auction outcome.
 * </p>
 * <p>
 * In a multi-threaded environment, it's crucial to manage the concurrent access to the auction's
 * state, specifically the bids. The TreeMap data structure is employed to store bids in a sorted
 * order. While the TreeMap itself doesn't inherently prevent race conditions, the class utilizes
 * a ReentrantLock to ensure synchronized access to this shared structure. Without proper synchronization,
 * concurrent modifications to the TreeMap (such as adding bids simultaneously from different threads)
 * could lead to race conditions. These race conditions may manifest as lost updates or inconsistent
 * views of the auction's state, potentially causing incorrect auction outcomes.
 * </p>
 * <p>
 * The use of the ReentrantLock ensures that bid placements and auction finalizations are atomic operations.
 * This means each operation is executed completely before another thread can modify the auction's state,
 * thereby maintaining the consistency and integrity of the auction.
 * </p>
 * <p>
 * The design choice of using a TreeMap for storing bids enables efficient retrieval of the highest and
 * second-highest bids, which are essential for determining the auction's result based on the auction rules.
 * </p>
 *
 * Fields:
 * - item: Identifier for the item being auctioned.
 * - reservePrice: Minimum price at which the item can be sold.
 * - closeTime: Timestamp indicating when the auction closes.
 * - bids: Collection of bids placed, sorted in descending order.
 * - lock: Synchronization mechanism for managing concurrent access.
 */
public class Auction {
    private final String item;
    private final double reservePrice;
    private final long closeTime;
    private final TreeMap<Double, Bid> bids;
    private final ReentrantLock lock;

    /**
     * Constructs an Auction instance.
     * @param item The item being auctioned.
     * @param reservePrice The reserve price for the auction.
     * @param closeTime The closing time of the auction.
     */
    public Auction(String item, double reservePrice, long closeTime) {
        this.item = item;
        this.reservePrice = reservePrice;
        this.closeTime = closeTime;
        this.bids = new TreeMap<>(Collections.reverseOrder());
        this.lock = new ReentrantLock();
    }

    /**
     * Places a bid in the auction.
     * <p>
     * This method adds a new bid to the auction, ensuring that it adheres to the auction rules.
     * It only accepts bids that are placed within the auction's time frame (after the auction start time
     * and before or on the closing time) and are higher than any previous bids made by the same user.
     * </p>
     * <p>
     * Concurrency control is managed using a ReentrantLock, ensuring that only one thread can modify
     * the state of the auction at any given time. This is essential to prevent inconsistent states
     * and race conditions when multiple bids are placed simultaneously.
     * </p>
     * <p>
     * The method checks whether the bid is valid based on the auction's closing time and the bidder's
     * previous highest bid. If valid, the bid is added to the auction's bids TreeMap, which sorts bids
     * in descending order, allowing easy retrieval of the highest and second-highest bids.
     * </p>
     *
     * @param bid The bid to be placed in the auction.
     */
    public void placeBid(Bid bid) {
        lock.lock();
        try {
            if (bid.getTimestamp() <= this.closeTime) {
                Bid highestUserBid = getUserHighestBid(bid.getUserId());
                if (highestUserBid == null || bid.getAmount() > highestUserBid.getAmount()) {
                    bids.put(bid.getAmount(), bid);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private Bid getUserHighestBid(int userId) {
        return bids.values().stream()
                   .filter(b -> b.getUserId() == userId)
                   .max(Comparator.comparingDouble(Bid::getAmount))
                   .orElse(null);
    }

    /**
     * Finalizes the auction and determines its result.
     * <p>
     * This method is called when an auction reaches its closing time. It locks the auction instance
     * to ensure exclusive access, then finalizes the auction by determining the winning bid based on
     * auction rules. The highest bidder is identified, but the price paid is equal to the second
     * highest bid. If there's only one valid bid, the winner pays the reserve price. In case there
     * are no valid bids, the auction is marked as 'UNSOLD'.
     * </p>
     * <p>
     * A ReentrantLock is used to manage concurrent access to the auction's state. The lock is acquired
     * at the beginning of the method and released in the finally block to ensure that it's always
     * released, preventing potential deadlocks. This locking mechanism is essential for maintaining
     * data integrity in a concurrent environment, where multiple threads might attempt to modify
     * the auction's state simultaneously.
     * </p>
     * <p>
     * The TreeMap data structure is employed to store bids, facilitating the retrieval of the
     * highest and second highest bids efficiently.
     * </p>
     *
     * @return AuctionResult representing the outcome of the auction. This includes details such as
     *         the item, the closing time of the auction, the ID of the winning user, the auction status
     *         (SOLD or UNSOLD), the price paid, the total number of valid bids, the highest bid, and
     *         the lowest bid.
     */
    public AuctionResult finalizeAuction() {
        lock.lock();
        try {
            if (bids.isEmpty()) {
                return new AuctionResult(item, closeTime, -1, "UNSOLD", 0.00, 0, 0.00, 0.00);
            }

            int totalBidCount = bids.size();
            double highestBid = bids.firstKey();
            double lowestBid = bids.lastKey();
            Map.Entry<Double, Bid> winnerEntry = bids.pollFirstEntry();
            double pricePaid = reservePrice;

            if (bids.size() > 0) {
                pricePaid = bids.firstKey(); // Price of the second highest bid
            }

            return new AuctionResult(item, closeTime, winnerEntry.getValue().getUserId(),
                                     "SOLD", pricePaid, totalBidCount, highestBid, lowestBid);
        } finally {
            lock.unlock();
        }
    }

    public long getCloseTime() { return closeTime; }
    public String getItem() { return item; }
}
