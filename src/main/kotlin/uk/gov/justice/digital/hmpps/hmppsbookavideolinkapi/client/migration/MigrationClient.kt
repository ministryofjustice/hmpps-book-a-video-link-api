package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisMappingClient
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
  private val whereaboutsApiWebClient: WebClient,
  private val prisonApiWebClient: WebClient,
  private val nomisMappingClient: NomisMappingClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  // Unsure how effective this will be as requests will spread out over the pods unless we reduce number of pods.
  @Cacheable(CacheConfiguration.MIGRATION_PRISONERS_CACHE_NAME)
  fun getPrisonerByBookingId(bookingId: Long): Prisoner? =
    prisonApiWebClient
      .get()
      .uri("/api/bookings/{bookingId}?basicInfo=true", bookingId)
      .retrieve()
      .bodyToMono(Prisoner::class.java)
      .doOnError { error -> log.info("Error looking up prisoner by booking id $bookingId in migration client", error) }
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()

  data class Prisoner(val offenderNo: String)

  // Unsure how effective this will be as requests will spread out over the pods unless we reduce number of pods.
  @Cacheable(CacheConfiguration.MIGRATION_LOCATIONS_CACHE_NAME)
  fun getLocationIdByInternalId(id: Long) = nomisMappingClient.getNomisLocationMappingBy(id)?.dpsLocationId

  fun findBookingToMigrate(videoBookingId: Long): VideoBookingMigrateResponse? = whereaboutsApiWebClient
    .get()
    .uri("/migrate/video-link-booking/{videoBookingId}", videoBookingId)
    .retrieve()
    .bodyToMono(VideoBookingMigrateResponse::class.java)
    .doOnError { error -> log.info("Error looking up booking to migrate by booking id $videoBookingId in migration client", error) }
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
) {
  fun isOtherCourt() = courtCode != null && courtCode.uppercase() == "OTHER"
}

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
