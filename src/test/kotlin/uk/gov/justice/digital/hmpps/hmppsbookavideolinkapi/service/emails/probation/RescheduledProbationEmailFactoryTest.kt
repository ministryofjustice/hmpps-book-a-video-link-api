package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDate
import java.time.LocalTime

class RescheduledProbationEmailFactoryTest {
  private val locationService: LocationsService = mock()
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository = mock()
  private val factory = RescheduledProbationEmailFactory(locationService, additionalBookingDetailRepository)
  private val old = oldBooking(
    date = tomorrow(),
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    birminghamLocation,
  ).toModel(setOf(birminghamLocation.toModel()))
  private val amended = amendedBooking(
    date = tomorrow(),
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(12, 0),
    birminghamLocation,
  )

  @BeforeEach
  fun before() {
    whenever(locationService.getLocationById(birminghamLocation.id)) doReturn birminghamLocation.toModel()
    whenever(locationService.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation.toModel()
  }

  @Test
  fun `should create a user email`() {
    val email = factory.user(
      oldBooking = old,
      amendedBooking = amended,
      prison = prison(prisonCode = BIRMINGHAM),
      prisoner = prisoner(prisonCode = BIRMINGHAM, prisonerNumber = "123456"),
      contact = bookingContact(contactType = ContactType.USER, email = "user@email.com"),
    )

    email isInstanceOf RescheduledProbationBookingUserEmail::class.java
    email.address isEqualTo "user@email.com"
    email.personalisation() containsEntriesExactlyInAnyOrder mapOf(
      "comments" to "None entered",
      "probationTeam" to "probation team description",
      "date" to amended.startDateTime()!!.toLocalDate().toMediumFormatStyle(),
      "appointmentInfo" to "Birmingham room - 11:00 to 12:00",
      "offenderNo" to "123456",
      "oldAppointmentDate" to old.startDateTime().toLocalDate().toMediumFormatStyle(),
      "oldAppointmentInfo" to "Birmingham Room - 10:00 to 11:00",
      "prison" to "Birmingham",
      "prisonerName" to "Fred Bloggs",
      "prisonVideoUrl" to "Not yet known",
      "probationOfficerEmailAddress" to "Not yet known",
      "probationOfficerContactNumber" to "Not yet known",
      "probationOfficerName" to "Not yet known",
      "userName" to "Book Video",
    )
  }

  @Test
  fun `should create a probation email`() {
    val email = factory.probation(
      oldBooking = old,
      amendedBooking = amended,
      prison = prison(prisonCode = BIRMINGHAM),
      prisoner = prisoner(prisonCode = BIRMINGHAM, prisonerNumber = "123456"),
      contact = bookingContact(contactType = ContactType.PROBATION, email = "probation@email.com"),
    )

    email isInstanceOf RescheduledProbationBookingProbationEmail::class.java
    email.address isEqualTo "probation@email.com"
    email.personalisation() containsEntriesExactlyInAnyOrder mapOf(
      "comments" to "None entered",
      "probationTeam" to "probation team description",
      "date" to amended.startDateTime()!!.toLocalDate().toMediumFormatStyle(),
      "appointmentInfo" to "Birmingham room - 11:00 to 12:00",
      "offenderNo" to "123456",
      "oldAppointmentDate" to old.startDateTime().toLocalDate().toMediumFormatStyle(),
      "oldAppointmentInfo" to "Birmingham Room - 10:00 to 11:00",
      "prison" to "Birmingham",
      "prisonVideoUrl" to "Not yet known",
      "prisonerName" to "Fred Bloggs",
      "probationOfficerName" to "Not yet known",
      "probationOfficerEmailAddress" to "Not yet known",
      "probationOfficerContactNumber" to "Not yet known",
    )
  }

  @Test
  fun `should create a prison email, no probation contact`() {
    val email = factory.prison(
      oldBooking = old,
      amendedBooking = amended,
      prison = prison(prisonCode = BIRMINGHAM),
      prisoner = prisoner(prisonCode = BIRMINGHAM, prisonerNumber = "123456"),
      contact = bookingContact(contactType = ContactType.PRISON, email = "prison@email.com"),
      contacts = emptyList(),
    )

    email isInstanceOf RescheduledProbationBookingPrisonNoProbationEmail::class.java
    email.address isEqualTo "prison@email.com"
    email.personalisation() containsEntriesExactlyInAnyOrder mapOf(
      "comments" to "None entered",
      "probationTeam" to "probation team description",
      "date" to amended.startDateTime()!!.toLocalDate().toMediumFormatStyle(),
      "appointmentInfo" to "Birmingham room - 11:00 to 12:00",
      "offenderNo" to "123456",
      "oldAppointmentDate" to old.startDateTime().toLocalDate().toMediumFormatStyle(),
      "oldAppointmentInfo" to "Birmingham Room - 10:00 to 11:00",
      "prison" to "Birmingham",
      "prisonVideoUrl" to "Not yet known",
      "prisonerName" to "Fred Bloggs",
      "probationOfficerName" to "Not yet known",
      "probationOfficerEmailAddress" to "Not yet known",
      "probationOfficerContactNumber" to "Not yet known",
    )
  }

  @Test
  fun `should create a prison email, with probation contact`() {
    val email = factory.prison(
      oldBooking = old,
      amendedBooking = amended,
      prison = prison(prisonCode = BIRMINGHAM),
      prisoner = prisoner(prisonCode = BIRMINGHAM, prisonerNumber = "123456"),
      contact = bookingContact(contactType = ContactType.PRISON, email = "prison@email.com"),
      contacts = listOf(bookingContact(contactType = ContactType.PROBATION, email = "probation@email.com")),
    )

    email isInstanceOf RescheduledProbationBookingPrisonProbationEmail::class.java
    email.address isEqualTo "prison@email.com"
    email.personalisation() containsEntriesExactlyInAnyOrder mapOf(
      "comments" to "None entered",
      "probationTeam" to "probation team description",
      "date" to amended.startDateTime()!!.toLocalDate().toMediumFormatStyle(),
      "appointmentInfo" to "Birmingham room - 11:00 to 12:00",
      "offenderNo" to "123456",
      "oldAppointmentDate" to old.startDateTime().toLocalDate().toMediumFormatStyle(),
      "oldAppointmentInfo" to "Birmingham Room - 10:00 to 11:00",
      "prison" to "Birmingham",
      "prisonVideoUrl" to "Not yet known",
      "prisonerName" to "Fred Bloggs",
      "probationEmailAddress" to "probation@email.com",
      "probationOfficerName" to "Not yet known",
      "probationOfficerEmailAddress" to "Not yet known",
      "probationOfficerContactNumber" to "Not yet known",
    )
  }

  private fun oldBooking(date: LocalDate, startTime: LocalTime, endTime: LocalTime, location: Location) = run {
    probationBooking().withProbationPrisonAppointment(
      date = date,
      startTime = startTime,
      endTime = endTime,
      location = location,
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
    )
  }

  private fun amendedBooking(date: LocalDate, startTime: LocalTime, endTime: LocalTime, location: Location) = run {
    probationBooking().withProbationPrisonAppointment(
      date = date,
      startTime = startTime,
      endTime = endTime,
      location = location,
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
    )
  }
}
