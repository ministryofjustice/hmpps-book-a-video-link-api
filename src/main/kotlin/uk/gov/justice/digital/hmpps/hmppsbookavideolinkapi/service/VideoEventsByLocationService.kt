package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.appointmentCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.VideoEventRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.LocationEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoEventResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.findByCourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.findByProbationMeetingType
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
  private val referenceCodeRepository: ReferenceCodeRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val nonBvlsVideoAppointmentTypes = listOf("VLOO", "VLAP", "VLLA", "VLPA")

  fun videoEventsByLocation(prisonCode: String, request: VideoEventRequest): VideoEventResponse {
    // Get the video link locations at this prison
    val videoLinkLocations = locationsService.getVideoLinkLocationsAtPrison(prisonCode, enabledOnly = false)
      .map { it.toBasicLocation() }

    if (videoLinkLocations.isEmpty()) {
      return VideoEventResponse(prisonCode, request.startDate, request.endDate, emptyList())
    }

    // Gets scheduled appointments at the prison from A&A that are planned between the start/end dates
    // It will then filter to the four non-BVLS video appointment types
    val videoAppointmentEvents = if (activitiesAppointmentsClient.isAppointmentsRolledOutAt(prisonCode)) {
      activitiesAppointmentsClient
        .getScheduledAppointmentsBetween(prisonCode, request.startDate, request.endDate)
        .filter { nonBvlsVideoAppointmentTypes.contains(it.appointmentCode()) }
        .map { it.toBookedEvent() }
    } else {
      emptyList()
    }

    // Get active court and probation BVLS prison appointments between these dates (including pre, main, post)
    val bvlsEvents = prisonAppointmentRepository
      .findActivePrisonAppointmentsBetweenDates(prisonCode, request.startDate, request.endDate)
      .map { it.toBookedEvent() }

    val combinedEvents = bvlsEvents + videoAppointmentEvents

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
      locations = videoLinkLocations.map {
        LocationEvent(
          dpsLocationId = it.dpsLocationId,
          localName = it.localName,
          capacity = it.workingCapacity,
          events = videoEventsByLocation[it.dpsLocationId] ?: emptyList(),
        )
      },
    )
  }

  fun PrisonAppointment.toBookedEvent() = BookedEvent(
    dpsLocationId = this.prisonLocationId,
    eventType = if (this.videoBooking.isBookingType(BookingType.PROBATION)) "PROBATION" else "COURT",
    subType = getHearingOrMeetingCode(this.videoBooking),
    subTypeDescription = getHearingOrMeetingDescription(this.videoBooking),
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
    workingCapacity = this.workingCapacity,
  )

  private fun getHearingOrMeetingCode(videoBooking: VideoBooking): String? {
    log.info("Get code for ${videoBooking.bookingType.name} - probation [${videoBooking.probationMeetingType}] court [${videoBooking.hearingType}]")

    return if (videoBooking.isBookingType(BookingType.PROBATION)) {
      videoBooking.probationMeetingType
    } else if (videoBooking.isBookingType(BookingType.COURT)) {
      videoBooking.hearingType
    } else {
      null
    }
  }

  private fun getHearingOrMeetingDescription(videoBooking: VideoBooking): String? {
    log.info("Get description for ${videoBooking.bookingType.name} - probation [${videoBooking.probationMeetingType}] court [${videoBooking.hearingType}]")

    return if (videoBooking.isBookingType(BookingType.PROBATION)) {
      videoBooking.probationMeetingType?.let {
        referenceCodeRepository.findByProbationMeetingType(videoBooking.probationMeetingType!!)?.description
      } ?: "Unknown"
    } else if (videoBooking.isBookingType(BookingType.COURT)) {
      videoBooking.hearingType?.let {
        referenceCodeRepository.findByCourtHearingType(videoBooking.hearingType!!)?.description
      } ?: "Unknown"
    } else {
      null
    }
  }
}

data class BasicLocation(
  val dpsLocationId: UUID?,
  val localName: String? = null,
  val workingCapacity: Int? = null,
)
