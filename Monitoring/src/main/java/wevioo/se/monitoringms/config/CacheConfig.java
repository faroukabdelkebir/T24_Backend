package wevioo.se.monitoringms.config;


import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cache Caffeine avec TTL différencié par cache :
 *  - dashboard  : 30 secondes  (données quasi-live)
 *  - kpis       : 2 minutes    (agrégats tolèrent du délai)
 *  - schema     : 10 minutes   (rarement modifié)
 *  - config     : 5 minutes
 *  - reports    : 15 minutes
 */
@Configuration
public class CacheConfig {

    public static final String CACHE_DASHBOARD   = "dashboard";
    public static final String CACHE_KPIS        = "kpis";
    public static final String CACHE_SCHEMA      = "schema";
    public static final String CACHE_CONFIG      = "batchConfig";
    public static final String CACHE_REPORTS     = "reports";
    public static final String CACHE_LIVE_JOBS   = "liveJobs";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                build(CACHE_LIVE_JOBS, 20, TimeUnit.SECONDS,  200),
                build(CACHE_DASHBOARD, 30, TimeUnit.SECONDS,  200),
                build(CACHE_KPIS,       2, TimeUnit.MINUTES,  500),
                build(CACHE_SCHEMA,    10, TimeUnit.MINUTES,  1000),
                build(CACHE_CONFIG,     5, TimeUnit.MINUTES,  200),
                build(CACHE_REPORTS,   15, TimeUnit.MINUTES,  100)
        ));
        return manager;
    }

    private CaffeineCache build(String name, long duration, TimeUnit unit, int maxSize) {
        return new CaffeineCache(name,
                Caffeine.newBuilder()
                        .expireAfterWrite(duration, unit)
                        .maximumSize(maxSize)
                        .recordStats()
                        .build());
    }
}
