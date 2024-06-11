package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches

@Component
class OutboundEventsPublisher(private val features: FeatureSwitches) {

  fun send(event: OutboundHMPPSDomainEvent) {
    if (features.isEnabled(Feature.SNS_ENABLED)) {
      TODO()
    }
  }
}
