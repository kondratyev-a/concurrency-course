package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private final Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicReference<Bid> latestBid = new AtomicReference<>(new Bid(null, null, 0L));

    public boolean propose(Bid bid) {
        Bid currentLatestBid;
        do {
            currentLatestBid = latestBid.get();
            if (bid.getPrice() <= currentLatestBid.getPrice()) {
                return false;
            }
        } while (!latestBid.compareAndSet(currentLatestBid, bid));

        notifier.sendOutdatedMessage(latestBid.get());
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
