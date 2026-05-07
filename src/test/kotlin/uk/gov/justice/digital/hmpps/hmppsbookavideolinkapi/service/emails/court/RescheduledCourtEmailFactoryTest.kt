package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court

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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withPreMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDate
import java.time.LocalTime

class RescheduledCourtEmailFactoryTest {
  private val locationService: LocationsService = mock()
  private val factory = RescheduledCourtEmailFactory(locationService)
  private val old = bookingWithPreAndMain(
    date = tomorrow(),
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    birminghamLocation,
  ).toModel(setOf(birminghamLocation.toModel()))
  private val amended = bookingWithMainOnly(
    date = tomorrow(),
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
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
      userContact = bookingContact(contactType = ContactType.USER, email = "user@email.com"),
    )

    email isInstanceOf RescheduledCourtBookingUserEmail::class.java
    email.address isEqualTo "user@email.com"
    email.personalisation() containsEntriesExactlyInAnyOrder mapOf(
      "comments" to "None entered",
      "court" to "DRBYMC",
      "courtHearingLink" to "https://court.hearing.link",
      "date" to amended.startDateTime()!!.toLocalDate().toMediumFormatStyle(),
      "mainAppointmentInfo" to "Birmingham room - 10:00 to 11:00",
      "offenderNo" to "123456",
      "oldAppointmentDate" to old.startDateTime().toLocalDate().toMediumFormatStyle(),
      "oldMainAppointmentInfo" to "Birmingham Room - 10:00 to 11:00",
      "oldPostAppointmentInfo" to "Not required",
      "oldPreAppointmentInfo" to "Birmingham Room - 09:45 to 10:45",
      "postAppointmentInfo" to "Not required",
      "postPrisonVideoUrl" to "",
      "preAppointmentInfo" to "Not required",
      "prePrisonVideoUrl" to "",
      "prison" to "Birmingham",
      "prisonerName" to "Fred Bloggs",
      "userName" to "Book Video",
    )
  }

  @Test
  fun `should create a court email`() {
    val email = factory.court(
      oldBooking = old,
      amendedBooking = amended,
      prison = prison(prisonCode = BIRMINGHAM),
      prisoner = prisoner(prisonCode = BIRMINGHAM, prisonerNumber = "123456"),
      courtContact = bookingContact(contactType = ContactType.COURT, email = "court@email.com"),
    )

    email isInstanceOf RescheduledCourtBookingCourtEmail::class.java
    email.address isEqualTo "court@email.com"
    email.personalisation() containsEntriesExactlyInAnyOrder mapOf(
      "comments" to "None entered",
      "court" to "DRBYMC",
      "courtHearingLink" to "https://court.hearing.link",
      "date" to amended.startDateTime()!!.toLocalDate().toMediumFormatStyle(),
      "mainAppointmentInfo" to "Birmingham room - 10:00 to 11:00",
      "offenderNo" to "123456",
      "oldAppointmentDate" to old.startDateTime().toLocalDate().toMediumFormatStyle(),
      "oldMainAppointmentInfo" to "Birmingham Room - 10:00 to 11:00",
      "oldPostAppointmentInfo" to "Not required",
      "oldPreAppointmentInfo" to "Birmingham Room - 09:45 to 10:45",
      "postAppointmentInfo" to "Not required",
      "postPrisonVideoUrl" to "",
      "preAppointmentInfo" to "Not required",
      "prePrisonVideoUrl" to "",
      "prison" to "Birmingham",
      "prisonerName" to "Fred Bloggs",
    )
  }

  @Test
  fun `should create a prison email, no court contact`() {
    val email = factory.prison(
      oldBooking = old,
      amendedBooking = amended,
      prison = prison(prisonCode = BIRMINGHAM),
      prisoner = prisoner(prisonCode = BIRMINGHAM, prisonerNumber = "123456"),
      prisonContact = bookingContact(contactType = ContactType.PRISON, email = "prison@email.com"),
      contacts = emptyList(),
    )

    email isInstanceOf RescheduledCourtBookingPrisonNoCourtEmail::class.java
    email.address isEqualTo "prison@email.com"
    email.personalisation() containsEntriesExactlyInAnyOrder mapOf(
      "comments" to "None entered",
      "court" to "DRBYMC",
      "courtHearingLink" to "https://court.hearing.link",
      "date" to amended.startDateTime()!!.toLocalDate().toMediumFormatStyle(),
      "mainAppointmentInfo" to "Birmingham room - 10:00 to 11:00",
      "offenderNo" to "123456",
      "oldAppointmentDate" to old.startDateTime().toLocalDate().toMediumFormatStyle(),
      "oldMainAppointmentInfo" to "Birmingham Room - 10:00 to 11:00",
      "oldPostAppointmentInfo" to "Not required",
      "oldPreAppointmentInfo" to "Birmingham Room - 09:45 to 10:45",
      "postAppointmentInfo" to "Not required",
      "postPrisonVideoUrl" to "",
      "preAppointmentInfo" to "Not required",
      "prePrisonVideoUrl" to "",
      "prison" to "Birmingham",
      "prisonerName" to "Fred Bloggs",
    )
  }

  @Test
  fun `should create a prison email, with court contact`() {
    val email = factory.prison(
      oldBooking = old,
      amendedBooking = amended,
      prison = prison(prisonCode = BIRMINGHAM),
      prisoner = prisoner(prisonCode = BIRMINGHAM, prisonerNumber = "123456"),
      prisonContact = bookingContact(contactType = ContactType.PRISON, email = "prison@email.com"),
      contacts = listOf(bookingContact(contactType = ContactType.COURT, email = "court@email.com")),
    )

    email isInstanceOf RescheduledCourtBookingPrisonCourtEmail::class.java
    email.address isEqualTo "prison@email.com"
    email.personalisation() containsEntriesExactlyInAnyOrder mapOf(
      "comments" to "None entered",
      "court" to "DRBYMC",
      "courtEmailAddress" to "court@email.com",
      "courtHearingLink" to "https://court.hearing.link",
      "date" to amended.startDateTime()!!.toLocalDate().toMediumFormatStyle(),
      "mainAppointmentInfo" to "Birmingham room - 10:00 to 11:00",
      "offenderNo" to "123456",
      "oldAppointmentDate" to old.startDateTime().toLocalDate().toMediumFormatStyle(),
      "oldMainAppointmentInfo" to "Birmingham Room - 10:00 to 11:00",
      "oldPostAppointmentInfo" to "Not required",
      "oldPreAppointmentInfo" to "Birmingham Room - 09:45 to 10:45",
      "postAppointmentInfo" to "Not required",
      "postPrisonVideoUrl" to "",
      "preAppointmentInfo" to "Not required",
      "prePrisonVideoUrl" to "",
      "prison" to "Birmingham",
      "prisonerName" to "Fred Bloggs",
    )
  }

  private fun bookingWithPreAndMain(date: LocalDate, startTime: LocalTime, endTime: LocalTime, location: Location) = run {
    courtBooking().withPreMainCourtPrisonAppointment(
      date = date,
      startTime = startTime,
      endTime = endTime,
      location = location,
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
    )
  }

  private fun bookingWithMainOnly(date: LocalDate, startTime: LocalTime, endTime: LocalTime, location: Location) = run {
    courtBooking().withMainCourtPrisonAppointment(
      date = date,
      startTime = startTime,
      endTime = endTime,
      location = location,
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
    )
  }
}
