package course.concurrency.exams.refactoring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

class MountTableRefresherServiceTests {

    private MountTableRefresherService service;

    private Others.RouterStore routerStore;
    private Others.MountTableManager manager;
    private Others.LoadingCache routerClientsCache;

    @BeforeEach
    public void setUpStreams() {
        service = Mockito.spy(new MountTableRefresherService());
        service.setCacheUpdateTimeout(1000);
        routerStore = mock(Others.RouterStore.class);
        manager = mock(Others.MountTableManager.class);
        service.setRouterStore(routerStore);
        routerClientsCache = mock(Others.LoadingCache.class);
        service.setRouterClientsCache(routerClientsCache);
        // service.serviceInit(); // needed for complex class testing, not for now
    }

    @AfterEach
    public void restoreStreams() {
        // service.serviceStop();
    }

    @Test
    @DisplayName("All tasks are completed successfully")
    void allDone() {
        // given
        initialize();
        when(manager.refresh()).thenReturn(true);

        // when
        service.refresh();

        // then
        verify(service, never()).log("Not all router admins updated their cache");
        verify(service).log("Mount table entries cache refresh successCount=4,failureCount=0");
        verify(routerClientsCache, never()).invalidate(anyString());
    }

    @Test
    @DisplayName("All tasks failed")
    void noSuccessfulTasks() {
        // given
        initialize();
        when(manager.refresh()).thenReturn(false);

        // when
        service.refresh();

        // then
        verify(service, times(1)).log("Not all router admins updated their cache");
        verify(service).log("Mount table entries cache refresh successCount=0,failureCount=4");
        verify(routerClientsCache, times(4)).invalidate(anyString());
    }

    @Test
    @DisplayName("Some tasks failed")
    void halfSuccessedTasks() {
        // given
        initialize();
        when(manager.refresh()).thenReturn(true, false, true, false);

        // when
        service.refresh();

        // then
        verify(service, times(1)).log("Not all router admins updated their cache");
        verify(service).log("Mount table entries cache refresh successCount=2,failureCount=2");
        verify(routerClientsCache, times(2)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task completed with exception")
    void exceptionInOneTask() {
        // given
        initialize();
        mockManagerAnswer("exception");

        // when
        service.refresh();

        // then
        verify(service, times(1)).log("Not all router admins updated their cache");
        verify(service).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task exceeds timeout")
    void oneTaskExceedTimeout() {
        // given
        initialize();
        mockManagerAnswer("timeout");

        // when
        service.refresh();

        // then
        verify(service, times(1)).log("Not all router admins updated their cache");
        verify(service).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    private void initialize() {
        List<String> addresses = List.of("123", "local6", "789", "local");

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
        when(service.createMountTableManager(anyString())).thenReturn(manager);
    }

    private void mockManagerAnswer(String action) {
        var count = new AtomicInteger();

        when(manager.refresh()).thenAnswer(a -> {
            count.incrementAndGet();
            if (count.get() == 1) {
                if (action.equals("timeout"))
                    Thread.sleep(10000);
                else if (action.equals("exception")) {
                    throw new RuntimeException();
                }
                return false;
            } else {
                return true;
            }
        });
    }

}
