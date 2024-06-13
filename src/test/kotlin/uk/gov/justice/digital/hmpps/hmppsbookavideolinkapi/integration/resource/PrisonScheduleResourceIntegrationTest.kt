package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ScheduleItem
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WERRINGTON
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.werringtonLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalDate
import java.time.LocalTime

class PrisonScheduleResourceIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Test
  fun `should return no items on the schedule with no bookings present`() {
    videoBookingRepository.findAll() hasSize 0

    val scheduleResponse = webTestClient.getPrisonSchedule(
      username = "PRISON-USER",
      prisonCode = MOORLAND,
      date = LocalDate.now(),
    )

    assertThat(scheduleResponse).hasSize(0)
  }

  @Test
  fun `should return one scheduled item when a court booking is made with one appointment`() {
    val bookingCreator = "BOOKING_CREATOR"

    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("A1111AA", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

    // Will default to tomorrow's date
    val courtBookingRequest = courtBookingRequest(
      courtCode = "DRBYMC",
      prisonerNumber = "A1111AA",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    webTestClient.createBooking(bookingCreator, courtBookingRequest)

    // Check for tomorrow's date
    val scheduleResponse = webTestClient.getPrisonSchedule(
      username = "PRISON-USER",
      prisonCode = WERRINGTON,
      date = LocalDate.now().plusDays(1),
    )

    assertThat(scheduleResponse).isNotNull
    assertThat(scheduleResponse).hasSize(1)

    with(scheduleResponse.first()) {
      assertThat(courtCode).isEqualTo("DRBYMC")
      assertThat(courtDescription).isEqualTo("Derby Justice Centre")
      assertThat(prisonCode).isEqualTo(WERRINGTON)
      assertThat(appointmentType).isEqualTo(AppointmentType.VLB_COURT_MAIN.name)
      assertThat(prisonerNumber).isEqualTo("A1111AA")
      assertThat(startTime).isEqualTo(LocalTime.of(12, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(12, 30))
      assertThat(bookingComments).isEqualTo("integration test court booking comments")
    }
  }

  @Test
  fun `should return court and probation schedule items when both exist`() {
    val bookingCreator = "BOOKING_CREATOR"

    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("A1111AA", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

    // Will default to tomorrow's date
    val courtBookingRequest = courtBookingRequest(
      courtCode = "DRBYMC",
      prisonerNumber = "A1111AA",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    webTestClient.createBooking(bookingCreator, courtBookingRequest)

    // Will default to tomorrow's date
    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = "BLKPPP",
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = WERRINGTON,
      prisonerNumber = "A1111AA",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = werringtonLocation,
    )

    webTestClient.createBooking(bookingCreator, probationBookingRequest)

    // Check for tomorrow's date
    val scheduleResponse = webTestClient.getPrisonSchedule(
      username = "PRISON-USER",
      prisonCode = WERRINGTON,
      date = LocalDate.now().plusDays(1),
    )

    assertThat(scheduleResponse).isNotNull
    assertThat(scheduleResponse).hasSize(2)

    assertThat(scheduleResponse.map { it.bookingType }).containsAll(
      listOf(BookingType.COURT.name, BookingType.PROBATION.name),
    )

    assertThat(scheduleResponse.map { it.appointmentType }).containsAll(
      listOf(AppointmentType.VLB_COURT_MAIN.name, AppointmentType.VLB_PROBATION.name),
    )

    assertThat(scheduleResponse.map { it.startTime }).containsAll(
      listOf(LocalTime.of(9, 0), LocalTime.of(12, 0)),
    )

    assertThat(scheduleResponse.map { it.videoUrl }).containsAll(
      listOf("https://video.link.com", "https://probation.videolink.com"),
    )
  }

  private fun WebTestClient.createBooking(username: String, request: CreateVideoBookingRequest) =
    this
      .post()
      .uri("/video-link-booking")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Long::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.getPrisonSchedule(username: String, prisonCode: String, date: LocalDate) =
    this
      .get()
      .uri("/schedule?prisonCode=$prisonCode&date=$date", prisonCode, date)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ScheduleItem::class.java)
      .returnResult().responseBody!!
}
