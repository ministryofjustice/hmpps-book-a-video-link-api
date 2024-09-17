package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.typeReference
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
@Deprecated(message = "Can be removed when migration is completed")
class MigrationClient(
  private val prisonerSearchApiWebClient: WebClient,
  private val locationsInsidePrisonApiWebClient: WebClient,
  private val whereaboutsApiWebClient: WebClient,
  private val nomisMappingApiWebClient: WebClient,
) {
  fun getPrisoner(bookingId: Long): Prisoner? =
    prisonerSearchApiWebClient.post()
      .uri("/prisoner-search/booking-ids")
      .bodyValue(BookingIds(listOf(bookingId)))
      .retrieve()
      .bodyToMono(typeReference<List<Prisoner>>())
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()?.singleOrNull()

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

data class VideoBookingMigrateResponse(
  val videoBookingId: Long,
  val offenderBookingId: Long,
  val courtCode: String?,
  val courtName: String?,
  val madeByTheCourt: Boolean,
  val createdByUsername: String,
  val prisonCode: String,
  val probation: Boolean,
  val cancelled: Boolean,
  val comment: String?,
  val pre: AppointmentLocationTimeSlot?,
  val main: AppointmentLocationTimeSlot,
  val post: AppointmentLocationTimeSlot?,
  val events: List<VideoBookingMigrateEvent> = emptyList(),
)

data class VideoBookingMigrateEvent(
  val eventId: Long,
  val eventTime: LocalDateTime,
  val eventType: VideoLinkBookingEventType,
  val createdByUsername: String,
  val prisonCode: String,
  val courtCode: String?,
  val courtName: String?,
  val madeByTheCourt: Boolean,
  val comment: String?,
  val pre: AppointmentLocationTimeSlot?,
  val main: AppointmentLocationTimeSlot,
  val post: AppointmentLocationTimeSlot?,
)

data class AppointmentLocationTimeSlot(
  val locationId: Long,
  val date: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
)

fun VideoBookingMigrateResponse.createdAt() =
  events.single { it.eventType == VideoLinkBookingEventType.CREATE }.eventTime

fun VideoBookingMigrateResponse.updatedAt() =
  events.sortedBy(VideoBookingMigrateEvent::eventId).lastOrNull { it.eventType != VideoLinkBookingEventType.CREATE }?.eventTime

fun VideoBookingMigrateResponse.updatedBy() =
  events.sortedBy(VideoBookingMigrateEvent::eventId).lastOrNull { it.eventType != VideoLinkBookingEventType.CREATE }?.createdByUsername

fun VideoBookingMigrateResponse.cancelledAt(): LocalDateTime? =
  if (cancelled) {
    events
      .sortedBy(VideoBookingMigrateEvent::eventId)
      .last { it.eventType == VideoLinkBookingEventType.DELETE }
      .let(VideoBookingMigrateEvent::eventTime)
  } else {
    null
  }

fun VideoBookingMigrateResponse.cancelledBy(): String? =
  if (cancelled) {
    events
      .sortedBy(VideoBookingMigrateEvent::eventId)
      .last { it.eventType == VideoLinkBookingEventType.DELETE }
      .let(VideoBookingMigrateEvent::createdByUsername)
  } else {
    null
  }

enum class VideoLinkBookingEventType {
  CREATE,
  UPDATE,
  DELETE,
}

data class NomisDpsLocationMapping(
  val dpsLocationId: String,
  val nomisLocationId: Long,
)

data class BookingIds(val bookingIds: List<Long>)
