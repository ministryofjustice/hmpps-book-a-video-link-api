package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.facade

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withPreMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ContactsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.RescheduledCourtEmailFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.RescheduledProbationEmailFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDate
import java.time.LocalTime

class RescheduleEmailsFacadeTest {
  private val prisonRepository: PrisonRepository = mock()
  private val contactsService: ContactsService = mock()
  private val rescheduledCourtEmailFactory: RescheduledCourtEmailFactory = mock()
  private val rescheduledProbationEmailFactory: RescheduledProbationEmailFactory = mock()
  private val emailService: EmailService = mock()
  private val notificationRepository: NotificationRepository = mock()
  private val facade = RescheduleEmailsFacade(
    prisonRepository,
    contactsService,
    rescheduledCourtEmailFactory,
    rescheduledProbationEmailFactory,
    emailService,
    notificationRepository,
  )
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

  @Test
  fun `should be considered rescheduled`() {
    facade.isConsideredRescheduled(old, amended) isBool true
  }

  @Test
  fun `should not be considered rescheduled`() {
    facade.isConsideredRescheduled(amended.toModel(setOf(birminghamLocation.toModel())), amended) isBool false
  }

  @Test
  fun `should fail rescheduled check if bookings are not the same`() {
    val originalBooking: VideoLinkBooking = mock()
    whenever { originalBooking.videoLinkBookingId } doReturn 1

    val amendedBooking: VideoBooking = mock()
    whenever { amendedBooking.videoBookingId } doReturn 2

    assertThrows<IllegalArgumentException> {
      facade.isConsideredRescheduled(originalBooking, amendedBooking)
    }.message isEqualTo "Original and amended bookings must have the same video booking ID"
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
