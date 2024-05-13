package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalDateTime
import java.util.*

class CreateVideoBookingServiceTest {

  private val courtRepository: CourtRepository = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val probationTeamRepository: ProbationTeamRepository = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val persistedVideoBooking: VideoBooking = mock()

  private val service = CreateVideoBookingService(
    courtRepository,
    prisonerSearchClient,
    probationTeamRepository,
    videoBookingRepository,
  )

  private var newBookingCaptor = argumentCaptor<VideoBooking>()

  @Test
  fun `should create a court video booking`() {
    val prisonCode = "MDI"
    val prisonerNumber = "123456"
    val courtBookingRequest = courtBookingRequest(prisonCode = prisonCode, prisonerNumber = prisonerNumber)
    val requestedCourt = court(courtBookingRequest.courtId!!)

    whenever(courtRepository.findById(courtBookingRequest.courtId!!)) doReturn Optional.of(requestedCourt)
    whenever(prisonerSearchClient.getPrisonerAtPrison(prisonCode = prisonCode, prisonerNumber = prisonerNumber)) doReturn Prisoner(prisonerNumber = prisonerNumber, prisonId = prisonCode)
    whenever(videoBookingRepository.save(any())) doReturn persistedVideoBooking

    service.create(courtBookingRequest)

    verify(videoBookingRepository).save(newBookingCaptor.capture())

    with(newBookingCaptor.firstValue) {
      bookingType isEqualTo "COURT"
      court isEqualTo requestedCourt
      createdTime isCloseTo LocalDateTime.now()
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
    }
  }

  @Test
  fun `should create a probation video booking`() {
    val prisonCode = "MDI"
    val prisonerNumber = "123456"
    val probationBookingRequest = probationBookingRequest(prisonCode = prisonCode, prisonerNumber = prisonerNumber)
    val requestedProbationTeam = probationTeam(probationBookingRequest.probationTeamId!!)

    whenever(probationTeamRepository.findById(probationBookingRequest.probationTeamId!!)) doReturn Optional.of(requestedProbationTeam)
    whenever(prisonerSearchClient.getPrisonerAtPrison(prisonCode = prisonCode, prisonerNumber = prisonerNumber)) doReturn Prisoner(prisonerNumber = prisonerNumber, prisonId = prisonCode)
    whenever(videoBookingRepository.save(any())) doReturn persistedVideoBooking

    service.create(probationBookingRequest)

    verify(videoBookingRepository).save(newBookingCaptor.capture())

    with(newBookingCaptor.firstValue) {
      bookingType isEqualTo "PROBATION"
      probationTeam isEqualTo requestedProbationTeam
      createdTime isCloseTo LocalDateTime.now()
      probationMeetingType isEqualTo probationBookingRequest.probationMeetingType?.name
      videoUrl isEqualTo probationBookingRequest.videoLinkUrl
    }
  }
}
