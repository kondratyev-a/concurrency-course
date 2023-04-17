package course.concurrency.exams.auction;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private final Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
        this.latestBid = new Bid(null, null, 0L);
    }

    private volatile Bid latestBid;

    private volatile boolean isStopped;

    public boolean propose(Bid bid) {
        if (!isStopped && bid.getPrice() > latestBid.getPrice()) {
            synchronized (this) {
                if (!isStopped && bid.getPrice() > latestBid.getPrice()) {
                    notifier.sendOutdatedMessage(latestBid);
                    latestBid = bid;
                    return true;
                }
            }
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }

    public synchronized Bid stopAuction() {
        isStopped = true;
        return latestBid;
    }
}
