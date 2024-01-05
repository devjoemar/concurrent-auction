package org.devjoemar;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the operations and state of the auction system.
 * <p>
 * This class is responsible for coordinating various actions within the auction system, including
 * the creation of new auctions, placing bids on items, and handling the passage of time through
 * heartbeat messages. It serves as the central point for processing input actions and maintaining
 * the current state of all ongoing auctions.
 * </p>
 * <p>
 * The AuctionManager uses concurrent data structures and synchronization mechanisms to ensure
 * thread safety, as it is designed to handle concurrent operations in a multi-threaded environment.
 * Shared variables such as the auctions map and the auction results are crucial for maintaining
 * consistent states across different actions. Without these shared structures, tracking ongoing
 * auctions and their outcomes would be leading to potential data inconsistencies
 * </p>
 * <p>
 * The design of the class reflects an intention to keep auction management centralized and
 * encapsulated, allowing for easy tracking, modification, and retrieval of auction data. It
 * supports scalability and concurrency, ensuring that the system can handle multiple simultaneous
 * actions without compromising data integrity or consistency.
 * </p>
 *
 * Fields:
 * - auctions: A concurrent map that stores active auctions, allowing efficient retrieval and updates.
 * - auctionResults: A concurrent map for storing finalized auction results for retrieval.
 * - lock: A reentrant lock to control synchronization for certain operations.
 */
public class AuctionManager {
    private static final int PARTS_LENGTH_FOR_ACTION = 3;
    private static final int TIMESTAMP_INDEX = 0;
    private static final int USER_ID_INDEX = 1;
    private static final int ITEM_INDEX = 3;
    private static final int RESERVE_PRICE_INDEX = 4;
    private static final int CLOSE_TIME_INDEX = 5;
    private static final int BID_AMOUNT_INDEX = 4;

    private final ConcurrentHashMap<String, Auction> auctions;
    private final ReentrantLock lock;
    private final ConcurrentHashMap<String, AuctionResult> auctionResults;

    public AuctionManager() {
        this.auctions = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        this.auctionResults = new ConcurrentHashMap<>();
    }


    /**
     * Processes an input line and executes the corresponding action in the auction system.
     * <p>
     * This method is the primary entry point for interacting with the auction system based on textual input.
     * It parses the input line and determines the type of action to perform, which can be one of 'SELL',
     * 'BID', or a heartbeat action (indicated by just a timestamp). Depending on the action type, it
     * delegates the processing to the respective handler methods: {@code handleSellAction},
     * {@code handleBidAction}, or {@code handleHeartbeatAction}.
     * </p>
     * <p>
     * The method expects the input line to be in a pipe-delimited format. For 'SELL' and 'BID' actions,
     * it expects additional information such as user ID, item code, bid amount, etc., while for
     * heartbeat actions, only a timestamp is expected.
     * </p>
     * <p>
     * This approach allows the auction system to process various types of actions in a unified and
     * extensible manner. It is designed to easily accommodate additional action types in the future if needed.
     * </p>
     *
     * @param inputLine The input line representing an action in the auction system. The line is
     *                  expected to be in a pipe-delimited format, with the first element being the
     *                  timestamp and the third element (if present) indicating the action type.
     */
    public void processInput(String inputLine) {
        String[] parts = inputLine.split("\\|");
        String action = parts.length > PARTS_LENGTH_FOR_ACTION ? parts[2] : "";

        switch (action) {
            case "SELL":
                handleSellAction(parts);
                break;
            case "BID":
                handleBidAction(parts);
                break;
            default:
                handleHeartbeatAction(Long.parseLong(parts[TIMESTAMP_INDEX]));
                break;
        }
    }

