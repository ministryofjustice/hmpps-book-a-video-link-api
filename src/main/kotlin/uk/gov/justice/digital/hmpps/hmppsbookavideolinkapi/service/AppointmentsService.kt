package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isTimesOverlap
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository

@Service
class AppointmentsService(
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val prisonRepository: PrisonRepository,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
  private val locationValidator: LocationValidator,
) {
  // TODO: Assumes one person per booking, so revisit for co-defendant cases
  fun createAppointmentsForCourt(videoBooking: VideoBooking, prisoner: PrisonerDetails, user: User) {
    checkCourtAppointments(prisoner.appointments, prisoner.prisonCode!!, user)

    // We don't need to do a check for non-prison users as this is covered by the overlapping appointment check.
    if (user is PrisonUser) {
      prisoner.checkForDuplicateAppointment(AppointmentType.VLB_COURT_MAIN)
    }

    val prison = prisonRepository.findByCode(prisoner.prisonCode)
      ?.also { it.rejectIfCannotSelfServeAtPrisonFor(user) }
      ?: throw EntityNotFoundException("Prison with code ${prisoner.prisonCode} not found")

    val locations = locationsInsidePrisonClient.getLocationsByKeys(prisoner.appointments.mapNotNull { it.locationKey }.toSet())

    // Add all appointments to the booking - they will be saved when the booking is saved
    prisoner.appointments.forEach {
      videoBooking.addAppointment(
        prison = prison,
        prisonerNumber = prisoner.prisonerNumber!!,
        appointmentType = it.type!!.name,
        date = it.date!!,
        startTime = it.startTime!!,
        endTime = it.endTime!!,
        locationId = locations.single { location -> location.key == it.locationKey }.id,
      )
    }
  }

  fun checkCourtAppointments(appointments: List<Appointment>, prisonCode: String, user: User) {
    val locations = locationValidator.validatePrisonLocations(prisonCode, appointments.mapNotNull { it.locationKey }.toSet())

    appointments.checkCourtAppointmentTypesOnly()
    appointments.checkSuppliedCourtAppointmentDateAndTimesDoNotOverlap()

    // Prison users can have overlapping appointments
    if (user !is PrisonUser) {
      appointments.checkExistingCourtAppointmentDateAndTimesDoNotOverlap(prisonCode, locations)
    }
  }

  private fun List<Appointment>.checkCourtAppointmentTypesOnly() {
    require(
      size <= 3 &&
        count { it.type == AppointmentType.VLB_COURT_PRE } <= 1 &&
        count { it.type == AppointmentType.VLB_COURT_POST } <= 1 &&
        count { it.type == AppointmentType.VLB_COURT_MAIN } == 1 &&
        all { it.type!!.isCourt },
    ) {
      "Court bookings can only have one pre hearing, one hearing and one post hearing."
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

  private fun List<Appointment>.checkExistingCourtAppointmentDateAndTimesDoNotOverlap(prisonCode: String, locations: List<Location>) {
    forEach { newAppointment ->
      val locationId = locations.single { it.key == newAppointment.locationKey }.id

      prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(
        prisonCode,
        locationId,
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

  fun createAppointmentForProbation(videoBooking: VideoBooking, prisoner: PrisonerDetails, user: User) {
    checkProbationAppointments(prisoner.appointments, prisoner.prisonCode!!, user)

    // We don't need to do a check for non-prison users as this is covered by the overlapping appointment check.
    if (user is PrisonUser) {
      prisoner.checkForDuplicateAppointment(AppointmentType.VLB_PROBATION)
    }

    val prison = prisonRepository.findByCode(prisoner.prisonCode)
      ?.also { it.rejectIfCannotSelfServeAtPrisonFor(user) }
      ?: throw EntityNotFoundException("Prison with code ${prisoner.prisonCode} not found")

    with(prisoner.appointments.single()) {
      val location = locationsInsidePrisonClient.getLocationByKey(this.locationKey!!)

      videoBooking.addAppointment(
        prison = prison,
        prisonerNumber = prisoner.prisonerNumber!!,
        appointmentType = this.type!!.name,
        date = this.date!!,
        startTime = this.startTime!!,
        endTime = this.endTime!!,
        locationId = location!!.id,
      )
    }
  }

  private fun Prison.rejectIfCannotSelfServeAtPrisonFor(user: User) {
    if (user !is PrisonUser) require(enabled) { "Prison with code $code is not enabled for self service" }
  }

  fun checkProbationAppointments(appointments: List<Appointment>, prisonCode: String, user: User) {
    with(appointments.single()) {
      require(type!!.isProbation) {
        "Appointment type $type is not valid for probation appointments"
      }

      val location = locationValidator.validatePrisonLocation(prisonCode, this.locationKey!!)

      // Prison users can have overlapping appointments
      if (user !is PrisonUser) {
        checkExistingProbationAppointmentDateAndTimesDoNotOverlap(prisonCode, location)
      }
    }
  }

  private fun PrisonerDetails.checkForDuplicateAppointment(appointmentType: AppointmentType) {
    val appointment = appointments.single { it.type == appointmentType }
    val location = locationsInsidePrisonClient.getLocationByKey(appointment.locationKey!!)

    if (prisonAppointmentRepository.existsActivePrisonAppointmentsByPrisonerNumberLocationDateAndTime(
        prisonCode = prisonCode!!,
        prisonerNumber = prisonerNumber!!,
        date = appointment.date!!,
        startTime = appointment.startTime!!,
        endTime = appointment.endTime!!,
        prisonLocationId = location!!.id,
      )
    ) {
      throw IllegalArgumentException("Duplicate appointment requested for prisoner $prisonerNumber")
    }
  }

  private fun Appointment.checkExistingProbationAppointmentDateAndTimesDoNotOverlap(prisonCode: String, location: Location) {
    prisonAppointmentRepository.findActivePrisonAppointmentsAtLocationOnDate(
      prisonCode,
      location.id,
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
