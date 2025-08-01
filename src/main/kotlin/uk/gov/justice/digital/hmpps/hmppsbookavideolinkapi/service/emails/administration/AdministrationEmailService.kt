package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.administration

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService

@Service
class AdministrationEmailService(
  private val prisonsService: PrisonsService,
  private val locationsService: LocationsService,
  private val emailService: EmailService,
  private val notificationRepository: NotificationRepository,
  @Value("\${administration.emails:#{null}}") private val administrationEmails: String? = null,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  init {
    if (administrationEmails.isNullOrBlank()) {
      log.info("ADMINISTRATION_EMAIL: no email address configured for sending administration emails")
    }
  }

  fun sendEmailsForNewPrisonVideoRoom() {
    log.info("ADMINISTRATION_EMAIL: new video link prison room check")

    if (!administrationEmails.isNullOrBlank()) {
      // We are only interested in enabled prisons
      val sortedNewPrisonVideoRooms = prisonsService.getListOfPrisons(true)
        .associateWith { prison ->
          locationsService.getVideoLinkLocationsAtPrison(
            prisonCode = prison.code,
            enabledOnly = true,
          ).allNewPrisonRooms()
        }.mapKeys { it.key.name }.filterValues { rooms -> rooms.isNotEmpty() }.toSortedMap()

      if (sortedNewPrisonVideoRooms.isNotEmpty()) {
        administrationEmails.split(',').map { it.lowercase().trim() }.distinct().forEach { email ->
          emailService.send(AdministrationNewVideoRoomEmail(email, sortedNewPrisonVideoRooms)).onSuccess { (govNotifyId, templateId) ->
            notificationRepository.saveAndFlush(
              Notification(
                email = email,
                govNotifyNotificationId = govNotifyId,
                templateName = templateId,
                reason = "NEW_PRISON_VIDEO_ROOM",
              ),
            )
          }.onFailure {
            log.info("ADMINISTRATION_EMAIL: failed to send email for new prison video room.")
          }
        }
      }
    }
  }

  // Rooms without any extra attributes (not decorated) are considered new
  private fun List<Location>.allNewPrisonRooms() = filter { it.extraAttributes == null }
    .mapNotNull { it.description }.toSortedSet()
}
