package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WERRINGTON
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.werringtonLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalTime

class BookingContactsResourceIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Test
  fun `should return a list of prison and court contacts for a booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("A1111AA", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

    // For court DRBYMC
    val courtBookingRequest = courtBookingRequest(
      courtId = 1L,
      prisonerNumber = "A1111AA",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest)

    videoBookingRepository.findAll() hasSize 1

    // DRBYMC court has 3 contacts, WNI prison has 3 contacts
    val listOfContacts = webTestClient.getBookingContacts("user", bookingId)

    // Check contacts returned
    assertThat(listOfContacts).extracting("contactType").contains(ContactType.PRISON)
    assertThat(listOfContacts).extracting("contactType").contains(ContactType.COURT)
    assertThat(listOfContacts).extracting("contactType").doesNotContain(ContactType.PROBATION)
    assertThat(listOfContacts).hasSize(6)
    assertThat(listOfContacts).extracting("email").containsAll(
      listOf("m@m.com", "t@t.com", "s@s.com"),
    )
  }

  @Test
  fun `should return a list of prison and probation contacts for a booking`() {
    prisonSearchApi().stubGetPrisoner("A1111AA", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

    // For probation team BLKPPP
    val probationBookingRequest = probationBookingRequest(
      probationTeamId = 1L,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = WERRINGTON,
      prisonerNumber = "A1111AA",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = werringtonLocation,
    )

    val bookingId = webTestClient.createBooking(probationBookingRequest)

    videoBookingRepository.findAll() hasSize 1

    // BLKPPP probation team has 3 contacts, WNI prison has 3 contacts
    val listOfContacts = webTestClient.getBookingContacts("user", bookingId)

    // Check contacts returned
    assertThat(listOfContacts).extracting("contactType").contains(ContactType.PRISON)
    assertThat(listOfContacts).extracting("contactType").contains(ContactType.PROBATION)
    assertThat(listOfContacts).extracting("contactType").doesNotContain(ContactType.COURT)
    assertThat(listOfContacts).hasSize(6)
    assertThat(listOfContacts).extracting("email").containsAll(
      listOf("m@m.com", "t@t.com", "s@s.com"),
    )
  }

  @Test
  fun `should return an empty list where no prison or court contacts are defined for a booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("A1111AA", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    // For court NWPIAC
    val courtBookingRequest = courtBookingRequest(
      courtId = 2L,
      prisonerNumber = "A1111AA",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest)

    videoBookingRepository.findAll() hasSize 1

    // NWPIAC court has no contacts, and BMI prison has 0 contacts
    val listOfContacts = webTestClient.getBookingContacts("user", bookingId)

    assertThat(listOfContacts).isEmpty()
  }

  @Test
  fun `should return a list of third party contacts for a booking`() {
    // TODO: Not implemented yet
  }

  @Test
  fun `should return the owner contact details for a booking`() {
    // TODO: Not implemented yet
  }

  private fun WebTestClient.getBookingContacts(username: String, videoBookingId: Long) =
    get()
      .uri("/booking-contacts/id/{videoBookingId}", videoBookingId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(BookingContact::class.java)
      .returnResult().responseBody

  private fun WebTestClient.createBooking(request: CreateVideoBookingRequest) =
    this
      .post()
      .uri("/video-link-booking")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "BOOKING_CREATOR", roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Long::class.java)
      .returnResult().responseBody!!
}
