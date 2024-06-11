package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import java.time.LocalDateTime

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
data class VideoBookingCreatedInformation(val videoBookingId: Long) : AdditionalInformation

interface AdditionalInformation

data class OutboundHMPPSDomainEvent(
  val eventType: String,
  val additionalInformation: AdditionalInformation,
  val version: String = "1",
  val description: String,
  val occurredAt: LocalDateTime = LocalDateTime.now(),
)
