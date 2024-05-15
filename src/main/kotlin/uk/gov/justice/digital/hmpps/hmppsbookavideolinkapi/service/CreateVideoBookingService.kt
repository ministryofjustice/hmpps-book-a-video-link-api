package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isTimesOverlap
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
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
    // TODO need to check locations against locations API

    prisoner.appointments.checkCourtAppointmentTypesOnly()
    prisoner.appointments.checkSuppliedCourtAppointmentDateAndTimesDoNotOverlap()
    prisoner.appointments.checkExistingCourtAppointmentDateAndTimesDoNotOverlap(prisoner.prisonCode!!)

    prisoner.appointments.map {
      PrisonAppointment.newAppointment(
        videoBooking = videoBooking,
        prisonCode = prisoner.prisonCode!!,
        prisonerNumber = prisoner.prisonerNumber!!,
        appointmentType = it.type!!.name,
        appointmentDate = it.date!!,
        startTime = it.startTime!!,
        endTime = it.endTime!!,
        locationKey = it.locationKey!!,
        createdBy = "TBD",
      )
    }.forEach(prisonAppointmentRepository::saveAndFlush)
  }

  private fun List<Appointment>.checkCourtAppointmentTypesOnly() {
    require(
      size <= 3 &&
        count { it.type == AppointmentType.VLB_COURT_PRE } <= 1 &&
        count { it.type == AppointmentType.VLB_COURT_POST } <= 1 &&
        count { it.type == AppointmentType.VLB_COURT_MAIN } == 1 &&
        all { it.type!!.isCourt },
    ) {
      "Court bookings can only have one pre-conference, one hearing and one post-conference."
    }
  }

  private fun List<Appointment>.checkSuppliedCourtAppointmentDateAndTimesDoNotOverlap() {
    val pre = singleOrNull { it.type == AppointmentType.VLB_COURT_PRE }
    val hearing = single { it.type == AppointmentType.VLB_COURT_MAIN }
    val post = singleOrNull { it.type == AppointmentType.VLB_COURT_POST }

    require((pre == null || pre.isBefore(hearing)) && (post == null || hearing.isBefore(post))) {
      "Requested court booking appointments must not overlap."
    }
  }

  private fun List<Appointment>.checkExistingCourtAppointmentDateAndTimesDoNotOverlap(prisonCode: String) {
    forEach { newAppointment ->
      prisonAppointmentRepository.findByPrisonCodeAndPrisonLocKeyAndAppointmentDate(
        prisonCode,
        newAppointment.locationKey!!,
        newAppointment.date!!,
      ).forEach { existingAppointment ->
        require(
          !isTimesOverlap(
            newAppointment.startTime!!,
            newAppointment.endTime!!,
            existingAppointment.startTime,
            existingAppointment.endTime,
          ),
        ) {
          "One or more requested court appointments overlaps with an existing appointment at location ${newAppointment.locationKey}"
        }
      }
    }
  }

  private fun Appointment.isBefore(other: Appointment): Boolean =
    this.date!! <= other.date && this.endTime!! <= other.startTime

  private fun createProbation(request: CreateVideoBookingRequest): VideoBooking {
    // TODO need to check locations against locations API

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
    val appointment = with(prisoner.appointments.single()) {
      require(type!!.isProbation) {
        "Appointment type $type is not valid for probation appointments"
      }

      checkExistingProbationAppointmentDateAndTimesDoNotOverlap(prisoner.prisonCode!!)

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

  private fun Appointment.checkExistingProbationAppointmentDateAndTimesDoNotOverlap(prisonCode: String) {
    prisonAppointmentRepository.findByPrisonCodeAndPrisonLocKeyAndAppointmentDate(
      prisonCode,
      locationKey!!,
      date!!,
    ).forEach { existingAppointment ->
      require(
        !isTimesOverlap(
          startTime!!,
          endTime!!,
          existingAppointment.startTime,
          existingAppointment.endTime,
        ),
      ) {
        "Requested probation appointment overlaps with an existing appointment at location $locationKey"
      }
    }
  }

  // We will only be creating appointments for one single prisoner as part of the initial rollout.
  private fun CreateVideoBookingRequest.prisoner() = prisoners.first()
}
