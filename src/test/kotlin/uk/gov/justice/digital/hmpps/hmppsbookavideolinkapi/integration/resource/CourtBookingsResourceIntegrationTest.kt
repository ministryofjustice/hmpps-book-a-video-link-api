package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.CvpLinkDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.AmendCourtBookingRequestBuilder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.CreateCourtBookingRequestBuilder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasAppointmentDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasCourt
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasCreatedBy
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasEndTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasGuestPin
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasHmctsNumber
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasPrisonCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasNotesForStaff
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasStartTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasVideoUrl
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvillePrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsPublisher
import java.time.LocalTime

/**
 * This integration test focuses on court booking related operations only.
 */
class CourtBookingsResourceIntegrationTest : IntegrationTestBase() {

  @MockitoBean
  private lateinit var publisher: OutboundEventsPublisher

  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Autowired
  private lateinit var prisonAppointmentRepository: PrisonAppointmentRepository

  @BeforeEach
  fun before() {
    prisonSearchApi().stubGetPrisoner(pentonvillePrisoner)
    nomisMappingApi().stubGetNomisLocationMappingBy(pentonvilleLocation, 1L)
    prisonApi().stubGetScheduledAppointments(PENTONVILLE, tomorrow(), 1)
  }

  @Nested
  inner class CreateByCourtUser {
    @Test
    fun `create with CVP link and guest PIN`() {
      val bookingId = webTestClient.createBooking(
        CreateCourtBookingRequestBuilder.builder {
          court(DERBY_JUSTICE_CENTRE)
          prisoner(pentonvillePrisoner)
          main(pentonvilleLocation, tomorrow(), LocalTime.of(1, 0), LocalTime.of(1, 30))
          cvpLinkDetails(CvpLinkDetails.url("cvp-link"))
          pin("1234")
          staffNotes("Some staff notes")
        }.build(),
        COURT_USER,
      )
      val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

      persistedBooking
        .hasCourt(DERBY_JUSTICE_CENTRE)
        .hasHearingType(CourtHearingType.TRIBUNAL)
        .hasVideoUrl("cvp-link")
        .hasHmctsNumber(null)
        .hasGuestPin("1234")
        .hasCreatedBy(COURT_USER)
        .hasNotesForStaff("Some staff notes")

      val persistedAppointment = prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()

      persistedAppointment
        .hasPrisonCode(PENTONVILLE)
        .hasPrisonerNumber(pentonvillePrisoner.number)
        .hasLocation(pentonvilleLocation)
        .hasAppointmentDate(tomorrow())
        .hasStartTime(LocalTime.of(1, 0))
        .hasEndTime(LocalTime.of(1, 30))
    }

    @Test
    fun `create with CVP HMCTS number and guest PIN`() {
      val bookingId = webTestClient.createBooking(
        CreateCourtBookingRequestBuilder.builder {
          court(DERBY_JUSTICE_CENTRE)
          prisoner(pentonvillePrisoner)
          main(pentonvilleLocation, tomorrow(), LocalTime.of(1, 30), LocalTime.of(2, 0))
          cvpLinkDetails(CvpLinkDetails.hmctsNumber("HMCTS1"))
          pin("5678")
          staffNotes("Some staff notes")
        }.build(),
        COURT_USER,
      )

      val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

      persistedBooking
        .hasCourt(DERBY_JUSTICE_CENTRE)
        .hasHearingType(CourtHearingType.TRIBUNAL)
        .hasHmctsNumber("HMCTS1")
        .hasVideoUrl(null)
        .hasGuestPin("5678")
        .hasCreatedBy(COURT_USER)
        .hasNotesForStaff("Some staff notes")

      val persistedAppointment = prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()

      persistedAppointment
        .hasPrisonCode(PENTONVILLE)
        .hasPrisonerNumber(pentonvillePrisoner.number)
        .hasLocation(pentonvilleLocation)
        .hasAppointmentDate(tomorrow())
        .hasStartTime(LocalTime.of(1, 30))
        .hasEndTime(LocalTime.of(2, 0))
    }

    @Test
    fun `create without CVP link details, guest PIN or staff notes`() {
      val bookingId = webTestClient.createBooking(
        CreateCourtBookingRequestBuilder.builder {
          court(DERBY_JUSTICE_CENTRE)
          prisoner(pentonvillePrisoner)
          main(pentonvilleLocation, tomorrow(), LocalTime.of(2, 0), LocalTime.of(2, 30))
        }.build(),
        COURT_USER,
      )

      val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

      persistedBooking
        .hasCourt(DERBY_JUSTICE_CENTRE)
        .hasHearingType(CourtHearingType.TRIBUNAL)
        .hasHmctsNumber(null)
        .hasVideoUrl(null)
        .hasGuestPin(null)
        .hasCreatedBy(COURT_USER)
        .hasNotesForStaff(null)

      val persistedAppointment = prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()

      persistedAppointment
        .hasPrisonCode(PENTONVILLE)
        .hasPrisonerNumber(pentonvillePrisoner.number)
        .hasLocation(pentonvilleLocation)
        .hasAppointmentDate(tomorrow())
        .hasStartTime(LocalTime.of(2, 0))
        .hasEndTime(LocalTime.of(2, 30))
    }
  }

  @Nested
  inner class AmendByCourtUser {
    @Test
    fun `amend hearing time, CVP link details, guest PIN and staff notes`() {
      val bookingId = webTestClient.createBooking(
        CreateCourtBookingRequestBuilder.builder {
          court(DERBY_JUSTICE_CENTRE)
          prisoner(pentonvillePrisoner)
          main(pentonvilleLocation, tomorrow(), LocalTime.of(2, 30), LocalTime.of(2, 45))
          cvpLinkDetails(CvpLinkDetails.url("cvp-link"))
          pin("1234")
          staffNotes("Some staff notes")
        }.build(),
        COURT_USER,
      )

      webTestClient.amendBooking(
        bookingId,
        AmendCourtBookingRequestBuilder.builder {
          prisoner(pentonvillePrisoner)
          main(pentonvilleLocation, tomorrow(), LocalTime.of(2, 30), LocalTime.of(3, 0))
          cvpLinkDetails(CvpLinkDetails.hmctsNumber("12345"))
          pin("4321")
          staffNotes("Amended staff notes")
        }.build(),
        COURT_USER,
      )

      val amendedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

      amendedBooking
        .hasCourt(DERBY_JUSTICE_CENTRE)
        .hasHearingType(CourtHearingType.TRIBUNAL)
        .hasVideoUrl(null)
        .hasHmctsNumber("12345")
        .hasGuestPin("4321")
        .hasCreatedBy(COURT_USER)
        .hasNotesForStaff("Amended staff notes")

      val amendedAppointment = prisonAppointmentRepository.findByVideoBooking(amendedBooking).single()

      amendedAppointment
        .hasPrisonCode(PENTONVILLE)
        .hasPrisonerNumber(pentonvillePrisoner.number)
        .hasLocation(pentonvilleLocation)
        .hasAppointmentDate(tomorrow())
        .hasStartTime(LocalTime.of(2, 30))
        .hasEndTime(LocalTime.of(3, 0))
    }
  }
}
