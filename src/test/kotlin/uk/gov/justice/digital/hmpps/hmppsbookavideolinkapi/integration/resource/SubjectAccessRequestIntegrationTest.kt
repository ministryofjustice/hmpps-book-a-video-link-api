package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.CvpLinkDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.AmendCourtBookingRequestBuilder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.AmendProbationBookingRequestBuilder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.CreateCourtBookingRequestBuilder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.CreateProbationBookingRequestBuilder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.fifth
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.fourth
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isNotEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvillePrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.second
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.sixth
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.third
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.TestConfiguration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.SubjectAccessRequestDto
import java.time.LocalDate
import java.time.LocalDateTime.now
import java.time.LocalTime

@Import(TestConfiguration::class)
class SubjectAccessRequestIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  fun `set up prison user and prisoner`() {
    stubUser(PRISON_USER_PENTONVILLE)
    prisonSearchApi().stubGetPrisoner(pentonvillePrisoner)
  }

  @Nested
  inner class CourtBookings {
    @BeforeEach
    fun `set up court bookings`() {
      val bookingId = CreateCourtBookingRequestBuilder.builder {
        court(DERBY_JUSTICE_CENTRE)
        prisoner(pentonvillePrisoner)
        hearingType(CourtHearingType.TRIBUNAL)
        pre(pentonvilleLocation)
        main(pentonvilleLocation, tomorrow(), LocalTime.of(1, 0), LocalTime.of(1, 30))
        post(pentonvilleLocation)
        cvpLinkDetails(CvpLinkDetails.url("cvp-link"))
        pin("1234")
        staffNotes("Some staff notes")
        prisonerNotes("Some prisoner notes")
      }.build().let { webTestClient.createBooking(it, PRISON_USER_PENTONVILLE) }

      AmendCourtBookingRequestBuilder.builder {
        prisoner(pentonvillePrisoner)
        hearingType(CourtHearingType.CIVIL)
        pre(pentonvilleLocation)
        main(pentonvilleLocation, tomorrow(), LocalTime.of(2, 30), LocalTime.of(3, 0))
        post(pentonvilleLocation)
        cvpLinkDetails(CvpLinkDetails.hmctsNumber("12345"))
        pin("4321")
        staffNotes("Amended staff notes")
        prisonerNotes("Amended prisoner notes")
      }.build().let { webTestClient.amendBooking(bookingId, it, PRISON_USER_PENTONVILLE) }
    }

    @Test
    fun `SAR API should return expected data`() {
      val response = webTestClient.getSarContent(pentonvillePrisoner.number, yesterday(), today())

      response.content hasSize 6

      // Creatied PRE
      with(response.content.first()) {
        prisonCode isEqualTo PENTONVILLE
        prisonerNumber isEqualTo pentonvillePrisoner.number
        historyType isEqualTo HistoryType.CREATE
        courtId isNotEqualTo null
        probationTeamId isEqualTo null
        hearingType isEqualTo CourtHearingType.TRIBUNAL.name
        appointmentType isEqualTo "VLB_COURT_PRE"
        staffNotes isEqualTo "Some staff notes"
        prisonerNotes isEqualTo "Some prisoner notes"
        prisonLocationId isEqualTo pentonvilleLocation.id
        appointmentDate isEqualTo tomorrow()
        startTime isEqualTo LocalTime.of(0, 45)
        endTime isEqualTo LocalTime.of(1, 0)
        createdBy isEqualTo PRISON_USER_PENTONVILLE.username
        createdTime isCloseTo now()
      }
      // Created MAIN
      with(response.content.second()) {
        prisonCode isEqualTo PENTONVILLE
        prisonerNumber isEqualTo pentonvillePrisoner.number
        historyType isEqualTo HistoryType.CREATE
        courtId isNotEqualTo null
        probationTeamId isEqualTo null
        hearingType isEqualTo CourtHearingType.TRIBUNAL.name
        appointmentType isEqualTo "VLB_COURT_MAIN"
        staffNotes isEqualTo "Some staff notes"
        prisonerNotes isEqualTo "Some prisoner notes"
        prisonLocationId isEqualTo pentonvilleLocation.id
        appointmentDate isEqualTo tomorrow()
        startTime isEqualTo LocalTime.of(1, 0)
        endTime isEqualTo LocalTime.of(1, 30)
        createdBy isEqualTo PRISON_USER_PENTONVILLE.username
        createdTime isCloseTo now()
      }
      // Created POST
      with(response.content.third()) {
        prisonCode isEqualTo PENTONVILLE
        prisonerNumber isEqualTo pentonvillePrisoner.number
        historyType isEqualTo HistoryType.CREATE
        courtId isNotEqualTo null
        probationTeamId isEqualTo null
        hearingType isEqualTo CourtHearingType.TRIBUNAL.name
        appointmentType isEqualTo "VLB_COURT_POST"
        staffNotes isEqualTo "Some staff notes"
        prisonerNotes isEqualTo "Some prisoner notes"
        prisonLocationId isEqualTo pentonvilleLocation.id
        appointmentDate isEqualTo tomorrow()
        startTime isEqualTo LocalTime.of(1, 30)
        endTime isEqualTo LocalTime.of(1, 45)
        createdBy isEqualTo PRISON_USER_PENTONVILLE.username
        createdTime isCloseTo now()
      }

      // Amended PRE
      with(response.content.fourth()) {
        prisonCode isEqualTo PENTONVILLE
        prisonerNumber isEqualTo pentonvillePrisoner.number
        historyType isEqualTo HistoryType.AMEND
        courtId isNotEqualTo null
        probationTeamId isEqualTo null
        hearingType isEqualTo CourtHearingType.CIVIL.name
        appointmentType isEqualTo "VLB_COURT_PRE"
        staffNotes isEqualTo "Amended staff notes"
        prisonerNotes isEqualTo "Amended prisoner notes"
        prisonLocationId isEqualTo pentonvilleLocation.id
        appointmentDate isEqualTo tomorrow()
        startTime isEqualTo LocalTime.of(2, 15)
        endTime isEqualTo LocalTime.of(2, 30)
        createdBy isEqualTo PRISON_USER_PENTONVILLE.username
        createdTime isCloseTo now()
      }
      // Amended MAIN
      with(response.content.fifth()) {
        prisonCode isEqualTo PENTONVILLE
        prisonerNumber isEqualTo pentonvillePrisoner.number
        historyType isEqualTo HistoryType.AMEND
        courtId isNotEqualTo null
        probationTeamId isEqualTo null
        hearingType isEqualTo CourtHearingType.CIVIL.name
        appointmentType isEqualTo "VLB_COURT_MAIN"
        staffNotes isEqualTo "Amended staff notes"
        prisonerNotes isEqualTo "Amended prisoner notes"
        prisonLocationId isEqualTo pentonvilleLocation.id
        appointmentDate isEqualTo tomorrow()
        startTime isEqualTo LocalTime.of(2, 30)
        endTime isEqualTo LocalTime.of(3, 0)
        createdBy isEqualTo PRISON_USER_PENTONVILLE.username
        createdTime isCloseTo now()
      }
      // Amended POST
      with(response.content.sixth()) {
        prisonCode isEqualTo PENTONVILLE
        prisonerNumber isEqualTo pentonvillePrisoner.number
        historyType isEqualTo HistoryType.AMEND
        courtId isNotEqualTo null
        probationTeamId isEqualTo null
        hearingType isEqualTo CourtHearingType.CIVIL.name
        appointmentType isEqualTo "VLB_COURT_POST"
        staffNotes isEqualTo "Amended staff notes"
        prisonerNotes isEqualTo "Amended prisoner notes"
        prisonLocationId isEqualTo pentonvilleLocation.id
        appointmentDate isEqualTo tomorrow()
        startTime isEqualTo LocalTime.of(3, 0)
        endTime isEqualTo LocalTime.of(3, 15)
        createdBy isEqualTo PRISON_USER_PENTONVILLE.username
        createdTime isCloseTo now()
      }
    }
  }

  @Nested
  inner class ProbationBookings {
    @BeforeEach
    fun `set up probation bookings`() {
      val bookingId = CreateProbationBookingRequestBuilder.builder {
        prisoner(pentonvillePrisoner)
        probationTeam(BLACKPOOL_MC_PPOC)
        meetingType(ProbationMeetingType.FTR56)
        main(pentonvilleLocation, tomorrow(), LocalTime.of(1, 0), LocalTime.of(1, 30))
        staffNotes("Some staff notes")
        prisonerNotes("Some prisoner notes")
      }.build().let { webTestClient.createBooking(it, PRISON_USER_PENTONVILLE) }

      AmendProbationBookingRequestBuilder.builder {
        prisoner(pentonvillePrisoner)
        meetingType(ProbationMeetingType.PSR)
        main(pentonvilleLocation, tomorrow(), LocalTime.of(7, 0), LocalTime.of(7, 30))
        staffNotes("Amended staff notes")
        prisonerNotes("Amended prisoner notes")
      }.build().let { webTestClient.amendBooking(bookingId, it, PRISON_USER_PENTONVILLE) }
    }

    @Test
    fun `SAR API should return expected data`() {
      val response = webTestClient.getSarContent(pentonvillePrisoner.number, yesterday(), today())

      response.content hasSize 2

      // Created MAIN
      with(response.content.first()) {
        prisonCode isEqualTo PENTONVILLE
        prisonerNumber isEqualTo pentonvillePrisoner.number
        historyType isEqualTo HistoryType.CREATE
        courtId isEqualTo null
        probationTeamId isNotEqualTo null
        probationMeetingType isEqualTo ProbationMeetingType.FTR56.name
        appointmentType isEqualTo "VLB_PROBATION"
        staffNotes isEqualTo "Some staff notes"
        prisonerNotes isEqualTo "Some prisoner notes"
        prisonLocationId isEqualTo pentonvilleLocation.id
        appointmentDate isEqualTo tomorrow()
        startTime isEqualTo LocalTime.of(1, 0)
        endTime isEqualTo LocalTime.of(1, 30)
        createdBy isEqualTo PRISON_USER_PENTONVILLE.username
        createdTime isCloseTo now()
      }

      // Amended MAIN
      with(response.content.second()) {
        prisonCode isEqualTo PENTONVILLE
        prisonerNumber isEqualTo pentonvillePrisoner.number
        historyType isEqualTo HistoryType.AMEND
        courtId isEqualTo null
        probationTeamId isNotEqualTo null
        probationMeetingType isEqualTo ProbationMeetingType.PSR.name
        appointmentType isEqualTo "VLB_PROBATION"
        staffNotes isEqualTo "Amended staff notes"
        prisonerNotes isEqualTo "Amended prisoner notes"
        prisonLocationId isEqualTo pentonvilleLocation.id
        appointmentDate isEqualTo tomorrow()
        startTime isEqualTo LocalTime.of(7, 0)
        endTime isEqualTo LocalTime.of(7, 30)
        createdBy isEqualTo PRISON_USER_PENTONVILLE.username
        createdTime isCloseTo now()
      }
    }
  }

  private fun WebTestClient.getSarContent(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) = get()
    .uri("/subject-access-request?prn=$prisonerNumber&fromDate=$fromDate&toDate=$toDate")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(SubjectAccessRequestContent::class.java)
    .returnResult().responseBody!!

  @Nested
  inner class NoBookings {
    @Test
    fun `SAR API should return a 204 no content when no data`() {
      webTestClient.noSarContent(pentonvillePrisoner.number, yesterday(), today())
    }
  }

  private fun WebTestClient.noSarContent(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) = get()
    .uri("/subject-access-request?prn=$prisonerNumber&fromDate=$fromDate&toDate=$toDate")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
    .exchange()
    .expectStatus().isNoContent
}

data class SubjectAccessRequestContent(val content: List<SubjectAccessRequestDto>)
