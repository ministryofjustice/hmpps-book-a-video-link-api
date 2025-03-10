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
    const val ROLLED_OUT_PRISONS_CACHE_NAME = "rolled_out_prisons"
    const val NOMIS_MAPPING_CACHE_NAME = "nomis_mapping"
    const val TWO = 2L
    const val FIVE = 5L
  }

  @Bean
  fun cacheManager(): CacheManager = ConcurrentMapCacheManager(
    VIDEO_LINK_LOCATIONS_CACHE_NAME,
    NON_RESIDENTIAL_LOCATIONS_CACHE_NAME,
    ROLLED_OUT_PRISONS_CACHE_NAME,
    NOMIS_MAPPING_CACHE_NAME,
  )

  @CacheEvict(value = [VIDEO_LINK_LOCATIONS_CACHE_NAME], allEntries = true)
  @Scheduled(fixedDelay = FIVE, timeUnit = TimeUnit.MINUTES)
  fun cacheEvictVideoLinkLocations() {
    log.info("Evicting cache: $VIDEO_LINK_LOCATIONS_CACHE_NAME after $FIVE mins")
  }

  @CacheEvict(value = [NON_RESIDENTIAL_LOCATIONS_CACHE_NAME], allEntries = true)
  @Scheduled(fixedDelay = FIVE, timeUnit = TimeUnit.MINUTES)
  fun cacheEvictNonResidentialLocations() {
    log.info("Evicting cache: $NON_RESIDENTIAL_LOCATIONS_CACHE_NAME after $FIVE mins")
  }

  @CacheEvict(value = [ROLLED_OUT_PRISONS_CACHE_NAME], allEntries = true)
  @Scheduled(fixedDelay = FIVE, timeUnit = TimeUnit.MINUTES)
  fun cacheEvictRolledOutPrisons() {
    log.info("Evicting cache: $ROLLED_OUT_PRISONS_CACHE_NAME after $FIVE mins")
  }

  @CacheEvict(value = [NOMIS_MAPPING_CACHE_NAME], allEntries = true)
  @Scheduled(fixedDelay = TWO, timeUnit = TimeUnit.HOURS)
  fun cacheEvictNomisMapping() {
    log.info("Evicting cache: $NOMIS_MAPPING_CACHE_NAME after $TWO hours")
  }
}
