package org.devjoemar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AuctionSystemTest {
    private AuctionManager manager;

    @BeforeEach
    public void setUp() {
        manager = new AuctionManager();
    }

    /**
     * Tests that an auction with a single bid results in the item being sold at the reserve price.
     * <p>
     * This test verifies the auction rule that if there is only one valid bid which is higher than
     * the reserve price, the item is sold at the reserve price. This scenario is common in auctions
     * where limited interest in an item leads to a single bid, and the auction system must ensure
     * the item is sold at no less than the reserve price.
     * </p>
     * <p>
     * Input Format:
     * - SELL action: [timestamp]|[user_id]|SELL|[item_code]|[reserve_price]|[close_time]
     * - BID action: [timestamp]|[user_id]|BID|[item_code]|[bid_amount]
     * - Heartbeat: [timestamp]
     *
     * The test processes these inputs to simulate an auction and then asserts the following to validate
     * the auction behavior:
     * 1. Auction Status ('SOLD'): Confirms the auction successfully concluded with the item being sold.
     *    This checks if the auction system correctly finalizes auctions with valid bids.
     * 2. Winning Bid User: Validates that the user who placed the bid is correctly recognized as the winner.
     *    This ensures the auction system accurately tracks and awards the winning bidder.
     * 3. Price Paid (Reserve Price): Checks if the final price paid is equal to the reserve price, not the bid amount.
     *    This assertion is crucial as it confirms the auction rule that in single-bid scenarios, the item
     *    is sold at the reserve price.
     * 4. Total Number of Valid Bids (1): Verifies that the auction system correctly identifies and counts
     *    valid bids. In this case, there should be exactly one valid bid.
     * </p>
     * <p>
     * By validating these conditions, the test ensures that the auction system adheres to its defined
     * rules in single-bid scenarios, maintaining the integrity and fairness of the auction process.
     * </p>
     */
    @Test
    public void testSingleBidWinsAndPaysReservePrice() {
        manager.processInput("1|1|SELL|item1|10.00|100");
        manager.processInput("2|2|BID|item1|15.00");
        manager.processInput("100");

        AuctionResult result = manager.getAuctionResult("item1");
        assertNotNull(result);
        assertEquals("2", String.valueOf(result.getUserId()));
        assertEquals("SOLD", result.getStatus());
        assertEquals(10.00, result.getPricePaid(), 0.01);
        assertEquals(1, result.getTotalBidCount());
    }

    /**
     * Tests that the highest bidder in an auction pays the amount of the second-highest bid.
     * <p>
     * This test validates a key auction rule: in a scenario with multiple bids, the highest bidder
     * wins the auction but pays the amount equal to the second-highest bid. This rule encourages
     * competitive bidding by ensuring that the winning bid is not necessarily the final price paid.
     * </p>
     * <p>
     * Input Format:
     * - SELL action: [timestamp]|[user_id]|SELL|[item_code]|[reserve_price]|[close_time]
     * - BID action: [timestamp]|[user_id]|BID|[item_code]|[bid_amount]
     * - Heartbeat: [timestamp]
     *
     * The test processes these inputs to create an auction with multiple bids and then validates:
     * 1. Auction Status ('SOLD'): Confirms that the auction concluded with the item being sold,
     *    indicating that there were valid bids that met or exceeded the reserve price.
     * 2. Winning Bid User: Ensures that the user with the highest bid is correctly identified as the winner.
     *    This checks the auction system's capability to track and recognize the highest bidder.
     * 3. Price Paid (Second-Highest Bid): Verifies that the price paid by the winner is equal to the
     *    second-highest bid. This is a critical assertion to confirm that the auction system follows
     *    the rule of selling at the second-highest price in multiple-bid scenarios.
     * 4. Total Number of Valid Bids: Checks that the system accurately counts and recognizes all valid
     *    bids placed in the auction. This ensures the integrity of the bidding process.
     * </p>
     * <p>
     * Through these assertions, the test case ensures the auction system's compliance with its bidding
     * rules, particularly in handling auctions with multiple bids and determining the final selling price.
     * </p>
     */
    @Test
    public void testHighestBidderPaysSecondHighestPrice() {
        manager.processInput("1|1|SELL|item2|10.00|100");
        manager.processInput("2|2|BID|item2|12.00");
        manager.processInput("3|3|BID|item2|15.00");
        manager.processInput("100");

        AuctionResult result = manager.getAuctionResult("item2");
        assertNotNull(result);
        assertEquals("3", String.valueOf(result.getUserId()));
        assertEquals("SOLD", result.getStatus());
        assertEquals(12.00, result.getPricePaid(), 0.01);
        assertEquals(2, result.getTotalBidCount());
    }

    /**
     * Tests the auction behavior when no bids are placed.
     * <p>
     * This test case examines the situation where an auction receives no bids. It validates the
     * auction rule that if an auction closes without any valid bids, the item remains unsold. This
     * scenario is essential to test as it checks the auction system's ability to handle cases of
     * no interest in an item and to conclude the auction appropriately.
     * </p>
     * <p>
     * Input Format:
     * - SELL action: [timestamp]|[user_id]|SELL|[item_code]|[reserve_price]|[close_time]
     * - Heartbeat: [timestamp]
     *
     * The test simulates an auction with these inputs but without any BID actions. It then proceeds
     * to validate:
     * 1. Auction Status ('UNSOLD'): Ensures that the auction status is marked as 'UNSOLD', confirming
     *    that the system recognizes and correctly handles auctions without any bids.
     * 2. Winning Bid User: Checks that the winning user ID is empty or null, which is expected for
     *    unsold items.
     * 3. Price Paid: Verifies that the price paid is zero, aligning with the outcome of an unsold auction.
     * 4. Total Number of Valid Bids: Asserts that the total count of valid bids is zero, indicating no
     *    bids were placed during the auction.
     * </p>
     * <p>
     * Through these validations, this test case ensures that the auction system properly handles
     * scenarios where auctions receive no bids, maintaining the integrity and expected functionality
     * of the auction process.
     * </p>
     */
    @Test
    public void testAuctionWithNoBids() {
        manager.processInput("1|1|SELL|item3|10.00|100");
        manager.processInput("100");

        AuctionResult result = manager.getAuctionResult("item3");
        assertNotNull(result);
        assertEquals("-1", String.valueOf(result.getUserId()));
        assertEquals("UNSOLD", result.getStatus());
        assertEquals(0.00, result.getPricePaid(), 0.01);
        assertEquals(0, result.getTotalBidCount());
    }

    /**
     * Tests the behavior of the auction system when a bid is placed exactly at the auction's closing time.
     * <p>
     * This test case simulates a scenario where a bid is made at the exact moment the auction closes. The
     * primary objective is to validate whether such a bid is considered valid by the auction system, and if
     * so, how it impacts the auction's outcome. This scenario tests the system's precision in handling
     * timing-related aspects of bids, ensuring that bids made at the last possible moment are processed correctly.
     * </p>
     * <p>
     * Input Format:
     * - SELL action: [timestamp]|[user_id]|SELL|[item_code]|[reserve_price]|[close_time]
     * - BID action: [timestamp]|[user_id]|BID|[item_code]|[bid_amount]
     * - Heartbeat: [timestamp]
     *
     * The test sets up an auction and processes a bid made precisely at the auction's closing time
     * (indicated by the timestamp). It then proceeds to validate:
     * 1. Auction Status ('SOLD' or 'UNSOLD'): Confirms the auction status based on whether the
     *    closing-time bid was considered valid.
     * 2. Winning Bid User: If the auction is sold, checks that the winning user ID corresponds to
     *    the bidder of the closing-time bid.
     * 3. Price Paid: Verifies the price paid, especially in relation to the reserve price and any
     *    other bids that might have been made.
     * 4. Total Number of Valid Bids: Asserts the count of valid bids, which should include the
     *    closing-time bid if it is deemed valid.
     * </p>
     * <p>
     * This test is crucial for ensuring that the auction system accurately respects the defined
     * auction closing times and includes bids that are made exactly at the closing time, thereby
     * upholding the fairness and integrity of the auction process.
     * </p>
     */
    @Test
    public void testBidAtClosingTime() {
        manager.processInput("1|1|SELL|item5|10.00|100");
        manager.processInput("2|2|BID|item5|15.00");
        manager.processInput("100|3|BID|item5|20.00"); // Bid at closing time
        manager.processInput("100");

        AuctionResult result = manager.getAuctionResult("item5");
        assertNotNull(result);
        assertEquals("3", String.valueOf(result.getUserId()));
        assertEquals("SOLD", result.getStatus());
        assertEquals(15.00, result.getPricePaid(), 0.01);
        assertEquals(2, result.getTotalBidCount());
    }

    /**
     * Tests the auction system's handling of concurrent bid submissions.
     * <p>
     * This test simulates a scenario where multiple bids are placed on an item simultaneously,
     * reflecting a common real-world auction situation. It aims to validate the system's capability
     * to process these bids concurrently and maintain a consistent and accurate state of the auction.
     * </p>
     * <p>
     * Test Setup:
     * - An auction for 'item4' is created with a reserve price of 10.00 and a closing time at timestamp 100.
     * - Using an ExecutorService, the test submits 10 bid tasks to be executed concurrently. Each task
     *   represents a bid from a unique user with incrementally increasing bid amounts from 10.00 to 19.00.
     * - The ExecutorService is then shut down, and the test waits for the completion of all bid tasks
     *   using awaitTermination, ensuring that all bids are processed before the auction is closed.
     * - The auction is closed at timestamp 100 by processing a heartbeat action.
     * </p>
     * <p>
     * Assertions:
     * 1. Winning Bidder ('userId = 11'): Verifies that the user with the highest bid wins the auction.
     *    Given the setup, userId 11 places the highest bid of 19.00.
     * 2. Auction Status ('SOLD'): Confirms that the auction concludes successfully, indicating valid bids
     *    were processed.
     * 3. Price Paid (18.00): Ensures the final price paid matches the second-highest bid, as per the auction rules.
     * 4. Total Bid Count (10): Validates that the system accurately counts all bids, affirming successful
     *    concurrent handling.
     * </p>
     * <p>
     * The use of awaitTermination is crucial in this test to ensure all concurrent bids are fully processed,
     * providing a reliable basis for assessing the auction outcome. It prevents the test from prematurely
     * evaluating results, thus upholding the integrity and accuracy of the test under concurrent conditions.
     * </p>
     */
    @Test
    public void testConcurrentBidding() throws InterruptedException {
        manager.processInput("1|1|SELL|item4|10.00|100");

        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            int userId = i + 2;
            double bidAmount = 10.00 + i; // Bids from 10.00 to 19.00
            executor.execute(() -> manager.processInput("50|" + userId + "|BID|item4|" + bidAmount));
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        manager.processInput("100");

        AuctionResult result = manager.getAuctionResult("item4");
        assertNotNull(result);
        assertEquals("11", String.valueOf(result.getUserId()));
        assertEquals("SOLD", result.getStatus());
        assertEquals(18.00, result.getPricePaid(), 0.01);
        assertEquals(10, result.getTotalBidCount());
    }

    /**
     * Tests a complex auction scenario with multiple items and bids.
     * <p>
     * This test simulates two separate auctions, 'toaster_1' and 'tv_1', with varying bids and
     * closing times. It validates the auction system's ability to correctly process these bids,
     * handle auction closing times, and determine the final outcome of each auction.
     * </p>
     * <p>
     * The test follows a sequence of inputs to simulate the auction environment:
     * - 'toaster_1' receives bids both below and above its reserve price.
     * - 'tv_1' receives bids only below its reserve price during the auction period.
     * - A late bid is placed on 'tv_1' after its closing time.
     * </p>
     * <p>
     * Assertions made in this test:
     * - For 'toaster_1':
     *   - Asserts the auction is 'SOLD'.
     *   - Confirms the winning user and the price paid (second-highest bid).
     *   - Validates the total number of bids, highest bid, and lowest bid.
     * - For 'tv_1':
     *   - Asserts the auction is 'UNSOLD' due to no bids meeting the reserve price.
     *   - Confirms that no winning user is recorded.
     *   - Validates that the late bid after closing time is not considered.
     * </p>
     * <p>
     * This test case provides a comprehensive assessment of the auction system's core functionalities,
     * including bid processing, auction finalization, and rule enforcement (such as handling bids
     * post-auction closing and reserve price considerations).
     * </p>
     */
    @Test
    public void testAuctionScenario() {
        // Process inputs for toaster_1 and tv_1 auctions
        manager.processInput("10|1|SELL|toaster_1|10.00|20");
        manager.processInput("12|8|BID|toaster_1|7.50");
        manager.processInput("13|5|BID|toaster_1|12.50");
        manager.processInput("15|8|SELL|tv_1|250.00|20");
        manager.processInput("16");
        manager.processInput("17|8|BID|toaster_1|20.00");
        manager.processInput("18|1|BID|tv_1|150.00");
        manager.processInput("19|3|BID|tv_1|200.00");
        manager.processInput("20");
        manager.processInput("21|3|BID|tv_1|300.00");

        // Assert results for toaster_1
        AuctionResult toasterResult = manager.getAuctionResult("toaster_1");
        assertNotNull(toasterResult);
        assertEquals("8", String.valueOf(toasterResult.getUserId()));
        assertEquals("SOLD", toasterResult.getStatus());
        assertEquals(12.50, toasterResult.getPricePaid(), 0.01);
        assertEquals(3, toasterResult.getTotalBidCount());
        assertEquals(20.00, toasterResult.getHighestBid(), 0.01);
        assertEquals(7.50, toasterResult.getLowestBid(), 0.01);

        // Assert results for tv_1
        AuctionResult tvResult = manager.getAuctionResult("tv_1");
        assertNotNull(tvResult);
        assertEquals("-1", String.valueOf(tvResult.getUserId())); // No winning user
        assertEquals("UNSOLD", tvResult.getStatus());
        assertEquals(0.00, tvResult.getPricePaid(), 0.01);
        assertEquals(2, tvResult.getTotalBidCount());
        assertEquals(200.00, tvResult.getHighestBid(), 0.01);
        assertEquals(150.00, tvResult.getLowestBid(), 0.01);
    }

}
