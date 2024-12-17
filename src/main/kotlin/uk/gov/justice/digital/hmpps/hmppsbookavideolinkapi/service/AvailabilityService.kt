package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisMappingClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AppointmentSlot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailabilityRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Interval
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
    log.info("Availability check looking at BVLS and other non-VLB appointment types")

    // Gather the distinct list of locations from the request
    val locationKeys = setOfNotNull(
      request.preAppointment?.prisonLocKey,
      request.postAppointment?.prisonLocKey,
      request.mainAppointment!!.prisonLocKey,
    )

    log.info("Checking availability for locationKeys $locationKeys")

    val requestedLocations = locationsInsidePrisonClient.getLocationsByKeys(locationKeys).associateBy { it.key }

    if (request.isForAnExistingBooking() && request.dateTimeAndLocationIsTheSame(requestedLocations)) {
      return AvailabilityResponse(true)
    }

    // Build a list of AppointmentSlot for existing VLBs from BVLS at these locations on this date
    val (bvlsAppointmentSlotsToInclude: List<AppointmentSlot>, appointmentsToExclude: List<AppointmentSlot>) = videoAppointmentRepository.findVideoAppointmentsAtPrison(
      forDate = request.date!!,
      forPrison = request.prisonCode!!,
      forLocationIds = requestedLocations.values.map { it.id },
    ).partition { vlb -> vlb.videoBookingId != request.vlbIdToExclude }

    log.info("Found ${bvlsAppointmentSlotsToInclude.size}, including appointments in these rooms")
    log.info("Found ${appointmentsToExclude.size}, excluding appointments in these rooms")

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

    log.info("Found ${externalAppointmentSlotsToInclude.size}, including external appointments in these rooms")

    bvlsAppointmentSlotsToInclude.plus(externalAppointmentSlotsToInclude).map { slot ->
      log.info("${slot.prisonLocationId} - ${slot.prisonerNumber} - ${slot.appointmentDate} - ${slot.startTime} - ${slot.endTime}")
    }

    // Check if the requested times are free, and offer alternatives if not
    return availabilityFinderService.getOptions(
      request,
      bvlsAppointmentSlotsToInclude.plus(externalAppointmentSlotsToInclude),
      requestedLocations.values.toList(),
    )
  }

  private fun AvailabilityRequest.isForAnExistingBooking() = vlbIdToExclude != null

  private fun AvailabilityRequest.dateTimeAndLocationIsTheSame(requestedLocations: Map<String, Location>): Boolean {
    val mayBeExistingBooking = videoBookingRepository.findById(vlbIdToExclude!!).getOrNull()

    if (mayBeExistingBooking != null) {
      if (mayBeExistingBooking.isProbationBooking()) {
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
          mayBeExistingBooking.preHearing() != null && mayBeExistingBooking.preHearing()!!.dateTimeAndLocationIsTheSame(date, preAppointment?.interval, preLocation)

        val mainHearingIsTheSame =
          mayBeExistingBooking.mainHearing()!!.dateTimeAndLocationIsTheSame(date!!, mainAppointment.interval, mainLocation)

        val postHearingIsTheSame = (mayBeExistingBooking.postHearing() == null && postLocation == null) ||
          mayBeExistingBooking.postHearing() != null && mayBeExistingBooking.postHearing()!!.dateTimeAndLocationIsTheSame(date, postAppointment?.interval, postLocation)

        return preHearingIsTheSame && mainHearingIsTheSame && postHearingIsTheSame
      }
    }

    return false
  }

  private fun PrisonAppointment.dateTimeAndLocationIsTheSame(date: LocalDate?, interval: Interval?, location: Location?) =
    this.appointmentDate == date && this.startTime == interval?.start && this.endTime == interval.end && this.prisonLocationId == location?.id
}

@Service
class ExternalAppointmentsService(
  private val prisonApiClient: PrisonApiClient,
  private val nomisMappingClient: NomisMappingClient,
) {
  /**
   * Get the prison appointments on a date at a specific internal location ID in the prison
   * - filter any appointments where the end-time is null (we cannot infer the duration)
   * - filter any VLB appointment types as these will be retrieved from BVLS itself for checking.
   */
  fun getAppointmentSlots(prisonCode: String, date: LocalDate, location: UUID) =
    nomisMappingClient.getNomisLocationMappingBy(location)
      ?.let { prisonApiClient.getScheduledAppointments(prisonCode, date, it.nomisLocationId) }
      ?.filter { it.endTime != null }
      ?.filter { it.appointmentTypeCode != "VLB" }
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