    /**
     * Handles the action of creating a new auction based on the input parts.
     * <p>
     * This method is invoked when a 'SELL' action is identified in the input. It parses the input
     * parts to extract the necessary information for auction creation, including the item code,
     * reserve price, and auction close time. It then creates a new Auction instance and adds it to
     * the auctions map for tracking and management.
     * </p>
     * <p>
     * The input parts are expected to follow a specific format for a 'SELL' action, which includes
     * the timestamp, user ID, action type, item code, reserve price, and close time. This method
     * assumes that the input is correctly formatted and the relevant parts are at specific indices.
     * </p>
     *
     * @param parts The array of strings representing the components of an input line.
     *              For a 'SELL' action, the expected format is:
     *              [timestamp, user_id, "SELL", item_code, reserve_price, close_time]
     */
    private void handleSellAction(String[] parts) {
        String item = parts[ITEM_INDEX];
        double reservePrice = Double.parseDouble(parts[RESERVE_PRICE_INDEX]);
        long closeTime = Long.parseLong(parts[CLOSE_TIME_INDEX]);

        Auction auction = new Auction(item, reservePrice, closeTime);
        auctions.put(item, auction);
    }


    /**
     * Handles the action of placing a bid on an auction based on the input parts.
     * <p>
     * This method is invoked when a 'BID' action is identified in the input. It parses the input
     * parts to extract necessary information for placing a bid, including the item code, user ID,
     * and bid amount. If the auction for the specified item exists, the method creates a new Bid
     * instance and adds it to the auction.
     * </p>
     * <p>
     * The input parts are expected to adhere to a specific format for a 'BID' action, which includes
     * the timestamp, user ID, action type, item code, and bid amount. This method assumes that the
     * input is correctly formatted and the relevant parts are at specific indices.
     * </p>
     * <p>
     * It's important to ensure the auction exists and is still open before attempting to place the
     * bid. Bids for non-existent or closed auctions are ignored.
     * </p>
     *
     * @param parts The array of strings representing the components of an input line.
     *              For a 'BID' action, the expected format is:
     *              [timestamp, user_id, "BID", item_code, bid_amount]
     */
    private void handleBidAction(String[] parts) {
        String item = parts[ITEM_INDEX];
        int userId = Integer.parseInt(parts[USER_ID_INDEX]);
        double bidAmount = Double.parseDouble(parts[BID_AMOUNT_INDEX]);
        long timestamp = Long.parseLong(parts[TIMESTAMP_INDEX]);

        Auction auction = auctions.get(item);
        if (auction != null) {
            auction.placeBid(new Bid(userId, bidAmount, timestamp));
        }
    }

    /**
     * Handles the heartbeat action to progress the auction system based on the given timestamp.
     * <p>
     * This method is invoked when a heartbeat message is identified in the input, represented by a
     * standalone timestamp. Heartbeat messages are used to simulate the passage of time in the auction
     * system. The method iterates through all active auctions and finalizes those whose closing time
     * is less than or equal to the given timestamp.
     * </p>
     * <p>
     * Each auction is checked against the heartbeat timestamp. If the auction's close time is passed,
     * it is finalized using the auction's {@code finalizeAuction} method, and the auction result is
     * stored. Once finalized, the auction is removed from the active auctions map. This process ensures
     * that auctions are concluded in a timely manner, even in the absence of new bids or sell actions.
     * </p>
     * <p>
     * The method employs a ReentrantLock to ensure thread safety while iterating and modifying the
     * collection of auctions. This lock prevents concurrent modifications to the auctions, maintaining
     * the integrity of the auction system in a multi-threaded environment.
     * </p>
     *
     * @param timestamp The timestamp of the heartbeat message, used to determine if auctions should
     *                  be closed and finalized.
     */
    private void handleHeartbeatAction(long timestamp) {
        lock.lock();
        try {
            for (Auction auction : auctions.values()) {
                if (timestamp >= auction.getCloseTime()) {
                    AuctionResult result = auction.finalizeAuction();
                    auctionResults.put(auction.getItem(), result);
                    auctions.remove(auction.getItem());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public AuctionResult getAuctionResult(String item) {
        return auctionResults.get(item);
    }
}
