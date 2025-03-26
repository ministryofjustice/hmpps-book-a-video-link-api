package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisMappingClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
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
import java.time.LocalTime
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
      courtOrProbationCode = request.courtCode ?: request.probationTeamCode,
      prisonCode = request.prisoners.first().prisonCode,
      date = request.prisoners.first().appointments.first().date,
      preAppointment = request.appointment(AppointmentType.VLB_COURT_PRE),
      mainAppointment = request.appointment(AppointmentType.VLB_COURT_MAIN)
        ?: request.appointment(AppointmentType.VLB_PROBATION),
      postAppointment = request.appointment(AppointmentType.VLB_COURT_POST),
    ),
  ).availabilityOk

  private fun CreateVideoBookingRequest.appointment(type: AppointmentType) = prisoners.single().appointments.singleOrNull { it.type == type }
    ?.let { LocationAndInterval(it.locationKey, Interval(it.startTime, it.endTime)) }

  fun isAvailable(videoBookingId: Long, request: AmendVideoBookingRequest): Boolean {
    val existingBooking = videoBookingRepository.findById(videoBookingId).orElseThrow()

    return checkAvailability(
      AvailabilityRequest(
        bookingType = request.bookingType,
        courtOrProbationCode = existingBooking?.court?.code ?: existingBooking?.probationTeam?.code,
        prisonCode = request.prisoners.first().prisonCode,
        date = request.prisoners.first().appointments.first().date,
        preAppointment = request.appointment(AppointmentType.VLB_COURT_PRE),
        mainAppointment = request.appointment(AppointmentType.VLB_COURT_MAIN)
          ?: request.appointment(AppointmentType.VLB_PROBATION),
        postAppointment = request.appointment(AppointmentType.VLB_COURT_POST),
        vlbIdToExclude = videoBookingId,
      ),
    ).availabilityOk
  }

  private fun AmendVideoBookingRequest.appointment(type: AppointmentType) = prisoners.single().appointments.singleOrNull { it.type == type }?.let { LocationAndInterval(it.locationKey, Interval(it.startTime, it.endTime)) }

  /**
   * Assumptions:
   *  - current booking journeys for BVLS allows only one person at one prison per booking.
   *  - we consider ONLY the prison locations in the request for alternative times (we do not consider others - yet)
   *  - the decision about whether a user is allowed to book into a locations is still the users own.
   *  - we assume all appointments for this booking are on the same day and at the same prison
   *
   * To consider later:
   *  - Get other VCC-enabled rooms at these prison(s) and offer them?
   *  - Incorporate prison room decoration logic here, to find other rooms?
   */
  fun checkAvailability(request: AvailabilityRequest): AvailabilityResponse {
    log.info("AVAILABILITY CHECK: looking at BVLS and other non-VLB appointment types")

    // Gather the distinct list of locations from the request
    val locationKeys = setOfNotNull(
      request.preAppointment?.prisonLocKey,
      request.postAppointment?.prisonLocKey,
      request.mainAppointment!!.prisonLocKey,
    )

    log.info("AVAILABILITY CHECK: for the following location keys $locationKeys")

    val requestedLocations = locationsInsidePrisonClient.getLocationsByKeys(locationKeys).associateBy { it.key }

    if (request.isForAnExistingBooking() && request.dateTimeAndLocationIsTheSame(requestedLocations)) {
      return AvailabilityResponse(true)
    }

    // Build a list of AppointmentSlots for existing VLBs from BVLS at these locations on this date
    val (bvlsAppointmentSlotsToInclude: List<AppointmentSlot>, appointmentsToExclude: List<AppointmentSlot>) = videoAppointmentRepository.findVideoAppointmentsAtPrison(
      forDate = request.date!!,
      forPrison = request.prisonCode!!,
      forLocationIds = requestedLocations.values.map { it.id },
    ).partition { vlb -> vlb.videoBookingId != request.vlbIdToExclude }

    // Build list of AppointmentSlot for all external non-VLB appointments from NOMIS in these locations on this date
    val externalAppointmentSlotsToInclude: List<AppointmentSlot> =
      requestedLocations.values
        .flatMap { externalAppointmentsService.getAppointmentSlots(request.prisonCode, request.date, it.id) }
        .filterNot { external ->
          // We can only match on these values, there is no direct match between a BVLS appointment and what is in NOMIS
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
    return availabilityFinderService.getOptions(request, slotsToCheck, requestedLocations.values.toList())
  }

  private fun AvailabilityRequest.isForAnExistingBooking() = vlbIdToExclude != null

  private fun AvailabilityRequest.dateTimeAndLocationIsTheSame(requestedLocations: Map<String, Location>): Boolean {
    val mayBeExistingBooking = videoBookingRepository.findById(vlbIdToExclude!!).getOrNull()

    if (mayBeExistingBooking != null) {
      if (mayBeExistingBooking.isBookingType(PROBATION)) {
        return mayBeExistingBooking.probationMeeting()!!.dateTimeAndLocationIsTheSame(
          date!!,
          mainAppointment!!.interval,
          requestedLocations[mainAppointment.prisonLocKey]!!,
        )
      } else {
        val preLocation = preAppointment?.prisonLocKey?.let { requestedLocations[it] }
        val mainLocation = requestedLocations[mainAppointment!!.prisonLocKey]!!
        val postLocation = postAppointment?.prisonLocKey?.let { requestedLocations[it] }

        val preHearingIsTheSame = (mayBeExistingBooking.preHearing() == null && preLocation == null) ||
          mayBeExistingBooking.preHearing() != null &&
          mayBeExistingBooking.preHearing()!!.dateTimeAndLocationIsTheSame(date, preAppointment?.interval, preLocation)

        val mainHearingIsTheSame =
          mayBeExistingBooking.mainHearing()!!.dateTimeAndLocationIsTheSame(date!!, mainAppointment.interval, mainLocation)

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

@Service
class ExternalAppointmentsService(
  private val prisonApiClient: PrisonApiClient,
  private val nomisMappingClient: NomisMappingClient,
  private val supportedAppointmentTypes: SupportedAppointmentTypes,
) {
  /**
   * Get the prison appointments on a date at a specific internal location ID in the prison
   * - filter any appointments where the end-time is null (we cannot infer the duration)
   * - filter any VLB appointment types as these will be retrieved from BVLS itself for checking.
   */
  fun getAppointmentSlots(prisonCode: String, date: LocalDate, location: UUID) = nomisMappingClient.getNomisLocationMappingBy(location)
    ?.let { prisonApiClient.getScheduledAppointments(prisonCode, date, it.nomisLocationId) }
    ?.filter { it.endTime != null }
    ?.filterNot { supportedAppointmentTypes.isSupported(it.appointmentTypeCode) }
    ?.map { ExternalAppointmentSlot(location, it.offenderNo, it.startTime.toLocalDate(), it.startTime.toLocalTime(), it.endTime!!.toLocalTime()) }
    ?: emptyList()

  class ExternalAppointmentSlot(
    override val prisonLocationId: UUID,
    override val prisonerNumber: String,
    override val appointmentDate: LocalDate,
    override val startTime: LocalTime,
    override val endTime: LocalTime,
  ) : AppointmentSlot
}
