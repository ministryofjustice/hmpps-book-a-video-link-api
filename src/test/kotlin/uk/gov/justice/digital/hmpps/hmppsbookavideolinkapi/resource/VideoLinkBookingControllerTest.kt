package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.BvlsRequestContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.HmppsBookAVideoLinkApiExceptionHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.contains
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtAppealReferenceCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.VideoBookingAccessException
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.RequestBookingService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.VideoLinkBookingsService
import uk.gov.justice.hmpps.kotlin.auth.HmppsResourceServerConfiguration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

/**
 * As a general guideline we primarily test controllers in integration tests. This is an exception due to wanting to get
 * some (early) test coverage in particular around the preventing the API accepting lenient dates.
 */
@ExtendWith(SpringExtension::class)
@AutoConfigureMockMvc
@Import(HmppsResourceServerConfiguration::class, HmppsBookAVideoLinkApiExceptionHandler::class)
@ActiveProfiles("test")
@WebAppConfiguration
@WebMvcTest(controllers = [VideoLinkBookingController::class])
@ContextConfiguration(classes = [VideoLinkBookingController::class])
@WithMockUser(roles = ["BOOK_A_VIDEO_LINK_ADMIN"])
class VideoLinkBookingControllerTest {

  @MockBean
  private lateinit var bookingFacade: BookingFacade

  @MockBean
  private lateinit var requestBookingService: RequestBookingService

  @SpyBean
  private lateinit var videoLinkBookingsService: VideoLinkBookingsService

  @MockBean
  private lateinit var videoBookingRepository: VideoBookingRepository

  @MockBean
  private lateinit var referenceCodeRepository: ReferenceCodeRepository

  @MockBean
  private lateinit var videoAppointmentRepository: VideoAppointmentRepository

  @MockBean
  private lateinit var locationsInsidePrisonClient: LocationsInsidePrisonClient

  @Autowired
  private lateinit var context: WebApplicationContext

  private val objectMapper = jacksonObjectMapper().registerModules(JavaTimeModule())

  private lateinit var mockMvc: MockMvc

