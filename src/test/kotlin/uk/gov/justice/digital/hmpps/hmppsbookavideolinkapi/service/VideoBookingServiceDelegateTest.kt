package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest

class VideoBookingServiceDelegateTest {
  private val createCourtBookingService: CreateCourtBookingService = mock()
  private val createProbationBookingService: CreateProbationBookingService = mock()
  private val amendCourtBookingService: AmendCourtBookingService = mock()
  private val amendProbationBookingService: AmendProbationBookingService = mock()
  private val cancelVideoBookingService: CancelVideoBookingService = mock()
  private val delegate =
    VideoBookingServiceDelegate(createCourtBookingService, createProbationBookingService, amendCourtBookingService, amendProbationBookingService, cancelVideoBookingService)

  private val courtBookingRequest = courtBookingRequest()
  private val probationBookingRequest = probationBookingRequest()
  private val amendCourtBookingRequest = amendCourtBookingRequest()
  private val amendProbationBookingRequest = amendProbationBookingRequest()

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
}
