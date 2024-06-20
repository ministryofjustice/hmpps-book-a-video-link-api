package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isTimesOverlap
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toPrisonerDetails

@Service
abstract class CreateVideoAppointmentService(
  private val prisonRepository: PrisonRepository,
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val locationValidator: LocationValidator,
  private val prisonerValidator: PrisonerValidator,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  // TODO: Assumes one person per booking, so revisit for co-defendant cases
  protected fun createAppointmentsForCourt(videoBooking: VideoBooking, prisoner: PrisonerDetails) {
    prisoner.appointments.checkCourtAppointmentTypesOnly()
    prisoner.appointments.checkSuppliedCourtAppointmentDateAndTimesDoNotOverlap()
    prisoner.appointments.checkExistingCourtAppointmentDateAndTimesDoNotOverlap(prisoner.prisonCode!!)
    locationValidator.validatePrisonLocations(prisoner.prisonCode, prisoner.appointments.mapNotNull { it.locationKey }.toSet())

    prisoner.appointments.map {
      PrisonAppointment.newAppointment(
        videoBooking = videoBooking,
        prisonCode = prisoner.prisonCode,
        prisonerNumber = prisoner.prisonerNumber!!,
        appointmentType = it.type!!.name,
        appointmentDate = it.date!!,
        startTime = it.startTime!!,
        endTime = it.endTime!!,
        locationKey = it.locationKey!!,
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

  protected fun PrisonerDetails.validate(): Prisoner {
    // We are not checking if the prison is enabled here as we need to support prison users also. Our UI should not be sending disabled prisons though.
    prisonRepository.findByCode(prisonCode!!) ?: throw EntityNotFoundException("Prison with code $prisonCode not found")

    return prisonerValidator.validatePrisonerAtPrison(prisonerNumber!!, prisonCode).toPrisonerDetails()
  }

  protected fun createAppointmentForProbation(videoBooking: VideoBooking, prisoner: PrisonerDetails) {
    val appointment = with(prisoner.appointments.single()) {
      require(type!!.isProbation) {
        "Appointment type $type is not valid for probation appointments"
      }

      checkExistingProbationAppointmentDateAndTimesDoNotOverlap(prisoner.prisonCode!!)
      locationValidator.validatePrisonLocation(prisoner.prisonCode, this.locationKey!!)

      PrisonAppointment.newAppointment(
        videoBooking = videoBooking,
        prisonCode = prisoner.prisonCode,
        prisonerNumber = prisoner.prisonerNumber!!,
        appointmentType = this.type.name,
        appointmentDate = this.date!!,
        startTime = this.startTime!!,
        endTime = this.endTime!!,
        locationKey = this.locationKey,
      )
    }

    prisonAppointmentRepository.saveAndFlush(appointment)
  }

  protected fun deletePrisonAppointments(videoBooking: VideoBooking) = prisonAppointmentRepository.deletePrisonAppointmentsByVideoBooking(videoBooking)

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
}
