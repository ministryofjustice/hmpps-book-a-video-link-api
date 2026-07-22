package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.VideoEventRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoEventResponse
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class VideoEventsByLocationIntegrationTest : IntegrationTestBase() {
  // Stub 2 x Birmingham video locations
  private val videoLocation1 = location(
    prisonCode = BIRMINGHAM,
    locationKeySuffix = "VR1",
    localName = "Video room 1",
    id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
  )

  private val videoLocation2 = location(
    prisonCode = BIRMINGHAM,
    locationKeySuffix = "VR2",
    localName = "Video room 2",
    id = UUID.fromString("6b29fc40-69a0-4693-b6d1-ed49ff2c8d23"),
  )

  private var videoBookingId: Long = 0

  @BeforeEach
  fun setup() {
    // Stub the Birmingham locations by ID
    locationsInsidePrisonApi().stubGetLocationById(videoLocation1)
    locationsInsidePrisonApi().stubGetLocationById(videoLocation2)

    // Stub the two video locations to report as VIDEO_LINK locations
    locationsInsidePrisonApi().stubVideoLinkLocationsAtPrison(BIRMINGHAM,videoLocation1, videoLocation2)

    // Ensure A&A reports as active at Birmingham
    activitiesAppointmentsApi().stubGetRolledOutPrison(BIRMINGHAM, true)

    // Stub prisoner search - for the probation booking create request
    prisonSearchApi().stubGetPrisoner("A1234AA", BIRMINGHAM)

    // Create a probation booking in video location 1
    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      prisonCode = BIRMINGHAM,
      prisonerNumber = "A1234AA",
      appointmentDate = tomorrow(),
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = videoLocation1,
    )

    videoBookingId = webTestClient.createBooking(probationBookingRequest, PRISON_USER_BIRMINGHAM)
  }

  @Test
  fun `should return video locations with filtered and sorted events within them`() {
    // Stub video appointments for tomorrow into videoLocation2
    activitiesAppointmentsApi().stubGetScheduledAppointmentsBetween(
      prisonCode = BIRMINGHAM,
      fromDate = tomorrow(),
      toDate = tomorrow(),
      dpsLocationId = videoLocation2.id,
    )

    val videoEvents = webTestClient.getVideoEventsAtPrison(BIRMINGHAM, tomorrow(), tomorrow())

    assertThat(videoEvents.locations).hasSize(2)

    // The BVLS probation booking should be in videoLocation1
    with(videoEvents.locations[0]) {
      assertThat(events).hasSize(1)
      assertThat(events[0].eventId).isEqualTo(videoBookingId)
      assertThat(events[0].eventType).isEqualTo("PROBATION")
      assertThat(events[0].dpsLocationId).isEqualTo(videoLocation1.id)
    }

    // The A&A appointments should be filtered to video and in videoLocation2
    with(videoEvents.locations[1]) {
      assertThat(events).hasSize(2)
      assertThat(events).extracting("eventType").containsOnly("APPOINTMENT")
      assertThat(events).extracting("dpsLocationId").containsOnly(videoLocation2.id)
    }
  }

  private fun WebTestClient.getVideoEventsAtPrison(
    prisonCode: String,
    fromDate: LocalDate,
    toDate: LocalDate,
    timeSlot: String? = null,
  ) = post()
    .uri("/video-events/prison/{prisonCode}/list-by-location", prisonCode)
    .bodyValue(VideoEventRequest(fromDate, toDate, timeSlot))
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<VideoEventResponse>()
    .returnResult().responseBody!!
}
