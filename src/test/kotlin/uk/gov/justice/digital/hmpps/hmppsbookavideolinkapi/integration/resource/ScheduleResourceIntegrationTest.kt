package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ScheduleItem
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WERRINGTON
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.werringtonLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalDate
import java.time.LocalTime

class ScheduleResourceIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Nested
  @DisplayName("Prison schedule")
  inner class PrisonSchedule {
    @Test
    fun `Prison - return no items when no bookings are present for the prison`() {
      videoBookingRepository.findAll() hasSize 0

      val scheduleResponse = webTestClient.getPrisonSchedule(
        username = "PRISON-USER",
        prisonCode = MOORLAND,
        date = LocalDate.now(),
      )

      assertThat(scheduleResponse).hasSize(0)
    }

    @Test
    fun `Prison - return items when a booking is present for the prison`() {
      val bookingCreator = "BOOKING_CREATOR"

      videoBookingRepository.findAll() hasSize 0

      prisonSearchApi().stubGetPrisoner("A1111AA", WERRINGTON)
      locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

      // Will default to tomorrow's date
      val courtBookingRequest = courtBookingRequest(
        courtCode = DERBY_JUSTICE_CENTRE,
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
        date = tomorrow(),
      )

      assertThat(scheduleResponse).isNotNull
      assertThat(scheduleResponse).hasSize(1)

      with(scheduleResponse.first()) {
        assertThat(courtCode).isEqualTo(DERBY_JUSTICE_CENTRE)
        assertThat(courtDescription).isEqualTo("Derby Justice Centre")
        assertThat(prisonCode).isEqualTo(WERRINGTON)
        assertThat(appointmentType).isEqualTo(AppointmentType.VLB_COURT_MAIN.name)
        assertThat(prisonerNumber).isEqualTo("A1111AA")
        assertThat(appointmentDate).isEqualTo(tomorrow())
        assertThat(startTime).isEqualTo(LocalTime.of(12, 0))
        assertThat(endTime).isEqualTo(LocalTime.of(12, 30))
        assertThat(bookingComments).isEqualTo("integration test court booking comments")
      }
    }

    @Test
    fun `Prison - return court and probation bookings for the prison`() {
      val bookingCreator = "BOOKING_CREATOR"

      videoBookingRepository.findAll() hasSize 0

      prisonSearchApi().stubGetPrisoner("A1111AA", WERRINGTON)
      locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

      // Will default to tomorrow's date
      val courtBookingRequest = courtBookingRequest(
        courtCode = DERBY_JUSTICE_CENTRE,
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
        date = tomorrow(),
      )

      assertThat(scheduleResponse).isNotNull
      assertThat(scheduleResponse).hasSize(2)

      assertThat(scheduleResponse.map { it.bookingType }).containsAll(
        listOf(BookingType.COURT.name, BookingType.PROBATION.name),
      )

      assertThat(scheduleResponse.map { it.appointmentType }).containsAll(
        listOf(AppointmentType.VLB_COURT_MAIN.name, AppointmentType.VLB_PROBATION.name),
      )

      assertThat(scheduleResponse.map { it.appointmentDate }).containsOnly(tomorrow())

      assertThat(scheduleResponse.map { it.startTime }).containsAll(
        listOf(LocalTime.of(9, 0), LocalTime.of(12, 0)),
      )

      assertThat(scheduleResponse.map { it.videoUrl }).containsAll(
        listOf("https://video.link.com", "https://probation.videolink.com"),
      )
    }
  }

  @Nested
  @DisplayName("Court schedule")
  inner class CourtSchedule {
    @Test
    fun `Court - return no items when only probation bookings are present`() {
      val bookingCreator = "BOOKING_CREATOR"

      videoBookingRepository.findAll() hasSize 0

      prisonSearchApi().stubGetPrisoner("A1111AA", WERRINGTON)
      locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

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

      // Check for tomorrow's date for court bookings
      val scheduleResponse = webTestClient.getCourtSchedule(
        username = "PRISON-USER",
        courtCode = DERBY_JUSTICE_CENTRE,
        date = tomorrow(),
      )

      assertThat(scheduleResponse).isNotNull
      assertThat(scheduleResponse).hasSize(0)
    }

    @Test
    fun `Court - return items when bookings are present for this court`() {
      val bookingCreator = "BOOKING_CREATOR"

      videoBookingRepository.findAll() hasSize 0

      prisonSearchApi().stubGetPrisoner("A1111AA", WERRINGTON)
      locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

      // Will default to tomorrow's date
      val courtBookingRequest = courtBookingRequest(
        courtCode = DERBY_JUSTICE_CENTRE,
        prisonerNumber = "A1111AA",
        prisonCode = WERRINGTON,
        location = werringtonLocation,
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(12, 30),
        comments = "integration test court booking comments",
      )

      webTestClient.createBooking(bookingCreator, courtBookingRequest)

      // Check for tomorrow's date
      val scheduleResponse = webTestClient.getCourtSchedule(
        username = "PRISON-USER",
        courtCode = DERBY_JUSTICE_CENTRE,
        date = tomorrow(),
      )

      assertThat(scheduleResponse).isNotNull
      assertThat(scheduleResponse).hasSize(1)

      with(scheduleResponse.first()) {
        assertThat(courtCode).isEqualTo(DERBY_JUSTICE_CENTRE)
        assertThat(courtDescription).isEqualTo("Derby Justice Centre")
        assertThat(prisonCode).isEqualTo(WERRINGTON)
        assertThat(appointmentType).isEqualTo(AppointmentType.VLB_COURT_MAIN.name)
        assertThat(prisonerNumber).isEqualTo("A1111AA")
        assertThat(appointmentDate).isEqualTo(tomorrow())
        assertThat(startTime).isEqualTo(LocalTime.of(12, 0))
        assertThat(endTime).isEqualTo(LocalTime.of(12, 30))
        assertThat(bookingComments).isEqualTo("integration test court booking comments")
      }
    }
  }

  @Nested
  @DisplayName("Probation schedule")
  inner class ProbationSchedule {
    @Test
    fun `Probation - return no items when only court bookings are present`() {
      val bookingCreator = "BOOKING_CREATOR"

      videoBookingRepository.findAll() hasSize 0

      prisonSearchApi().stubGetPrisoner("A1111AA", WERRINGTON)
      locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

      // Will default to tomorrow's date
      val courtBookingRequest = courtBookingRequest(
        courtCode = DERBY_JUSTICE_CENTRE,
        prisonerNumber = "A1111AA",
        prisonCode = WERRINGTON,
        location = werringtonLocation,
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(12, 30),
        comments = "integration test court booking comments",
      )

      webTestClient.createBooking(bookingCreator, courtBookingRequest)

      // Check for tomorrow's date
      val scheduleResponse = webTestClient.getProbationSchedule(
        username = "PRISON-USER",
        probationTeamCode = "BLKPPP",
        date = tomorrow(),
      )

      assertThat(scheduleResponse).isNotNull
      assertThat(scheduleResponse).hasSize(0)
    }

    @Test
    fun `Probation - return items when bookings are present for this probation team`() {
      val bookingCreator = "BOOKING_CREATOR"

      videoBookingRepository.findAll() hasSize 0

      prisonSearchApi().stubGetPrisoner("A1111AA", WERRINGTON)
      locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

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
      val scheduleResponse = webTestClient.getProbationSchedule(
        username = "PRISON-USER",
        probationTeamCode = "BLKPPP",
        date = tomorrow(),
      )

      assertThat(scheduleResponse).isNotNull
      assertThat(scheduleResponse).hasSize(1)

      with(scheduleResponse.first()) {
        assertThat(probationTeamCode).isEqualTo("BLKPPP")
        assertThat(prisonCode).isEqualTo(WERRINGTON)
        assertThat(appointmentType).isEqualTo(AppointmentType.VLB_PROBATION.name)
        assertThat(prisonerNumber).isEqualTo("A1111AA")
        assertThat(appointmentDate).isEqualTo(tomorrow())
        assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(endTime).isEqualTo(LocalTime.of(9, 30))
      }
    }
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

  private fun WebTestClient.getPrisonSchedule(username: String, prisonCode: String, date: LocalDate, cancelled: Boolean = false) =
    this
      .get()
      .uri("/schedule/prison/{prisonCode}?date=$date&includeCancelled=$cancelled", prisonCode, date, cancelled)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ScheduleItem::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.getCourtSchedule(username: String, courtCode: String, date: LocalDate, cancelled: Boolean = false) =
    this
      .get()
      .uri("/schedule/court/{courtCode}?date=$date&includeCancelled=$cancelled", courtCode, date, cancelled)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ScheduleItem::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.getProbationSchedule(username: String, probationTeamCode: String, date: LocalDate, cancelled: Boolean = false) =
    this
      .get()
      .uri("/schedule/probation/{probationTeamCode}?date=$date&includeCancelled=$cancelled", probationTeamCode, date, cancelled)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ScheduleItem::class.java)
      .returnResult().responseBody!!
}
