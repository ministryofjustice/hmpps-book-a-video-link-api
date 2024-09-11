package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.whereaboutsapi

import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class WhereaboutsApiClient {
  fun findBookingDetails(videoBookingId: Long): VideoBookingMigrateResponse? = TODO()
}

// TODO temporary placeholder type until we have something more concrete
@Deprecated(message = "temporary placeholder type until we have something more concrete")
data class VideoBookingMigrateResponse(
  val offenderBookingId: Long,
  val courtCode: String? = null,
  val probationTeamCode: String? = null,
  val prisonCode: String,
  val madeByTheCourt: Boolean,
  val createdBy: String,
  val comments: String? = null,
  val events: List<VideoBookingEvent>,
)

data class VideoBookingEvent(
  val eventId: Long,
  val eventTime: LocalDateTime,
  val eventType: EventType,
  val createdByUsername: String,
  val prisonCode: String?,
  val courtCode: String?,
  val courtName: String?,
  val madeByTheCourt: Boolean,
  val comment: String?,
  val mainLocationId: Long,
  val mainStartTime: LocalDateTime?,
  val mainEndTime: LocalDateTime?,
  val preLocationId: Long?,
  val preStartTime: LocalDateTime?,
  val preEndTime: LocalDateTime?,
  val postLocationId: Long?,
  val postStartTime: LocalDateTime?,
  val postEndTime: LocalDateTime?,
)

enum class EventType {
  CREATE,
  UPDATE,
  DELETE,
}
