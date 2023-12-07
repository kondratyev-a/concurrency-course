package course.concurrency.exams.refactoring;

import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class MountTableRefresherService {

    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;

    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */
    private ScheduledExecutorService clientCacheCleanerScheduler;

    public void serviceInit()  {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<String, Others.RouterClient>();
        routerStore.getCachedRecords().stream().map(Others.RouterState::getAdminAddress)
                .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));

        initClientCacheCleaner(routerClientMaxLiveTime);
    }

    public void serviceStop() {
        clientCacheCleanerScheduler.shutdown();
        // remove and close all admin clients
        routerClientsCache.cleanUp();
    }

    private void initClientCacheCleaner(long routerClientMaxLiveTime) {
        ThreadFactory tf = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread();
                t.setName("MountTableRefresh_ClientsCacheCleaner");
                t.setDaemon(true);
                return t;
            }
        };

        clientCacheCleanerScheduler =
                Executors.newSingleThreadScheduledExecutor(tf);
        /*
         * When cleanUp() method is called, expired RouterClient will be removed and
         * closed.
         */
        clientCacheCleanerScheduler.scheduleWithFixedDelay(
                () -> routerClientsCache.cleanUp(), routerClientMaxLiveTime,
                routerClientMaxLiveTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Refresh mount table cache of this router as well as all other routers.
     */
    public void refresh()  {

        var tasks = routerStore.getCachedRecords().stream()
                .filter(r -> r.getAdminAddress() != null && r.getAdminAddress().length() != 0)
                .collect(Collectors.toMap(Others.RouterState::getAdminAddress,
                        r -> CompletableFuture
                                .supplyAsync(() -> createMountTableManager(r.getAdminAddress()).refresh())
                                .orTimeout(cacheUpdateTimeout, TimeUnit.MILLISECONDS)
                                .handle((result, ex) -> ex == null && result)));

        CompletableFuture
                .allOf(tasks.values().toArray(new CompletableFuture[0]))
                .join();

        var results = tasks.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, t -> t.getValue().join()));

        logResult(results);
    }

    public Others.MountTableManager createMountTableManager(String address) {
        var localAddress = isLocalAdmin(address) ? "local": address;
        return new Others.MountTableManager(localAddress);
    }

    private void logResult(Map<String, Boolean> results) {

        var failureCount = results.entrySet().stream()
                .filter(r -> !r.getValue())
                .peek(r -> removeFromCache(r.getKey()))
                .count();

        if (failureCount != 0) {
            log("Not all router admins updated their cache");
        }

        log(String.format(
                "Mount table entries cache refresh successCount=%d,failureCount=%d",
                results.size() - failureCount, failureCount));
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }


    private boolean isLocalAdmin(String adminAddress) {
        return adminAddress.contains("local");
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }
    public void setRouterClientsCache(Others.LoadingCache cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }
}