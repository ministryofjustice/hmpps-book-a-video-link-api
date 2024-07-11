package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isTimesOverlap
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository

@Service
class AppointmentsService(
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val locationValidator: LocationValidator,
) {
  // TODO: Assumes one person per booking, so revisit for co-defendant cases
  fun createAppointmentsForCourt(videoBooking: VideoBooking, prisoner: PrisonerDetails) {
    checkCourtAppointments(prisoner.appointments, prisoner.prisonCode!!)

    // Add all appointments to the booking - they will be saved when the booking is saved
    prisoner.appointments.forEach {
      videoBooking.addAppointment(
        prisonCode = prisoner.prisonCode,
        prisonerNumber = prisoner.prisonerNumber!!,
        appointmentType = it.type!!.name,
        date = it.date!!,
        startTime = it.startTime!!,
        endTime = it.endTime!!,
        locationKey = it.locationKey!!,
      )
    }
  }

  fun checkCourtAppointments(appointments: List<Appointment>, prisonCode: String) {
    appointments.checkCourtAppointmentTypesOnly()
    appointments.checkSuppliedCourtAppointmentDateAndTimesDoNotOverlap()
    appointments.checkExistingCourtAppointmentDateAndTimesDoNotOverlap(prisonCode)
    locationValidator.validatePrisonLocations(prisonCode, appointments.mapNotNull { it.locationKey }.toSet())
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
      prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(
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

  fun createAppointmentForProbation(videoBooking: VideoBooking, prisoner: PrisonerDetails) {
    checkProbationAppointments(prisoner.appointments, prisoner.prisonCode!!)

    with(prisoner.appointments.single()) {
      videoBooking.addAppointment(
        prisonCode = prisoner.prisonCode,
        prisonerNumber = prisoner.prisonerNumber!!,
        appointmentType = this.type!!.name,
        date = this.date!!,
        startTime = this.startTime!!,
        endTime = this.endTime!!,
        locationKey = this.locationKey!!,
      )
    }
  }

  fun checkProbationAppointments(appointments: List<Appointment>, prisonCode: String) {
    with(appointments.single()) {
      require(type!!.isProbation) {
        "Appointment type $type is not valid for probation appointments"
      }

      checkExistingProbationAppointmentDateAndTimesDoNotOverlap(prisonCode)
      locationValidator.validatePrisonLocation(prisonCode, this.locationKey!!)
    }
  }

  private fun Appointment.checkExistingProbationAppointmentDateAndTimesDoNotOverlap(prisonCode: String) {
    prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(
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
}
