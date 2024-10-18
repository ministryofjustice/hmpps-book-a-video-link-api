package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.MigrationClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.StandardTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration.MigrateVideoBookingService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration.MigrationException

@Component
@Deprecated(message = "Can be removed when migration is completed")
class MigrateVideoBookingEventHandler(
  private val videoBookingRepository: VideoBookingRepository,
  private val migrationClient: MigrationClient,
  private val migrateVideoBookingService: MigrateVideoBookingService,
  private val telemetryService: TelemetryService,
) :
  DomainEventHandler<MigrateVideoBookingEvent> {

  override fun handle(event: MigrateVideoBookingEvent) {
    if (bookingToMigrateDoesNotAlreadyExist(event)) {
      runCatching {
        val bookingToMigrate =
          migrationClient.findBookingToMigrate(event.additionalInformation.videoLinkBookingId)
            ?: throw MigrationException("booking not found in whereabouts-api")

        migrateVideoBookingService.migrate(bookingToMigrate)
      }
        .onFailure {
          telemetryService.track(MigratedBookingFailureStandardTelemetryEvent(event.additionalInformation.videoLinkBookingId, message = it.message))

          if (it !is MigrationException) {
            throw it
          }
        }
        .onSuccess {
          telemetryService.track(
            MigratedBookingSuccessStandardTelemetryEvent(
              videoBookingId = it.videoBookingId,
              migratedVideoBookingId = it.migratedVideoBookingId!!,
              court = it.court?.code ?: it.probationTeam?.code!!,
              prison = it.prisonCode(),
            ),
          )
        }
    } else {
      telemetryService.track(MigratedBookingFailureStandardTelemetryEvent(event.additionalInformation.videoLinkBookingId, "booking already migrated"))
    }
  }

  private fun bookingToMigrateDoesNotAlreadyExist(event: MigrateVideoBookingEvent) =
    videoBookingRepository.existsByMigratedVideoBookingId(event.additionalInformation.videoLinkBookingId).not()
}

class MigrateVideoBookingEvent(additionalInformation: VideoLinkBookingMigrate) :
  DomainEvent<VideoLinkBookingMigrate>(DomainEventType.MIGRATE_VIDEO_BOOKING, additionalInformation) {
  constructor(id: Long) : this(VideoLinkBookingMigrate(id))
}
data class VideoLinkBookingMigrate(val videoLinkBookingId: Long) : AdditionalInformation

class MigratedBookingFailureStandardTelemetryEvent(
  private val videoBookingId: Long,
  private val message: String? = null,
) : StandardTelemetryEvent("BVLS-migrated-booking-failure") {
  override fun properties(): Map<String, String> =
    mapOf(
      "video_booking_id" to videoBookingId.toString(),
      "message" to "".plus(message),
    )
}

class MigratedBookingSuccessStandardTelemetryEvent(
  val videoBookingId: Long,
  val migratedVideoBookingId: Long,
  val court: String,
  val prison: String,
) : StandardTelemetryEvent("BVLS-migrated-booking-success") {
  override fun properties(): Map<String, String> = mapOf(
    "video_booking_id" to videoBookingId.toString(),
    "migrated_video_booking_id" to migratedVideoBookingId.toString(),
    "court" to court,
    "prison" to prison,
  )
}
