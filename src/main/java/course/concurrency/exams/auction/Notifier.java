package course.concurrency.exams.auction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Notifier {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void sendOutdatedMessage(Bid bid) {
        imitateSending();
    }

    private void imitateSending() {
        executor.execute(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {}
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
