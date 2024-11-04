package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailabilityRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailabilityResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoAppointmentRepository

@Service
class AvailabilityService(
  private val videoAppointmentRepository: VideoAppointmentRepository,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
  private val availabilityFinderService: AvailabilityFinderService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Assumptions:
   *  - we consider ONLY video link bookings (not other appointment types from NOMIS or A&A in these locations)
   *  - we consider ONLY bookings present in the BVLS service, not in A&A or NOMIS (i.e. all video link bookings)
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
    // Gather the distinct list of locations from the request
    val locationKeys = setOfNotNull(
      request.preAppointment?.prisonLocKey,
      request.postAppointment?.prisonLocKey,
      request.mainAppointment!!.prisonLocKey,
    )

    log.info("Checking availability for locationKeys $locationKeys")

    val locations = locationsInsidePrisonClient.getLocationsByKeys(locationKeys)

    // Get the existing VLB appointments at these locations, on this date
    val videoAppointments = videoAppointmentRepository.findVideoAppointmentsAtPrison(
      forDate = request.date!!,
      forPrison = request.prisonCode!!,
      forLocationIds = locations.map { it.id },
    ).filter { vlb -> vlb.videoBookingId != request.vlbIdToExclude }

    log.info("Found ${videoAppointments.size} appointments in these rooms")

    // Check if the requested times are free, and offer alternatives if not
    return availabilityFinderService.getOptions(request, videoAppointments, locations)
  }
}
