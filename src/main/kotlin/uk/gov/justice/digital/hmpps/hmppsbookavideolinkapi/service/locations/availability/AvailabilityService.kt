package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AppointmentSlot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.PROBATION
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailabilityRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Interval
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.LocationAndInterval
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailabilityResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalDate
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Service
@Transactional(readOnly = true)
class AvailabilityService(
  private val videoAppointmentRepository: VideoAppointmentRepository,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
  private val availabilityFinderService: AvailabilityFinderService,
  private val externalAppointmentsService: ExternalAppointmentsService,
  private val videoBookingRepository: VideoBookingRepository,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun isAvailable(request: CreateVideoBookingRequest) = checkAvailability(
    AvailabilityRequest(
      bookingType = request.bookingType,
      courtOrProbationCode = request.courtCode ?: request.probationTeamCode!!,
      prisonCode = request.prisoners.first().prisonCode,
      date = request.prisoners.first().appointments.first().date,
      preAppointment = request.appointment(AppointmentType.VLB_COURT_PRE),
      mainAppointment = request.appointment(AppointmentType.VLB_COURT_MAIN)
        ?: request.appointment(AppointmentType.VLB_PROBATION)!!,
      postAppointment = request.appointment(AppointmentType.VLB_COURT_POST),
    ),
  ).availabilityOk

  private fun CreateVideoBookingRequest.appointment(type: AppointmentType) = prisoners.single().appointments.singleOrNull { it.type == type }
    ?.let { LocationAndInterval(it.dpsLocationId, Interval(it.startTime, it.endTime)) }

  fun isAvailable(videoBookingId: Long, request: AmendVideoBookingRequest): Boolean {
    val existingBooking = videoBookingRepository.findById(videoBookingId).orElseThrow()

    return checkAvailability(
      AvailabilityRequest(
        bookingType = request.bookingType,
        courtOrProbationCode = existingBooking?.court?.code ?: existingBooking?.probationTeam?.code!!,
        prisonCode = request.prisoners.first().prisonCode,
        date = request.prisoners.first().appointments.first().date,
        preAppointment = request.appointment(AppointmentType.VLB_COURT_PRE),
        mainAppointment = request.appointment(AppointmentType.VLB_COURT_MAIN)
          ?: request.appointment(AppointmentType.VLB_PROBATION)!!,
        postAppointment = request.appointment(AppointmentType.VLB_COURT_POST),
        vlbIdToExclude = videoBookingId,
      ),
    ).availabilityOk
  }

  private fun AmendVideoBookingRequest.appointment(type: AppointmentType) = prisoners.single().appointments.singleOrNull { it.type == type }?.let { LocationAndInterval(it.dpsLocationId, Interval(it.startTime, it.endTime)) }

  /**
   * Assumptions:
   *  - current booking journeys for BVLS allow only one person at one prison per booking.
   *  - we consider ONLY the prison locations in the request for alternative times (we do not consider others - yet)
   *  - the decision about whether a user is allowed to book into a location is still the users' own.
   *  - We assume all appointments for this booking are on the same day and at the same prison
   *
   * To consider later:
   *  - Get other VCC-enabled rooms at these prison(s) and offer them?
   *  - Incorporate prison room decoration logic here, to find other rooms?
   */
  fun checkAvailability(request: AvailabilityRequest): AvailabilityResponse {
    log.info("AVAILABILITY CHECK: looking at BVLS and other non-VLB appointment types")

    val requestedLocations = run {
      log.info("AVAILABILITY CHECK: by location ID")

      setOfNotNull(
        request.preAppointment?.dpsLocationId,
        request.postAppointment?.dpsLocationId,
        request.mainAppointment.dpsLocationId,
      ).let { getLocationsByIds(it) }
    }

    log.info("AVAILABILITY CHECK: for the following locations ${requestedLocations.map { it.id }}")

    if (request.isForAnExistingBooking() && request.dateTimeAndLocationIsTheSame(requestedLocations)) {
      return AvailabilityResponse(true)
    }

    // Build a list of AppointmentSlots for existing VLBs from BVLS at these locations on this date
    val (bvlsAppointmentSlotsToInclude: List<AppointmentSlot>, appointmentsToExclude: List<AppointmentSlot>) = videoAppointmentRepository.findVideoAppointmentsAtPrison(
      forDate = request.date,
      forPrison = request.prisonCode,
      forLocationIds = requestedLocations.map { it.id },
    ).partition { vlb -> vlb.videoBookingId != request.vlbIdToExclude }

    // Build list of AppointmentSlot for all external non-VLB appointments for these locations on this date
    val externalAppointmentSlotsToInclude: List<AppointmentSlot> =
      requestedLocations
        .flatMap { externalAppointmentsService.getAppointmentSlots(request.prisonCode, request.date, it.id) }
        .filterNot { external ->
          // We can only match on these values, there is no direct match between a BVLS appointment and what is in the external service.
          appointmentsToExclude.any { internal ->
            internal.prisonerNumber == external.prisonerNumber &&
              internal.appointmentDate == external.appointmentDate &&
              internal.startTime == external.startTime &&
              internal.endTime == external.endTime
          }
        }

    val slotsToCheck = bvlsAppointmentSlotsToInclude.plus(externalAppointmentSlotsToInclude).onEach { slot ->
      log.info("AVAILABILITY CHECK: slot - ${slot.prisonLocationId} - ${slot.prisonerNumber} - ${slot.appointmentDate} - ${slot.startTime} - ${slot.endTime}")
    }

    // Check if the requested times are free, and offer alternatives if not
    return availabilityFinderService.getOptions(request, slotsToCheck, requestedLocations)
  }

  private fun getLocationsByIds(ids: Set<UUID>): List<Location> = ids.mapNotNull { locationsInsidePrisonClient.getLocationById(it) }

  private fun AvailabilityRequest.isForAnExistingBooking() = vlbIdToExclude != null

  private fun AvailabilityRequest.dateTimeAndLocationIsTheSame(requestedLocations: List<Location>): Boolean {
    val mayBeExistingBooking = videoBookingRepository.findById(vlbIdToExclude!!).getOrNull()

    if (mayBeExistingBooking != null) {
      if (mayBeExistingBooking.isBookingType(PROBATION)) {
        return mayBeExistingBooking.probationMeeting()!!.dateTimeAndLocationIsTheSame(
          date,
          mainAppointment.interval,
          requestedLocations.single { it.id == mainAppointment.dpsLocationId },
        )
      } else {
        val preLocation = preAppointment?.let { requestedLocations.singleOrNull { it.id == preAppointment.dpsLocationId } }
        val mainLocation = requestedLocations.single { it.id == mainAppointment.dpsLocationId }
        val postLocation = postAppointment?.let { requestedLocations.singleOrNull { it.id == postAppointment.dpsLocationId } }

        val preHearingIsTheSame = (mayBeExistingBooking.preHearing() == null && preLocation == null) ||
          mayBeExistingBooking.preHearing() != null &&
          mayBeExistingBooking.preHearing()!!.dateTimeAndLocationIsTheSame(date, preAppointment?.interval, preLocation)

        val mainHearingIsTheSame =
          mayBeExistingBooking.mainHearing()!!.dateTimeAndLocationIsTheSame(date, mainAppointment.interval, mainLocation)

        val postHearingIsTheSame = (mayBeExistingBooking.postHearing() == null && postLocation == null) ||
          mayBeExistingBooking.postHearing() != null &&
          mayBeExistingBooking.postHearing()!!.dateTimeAndLocationIsTheSame(date, postAppointment?.interval, postLocation)

        return preHearingIsTheSame && mainHearingIsTheSame && postHearingIsTheSame
      }
    }

    return false
  }

  private fun PrisonAppointment.dateTimeAndLocationIsTheSame(date: LocalDate?, interval: Interval?, location: Location?) = this.appointmentDate == date && this.startTime == interval?.start && this.endTime == interval.end && this.prisonLocationId == location?.id
}
