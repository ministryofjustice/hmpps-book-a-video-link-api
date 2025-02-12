package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TestEmailConfiguration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AdditionalBookingDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalTime

@ContextConfiguration(classes = [TestEmailConfiguration::class])
@TestPropertySource(properties = ["feature.master.vlpm.types=true"])
class ProbationBookingIntegrationTest : SqsIntegrationTestBase() {

  @MockitoBean
  private lateinit var activitiesAppointmentsClient: ActivitiesAppointmentsClient

  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Autowired
  private lateinit var prisonAppointmentRepository: PrisonAppointmentRepository

  @Autowired
  private lateinit var bookingHistoryRepository: BookingHistoryRepository

  private val psrProbationBookingRequest = probationBookingRequest(
    probationTeamCode = BLACKPOOL_MC_PPOC,
    probationMeetingType = ProbationMeetingType.PSR,
    videoLinkUrl = "https://probation.videolink.com",
    prisonCode = BIRMINGHAM,
    prisonerNumber = "123456",
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(9, 30),
    appointmentType = AppointmentType.VLB_PROBATION,
    location = birminghamLocation,
    comments = "integration test probation booking comments",
    additionalBookingDetails = AdditionalBookingDetails(
      contactName = "probation contact",
      contactEmail = "probation_contact@email.com",
      contactNumber = null,
      extraInformation = null,
    ),
  )

  @Test
  fun `should create a pre-sentence report probation booking using the VLPM appointment type`() {
    videoBookingRepository.findAll().isEmpty() isBool true

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    nomisMappingApi().stubGetNomisLocationMappingBy(birminghamLocation, 1)
    locationsInsidePrisonApi().stubGetLocationById(birminghamLocation)
    prisonerApi().stubGetScheduledAppointments(BIRMINGHAM, tomorrow(), 1)
    activitiesAppointmentsClient.stub { on { isAppointmentsRolledOutAt(BIRMINGHAM) } doReturn true }

    val bookingId = webTestClient.createBooking(psrProbationBookingRequest, PROBATION_USER)
    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo BookingType.PROBATION
      probationTeam?.probationTeamId isEqualTo 1
      probationMeetingType isEqualTo ProbationMeetingType.PSR.name
      comments isEqualTo "integration test probation booking comments"
      videoUrl isEqualTo "https://probation.videolink.com"
      createdBy isEqualTo PROBATION_USER.username
      createdByPrison isEqualTo false
    }

    getAppointmentAndCheckFor(persistedBooking) {
      videoBooking isEqualTo persistedBooking
      prisonCode() isEqualTo BIRMINGHAM
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_PROBATION.name
      appointmentDate isEqualTo tomorrow()
      prisonLocationId isEqualTo birminghamLocation.id
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(9, 30)
      comments isEqualTo "integration test probation booking comments"
    }

    getHistoryAndCheckFor(persistedBooking) {
      historyType isEqualTo HistoryType.CREATE
      videoBookingId isEqualTo persistedBooking.videoBookingId
      probationMeetingType isEqualTo persistedBooking.probationMeetingType
      probationTeamId isEqualTo persistedBooking.probationTeam?.probationTeamId
      appointments() hasSize 1
    }

    waitUntil {
      verify(activitiesAppointmentsClient).createAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = "123456",
        startDate = tomorrow(),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(9, 30),
        internalLocationId = 1,
        comments = "integration test probation booking comments",
        appointmentType = SupportedAppointmentTypes.Type.PROBATION,
      )
    }
  }

  private fun getAppointmentAndCheckFor(booking: VideoBooking, init: PrisonAppointment.() -> Unit) {
    prisonAppointmentRepository.findByVideoBooking(booking).single().init()
  }

  private fun getHistoryAndCheckFor(booking: VideoBooking, init: BookingHistory.() -> Unit) {
    bookingHistoryRepository.findAllByVideoBookingIdOrderByCreatedTime(booking.videoBookingId).first().init()
  }
}
