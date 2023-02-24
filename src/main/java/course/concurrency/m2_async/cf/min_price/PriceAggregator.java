package course.concurrency.m2_async.cf.min_price;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {

        List<CompletableFuture<Double>> completableFutures = new ArrayList<>();

        for (var shopId : shopIds) {
            completableFutures.add(
                    CompletableFuture
                            .supplyAsync(() -> priceRetriever.getPrice(itemId, shopId))
                            .orTimeout(2, TimeUnit.SECONDS) // Поставил 2 чтобы общее время было меньше 3
                            .handle((price, ex) -> ex != null ? Double.NaN : price)
            );
        }

        CompletableFuture
                .allOf(completableFutures.toArray(new CompletableFuture[shopIds.size()]))
                .join();

        return completableFutures.stream()
                .filter(CompletableFuture::isDone)
                .map(CompletableFuture::join)
                .min(Comparator.naturalOrder())
                .orElse(Double.NaN);
    }
}
