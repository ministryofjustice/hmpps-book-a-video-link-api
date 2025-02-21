package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.isTheSameAppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.isTheSameTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisMappingClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.ScheduledAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isTimesOverlap
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalDate
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class BookedLocationsService(
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient,
  private val prisonApiClient: PrisonApiClient,
  private val nomisMappingClient: NomisMappingClient,
  private val videoBookingRepository: VideoBookingRepository,
  private val supportedAppointmentTypes: SupportedAppointmentTypes,
) {
  fun findBooked(lookup: BookedLookup): BookedLocations = findBooked(
    lookup.prisonCode,
    lookup.date,
    lookup.locations,
    lookup.videoBookingIdToExclude?.let {
      videoBookingRepository.findById(it).orElseThrow {
        EntityNotFoundException("Video booking with ID $it not found.")
      }?.also { existingBooking ->
        require(existingBooking.isStatus(StatusCode.ACTIVE)) {
          "Video booking ${existingBooking.videoBookingId} is not active"
        }
      }?.appointments()
    } ?: emptyList(),
  )

  private fun findBooked(
    prisonCode: String,
    date: LocalDate,
    locations: Collection<Location>,
    existingAppointments: List<PrisonAppointment>,
  ): BookedLocations {
    val nomisToDpsLocations = nomisMappingClient.getNomisLocationMappingsBy(locations.map(Location::dpsLocationId))
      .associate { it.nomisLocationId to it.dpsLocationId }

    return when (activitiesAppointmentsClient.isAppointmentsRolledOutAt(prisonCode)) {
      true -> activitiesAppointmentsClient.getScheduledAppointments(prisonCode, date, nomisToDpsLocations.keys)
        .mapNotNull { result ->
          // Only include if result does not match existing BVLS appointment
          if (existingAppointments.none { existing -> result.matches(existing) }) {
            BookedLocation(
              locations.single { l -> l.dpsLocationId == nomisToDpsLocations[result.internalLocation!!.id] },
              startTime = LocalTime.parse(result.startTime),
              endTime = LocalTime.parse(result.endTime),
            )
          } else {
            null
          }
        }
        .let { BookedLocations(it) }

      false -> prisonApiClient.getScheduledAppointments(prisonCode, date, nomisToDpsLocations.keys)
        .mapNotNull { result ->
          // Only include if result does not match existing BVLS appointment
          if (existingAppointments.none { existing -> result.matches(existing) }) {
            BookedLocation(
              locations.single { l -> l.dpsLocationId == nomisToDpsLocations[result.locationId] },
              startTime = result.startTime.toLocalTime(),
              endTime = result.endTime!!.toLocalTime(),
            )
          } else {
            null
          }
        }
        .let { BookedLocations(it) }
    }
  }

  private fun AppointmentSearchResult.matches(prisonAppointment: PrisonAppointment) = isTheSameTime(prisonAppointment) &&
    isTheSameAppointmentType(supportedAppointmentTypes.typeOf(prisonAppointment.bookingType())) &&
    attendees.any { it.prisonerNumber == prisonAppointment.prisonerNumber }

  private fun ScheduledAppointment.matches(prisonAppointment: PrisonAppointment) = isTheSameTime(prisonAppointment) &&
    isTheSameAppointmentType(supportedAppointmentTypes.typeOf(prisonAppointment.bookingType())) &&
    offenderNo == prisonAppointment.prisonerNumber
}

data class BookedLookup(
  val prisonCode: String,
  val date: LocalDate,
  val locations: Collection<Location>,
  val videoBookingIdToExclude: Long? = null,
)

data class BookedLocation(val location: Location, val startTime: LocalTime, val endTime: LocalTime)

class BookedLocations(booked: Collection<BookedLocation>) {

  private val locations = booked.groupBy { it.location.dpsLocationId }

  fun isBooked(location: Location, startTime: LocalTime, endTime: LocalTime) = locations[location.dpsLocationId]?.any { isTimesOverlap(it.startTime, it.endTime, startTime, endTime) } ?: false
}
