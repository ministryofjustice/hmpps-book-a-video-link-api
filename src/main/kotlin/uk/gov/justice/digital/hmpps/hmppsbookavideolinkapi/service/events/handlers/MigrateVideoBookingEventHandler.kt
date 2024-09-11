package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.whereaboutsapi.WhereaboutsApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration.MigrateVideoBookingService

@Component
class MigrateVideoBookingEventHandler(
  private val videoBookingRepository: VideoBookingRepository,
  private val whereaboutsApiClient: WhereaboutsApiClient,
  private val migrateVideoBookingService: MigrateVideoBookingService,
  private val telemetryService: TelemetryService,
) :
  DomainEventHandler<MigrateVideoBookingEvent> {
  override fun handle(event: MigrateVideoBookingEvent) {
    if (bookingToMigrateDoesNotAlreadyExist(event)) {
      runCatching {
        val bookingToMigrate =
          whereaboutsApiClient.findBookingDetails(event.additionalInformation.videoBookingId)
            ?: throw NullPointerException(
              "Video booking ${event.additionalInformation.videoBookingId} not found in whereabouts-api",
            )

        migrateVideoBookingService.migrate(bookingToMigrate)
      }
        .onFailure {
          // TODO temporary telemetry details
          telemetryService.capture("Failed migration of booking ${event.additionalInformation.videoBookingId} from whereabouts-api")
        }
        .onSuccess {
          // TODO temporary telemetry details
          telemetryService.capture("Succeeded migration of booking ${event.additionalInformation.videoBookingId} from whereabouts-api")
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
