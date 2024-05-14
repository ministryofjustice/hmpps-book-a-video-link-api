package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

@Service
class CreateVideoBookingService(
  private val courtRepository: CourtRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val probationTeamRepository: ProbationTeamRepository,
  private val videoBookingRepository: VideoBookingRepository,
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
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
    request.prisoner().let { prisonerSearchClient.getPrisonerAtPrison(it.prisonerNumber!!, it.prisonCode!!) }

    return VideoBooking.newCourtBooking(
      court = court,
      hearingType = request.courtHearingType!!.name,
      videoUrl = request.videoLinkUrl,
      createdBy = "TBD",
    ).let(videoBookingRepository::saveAndFlush).also { booking -> createAppointmentsForCourt(booking, request.prisoner()) }
  }

  private fun createAppointmentsForCourt(videoBooking: VideoBooking, prisoner: PrisonerDetails) {
    // TODO to be implemented
  }

  private fun createProbation(request: CreateVideoBookingRequest): VideoBooking {
    // TODO consider what validation may be needed e.g. duplicate, room in already in use etc.

    val probationTeam = probationTeamRepository.findById(request.probationTeamId!!).orElseThrow { EntityNotFoundException("Probation team with ID ${request.probationTeamId} not found") }
    request.prisoner().let { prisonerSearchClient.getPrisonerAtPrison(it.prisonerNumber!!, it.prisonCode!!) }

    return VideoBooking.newProbationBooking(
      probationTeam = probationTeam,
      probationMeetingType = request.probationMeetingType!!.name,
      videoUrl = request.videoLinkUrl,
      createdBy = "TBD",
    ).let(videoBookingRepository::saveAndFlush).also { booking -> createAppointmentForProbation(booking, request.prisoner()) }
  }

  private fun createAppointmentForProbation(videoBooking: VideoBooking, prisoner: PrisonerDetails) {
    // TODO check location key against the locations API

    val appointment = with(prisoner.appointments.single()) {
      require(type!!.isProbation) {
        "Appointment type $type is not valid for probation appointments"
      }

      PrisonAppointment.newAppointment(
        videoBooking = videoBooking,
        prisonCode = prisoner.prisonCode!!,
        prisonerNumber = prisoner.prisonerNumber!!,
        appointmentType = this.type.name,
        appointmentDate = this.date!!,
        startTime = this.startTime!!,
        endTime = this.endTime!!,
        locationKey = this.locationKey!!,
        createdBy = "TBD",
      )
    }

    prisonAppointmentRepository.saveAndFlush(appointment)
  }

  // We will only be creating appointments for one single prisoner as part of the initial rollout.
  private fun CreateVideoBookingRequest.prisoner() = prisoners.first()
}
