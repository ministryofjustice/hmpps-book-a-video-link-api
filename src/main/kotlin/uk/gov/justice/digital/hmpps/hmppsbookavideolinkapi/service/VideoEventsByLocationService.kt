package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.VideoEventRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.LocationEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoEventResponse
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class VideoEventsByLocationService {

  fun videoEventsByLocation(prisonCode: String, request: VideoEventRequest): VideoEventResponse {
    // Get locations - service type VIDEO_LINK
    // Get appointments for the prison
    //   - filter to video types
    //   - remove VLB and VLPM
    // Get BVLS bookings for the prison
    // Get video visits at the prison
    //   - filter to video
    // Convert to canonical BookedEvent model
    // Sort into locations and startTime/endTime
    // Join lists
    return fixedResponse()
  }

  private fun fixedResponse(): VideoEventResponse = VideoEventResponse(
    prisonCode = "MDI",
    startDate = LocalDate.now(),
    endDate = LocalDate.now(),
    timeSlot = null,
    locations = listOf(
      LocationEvent(
        dpsLocationId = UUID.randomUUID(),
        localName = "Random location name",
        capacity = 6,
        events = fixedEvents(),
      ),
    ),
  )

  private fun fixedEvents(): List<BookedEvent> = listOf(
    BookedEvent(
      eventType = "APPOINTMENT",
      subType = "VLOO",
      subTypeDescription = "Video link - official other",
      eventDate = LocalDate.now(),
      startTime = LocalTime.of(9, 30),
      endTime = LocalTime.of(10, 30),
      prisonerCode = "G4950GV",
      eventId = 1234567,
    ),
    BookedEvent(
      eventType = "COURT",
      subType = "BAIL",
      subTypeDescription = "Bail hearing",
      eventDate = LocalDate.now(),
      startTime = LocalTime.of(10, 30),
      endTime = LocalTime.of(11, 30),
      prisonerCode = "G4950GV",
      eventId = 1234568,
    ),
    BookedEvent(
      eventType = "PROBATION",
      subType = "FTR56",
      subTypeDescription = "Fixed term recall",
      eventDate = LocalDate.now(),
      startTime = LocalTime.of(11, 30),
      endTime = LocalTime.of(12, 30),
      prisonerCode = "G4950GV",
      eventId = 1234569,
    ),
    BookedEvent(
      eventType = "OFFICIAL_VISIT",
      subType = "VIDEO",
      subTypeDescription = "Video official visit",
      eventDate = LocalDate.now(),
      startTime = LocalTime.of(14, 30),
      endTime = LocalTime.of(16, 0),
      prisonerCode = "G4950GV",
      eventId = 1234570,
    ),
  )
}
