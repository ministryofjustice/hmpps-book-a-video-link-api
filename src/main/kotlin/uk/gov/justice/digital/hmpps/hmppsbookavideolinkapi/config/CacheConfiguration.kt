package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
@EnableScheduling
class CacheConfiguration {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    const val VIDEO_LINK_LOCATIONS_CACHE_NAME: String = "video_link_locations"
    const val NON_RESIDENTIAL_LOCATIONS_CACHE_NAME: String = "non_residential_locations"
    const val MIGRATION_LOCATIONS_CACHE_NAME: String = "locations_by_internal_id"
    const val MIGRATION_PRISONERS_CACHE_NAME: String = "prisoners_by_booking_id"
    const val ROLLED_OUT_PRISONS_CACHE_NAME = "rolled_out_prisons"
  }

  @Bean
  fun cacheManager(): CacheManager =
    ConcurrentMapCacheManager(
      VIDEO_LINK_LOCATIONS_CACHE_NAME,
      NON_RESIDENTIAL_LOCATIONS_CACHE_NAME,
      MIGRATION_LOCATIONS_CACHE_NAME,
      MIGRATION_PRISONERS_CACHE_NAME,
      ROLLED_OUT_PRISONS_CACHE_NAME,
    )

  @CacheEvict(value = [VIDEO_LINK_LOCATIONS_CACHE_NAME])
  @Scheduled(fixedDelay = 12, timeUnit = TimeUnit.HOURS)
  fun cacheEvictVideoLinkLocations() {
    log.info("Evicting cache: $VIDEO_LINK_LOCATIONS_CACHE_NAME after 12 hours")
  }

  @CacheEvict(value = [NON_RESIDENTIAL_LOCATIONS_CACHE_NAME])
  @Scheduled(fixedDelay = 12, timeUnit = TimeUnit.HOURS)
  fun cacheEvictNonResidentialLocations() {
    log.info("Evicting cache: $NON_RESIDENTIAL_LOCATIONS_CACHE_NAME after 12 hours")
  }

  @Deprecated(message = "Can be removed when migration is completed")
  @CacheEvict(value = [MIGRATION_LOCATIONS_CACHE_NAME])
  @Scheduled(fixedDelay = 3, timeUnit = TimeUnit.HOURS)
  fun cacheEvictLocationsById() {
    log.info("Evicting cache: $MIGRATION_LOCATIONS_CACHE_NAME after 3 hours")
  }

  @Deprecated(message = "Can be removed when migration is completed")
  @CacheEvict(value = [MIGRATION_PRISONERS_CACHE_NAME])
  @Scheduled(fixedDelay = 3, timeUnit = TimeUnit.HOURS)
  fun cacheEvictPrisonersByBookingId() {
    log.info("Evicting cache: $MIGRATION_PRISONERS_CACHE_NAME after 3 hours")
  }

  @CacheEvict(value = [ROLLED_OUT_PRISONS_CACHE_NAME])
  @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.DAYS)
  fun cacheEvictRolledOutPrisons() {
    log.info("Evicting cache: $ROLLED_OUT_PRISONS_CACHE_NAME after 1 day")
  }
}
