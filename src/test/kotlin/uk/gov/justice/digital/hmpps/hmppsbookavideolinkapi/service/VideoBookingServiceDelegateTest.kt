package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest

class VideoBookingServiceDelegateTest {
  private val createVideoBookingService: CreateVideoBookingService = mock()
  private val createProbationBookingService: CreateProbationBookingService = mock()
  private val amendVideoBookingService: AmendVideoBookingService = mock()
  private val amendProbationBookingService: AmendProbationBookingService = mock()
  private val cancelVideoBookingService: CancelVideoBookingService = mock()
  private val featureSwitches: FeatureSwitches = mock()
  private val delegate =
    VideoBookingServiceDelegate(createVideoBookingService, createProbationBookingService, amendVideoBookingService, amendProbationBookingService, cancelVideoBookingService, featureSwitches)

  private val courtBookingRequest = courtBookingRequest()
  private val probationBookingRequest = probationBookingRequest()
  private val amendCourtBookingRequest = amendCourtBookingRequest()
  private val amendProbationBookingRequest = amendProbationBookingRequest()

  @Test
  fun `should delegate to correct create booking service when FEATURE_MASTER_VLPM_TYPES toggle off`() {
    whenever(featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) doReturn false

    delegate.create(courtBookingRequest, COURT_USER)
    verify(createVideoBookingService).create(courtBookingRequest, COURT_USER)

    delegate.create(probationBookingRequest, PROBATION_USER)
    verify(createVideoBookingService).create(probationBookingRequest, PROBATION_USER)

    verifyNoInteractions(createProbationBookingService)
  }

  @Test
  fun `should delegate to correct create booking service when FEATURE_MASTER_VLPM_TYPES toggle on`() {
    whenever(featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) doReturn true

    delegate.create(courtBookingRequest, COURT_USER)
    verify(createVideoBookingService).create(courtBookingRequest, COURT_USER)

    delegate.create(probationBookingRequest, PROBATION_USER)
    verify(createProbationBookingService).create(probationBookingRequest, PROBATION_USER)

    verifyNoMoreInteractions(createVideoBookingService)
  }

  @Test
  fun `should delegate to correct amend booking service when FEATURE_MASTER_VLPM_TYPES toggle off`() {
    whenever(featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) doReturn false

    delegate.amend(1, amendCourtBookingRequest, COURT_USER)
    verify(amendVideoBookingService).amend(1, amendCourtBookingRequest, COURT_USER)

    delegate.amend(2, amendProbationBookingRequest, PROBATION_USER)
    verify(amendVideoBookingService).amend(2, amendProbationBookingRequest, PROBATION_USER)

    verifyNoInteractions(createVideoBookingService, createProbationBookingService, amendProbationBookingService)
  }

  @Test
  fun `should delegate to correct amend booking service when FEATURE_MASTER_VLPM_TYPES toggle on`() {
    whenever(featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) doReturn true

    delegate.amend(1, amendCourtBookingRequest, COURT_USER)
    verify(amendVideoBookingService).amend(1, amendCourtBookingRequest, COURT_USER)

    delegate.amend(2, amendProbationBookingRequest, PROBATION_USER)
    verify(amendProbationBookingService).amend(2, amendProbationBookingRequest, PROBATION_USER)

    verifyNoInteractions(createVideoBookingService, createProbationBookingService)
  }

  @Test
  fun `should delegate to cancel booking service`() {
    delegate.cancel(1, COURT_USER)
    verify(cancelVideoBookingService).cancel(1, COURT_USER)

    verifyNoInteractions(createVideoBookingService, createProbationBookingService, amendVideoBookingService)
  }
}
