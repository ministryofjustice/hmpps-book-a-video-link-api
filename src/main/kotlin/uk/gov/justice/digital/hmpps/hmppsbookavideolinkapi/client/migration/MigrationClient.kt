package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.CacheConfiguration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * This is grouping calls to numerous web clients as these endpoints are only relevant to the migration process.
 *
 * When the migration is done this class can be removed in its entirety a long with other migration related classes and
 * files.
 */
@Component
class MigrationClient(
  private val locationsInsidePrisonApiWebClient: WebClient,
  private val whereaboutsApiWebClient: WebClient,
  private val nomisMappingApiWebClient: WebClient,
) {

  // Unsure how effective this will be as requests will spread out over the pods unless we reduce number of pods.
  @Cacheable(CacheConfiguration.LOCATIONS_BY_INTERNAL_ID)
  fun getLocationByInternalId(id: Long): Location? =
    // We have to do two trips to get the location, the NOMIS mapping service followed by the locations API.
    getNomisLocationMappingBy(id)?.let { getLocationByDpsId(it.dpsLocationId) }

  private fun getNomisLocationMappingBy(internalLocationId: Long): NomisDpsLocationMapping? = nomisMappingApiWebClient
    .get()
    .uri("/api/locations/nomis/{id}", internalLocationId)
    .retrieve()
    .bodyToMono(NomisDpsLocationMapping::class.java)
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()

  private fun getLocationByDpsId(id: String): Location? = locationsInsidePrisonApiWebClient
    .get()
    .uri("/locations/locations/{id}", id)
    .retrieve()
    .bodyToMono(Location::class.java)
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()

  fun findBookingToMigrate(videoBookingId: Long): VideoBookingMigrateResponse? = whereaboutsApiWebClient
    .get()
    .uri("/migrate/video-link-booking/{videoBookingId}", videoBookingId)
    .retrieve()
    .bodyToMono(VideoBookingMigrateResponse::class.java)
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()
}

// TODO temporary placeholder type until we the complated response object from the whereabouts-api
@Deprecated(message = "temporary placeholder type until we the complated response object from the whereabouts-api")
data class VideoBookingMigrateResponse(
  val videoBookingId: Long,
  val offenderBookingId: Long,
  val courtCode: String,
  val probation: Boolean,
  val cancelled: Boolean,
  val prisonCode: String,
  val madeByTheCourt: Boolean,
  val createdBy: String,
  val comments: String? = null,
  val main: AppointmentLocationTimeSlot,
  val pre: AppointmentLocationTimeSlot? = null,
  val post: AppointmentLocationTimeSlot? = null,
  val events: List<VideoBookingEvent>,
)

data class VideoBookingEvent(
  val eventId: Long,
  val eventTime: LocalDateTime,
  val eventType: VideoLinkBookingEventType,
  val createdByUsername: String,
  val prisonCode: String?,
  val courtCode: String?,
  val courtName: String?,
  val madeByTheCourt: Boolean,
  val comment: String?,
  val mainLocationId: Long,
  val mainStartTime: LocalDateTime,
  val mainEndTime: LocalDateTime,
  val preLocationId: Long?,
  val preStartTime: LocalDateTime?,
  val preEndTime: LocalDateTime?,
  val postLocationId: Long?,
  val postStartTime: LocalDateTime?,
  val postEndTime: LocalDateTime?,
)

fun VideoBookingMigrateResponse.cancelledAt(): LocalDateTime? =
  if (cancelled) {
    events
      .sortedBy(VideoBookingEvent::eventId)
      .last { it.eventType == VideoLinkBookingEventType.DELETE }
      .let(VideoBookingEvent::eventTime)
  } else {
    null
  }

fun VideoBookingMigrateResponse.cancelledBy(): String? =
  if (cancelled) {
    events
      .sortedBy(VideoBookingEvent::eventId)
      .last { it.eventType == VideoLinkBookingEventType.DELETE }
      .let(VideoBookingEvent::createdByUsername)
  } else {
    null
  }

fun VideoBookingEvent.preAppointment(): AppointmentLocationTimeSlot? =
  preLocationId?.let {
    AppointmentLocationTimeSlot(
      locationId = preLocationId,
      date = preStartTime!!.toLocalDate(),
      startTime = preStartTime.toLocalTime(),
      endTime = preEndTime!!.toLocalTime(),
    )
  }

fun VideoBookingEvent.mainAppointment(): AppointmentLocationTimeSlot =
  AppointmentLocationTimeSlot(
    locationId = mainLocationId,
    date = mainStartTime.toLocalDate(),
    startTime = mainStartTime.toLocalTime(),
    endTime = mainEndTime.toLocalTime(),
  )

fun VideoBookingEvent.postAppointment(): AppointmentLocationTimeSlot? =
  postLocationId?.let {
    AppointmentLocationTimeSlot(
      locationId = postLocationId,
      date = postStartTime!!.toLocalDate(),
      startTime = postStartTime.toLocalTime(),
      endTime = postEndTime!!.toLocalTime(),
    )
  }

enum class VideoLinkBookingEventType {
  CREATE,
  UPDATE,
  DELETE,
}

class AppointmentLocationTimeSlot(
  val locationId: Long,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
)

data class NomisDpsLocationMapping(
  val dpsLocationId: String,
  val nomisLocationId: Long,
)
