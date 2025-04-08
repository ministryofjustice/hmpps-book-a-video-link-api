package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AvailabilityStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.DateTimeAvailabilityRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocationsResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.slot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import java.time.LocalTime

/**
 * This provides a snapshot (date and time) of available rooms at time of calling. Note this does not guarantee the
 * rooms can be booked, by the time the user attempts to save the booking the room could already have been taken by
 * another user of the service.
 */
@Service
@Transactional
class DateTimeAvailabilityService(
  locationsService: LocationsService,
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val bookedLocationsService: BookedLocationsService,
  private val locationAttributesService: LocationAttributesAvailableService,
  private val timeSource: TimeSource,
) : LocationAvailabilityService<DateTimeAvailabilityRequest>(locationsService) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun findAvailable(request: DateTimeAvailabilityRequest): AvailableLocationsResponse {
    require(request.date!!.atTime(request.startTime!!).isAfter(timeSource.now())) {
      "Requested date and start time must be in the future."
    }

    val prisonVideoLinkLocations = getVideoLinkLocationsAt(request.prisonCode!!)
    val mayBeExistingAppointment = request.appointmentToExclude?.let { prisonAppointmentRepository.findById(it).orElseThrow { EntityNotFoundException("Prison appointment with ID $it not found.") } }
    val bookedLocations = bookedLocationsService.findBooked(BookedLookup(request.prisonCode, request.date, prisonVideoLinkLocations, mayBeExistingAppointment?.videoBooking?.videoBookingId ?: request.vlbIdToExclude))

    val availableLocationsBuilder = DateTimeLocationsBuilder.builder {
      prisonVideoLinkLocations.forEach { location ->
        val meetingStartTime = request.startTime
        val meetingEndTime = request.endTime!!

        if (mayBeExistingAppointment != null && isTheSame(request, location, mayBeExistingAppointment)) {
          add(
            availabilityStatus = AvailabilityStatus.SHARED,
            availableLocation = AvailableLocation(
              name = location.description ?: location.key,
              startTime = meetingStartTime,
              endTime = meetingEndTime,
              dpsLocationId = location.dpsLocationId,
              dpsLocationKey = location.key,
              usage = location.extraAttributes?.locationUsage?.let { LocationUsage.valueOf(it.name) }
                ?: LocationUsage.SHARED,
              timeSlot = slot(meetingStartTime),
            ),
          )
        } else {
          if (!bookedLocations.isBooked(location, meetingStartTime, meetingEndTime)) {
            add(
              availabilityStatus = location.allowsByAnyRuleOrSchedule(request, meetingStartTime),
              availableLocation = AvailableLocation(
                name = location.description ?: location.key,
                startTime = meetingStartTime,
                endTime = meetingEndTime,
                dpsLocationId = location.dpsLocationId,
                dpsLocationKey = location.key,
                usage = location.extraAttributes?.locationUsage?.let { LocationUsage.valueOf(it.name) }
                  ?: LocationUsage.SHARED,
                timeSlot = slot(meetingStartTime),
              ),
            )
          }
        }
      }
    }

    return AvailableLocationsResponse(
      availableLocationsBuilder
        .build()
        .also { log.info("DATE TIME AVAILABLE LOCATIONS: found ${it.size} available locations matching request $request") },
    )
  }

  private fun isTheSame(request: DateTimeAvailabilityRequest, location: Location, prisonAppointment: PrisonAppointment) = run {
    location.dpsLocationId == prisonAppointment.prisonLocationId && prisonAppointment.appointmentDate == request.date && prisonAppointment.startTime == request.startTime && prisonAppointment.endTime == request.endTime
  }

  protected fun Location.allowsByAnyRuleOrSchedule(request: DateTimeAvailabilityRequest, time: LocalTime): AvailabilityStatus {
    if (extraAttributes != null) {
      return when (request.bookingType!!) {
        BookingType.COURT -> locationAttributesService.isLocationAvailableFor(
          LocationAvailableRequest.court(
            extraAttributes.attributeId,
            request.courtCode!!,
            request.date!!.atTime(time),
          ),
        )
        BookingType.PROBATION -> locationAttributesService.isLocationAvailableFor(
          LocationAvailableRequest.probation(
            extraAttributes.attributeId,
            request.probationTeamCode!!,
            request.date!!.atTime(time),
          ),
        )
      }
    }

    return AvailabilityStatus.SHARED
  }
}

class DateTimeLocationsBuilder private constructor() : LocationAvailabilityService.AvailableLocationBuilder() {
  companion object {
    fun builder(init: DateTimeLocationsBuilder.() -> Unit) = DateTimeLocationsBuilder().also { it.init() }
  }

  fun build() = run {
    val probation = buildList {
      addAll(dedicatedProbationTeamLocations.sortedBy { it.name })
      addAll(anyProbationTeamLocations.sortedBy { it.name })
    }

    val court = buildList {
      addAll(dedicatedCourtLocations.sortedBy { it.name })
      addAll(anyCourtLocations.sortedBy { it.name })
    }

    if (probation.isNotEmpty() && court.isNotEmpty()) {
      throw IllegalStateException("Cannot mix probation and court locations")
    }

    buildList {
      addAll(probation)
      addAll(court)
      addAll(sharedLocations.sortedBy { it.name })
    }
  }
}
