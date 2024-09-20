package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.videoAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.VideoBookingSearchRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.VideoBookingAccessException
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

class VideoLinkBookingsServiceTest {
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val referenceCodeRepository: ReferenceCodeRepository = mock()
  private val videoAppointmentRepository: VideoAppointmentRepository = mock()

  private val service = VideoLinkBookingsService(
    videoBookingRepository,
    referenceCodeRepository,
    videoAppointmentRepository,
  )

  @BeforeEach
  fun before() {
    val courtHearingTypeRefCode = ReferenceCode(
      referenceCodeId = 1L,
      groupCode = "COURT_HEARING_TYPE",
      code = "TRIBUNAL",
      description = "Tribunal",
      createdBy = "test",
      createdTime = LocalDateTime.now(),
      enabled = true,
    )

    whenever(
      referenceCodeRepository.findByGroupCodeAndCode(
        courtHearingTypeRefCode.groupCode,
        courtHearingTypeRefCode.code,
      ),
    ) doReturn courtHearingTypeRefCode
  }

  @Nested
  @DisplayName("Tests for getVideoLinkBookingById")
  inner class GetBookings {

    @Test
    fun `should get a court video link booking by ID for court user`() {
      val prisonerNumber = "A1234FF"

      val courtBooking = courtBooking(createdBy = "test_user")
        .addAppointment(
          prisonCode = MOORLAND,
          prisonerNumber = prisonerNumber,
          appointmentType = AppointmentType.VLB_COURT_PRE.name,
          locationKey = moorlandLocation.key,
          date = tomorrow(),
          startTime = LocalTime.MIDNIGHT,
          endTime = LocalTime.MIDNIGHT.plusHours(1),
        )
        .addAppointment(
          prisonCode = MOORLAND,
          prisonerNumber = prisonerNumber,
          appointmentType = AppointmentType.VLB_COURT_MAIN.name,
          locationKey = moorlandLocation.key,
          date = tomorrow(),
          startTime = LocalTime.MIDNIGHT.plusHours(1),
          endTime = LocalTime.MIDNIGHT.plusHours(2),
        )
        .addAppointment(
          prisonCode = MOORLAND,
          prisonerNumber = prisonerNumber,
          appointmentType = AppointmentType.VLB_COURT_POST.name,
          locationKey = moorlandLocation.key,
          date = tomorrow(),
          startTime = LocalTime.MIDNIGHT.plusHours(3),
          endTime = LocalTime.MIDNIGHT.plusHours(4),
        )

      whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(courtBooking)

      with(service.getVideoLinkBookingById(1L, COURT_USER)) {
        prisonAppointments hasSize 3
        prisonAppointments.first().appointmentType isEqualTo AppointmentType.VLB_COURT_PRE.name
        prisonAppointments.second().appointmentType isEqualTo AppointmentType.VLB_COURT_MAIN.name
        prisonAppointments.third().appointmentType isEqualTo AppointmentType.VLB_COURT_POST.name

        // Should be present for a court booking
        courtCode isEqualTo DERBY_JUSTICE_CENTRE
        courtDescription isEqualTo DERBY_JUSTICE_CENTRE
        courtHearingType isEqualTo CourtHearingType.TRIBUNAL
        courtHearingTypeDescription isEqualTo "Tribunal"
        comments isEqualTo "Court hearing comments"
        videoLinkUrl isEqualTo "https://court.hearing.link"

        // Should be null for a court booking
        assertThat(probationTeamCode).isNull()
        assertThat(probationTeamDescription).isNull()
        assertThat(probationMeetingType).isNull()
        assertThat(probationMeetingTypeDescription).isNull()
      }
    }

    @Test
    fun `should fail to get a Moorland court video link booking by ID for a Birmingham prison user`() {
      whenever(videoBookingRepository.findById(any())) doReturn Optional.of(courtBooking().withMainCourtPrisonAppointment(MOORLAND))

      assertThrows<CaseloadAccessException> { service.getVideoLinkBookingById(1L, PRISON_USER.copy(activeCaseLoadId = BIRMINGHAM)) }
    }

    @Test
    fun `should fail to get a court video link booking by ID for a probation user`() {
      whenever(videoBookingRepository.findById(any())) doReturn Optional.of(courtBooking())

      assertThrows<VideoBookingAccessException> { service.getVideoLinkBookingById(1L, PROBATION_USER) }
    }

    @Test
    fun `should fail to get a probation video link booking by ID for a court user`() {
      whenever(videoBookingRepository.findById(any())) doReturn Optional.of(probationBooking())

      assertThrows<VideoBookingAccessException> { service.getVideoLinkBookingById(1L, COURT_USER) }
    }

    @Test
    fun `should get a probation video booking by ID for probation user`() {
      val probationMeetingGroup = "PROBATION_MEETING_TYPE"
      val probationMeetingCode = "PSR"
      val createdBy = "TIM"
      val createdTime = LocalDateTime.now()
      val prisonerNumber = "A1234FF"

      // Mock response for findById
      val probationBooking = probationBooking()
        .addAppointment(
          prisonCode = MOORLAND,
          prisonerNumber = prisonerNumber,
          appointmentType = AppointmentType.VLB_PROBATION.name,
          locationKey = moorlandLocation.key,
          date = tomorrow(),
          startTime = LocalTime.MIDNIGHT,
          endTime = LocalTime.MIDNIGHT.plusHours(1),
        )

      // Mock response for reference code lookup
      val probationMeetingType = ReferenceCode(
        referenceCodeId = 1L,
        groupCode = probationMeetingGroup,
        code = probationMeetingCode,
        description = "Pre-sentence report",
        createdBy = createdBy,
        createdTime = createdTime,
        enabled = true,
      )

      whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(probationBooking)
      whenever(
        referenceCodeRepository.findByGroupCodeAndCode(
          probationMeetingGroup,
          probationMeetingCode,
        ),
      ) doReturn probationMeetingType

      with(service.getVideoLinkBookingById(1L, PROBATION_USER)) {
        prisonAppointments.single().appointmentType isEqualTo AppointmentType.VLB_PROBATION.name

        // Should be present for a probation booking
        probationTeamCode isEqualTo "BLKPPP"
        probationTeamDescription isEqualTo "probation team description"
        this.probationMeetingType isEqualTo ProbationMeetingType.PSR
        probationMeetingTypeDescription isEqualTo "Pre-sentence report"
        videoLinkUrl isEqualTo "https://probation.meeting.link"
        comments isEqualTo "Probation meeting comments"

        // Should be null for a probation booking
        assertThat(courtCode).isNull()
        assertThat(courtDescription).isNull()
        assertThat(courtHearingType).isNull()
        assertThat(courtHearingTypeDescription).isNull()
      }
    }

    @Test
    fun `should throw error when video booking by ID - not found`() {
      whenever(videoBookingRepository.findById(1L)) doReturn Optional.empty()

      val error = assertThrows<EntityNotFoundException> { service.getVideoLinkBookingById(1L, PROBATION_USER) }
      error.message isEqualTo "Video booking with ID 1 not found"
    }
  }

