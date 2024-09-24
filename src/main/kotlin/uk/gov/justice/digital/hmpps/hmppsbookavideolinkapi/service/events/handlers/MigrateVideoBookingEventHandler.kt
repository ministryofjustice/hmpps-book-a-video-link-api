package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.MigrationClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TelemetryEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration.MigrateVideoBookingService

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
          migrationClient.findBookingToMigrate(event.additionalInformation.videoBookingId)
            ?: throw NullPointerException(
              "Video booking ${event.additionalInformation.videoBookingId} not found in whereabouts-api",
            )

        migrateVideoBookingService.migrate(bookingToMigrate)
      }
        .onFailure {
          telemetryService.track(MigratedBookingFailureTelemetryEvent(event.additionalInformation.videoBookingId))
        }
        .onSuccess {
          telemetryService.track(
            MigratedBookingSuccessTelemetryEvent(
              videoBookingId = it.videoBookingId,
              migratedVideoBookingId = it.migratedVideoBookingId!!,
              court = it.court?.code ?: it.probationTeam?.code!!,
              prison = it.prisonCode(),
            ),
          )
        }.getOrThrow()
    }
  }

  private fun bookingToMigrateDoesNotAlreadyExist(event: MigrateVideoBookingEvent) =
    videoBookingRepository.existsByMigratedVideoBookingId(event.additionalInformation.videoBookingId).not()
}

class MigrateVideoBookingEvent(additionalInformation: VideoBookingInformation) :
  DomainEvent<VideoBookingInformation>(DomainEventType.MIGRATE_VIDEO_BOOKING, additionalInformation) {
  constructor(id: Long) : this(VideoBookingInformation(id))
}

class MigratedBookingFailureTelemetryEvent(
  private val videoBookingId: Long,
) : TelemetryEvent(TelemetryEventType.MIGRATED_BOOKING_FAILURE) {
  override fun properties(): Map<String, String> = mapOf("video_booking_id" to videoBookingId.toString())
}

class MigratedBookingSuccessTelemetryEvent(
  val videoBookingId: Long,
  val migratedVideoBookingId: Long,
  val court: String,
  val prison: String,
) : TelemetryEvent(TelemetryEventType.MIGRATED_BOOKING_SUCCESS) {
  override fun properties(): Map<String, String> = mapOf(
    "video_booking_id" to videoBookingId.toString(),
    "migrated_video_booking_id" to migratedVideoBookingId.toString(),
    "court" to court,
    "prison" to prison,
  )
}
