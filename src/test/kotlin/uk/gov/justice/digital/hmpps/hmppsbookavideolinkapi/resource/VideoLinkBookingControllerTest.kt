package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.BvlsRequestContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.contains
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.VideoLinkBookingsService
import uk.gov.justice.hmpps.kotlin.auth.HmppsResourceServerConfiguration
import java.time.LocalDateTime

/**
 * As a general guideline we primarily test controllers in integration tests. This is an exception due to wanting to get
 * some (early) test coverage in particular around the preventing the API accepting lenient dates.
 */
@ExtendWith(SpringExtension::class)
@AutoConfigureMockMvc
@Import(HmppsResourceServerConfiguration::class)
@ActiveProfiles("test")
@WebAppConfiguration
@WebMvcTest(controllers = [VideoLinkBookingController::class])
@ContextConfiguration(classes = [VideoLinkBookingController::class])
class VideoLinkBookingControllerTest {

  @MockBean
  private lateinit var bookingFacade: BookingFacade

  @MockBean
  private lateinit var videoLinkBookingsService: VideoLinkBookingsService

  @Autowired
  private lateinit var context: WebApplicationContext

  private val objectMapper = jacksonObjectMapper().registerModules(JavaTimeModule())

  private lateinit var mockMvc: MockMvc

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders
      .webAppContextSetup(context)
      .build()
  }

  @WithMockUser(username = "FRED", roles = ["BOOK_A_VIDEO_LINK_ADMIN"])
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
      requestAttr(BvlsRequestContext::class.simpleName.toString(), BvlsRequestContext("FRED", LocalDateTime.now()))
    }
      .andExpect {
        status { isBadRequest() }
      }.andReturn()

    response.resolvedException?.message!! contains "JSON parse error: Cannot deserialize value of type `java.time.LocalDate` from String \"2200-02-31\""

    verifyNoInteractions(bookingFacade)
  }

  @WithMockUser(username = "FRED", roles = ["BOOK_A_VIDEO_LINK_ADMIN"])
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
      requestAttr(BvlsRequestContext::class.simpleName.toString(), BvlsRequestContext("FRED", LocalDateTime.now()))
    }
      .andExpect {
        status { isCreated() }
      }

    verify(bookingFacade).create(objectMapper.readValue(json, CreateVideoBookingRequest::class.java), "FRED")
  }
}
