package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.appointmentCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.VideoEventRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.LocationEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoEventResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
@Transactional(readOnly = true)
class VideoEventsByLocationService(
  private val locationsService: LocationsService,
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient,
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
) {

  fun videoEventsByLocation(prisonCode: String, request: VideoEventRequest): VideoEventResponse {
    // Get the video link locations at this prison
    val locations = locationsService.getVideoLinkLocationsAtPrison(prisonCode, enabledOnly = false)
      .map { it.toBasicLocation() }

    if (locations.isEmpty()) {
      return VideoEventResponse(prisonCode, request.startDate, request.endDate, request.timeSlot, emptyList())
    }

    // Get active video appointments from A&A taking place between the start/end dates
    // Filter court and probation video appointments as we get these from BVLS below
    val videoAppointments = if (activitiesAppointmentsClient.isAppointmentsRolledOutAt(prisonCode)) {
      activitiesAppointmentsClient
        .getUncancelledVideoAppointments(prisonCode, request.startDate, request.endDate)
        .filterNot { it.appointmentCode() == "VLB" || it.appointmentCode() == "VLPM" }
        .map { it.toBookedEvent() }
    } else {
      emptyList()
    }

    // Get active court and probation BVLS appointments between these dates (including pre, main, post)
    val bvlsAppointments = prisonAppointmentRepository
      .findActivePrisonAppointmentsBetweenDates(prisonCode, request.startDate, request.endDate)
      .map { it.toBookedEvent() }

    val combinedEvents = bvlsAppointments + videoAppointments

    // Assemble the booked events into sorted lists within each location
    val videoEventsByLocation: Map<UUID, List<BookedEvent>> = combinedEvents
      .groupBy { it.dpsLocationId }
      .mapValues { (_, bookings) ->
        bookings.sortedWith(compareBy<BookedEvent> { it.eventDate }.thenBy { it.startTime })
      }

    return VideoEventResponse(
      prisonCode = prisonCode,
      startDate = request.startDate,
      endDate = request.endDate,
      timeSlot = request.timeSlot,
      locations = locations.map {
        LocationEvent(
          dpsLocationId = it.dpsLocationId,
          localName = it.localName,
          capacity = it.capacity,
          events = videoEventsByLocation[it.dpsLocationId] ?: emptyList(),
        )
      },
    )
  }

  fun PrisonAppointment.toBookedEvent() = BookedEvent(
    dpsLocationId = this.prisonLocationId,
    eventType = if (this.appointmentType == "VLB_PROBATION") "PROBATION" else "COURT",
    subType = if (this.appointmentType == "VLB_PROBATION") this.videoBooking.probationMeetingType else this.videoBooking.hearingType,
    // TODO: Not getting hearing type or meeting type - use the reference code service to retrieve
    subTypeDescription = if (this.appointmentType == "VLB_PROBATION") this.videoBooking.probationMeetingType else this.videoBooking.hearingType,
    eventDate = this.appointmentDate,
    startTime = this.startTime,
    endTime = this.endTime,
    prisonerNumber = this.prisonerNumber,
    eventId = this.videoBooking.videoBookingId,
  )

  fun AppointmentSearchResult.toBookedEvent() = BookedEvent(
    dpsLocationId = this.internalLocation!!.dpsLocationId!!,
    eventType = "APPOINTMENT",
    subType = this.appointmentCode(),
    subTypeDescription = this.category.description,
    eventDate = this.startDate,
    startTime = LocalTime.parse(this.startTime, DateTimeFormatter.ofPattern("HH:mm")),
    endTime = this.endTime?.let {
      LocalTime.parse(this.endTime, DateTimeFormatter.ofPattern("HH:mm"))
    } ?: LocalTime.parse(this.startTime, DateTimeFormatter.ofPattern("HH:mm")).plusHours(1),
    prisonerNumber = if (this.attendees.size == 1) this.attendees.first().prisonerNumber else "MANY",
    eventId = this.appointmentId,
  )

  fun Location.toBasicLocation() = BasicLocation(
    dpsLocationId = this.dpsLocationId,
    localName = this.description,
    // TODO: Not getting capacity from the locations API - can use locations API client directly
    capacity = 1,
  )
}

data class BasicLocation(
  val dpsLocationId: UUID?,
  val localName: String? = null,
  val capacity: Int? = null,
)
