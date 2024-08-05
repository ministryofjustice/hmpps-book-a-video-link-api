package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WERRINGTON
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.werringtonLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.TEST_EXTERNAL_USER_EMAIL
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.BookingContact
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

    val courtBookingRequest = courtBookingRequest(
      courtCode = "DRBYMC",
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
    val listOfContacts = webTestClient.getBookingContacts(bookingId)

    // Check contacts returned
    assertThat(listOfContacts).extracting("contactType").contains(ContactType.PRISON)
    assertThat(listOfContacts).extracting("contactType").contains(ContactType.COURT)
    assertThat(listOfContacts).extracting("contactType").contains(ContactType.USER)
    assertThat(listOfContacts).extracting("contactType").doesNotContain(ContactType.PROBATION)
    assertThat(listOfContacts).hasSize(7)
    assertThat(listOfContacts).extracting("email").containsAll(listOf("m@m.com", "t@t.com", "s@s.com", TEST_EXTERNAL_USER_EMAIL))
  }

  @Test
  fun `should return a list of prison and probation contacts for a booking`() {
    prisonSearchApi().stubGetPrisoner("A1111AA", WERRINGTON)
    locationsInsidePrisonApi().stubGetLocationByKey(werringtonLocation.key, WERRINGTON)

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

    val bookingId = webTestClient.createBooking(probationBookingRequest)

    videoBookingRepository.findAll() hasSize 1

    // BLKPPP probation team has 3 contacts, WNI prison has 3 contacts
    val listOfContacts = webTestClient.getBookingContacts(bookingId)

    // Check contacts returned
    assertThat(listOfContacts).extracting("contactType").contains(ContactType.PRISON)
    assertThat(listOfContacts).extracting("contactType").contains(ContactType.PROBATION)
    assertThat(listOfContacts).extracting("contactType").contains(ContactType.USER)
    assertThat(listOfContacts).extracting("contactType").doesNotContain(ContactType.COURT)
    assertThat(listOfContacts).hasSize(7)
    assertThat(listOfContacts).extracting("email").containsAll(listOf("m@m.com", "t@t.com", "s@s.com", TEST_EXTERNAL_USER_EMAIL))
  }

  @Test
  fun `should return the user only, where no prison or court contacts are defined for a booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("A1111AA", MOORLAND)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(moorlandLocation.key), MOORLAND)

    val courtBookingRequest = courtBookingRequest(
      courtCode = "NWPIAC",
      prisonerNumber = "A1111AA",
      prisonCode = MOORLAND,
      location = moorlandLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest)

    videoBookingRepository.findAll() hasSize 1

    // NWPIAC court has 0 contacts, and BMI prison has 0 contacts
    val listOfContacts = webTestClient.getBookingContacts(bookingId)

    listOfContacts hasSize 1
    assertThat(listOfContacts).extracting("contactType").contains(ContactType.USER)
  }

  @Test
  fun `should return 404 where an invalid video booking ID was supplied`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("A1111AA", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val courtBookingRequest = courtBookingRequest(
      courtCode = "DRBYMC",
      prisonerNumber = "A1111AA",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest)

    videoBookingRepository.findAll() hasSize 1

    val errorResponse = webTestClient.getBookingContactsNotFound(bookingId + 300)

    assertThat(errorResponse?.status).isEqualTo(404)
  }

  @Test
  fun `should return a list of third party contacts for a booking`() {
    // TODO: Not implemented yet
  }

  @Test
  fun `should return the user contact details for a created booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("A1111AA", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

    val courtBookingRequest = courtBookingRequest(
      courtCode = "DRBYMC",
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
    val listOfContacts = webTestClient.getBookingContacts(bookingId)

    assertThat(listOfContacts).hasSize(7)

    // Check that all expected contact types are present
    assertThat(listOfContacts).extracting("contactType").containsOnly(
      ContactType.PRISON,
      ContactType.COURT,
      ContactType.USER,
    )

    // Check that the contact addresses are present
    assertThat(listOfContacts).extracting("email").containsAll(
      listOf("m@m.com", "t@t.com", "s@s.com", TEST_EXTERNAL_USER_EMAIL),
    )

    // Check the user email and name are as expected
    val userObject = listOfContacts.find { it.contactType == ContactType.USER }
    assertThat(userObject?.email).isEqualTo(TEST_EXTERNAL_USER_EMAIL)
    assertThat(userObject?.name).isEqualTo("Test Users Name")
  }

  @Test
  fun `should return two user contact details for an amended booking`() {
    // TODO: Not implemented yet - no way to set the amendedBy value yet
    // If the createdBy and amendedBy usernames are both email addresses, and different, they are returned as joint OWNERS
  }

  private fun WebTestClient.getBookingContactsNotFound(videoBookingId: Long) =
    get()
      .uri("/booking-contacts/id/{videoBookingId}", videoBookingId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getBookingContacts(videoBookingId: Long) =
    get()
      .uri("/booking-contacts/id/{videoBookingId}", videoBookingId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(BookingContact::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.createBooking(request: CreateVideoBookingRequest) =
    this
      .post()
      .uri("/video-link-booking")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Long::class.java)
      .returnResult().responseBody!!
}
