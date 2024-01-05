#### Auction System Simulation

#### Overview
This repository contains a Java-based simulation of an auction system, designed to mimic real-world auction dynamics. It offers a robust platform for listing items for auction and placing bids, with a focus on handling complex scenarios including concurrent bidding.

#### Purpose of the Simulation
The primary aim is to demonstrate effective handling of various auction scenarios, particularly focusing on concurrency and compliance with common auction rules.

#### Auction Rules
The application adheres to these rules:
- **Auction Time Frame**: Bids are valid if placed after the auction start time and before or on the closing time.
- **Bid Validity**: A bid is valid if it is higher than previous bids made by the same user.
- **Winning Criteria**:
   - If only one valid bid is received, the item is sold at the reserve price.
   - If multiple bids are received, the highest bidder wins but pays the price of the second-highest bid.
   - In the event of equal bids, the earliest bid prevails.
- **No Bids**: Without valid bids, the item remains unsold.

#### Implementation Details
#### Data Structures and Algorithms
- **ConcurrentHashMap**: Utilized for storing active auctions (`auctions`) and auction results (`auctionResults`) in the `AuctionManager` class, ensuring thread-safe operations.
- **TreeMap**: Employed within each `Auction` instance to store bids in a reverse-sorted order for efficient highest and second-highest bid retrieval.

#### Synchronization and Thread Safety
- **ReentrantLock**: Used within the `Auction` class to ensure thread safety for operations like bid placement and auction finalization.
- **Shared Variables**: Critical for maintaining the state of auctions and handling concurrent access.

#### Techniques
- **Concurrency Handling**: Demonstrates handling of concurrent bid submissions using `ExecutorService` in unit tests to simulate real-world auction scenarios.
- **Thread Safety**: Ensures consistent and accurate auction processing in a multi-threaded environment.

#### Input and Output Formats
#### Input
Actions are represented as pipe-delimited strings:
- SELL: `timestamp|user_id|SELL|item|reserve_price|close_time`
- BID: `timestamp|user_id|BID|item|bid_amount`
- Heartbeat: `timestamp`

#### Output
Each completed auction outputs:
- `close_time|item|user_id|status|price_paid|total_bid_count|highest_bid|lowest_bid`

#### Unit Test Cases
The repository includes comprehensive tests for scenarios like:
- Single Bid Wins and Pays Reserve Price
- Highest Bidder Pays Second Highest Price
- Auction with No Bids
- Concurrent Bidding
- Bid at Closing Time
These tests, located in `src/test/java/...`, validate the functionality of the auction system.

### Project Demonstration
This project demonstrates:
- Effective concurrent operations handling in software applications.
- Application of data structures (ConcurrentHashMap, TreeMap) for real-world problem-solving.
- Implementation of synchronization (ReentrantLock) for thread safety in multi-threaded environments.
- Adherence to complex auction rules and handling of various auction outcomes.
