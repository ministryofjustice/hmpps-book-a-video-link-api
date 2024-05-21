package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest

class BookingFacadeTest {
  private val bookingService: CreateVideoBookingService = mock()
  private val emailService: EmailService = mock()

  private val facade = BookingFacade(bookingService, emailService)

  @Test
  fun `should delegate court booking creation to booking creation service`() {
    val booking = courtBookingRequest(prisonCode = MOORLAND, prisonerNumber = "123456")

    whenever(bookingService.create(booking, "facade court user")) doReturn Pair(courtBooking(), prisoner("123456", MOORLAND))

    facade.create(booking, "facade court user")

    verify(bookingService).create(booking, "facade court user")
  }

  @Test
  fun `should delegate probation team booking creation to booking creation service`() {
    val booking = probationBookingRequest(prisonCode = BIRMINGHAM, prisonerNumber = "123456")

    whenever(bookingService.create(booking, "facade probation team user")) doReturn Pair(probationBooking(), prisoner("123456", BIRMINGHAM))

    facade.create(booking, "facade probation team user")

    verify(bookingService).create(booking, "facade probation team user")
  }
}
