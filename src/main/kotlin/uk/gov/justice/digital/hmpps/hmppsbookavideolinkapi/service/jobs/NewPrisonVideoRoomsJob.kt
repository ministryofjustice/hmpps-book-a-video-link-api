package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.administration.AdministrationEmailService

/**
 * The purpose of this job is to notify administration users via email of any new prison video link rooms.
 *
 * A prison video room is considered new if it is not yet decorated, e.g., no room link or room permissions.
 */
@Component
class NewPrisonVideoRoomsJob(private val administrationEmailService: AdministrationEmailService) :
  JobDefinition(
    JobType.NEW_PRISON_VIDEO_ROOM,
    block = administrationEmailService::sendEmailsForNewPrisonVideoRoom,
  )
