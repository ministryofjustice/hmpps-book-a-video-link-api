package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

@Service
class CreateVideoBookingService(
  private val courtRepository: CourtRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val probationTeamRepository: ProbationTeamRepository,
  private val videoBookingRepository: VideoBookingRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun create(booking: CreateVideoBookingRequest): VideoBooking =
    when (booking.bookingType!!) {
      BookingType.COURT -> createCourt(booking)
      BookingType.PROBATION -> createProbation(booking)
    }

  private fun createCourt(request: CreateVideoBookingRequest): VideoBooking {
    // TODO consider what validation may be needed e.g. duplicate, room in already in use etc.

    val court = courtRepository.findById(request.courtId!!).orElseThrow { EntityNotFoundException("Court with ID ${request.courtId} not found") }
    val prisoner = request.prisoner().let { prisonerSearchClient.getPrisonerAtPrison(it.prisonerNumber!!, it.prisonCode!!) }

    val newCourtBooking = VideoBooking.court(
      court = court,
      hearingType = request.courtHearingType!!.name,
      videoUrl = request.videoLinkUrl,
      createdBy = "TBD",
    )

    log.info("TODO - create the appointments linked to the booking.")

    val persistedBooking = videoBookingRepository.save(newCourtBooking)

    return persistedBooking
  }

  private fun createProbation(request: CreateVideoBookingRequest): VideoBooking {
    // TODO consider what validation may be needed e.g. duplicate, room in already in use etc.

    val probationTeam = probationTeamRepository.findById(request.probationTeamId!!).orElseThrow { EntityNotFoundException("Probation team with ID ${request.probationTeamId} not found") }
    val prisoner = request.prisoner().let { prisonerSearchClient.getPrisonerAtPrison(it.prisonerNumber!!, it.prisonCode!!) }

    val newProbationTeamBooking = VideoBooking.probation(
      probationTeam = probationTeam,
      probationMeetingType = request.probationMeetingType!!.name,
      videoUrl = request.videoLinkUrl,
      createdBy = "TBD",
    )

    log.info("TODO - create the appointments linked to the booking.")

    val persistedBooking = videoBookingRepository.save(newProbationTeamBooking)

    return persistedBooking
  }

  // We will only be creating appointments for one single prisoner as part of the initial rollout.
  private fun CreateVideoBookingRequest.prisoner() = prisoners.first()
}
