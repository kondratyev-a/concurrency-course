package course.concurrency.m3_shared.immutable;

import java.util.List;

public final class Order {

    public enum Status { NEW, IN_PROGRESS, DELIVERED }

    private final Long id;
    private final List<Item> items;
    private final PaymentInfo paymentInfo;
    private final boolean isPacked;
    private final Status status;

    public Order(Long id, List<Item> items, PaymentInfo paymentInfo, boolean isPacked, Status status) {
        this.id = id;
        this.items = items;
        this.paymentInfo = paymentInfo;
        this.isPacked = isPacked;
        this.status = status;
    }

    public Order withPaymentInfo(PaymentInfo paymentInfo) {
        return new Order(
                getId(),
                getItems(),
                paymentInfo,
                isPacked(),
                checkStatus(paymentInfo, isPacked()) ? Status.DELIVERED : Status.IN_PROGRESS);
    }

    public Order withIsPacked(boolean isPacked) {
        return new Order(
                getId(),
                getItems(),
                getPaymentInfo(),
                isPacked,
                checkStatus(getPaymentInfo(), isPacked) ? Status.DELIVERED : Status.IN_PROGRESS);
    }

    public boolean checkStatus(PaymentInfo paymentInfo, boolean isPacked) {
        if (items != null && !items.isEmpty() && paymentInfo != null && isPacked) {
            return true;
        }
        return false;
    }

    public Long getId() {
        return id;
    }

    public List<Item> getItems() {
        return items;
    }

    public PaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    public boolean isPacked() {
        return isPacked;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isDelivered() {
        return getStatus().equals(Order.Status.DELIVERED);
    }
}
