package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import java.time.LocalDateTime

@Service
class OutboundEventsService

@Component
class OutboundEventsPublisher(private val features: FeatureSwitches) {

  fun send(event: OutboundHMPPSDomainEvent) {
    if (features.isEnabled(Feature.SNS_ENABLED)) {
      TODO()
    }
  }
}

enum class OutboundEvent(val eventType: String) {
  VIDEO_BOOKING_CREATED("book-a-video-link.video-booking.created") {
    override fun event(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "A new video booking has been created in the book a video link service",
      )
  },
  ;

  abstract fun event(additionalInformation: AdditionalInformation): OutboundHMPPSDomainEvent
}

interface AdditionalInformation

data class OutboundHMPPSDomainEvent(
  val eventType: String,
  val additionalInformation: AdditionalInformation,
  val version: String = "1",
  val description: String,
  val occurredAt: LocalDateTime = LocalDateTime.now(),
)