  private val wandsworthPrisonCourtBooking = courtBooking()
    .addAppointment(
      prison = prison(prisonCode = WANDSWORTH),
      prisonerNumber = "ABCDEF",
      appointmentType = AppointmentType.VLB_COURT_MAIN.name,
      locationId = wandsworthLocation.id.toString(),
      date = tomorrow(),
      startTime = LocalTime.MIDNIGHT.plusHours(1),
      endTime = LocalTime.MIDNIGHT.plusHours(2),
    )

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders
      .webAppContextSetup(context)
      .build()
  }

  @Test
  fun `should fail to accept invalid appointment date provided when date leniency is disabled in configuration`() {
    val json = """
{
  "bookingType": "COURT",
  "prisoners": [
    {
      "prisonCode": "BMI",
      "prisonerNumber": "G5662GI",
      "appointments": [
        {
          "type": "VLB_COURT_MAIN",
          "locationKey": "BMI-VIDEOLINK-ROOM1",
          "date": "2200-02-31",
          "startTime": "10:00",
          "endTime": "11:00"
        }
      ]
    }
  ],
  "courtCode": "DRBYMC",
  "courtHearingType": "APPEAL",
  "comments": "Waiting to hear on legal representation",
  "videoLinkUrl": "https://video.here.com"
}
    """.trimIndent()

    val response = mockMvc.post("/video-link-booking") {
      contentType = MediaType.APPLICATION_JSON
      content = json
      requestAttr(BvlsRequestContext::class.simpleName.toString(), BvlsRequestContext(courtUser(), LocalDateTime.now()))
    }
      .andExpect {
        status { isBadRequest() }
      }.andReturn()

    response.resolvedException?.message!! contains "JSON parse error: Cannot deserialize value of type `java.time.LocalDate` from String \"2200-02-31\""

    verifyNoInteractions(bookingFacade)
  }

  @Test
  fun `should succeed when valid appointment date provided`() {
    val json = """
{
  "bookingType": "COURT",
  "prisoners": [
    {
      "prisonCode": "BMI",
      "prisonerNumber": "G5662GI",
      "appointments": [
        {
          "type": "VLB_COURT_MAIN",
          "locationKey": "BMI-VIDEOLINK-ROOM1",
          "date": "2200-02-28",
          "startTime": "10:00",
          "endTime": "11:00"
        }
      ]
    }
  ],
  "courtCode": "DRBYMC",
  "courtHearingType": "APPEAL",
  "comments": "Waiting to hear on legal representation",
  "videoLinkUrl": "https://video.here.com"
}
    """.trimIndent()

    mockMvc.post("/video-link-booking") {
      contentType = MediaType.APPLICATION_JSON
      content = json
      requestAttr(BvlsRequestContext::class.simpleName.toString(), BvlsRequestContext(courtUser(), LocalDateTime.now()))
    }
      .andExpect {
        status { isCreated() }
      }

    verify(bookingFacade).create(objectMapper.readValue(json, CreateVideoBookingRequest::class.java), courtUser())
  }

  @Test
  fun `should get Wandsworth prison court video booking for external user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(wandsworthPrisonCourtBooking)
    whenever(referenceCodeRepository.findByGroupCodeAndCode("COURT_HEARING_TYPE", wandsworthPrisonCourtBooking.hearingType!!)) doReturn courtAppealReferenceCode
    whenever(locationsInsidePrisonClient.getLocationById(wandsworthLocation.id.toString())) doReturn wandsworthLocation

    mockMvc.get("/video-link-booking/id/1") {
      contentType = MediaType.APPLICATION_JSON
      requestAttr(BvlsRequestContext::class.simpleName.toString(), BvlsRequestContext(COURT_USER, LocalDateTime.now()))
    }
      .andExpect {
        status { isOk() }
      }
  }

  @WithMockUser(roles = ["BOOK_A_VIDEO_LINK_ADMIN"])
  @Test
  fun `should get Wandsworth prison court video booking for Wandsworth prison user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(wandsworthPrisonCourtBooking)
    whenever(referenceCodeRepository.findByGroupCodeAndCode("COURT_HEARING_TYPE", wandsworthPrisonCourtBooking.hearingType!!)) doReturn courtAppealReferenceCode
    whenever(locationsInsidePrisonClient.getLocationById(wandsworthLocation.id.toString())) doReturn wandsworthLocation

    mockMvc.get("/video-link-booking/id/1") {
      contentType = MediaType.APPLICATION_JSON
      requestAttr(BvlsRequestContext::class.simpleName.toString(), BvlsRequestContext(PRISON_USER_WANDSWORTH, LocalDateTime.now()))
    }
      .andExpect {
        status { isOk() }
      }
  }

  @Test
  fun `should fail to get Wandsworth prison court video booking for Risley prison user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(wandsworthPrisonCourtBooking)
    whenever(referenceCodeRepository.findByGroupCodeAndCode("COURT_HEARING_TYPE", wandsworthPrisonCourtBooking.hearingType!!)) doReturn courtAppealReferenceCode

    val response = mockMvc.get("/video-link-booking/id/1") {
      contentType = MediaType.APPLICATION_JSON
      requestAttr(BvlsRequestContext::class.simpleName.toString(), BvlsRequestContext(PRISON_USER_RISLEY, LocalDateTime.now()))
    }
      .andExpect {
        status { isForbidden() }
      }.andReturn()

    response.resolvedException isInstanceOf CaseloadAccessException::class.java
  }

  @Test
  fun `should fail to get court video booking for probation user`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(courtBooking())

    val response = mockMvc.get("/video-link-booking/id/1") {
      contentType = MediaType.APPLICATION_JSON
      requestAttr(BvlsRequestContext::class.simpleName.toString(), BvlsRequestContext(PROBATION_USER, LocalDateTime.now()))
    }
      .andExpect { status { isNotFound() } }.andReturn()

    with(response.resolvedException!!) {
      this isInstanceOf VideoBookingAccessException::class.java
      message isEqualTo "Video booking 0 is not accessible by user"
    }
  }

  @Test
  fun `should fail to get probation video booking for court user`() {
    whenever(videoBookingRepository.findById(2)) doReturn Optional.of(probationBooking())

    val response = mockMvc.get("/video-link-booking/id/2") {
      contentType = MediaType.APPLICATION_JSON
      requestAttr(BvlsRequestContext::class.simpleName.toString(), BvlsRequestContext(COURT_USER, LocalDateTime.now()))
    }
      .andExpect { status { isNotFound() } }.andReturn()

    with(response.resolvedException!!) {
      this isInstanceOf VideoBookingAccessException::class.java
      message isEqualTo "Video booking 0 is not accessible by user"
    }
  }
}
