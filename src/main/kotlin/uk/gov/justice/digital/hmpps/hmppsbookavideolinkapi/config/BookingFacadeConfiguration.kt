package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.OriginalBookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ReplacementBookingFacade

/**
 * This configuration class is responsible for deciding which implementation of the BookingFacade to use. The intention
 * is to eventually use the replacement facade in all environments such that the original facade can be removed. When we
 * are happy with the replacement facade, we can remove the original facade and the facade configuration bean can also
 * be removed.
 */
@Configuration
class BookingFacadeConfiguration(
  private val featureSwitches: FeatureSwitches,
  private val originalBookingFacade: OriginalBookingFacade,
  private val replacementBookingFacade: ReplacementBookingFacade,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  fun bookingFacade(): BookingFacade {
    if (featureSwitches.isEnabled(BooleanFeature.FEATURE_EMAIL_FACADE_ENABLED)) {
      log.info("BOOKING_FACADE: using replacement booking facade")
      return replacementBookingFacade
    }

    log.info("BOOKING_FACADE: : using original booking facade")
    return originalBookingFacade
  }
}
