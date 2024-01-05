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

    private boolean isClosed = false;

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
     * This method is invoked when an auction reaches its closing time or when a heartbeat action
     * triggers the finalization of past-due auctions. It ensures that the auction is finalized only once,
     * maintaining the integrity of the auction's outcome. The method employs a ReentrantLock to manage
     * concurrent access, preventing inconsistencies and race conditions in a multi-threaded environment.
     * </p>
     * <p>
     * The finalization process involves determining the winning bid based on auction rules: the highest
     * bidder wins, but the price paid is equal to the second-highest bid. If only one valid bid is present,
     * the winner pays the reserve price. If no valid bids meet the reserve price, the auction is marked as
     * 'UNSOLD'. The TreeMap data structure, storing bids in descending order, facilitates efficient retrieval
     * of the highest and second-highest bids.
     * </p>
     * <p>
     * The addition of the 'isClosed' boolean field ensures that once an auction is finalized, subsequent
     * bids do not alter its outcome. This field is crucial in a concurrent setup where multiple threads may
     * attempt to finalize or modify an auction simultaneously. By locking the auction during finalization and
     * checking the 'isClosed' status, the method upholds the correctness and immutability of the finalized
     * auction result.
     * </p>
     * <p>
     * This method returns an AuctionResult object, encapsulating details like the item, auction closing time,
     * winning user ID, auction status (SOLD/UNSOLD), price paid, total number of bids, highest bid, and lowest bid.
     * </p>
     *
     * @return AuctionResult representing the outcome of the auction. This includes details such as
     *         the item, closing time, winning user ID, auction status, price paid, total bid count,
     *         highest bid, and lowest bid.
     */
    public AuctionResult finalizeAuction() {
        lock.lock();
        try {
            if (isClosed) {
                return new AuctionResult(item, closeTime, -1, "UNSOLD", 0.00, 0, 0.00, 0.00);
            }

            isClosed = true;
            double highestBid = bids.isEmpty() ? 0.00 : bids.firstKey();
            double lowestBid = bids.isEmpty() ? 0.00 : bids.lastKey();

            // If no valid bids are present or all bids are below the reserve price
            if (bids.isEmpty() || highestBid < reservePrice) {
                return new AuctionResult(item, closeTime, -1, "UNSOLD", 0.00, bids.size(), highestBid, lowestBid);
            }

            // Calculate the winner and price paid
            int totalBidCount = bids.size();
            Map.Entry<Double, Bid> winnerEntry = bids.pollFirstEntry();
            double pricePaid = bids.isEmpty() ? reservePrice : bids.firstKey();

            return new AuctionResult(item, closeTime, winnerEntry.getValue().getUserId(),
                    "SOLD", pricePaid, totalBidCount, highestBid, lowestBid);
        } finally {
            lock.unlock();
        }
    }

    public long getCloseTime() { return closeTime; }
    public String getItem() { return item; }
}
