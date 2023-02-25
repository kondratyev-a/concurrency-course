package course.concurrency.m2_async.cf.min_price;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    private ExecutorService executor = Executors.newCachedThreadPool();

    public double getMinPrice(long itemId) {

        List<CompletableFuture<Double>> completableFutures = new ArrayList<>();

        for (var shopId : shopIds) {
            completableFutures.add(
                    CompletableFuture
                            .supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executor)
                            .orTimeout(2950, TimeUnit.MILLISECONDS)
                            .handle((price, ex) -> ex != null ? null : price)
            );
        }

        CompletableFuture
                .allOf(completableFutures.toArray(new CompletableFuture[shopIds.size()]))
                .join();

        return completableFutures.stream()
                .filter(future -> future.isDone() && future.join() != null)
                .mapToDouble(CompletableFuture::join)
                .min()
                .orElse(Double.NaN);
    }
}
