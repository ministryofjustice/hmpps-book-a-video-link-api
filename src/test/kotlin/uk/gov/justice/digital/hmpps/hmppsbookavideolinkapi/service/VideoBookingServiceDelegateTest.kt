package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.validation.ValidationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.requestProbationVideoLinkRequest
import java.time.LocalDate

class VideoBookingServiceDelegateTest {
  private val createCourtBookingService: CreateCourtBookingService = mock()
  private val createProbationBookingService: CreateProbationBookingService = mock()
  private val amendCourtBookingService: AmendCourtBookingService = mock()
  private val amendProbationBookingService: AmendProbationBookingService = mock()
  private val cancelVideoBookingService: CancelVideoBookingService = mock()
  private val requestBookingService: RequestBookingService = mock()
  private val maxDaysIntoFuture = 100L

  private val delegate =
    VideoBookingServiceDelegate(
      createCourtBookingService,
      createProbationBookingService,
      amendCourtBookingService,
      amendProbationBookingService,
      cancelVideoBookingService,
      requestBookingService,
      maxDaysIntoFuture,
    )

  private val courtBookingRequest = courtBookingRequest()
  private val probationBookingRequest = probationBookingRequest()
  private val amendCourtBookingRequest = amendCourtBookingRequest()
  private val amendProbationBookingRequest = amendProbationBookingRequest()
  private val requestVideoBookingRequest = requestProbationVideoLinkRequest()

  @Test
  fun `should delegate to correct create booking service`() {
    delegate.create(courtBookingRequest, COURT_USER)
    verify(createCourtBookingService).create(courtBookingRequest, COURT_USER)

    delegate.create(probationBookingRequest, PROBATION_USER)
    verify(createProbationBookingService).create(probationBookingRequest, PROBATION_USER)

    verifyNoMoreInteractions(createCourtBookingService)
  }

  @Test
  fun `should delegate to correct amend booking service`() {
    delegate.amend(1, amendCourtBookingRequest, COURT_USER)
    verify(amendCourtBookingService).amend(1, amendCourtBookingRequest, COURT_USER)

    delegate.amend(2, amendProbationBookingRequest, PROBATION_USER)
    verify(amendProbationBookingService).amend(2, amendProbationBookingRequest, PROBATION_USER)

    verifyNoInteractions(createCourtBookingService, createProbationBookingService)
  }

  @Test
  fun `should delegate to cancel booking service`() {
    delegate.cancel(1, COURT_USER)
    verify(cancelVideoBookingService).cancel(1, COURT_USER)

    verifyNoInteractions(createCourtBookingService, createProbationBookingService, amendCourtBookingService)
  }

  @Test
  fun `should delegate to request booking service service`() {
    delegate.request(requestVideoBookingRequest, COURT_USER)

    verify(requestBookingService).request(requestVideoBookingRequest, COURT_USER)

    verifyNoInteractions(createCourtBookingService, createProbationBookingService, amendCourtBookingService, cancelVideoBookingService)
  }

  @Test
  fun `should fail when start date is too far into the future`() {
    val futureDate = LocalDate.now().plusDays(maxDaysIntoFuture + 1)

    val functions = listOf<() -> Unit>(
      { delegate.create(courtBookingRequest(date = futureDate), COURT_USER) },
      { delegate.amend(1, amendCourtBookingRequest(appointmentDate = futureDate), COURT_USER) },
      { delegate.request(requestProbationVideoLinkRequest(date = futureDate), COURT_USER) },
    )

    functions.forEach {
      assertThrows<ValidationException> { it.invoke() }.message isEqualTo "Start date cannot be more than $maxDaysIntoFuture days into the future"
    }
  }
}
