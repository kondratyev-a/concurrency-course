package course.concurrency.m3_shared.immutable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class OrderService {

    private final Map<Long, Order> currentOrders = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(0);

    private long nextId() {
        return nextId.getAndIncrement();
    }

    public long createOrder(List<Item> items) {
        long id = nextId();
        Order order = new Order(id, items, null, false, Order.Status.NEW);
        currentOrders.put(id, order);
        return id;
    }

    public synchronized void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
        currentOrders.put(orderId, currentOrders.get(orderId).withPaymentInfo(paymentInfo));
    }

    public synchronized void setPacked(long orderId) {
        currentOrders.put(orderId, currentOrders.get(orderId).withIsPacked(true));
    }

    public boolean isDelivered(long orderId) {
        return currentOrders.get(orderId).isDelivered();
    }
}