  @Nested
  @DisplayName("Tests for findMatchingVideoLinkBooking")
  inner class FindMatchingBookings {

    @Test
    fun `should find a matching court video link booking for court user`() {
      val searchRequest = VideoBookingSearchRequest(
        prisonerNumber = "123456",
        locationKey = moorlandLocation.key,
        date = tomorrow(),
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(13, 0),
      )

      val booking = courtBooking()
        .addAppointment(
          prisonCode = MOORLAND,
          prisonerNumber = searchRequest.prisonerNumber!!,
          appointmentType = AppointmentType.VLB_COURT_PRE.name,
          date = searchRequest.date!!,
          startTime = LocalTime.of(11, 0),
          endTime = LocalTime.of(12, 0),
          locationKey = searchRequest.locationKey!!,
        )
        .addAppointment(
          prisonCode = MOORLAND,
          prisonerNumber = searchRequest.prisonerNumber!!,
          appointmentType = AppointmentType.VLB_COURT_MAIN.name,
          date = searchRequest.date!!,
          startTime = searchRequest.startTime!!,
          endTime = searchRequest.endTime!!,
          locationKey = searchRequest.locationKey!!,
        )

      whenever(
        videoAppointmentRepository.findActiveVideoAppointment(
          prisonerNumber = "123456",
          appointmentDate = tomorrow(),
          prisonLocKey = moorlandLocation.key,
          startTime = LocalTime.of(12, 0),
          endTime = LocalTime.of(13, 0),
        ),
      ) doReturn videoAppointment(booking, booking.appointments().second())

      whenever(videoBookingRepository.findById(booking.videoBookingId)) doReturn Optional.of(booking)

      service.findMatchingVideoLinkBooking(searchRequest, COURT_USER) isEqualTo booking.toModel(
        prisonAppointments = booking.appointments(),
        courtDescription = DERBY_JUSTICE_CENTRE,
        courtHearingTypeDescription = "Tribunal",
      )
    }

    @Test
    fun `should fail to find a matching Moorland video link booking for Risley prison user`() {
      val searchRequest = VideoBookingSearchRequest(
        prisonerNumber = "123456",
        locationKey = moorlandLocation.key,
        date = tomorrow(),
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(13, 0),
      )

      val booking = courtBooking()
        .addAppointment(
          prisonCode = MOORLAND,
          prisonerNumber = searchRequest.prisonerNumber!!,
          appointmentType = AppointmentType.VLB_COURT_PRE.name,
          date = searchRequest.date!!,
          startTime = LocalTime.of(11, 0),
          endTime = LocalTime.of(12, 0),
          locationKey = searchRequest.locationKey!!,
        )
        .addAppointment(
          prisonCode = MOORLAND,
          prisonerNumber = searchRequest.prisonerNumber!!,
          appointmentType = AppointmentType.VLB_COURT_MAIN.name,
          date = searchRequest.date!!,
          startTime = searchRequest.startTime!!,
          endTime = searchRequest.endTime!!,
          locationKey = searchRequest.locationKey!!,
        )

      whenever(
        videoAppointmentRepository.findActiveVideoAppointment(
          prisonerNumber = "123456",
          appointmentDate = tomorrow(),
          prisonLocKey = moorlandLocation.key,
          startTime = LocalTime.of(12, 0),
          endTime = LocalTime.of(13, 0),
        ),
      ) doReturn videoAppointment(booking, booking.appointments().second())

      whenever(videoBookingRepository.findById(booking.videoBookingId)) doReturn Optional.of(booking)

      assertThrows<CaseloadAccessException> {
        service.findMatchingVideoLinkBooking(searchRequest, PRISON_USER.copy(activeCaseLoadId = RISLEY)) isEqualTo booking.toModel(
          prisonAppointments = booking.appointments(),
          courtDescription = DERBY_JUSTICE_CENTRE,
          courtHearingTypeDescription = "Tribunal",
        )
      }
    }

    @Test
    fun `should throw entity not found when no matching video link booking`() {
      val searchRequest = VideoBookingSearchRequest(
        prisonerNumber = "123456",
        locationKey = moorlandLocation.key,
        date = tomorrow(),
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(13, 0),
      )

      whenever(
        videoAppointmentRepository.findActiveVideoAppointment(
          any(),
          any(),
          any(),
          any(),
          any(),
        ),
      ) doReturn null

      val error = assertThrows<EntityNotFoundException> { service.findMatchingVideoLinkBooking(searchRequest, COURT_USER) }

      error.message isEqualTo "Video booking not found matching search criteria $searchRequest"
    }
  }

  private fun <T> List<T>.second() = this[1]

  private fun <T> List<T>.third() = this[2]
}
