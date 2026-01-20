package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.web.PagedModel
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.CHESTERFIELD_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AdditionalBookingDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.FindCourtBookingsRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.FindProbationBookingsRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.ScheduleItem
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService
import java.time.LocalDate
import java.time.LocalTime

class ScheduleResourceIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @MockitoBean
  private lateinit var outboundEventsService: OutboundEventsService

  @Nested
  @DisplayName("Prison schedule")
  inner class PrisonSchedule {
    @Test
    fun `Prison - return no items when no bookings are present for the prison`() {
      videoBookingRepository.findAll() hasSize 0

      val scheduleResponse = webTestClient.getPrisonSchedule(
        prisonCode = WANDSWORTH,
        date = LocalDate.now(),
      )

      assertThat(scheduleResponse).hasSize(0)
    }

    @Test
    fun `Prison - return items when a booking is present for the prison`() {
      videoBookingRepository.findAll() hasSize 0

      prisonSearchApi().stubGetPrisoner("A1111AA", PENTONVILLE)

      // Will default to tomorrow's date
      val courtBookingRequest = courtBookingRequest(
        courtCode = DERBY_JUSTICE_CENTRE,
        prisonerNumber = "A1111AA",
        prisonCode = PENTONVILLE,
        location = pentonvilleLocation,
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(12, 30),
      )

      webTestClient.createBooking(courtBookingRequest, COURT_USER)

      // Check for tomorrow's date
      val scheduleResponse = webTestClient.getPrisonSchedule(
        prisonCode = PENTONVILLE,
        date = tomorrow(),
      )

      assertThat(scheduleResponse).isNotNull
      assertThat(scheduleResponse).hasSize(1)

      with(scheduleResponse.first()) {
        assertThat(courtCode).isEqualTo(DERBY_JUSTICE_CENTRE)
        assertThat(courtDescription).isEqualTo("Derby Justice Centre")
        assertThat(prisonCode).isEqualTo(PENTONVILLE)
        assertThat(appointmentType).isEqualTo(AppointmentType.VLB_COURT_MAIN)
        assertThat(prisonLocKey).isEqualTo(pentonvilleLocation.key)
        assertThat(prisonerNumber).isEqualTo("A1111AA")
        assertThat(appointmentDate).isEqualTo(tomorrow())
        assertThat(startTime).isEqualTo(LocalTime.of(12, 0))
        assertThat(endTime).isEqualTo(LocalTime.of(12, 30))
      }
    }

    @Test
    fun `Prison - return court and probation bookings for the prison`() {
      nomisMappingApi().stubGetNomisLocationMappingBy(pentonvilleLocation, 1)
      prisonApi().stubGetScheduledAppointments(PENTONVILLE, tomorrow(), 1)
      locationsInsidePrisonApi().stubGetLocationById(pentonvilleLocation)

      videoBookingRepository.findAll() hasSize 0

      prisonSearchApi().stubGetPrisoner("A1111AA", PENTONVILLE)

      // Will default to tomorrow's date
      val courtBookingRequest = courtBookingRequest(
        courtCode = DERBY_JUSTICE_CENTRE,
        prisonerNumber = "A1111AA",
        prisonCode = PENTONVILLE,
        location = pentonvilleLocation,
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(12, 30),
      )

      webTestClient.createBooking(courtBookingRequest, COURT_USER)

      // Will default to tomorrow's date
      val probationBookingRequest = probationBookingRequest(
        probationTeamCode = "BLKPPP",
        probationMeetingType = ProbationMeetingType.PSR,
        prisonCode = PENTONVILLE,
        prisonerNumber = "A1111AA",
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(9, 30),
        appointmentType = AppointmentType.VLB_PROBATION,
        location = pentonvilleLocation,
      )

      webTestClient.createBooking(probationBookingRequest, PROBATION_USER)

      // Check for tomorrow's date
      val scheduleResponse = webTestClient.getPrisonSchedule(
        prisonCode = PENTONVILLE,
        date = tomorrow(),
      )

      assertThat(scheduleResponse).isNotNull
      assertThat(scheduleResponse).hasSize(2)

      assertThat(scheduleResponse.map { it.bookingType }).containsAll(listOf(BookingType.COURT, BookingType.PROBATION))
      assertThat(scheduleResponse.map { it.appointmentType }).containsAll(listOf(AppointmentType.VLB_COURT_MAIN, AppointmentType.VLB_PROBATION))
      assertThat(scheduleResponse.map { it.appointmentDate }).containsOnly(tomorrow())
      assertThat(scheduleResponse.map { it.startTime }).containsAll(
        listOf(LocalTime.of(9, 0), LocalTime.of(12, 0)),
      )
      assertThat(scheduleResponse.map { it.videoUrl }).containsAll(
        listOf("https://video.link.com"),
      )
    }
  }

  @Nested
  @DisplayName("Court schedule")
  inner class CourtSchedule {
    @Test
    fun `Court - return no items when only probation bookings are present`() {
      videoBookingRepository.findAll() hasSize 0

      prisonSearchApi().stubGetPrisoner("A1111AA", PENTONVILLE)

      // Will default to tomorrow's date
      val probationBookingRequest = probationBookingRequest(
        probationTeamCode = "BLKPPP",
        probationMeetingType = ProbationMeetingType.PSR,
        prisonCode = PENTONVILLE,
        prisonerNumber = "A1111AA",
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(9, 30),
        appointmentType = AppointmentType.VLB_PROBATION,
        location = pentonvilleLocation,
      )

      webTestClient.createBooking(probationBookingRequest, PROBATION_USER)

      // Check for tomorrow's date for court bookings
      val scheduleResponse = webTestClient.getCourtSchedule(
        courtCode = DERBY_JUSTICE_CENTRE,
        date = tomorrow(),
      )

      assertThat(scheduleResponse).isNotNull
      assertThat(scheduleResponse).hasSize(0)
    }

    @Test
    fun `Court - return items when bookings are present for a single court`() {
      videoBookingRepository.findAll() hasSize 0

      prisonSearchApi().stubGetPrisoner("A1111AA", PENTONVILLE)

      // Will default to tomorrow's date
      val courtBookingRequest = courtBookingRequest(
        courtCode = DERBY_JUSTICE_CENTRE,
        prisonerNumber = "A1111AA",
        prisonCode = PENTONVILLE,
        location = pentonvilleLocation,
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(12, 30),
      )

      webTestClient.createBooking(courtBookingRequest, COURT_USER)

      // Check for tomorrow's date
      val scheduleResponse = webTestClient.getCourtSchedule(
        courtCode = DERBY_JUSTICE_CENTRE,
        date = tomorrow(),
      )

      assertThat(scheduleResponse).isNotNull
      assertThat(scheduleResponse).hasSize(1)

      with(scheduleResponse.first()) {
        assertThat(courtCode).isEqualTo(DERBY_JUSTICE_CENTRE)
        assertThat(courtDescription).isEqualTo("Derby Justice Centre")
        assertThat(prisonCode).isEqualTo(PENTONVILLE)
        assertThat(appointmentType).isEqualTo(AppointmentType.VLB_COURT_MAIN)
        assertThat(prisonLocKey).isEqualTo(pentonvilleLocation.key)
        assertThat(prisonerNumber).isEqualTo("A1111AA")
        assertThat(appointmentDate).isEqualTo(tomorrow())
        assertThat(startTime).isEqualTo(LocalTime.of(12, 0))
        assertThat(endTime).isEqualTo(LocalTime.of(12, 30))
        assertThat(notesForStaff).isEqualTo("Some private staff notes")
      }
    }

    @Test
    fun `Court - return paginated list of bookings for multiple courts (default sort date and time)`() {
      prisonSearchApi().stubGetPrisoner("A1111AA", PENTONVILLE)

      videoBookingRepository.findAll() hasSize 0

      createThreeCourtBookings()

      videoBookingRepository.findAll() hasSize 3

      val scheduleResponsePage1 = webTestClient.getPaginatedCourtsSchedule(
        courtCodes = listOf(DERBY_JUSTICE_CENTRE, CHESTERFIELD_JUSTICE_CENTRE),
        date = tomorrow(),
        page = 0,
        size = 2,
      )

      assertThat(scheduleResponsePage1.content).hasSize(2)
      assertThat(scheduleResponsePage1.page.number).isEqualTo(0)
      with(scheduleResponsePage1.content.first()) {
        assertThat(startTime).isEqualTo("12:00")
      }

      val scheduleResponsePage2 = webTestClient.getPaginatedCourtsSchedule(
        courtCodes = listOf(DERBY_JUSTICE_CENTRE, CHESTERFIELD_JUSTICE_CENTRE),
        date = tomorrow(),
        page = 1,
        size = 2,
      )

      assertThat(scheduleResponsePage2.content).hasSize(1)
      assertThat(scheduleResponsePage2.page.number).isEqualTo(1)
      with(scheduleResponsePage2.content.first()) {
        assertThat(startTime).isEqualTo("15:00")
      }
    }

    private fun createThreeCourtBookings() {
      // Bookings will default to tomorrow's date
      val courtBookingRequest1 = courtBookingRequest(
        courtCode = DERBY_JUSTICE_CENTRE,
        prisonerNumber = "A1111AA",
        prisonCode = PENTONVILLE,
        location = pentonvilleLocation,
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(12, 30),
      )

      webTestClient.createBooking(courtBookingRequest1, COURT_USER)

      val courtBookingRequest2 = courtBookingRequest(
        courtCode = DERBY_JUSTICE_CENTRE,
        prisonerNumber = "A1111AA",
        prisonCode = PENTONVILLE,
        location = pentonvilleLocation,
        startTime = LocalTime.of(14, 0),
        endTime = LocalTime.of(14, 30),
      )

      webTestClient.createBooking(courtBookingRequest2, COURT_USER)

      val courtBookingRequest3 = courtBookingRequest(
        courtCode = CHESTERFIELD_JUSTICE_CENTRE,
        prisonerNumber = "A1111AA",
        prisonCode = PENTONVILLE,
        location = pentonvilleLocation,
        startTime = LocalTime.of(15, 0),
        endTime = LocalTime.of(15, 30),
      )

      webTestClient.createBooking(courtBookingRequest3, COURT_USER)
    }
  }

  @Nested
  @DisplayName("Probation schedule")
  inner class ProbationSchedule {
    @Test
    fun `Probation - return no items when only court bookings are present`() {
      videoBookingRepository.findAll() hasSize 0

      prisonSearchApi().stubGetPrisoner("A1111AA", PENTONVILLE)

      // Will default to tomorrow's date
      val courtBookingRequest = courtBookingRequest(
        courtCode = DERBY_JUSTICE_CENTRE,
        prisonerNumber = "A1111AA",
        prisonCode = PENTONVILLE,
        location = pentonvilleLocation,
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(12, 30),
      )

      webTestClient.createBooking(courtBookingRequest, COURT_USER)

      // Check for tomorrow's date
      val scheduleResponse = webTestClient.getProbationSchedule(
        probationTeamCode = "BLKPPP",
        date = tomorrow(),
      )

      assertThat(scheduleResponse).isNotNull
      assertThat(scheduleResponse).hasSize(0)
    }

    @Test
    fun `Probation - return items when bookings are present for this probation team`() {
      videoBookingRepository.findAll() hasSize 0

      prisonSearchApi().stubGetPrisoner("A1111AA", PENTONVILLE)

      // Will default to tomorrow's date
      val probationBookingRequest = probationBookingRequest(
        probationTeamCode = "BLKPPP",
        probationMeetingType = ProbationMeetingType.PSR,
        prisonCode = PENTONVILLE,
        prisonerNumber = "A1111AA",
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(9, 30),
        appointmentType = AppointmentType.VLB_PROBATION,
        location = pentonvilleLocation,
        additionalBookingDetails = AdditionalBookingDetails(
          contactName = "probation contact name",
          contactEmail = "probation.contact@email.com",
          contactNumber = null,
        ),
      )

      webTestClient.createBooking(probationBookingRequest, PROBATION_USER)

      // Check for tomorrow's date
      val scheduleResponse = webTestClient.getProbationSchedule(
        probationTeamCode = "BLKPPP",
        date = tomorrow(),
      )

      assertThat(scheduleResponse).isNotNull
      assertThat(scheduleResponse).hasSize(1)

      with(scheduleResponse.first()) {
        assertThat(probationTeamCode).isEqualTo("BLKPPP")
        assertThat(prisonCode).isEqualTo(PENTONVILLE)
        assertThat(appointmentType).isEqualTo(AppointmentType.VLB_PROBATION)
        assertThat(prisonLocKey).isEqualTo(pentonvilleLocation.key)
        assertThat(prisonerNumber).isEqualTo("A1111AA")
        assertThat(appointmentDate).isEqualTo(tomorrow())
        assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(endTime).isEqualTo(LocalTime.of(9, 30))
        assertThat(probationOfficerName).isEqualTo("probation contact name")
        assertThat(probationOfficerEmailAddress).isEqualTo("probation.contact@email.com")
      }
    }

    @Test
    fun `Probation - return paginated list of bookings for multiple teams (default sort date and time)`() {
      prisonSearchApi().stubGetPrisoner("A1111AA", PENTONVILLE)

      videoBookingRepository.findAll() hasSize 0

      createThreeProbationBookings()

      videoBookingRepository.findAll() hasSize 3

      val scheduleResponsePage1 = webTestClient.getPaginatedProbationTeamsSchedule(
        probationTeamCodes = listOf("BLKPPP", "SHEFCC"),
        date = tomorrow(),
        page = 0,
        size = 2,
      )

      assertThat(scheduleResponsePage1.content).hasSize(2)
      assertThat(scheduleResponsePage1.page.number).isEqualTo(0)
      with(scheduleResponsePage1.content.first()) {
        assertThat(startTime).isEqualTo("09:00")
      }

      val scheduleResponsePage2 = webTestClient.getPaginatedProbationTeamsSchedule(
        probationTeamCodes = listOf("BLKPPP", "SHEFCC"),
        date = tomorrow(),
        page = 1,
        size = 2,
      )

      assertThat(scheduleResponsePage2.content).hasSize(1)
      assertThat(scheduleResponsePage2.page.number).isEqualTo(1)
      with(scheduleResponsePage2.content.first()) {
        assertThat(startTime).isEqualTo("11:00")
      }
    }

    private fun createThreeProbationBookings() {
      // Bookings will default to tomorrow's date
      val probationBookingRequest1 = probationBookingRequest(
        probationTeamCode = "BLKPPP",
        prisonCode = PENTONVILLE,
        prisonerNumber = "A1111AA",
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(9, 30),
        location = pentonvilleLocation,
      )

      webTestClient.createBooking(probationBookingRequest1, PROBATION_USER)

      val probationBookingRequest2 = probationBookingRequest(
        probationTeamCode = "BLKPPP",
        prisonCode = PENTONVILLE,
        prisonerNumber = "A1111AA",
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(10, 30),
        location = pentonvilleLocation,
      )

      webTestClient.createBooking(probationBookingRequest2, PROBATION_USER)

      val probationBookingRequest3 = probationBookingRequest(
        probationTeamCode = "SHEFCC",
        prisonCode = PENTONVILLE,
        prisonerNumber = "A1111AA",
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        location = pentonvilleLocation,
      )

      webTestClient.createBooking(probationBookingRequest3, PROBATION_USER)
    }
  }

  private fun WebTestClient.getPrisonSchedule(prisonCode: String, date: LocalDate, cancelled: Boolean = false) = this
    .get()
    .uri("/schedule/prison/{prisonCode}?date=$date&includeCancelled=$cancelled", prisonCode, date, cancelled)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList<ScheduleItem>()
    .returnResult().responseBody!!

  private fun WebTestClient.getCourtSchedule(courtCode: String, date: LocalDate, cancelled: Boolean = false) = this
    .get()
    .uri("/schedule/court/{courtCode}?date=$date&includeCancelled=$cancelled", courtCode, date, cancelled)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList<ScheduleItem>()
    .returnResult().responseBody!!

  private fun WebTestClient.getProbationSchedule(probationTeamCode: String, date: LocalDate, cancelled: Boolean = false) = this
    .get()
    .uri("/schedule/probation/{probationTeamCode}?date=$date&includeCancelled=$cancelled", probationTeamCode, date, cancelled)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList<ScheduleItem>()
    .returnResult().responseBody!!

  private fun WebTestClient.getPaginatedCourtsSchedule(
    courtCodes: List<String>,
    date: LocalDate,
    page: Int? = 0,
    size: Int? = 10,
    sortField1: String? = "appointmentDate",
    sortField2: String? = "startTime",
  ) = this.post()
    .uri(
      UriComponentsBuilder.fromPath("/schedule/courts/paginated")
        .queryParam("page", page)
        .queryParam("size", size)
        .queryParam("sort", sortField1)
        .queryParam("sort", sortField2)
        .build()
        .toUri().toString(),
    )
    .bodyValue(FindCourtBookingsRequest(courtCodes = courtCodes, date = date))
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<PaginatedResponse>()
    .returnResult().responseBody!!

  private fun WebTestClient.getPaginatedProbationTeamsSchedule(
    probationTeamCodes: List<String>,
    date: LocalDate,
    page: Int? = 0,
    size: Int? = 10,
    sortField1: String? = "appointmentDate",
    sortField2: String? = "startTime",
  ) = this.post()
    .uri(
      UriComponentsBuilder.fromPath("/schedule/probation-teams/paginated")
        .queryParam("page", page)
        .queryParam("size", size)
        .queryParam("sort", sortField1)
        .queryParam("sort", sortField2)
        .build()
        .toUri().toString(),
    )
    .bodyValue(FindProbationBookingsRequest(probationTeamCodes = probationTeamCodes, date = date))
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<PaginatedResponse>()
    .returnResult().responseBody!!
}

data class PaginatedResponse(
  val content: List<ScheduleItem>,
  val page: PagedModel.PageMetadata,
)
