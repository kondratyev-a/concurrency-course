package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private final Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicMarkableReference<Bid> latestBid = new AtomicMarkableReference<>(
            new Bid(null, null, 0L), false);

    public boolean propose(Bid bid) {
        Bid currentLatestBid;
        boolean isStopped;
        do {
            currentLatestBid = latestBid.getReference();
            isStopped = latestBid.isMarked();
            if (isStopped || bid.getPrice() <= currentLatestBid.getPrice()) {
                return false;
            }
        } while (latestBid.compareAndSet(currentLatestBid, bid, false, latestBid.isMarked()));

        notifier.sendOutdatedMessage(latestBid.getReference());
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }

    public Bid stopAuction() {
        var currentLatestBid = getLatestBid();
        latestBid.set(currentLatestBid, true);
        return currentLatestBid;
    }
}
