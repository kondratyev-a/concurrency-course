package course.concurrency.exams.auction;

public class AuctionPessimistic implements Auction {

    private final Notifier notifier;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
        this.latestBid = new Bid(null, null, 0L);
    }

    private volatile Bid latestBid;

    public boolean propose(Bid bid) {
        if (bid.getPrice() > latestBid.getPrice()) {
            synchronized (this) {
                if (bid.getPrice() > latestBid.getPrice()) {
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
}
