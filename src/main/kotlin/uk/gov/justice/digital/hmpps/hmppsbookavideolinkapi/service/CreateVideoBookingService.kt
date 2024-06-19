package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

@Service
class CreateVideoBookingService(
  private val courtRepository: CourtRepository,
  private val probationTeamRepository: ProbationTeamRepository,
  private val videoBookingRepository: VideoBookingRepository,
  prisonAppointmentRepository: PrisonAppointmentRepository,
  prisonRepository: PrisonRepository,
  locationValidator: LocationValidator,
  prisonerValidator: PrisonerValidator,
) : CreateVideoAppointmentService(prisonRepository, prisonAppointmentRepository, locationValidator, prisonerValidator) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun create(booking: CreateVideoBookingRequest, createdBy: String): Pair<VideoBooking, Prisoner> =
    when (booking.bookingType!!) {
      BookingType.COURT -> createCourt(booking, createdBy)
      BookingType.PROBATION -> createProbation(booking, createdBy)
    }

  private fun createCourt(request: CreateVideoBookingRequest, createdBy: String): Pair<VideoBooking, Prisoner> {
    val court = courtRepository.findByCode(request.courtCode!!)
      ?.also { require(it.enabled) { "Court with code ${it.code} is not enabled" } }
      ?: throw EntityNotFoundException("Court with code ${request.courtCode} not found")

    val prisoner = request.prisoner().validate()

    return VideoBooking.newCourtBooking(
      court = court,
      hearingType = request.courtHearingType!!.name,
      comments = request.comments,
      videoUrl = request.videoLinkUrl,
      createdBy = createdBy,
      createdByPrison = request.createdByPrison!!,
    ).let(videoBookingRepository::saveAndFlush)
      .also { booking -> createAppointmentsForCourt(booking, request.prisoner()) }
      .also { log.info("BOOKINGS: court booking ${it.videoBookingId} created") } to prisoner
  }

  private fun createProbation(request: CreateVideoBookingRequest, createdBy: String): Pair<VideoBooking, Prisoner> {
    val probationTeam = probationTeamRepository.findByCode(request.probationTeamCode!!)
      ?.also { require(it.enabled) { "Probation team with code ${it.code} is not enabled" } }
      ?: throw EntityNotFoundException("Probation team with code ${request.probationTeamCode} not found")

    val prisoner = request.prisoner().validate()

    return VideoBooking.newProbationBooking(
      probationTeam = probationTeam,
      probationMeetingType = request.probationMeetingType!!.name,
      comments = request.comments,
      videoUrl = request.videoLinkUrl,
      createdBy = createdBy,
      createdByPrison = request.createdByPrison!!,
    ).let(videoBookingRepository::saveAndFlush)
      .also { booking -> createAppointmentForProbation(booking, request.prisoner()) }
      .also { log.info("BOOKINGS: probation team booking ${it.videoBookingId} created") } to prisoner
  }

  // We will only be creating appointments for one single prisoner as part of the initial rollout.
  private fun CreateVideoBookingRequest.prisoner() = prisoners.first()
}
