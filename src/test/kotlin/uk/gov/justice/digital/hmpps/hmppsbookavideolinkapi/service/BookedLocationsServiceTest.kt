package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentAttendeeSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisMappingClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.ScheduledAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

class BookedLocationsServiceTest {

  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val nomisMappingClient: NomisMappingClient = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val featureSwitches: FeatureSwitches = mock()
  private val supportedAppointmentTypes = SupportedAppointmentTypes(featureSwitches)

  private val service = BookedLocationsService(
    activitiesAppointmentsClient,
    prisonApiClient,
    nomisMappingClient,
    videoBookingRepository,
    supportedAppointmentTypes,
  )

  @Nested
  inner class ActivitiesAndAppointmentsRolledOut {

    @BeforeEach
    fun before() {
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(WANDSWORTH)) doReturn true

      nomisMappingClient.stub {
        on { getNomisLocationMappingsBy(listOf(wandsworthLocation.id, wandsworthLocation2.id)) } doReturn listOf(
          NomisDpsLocationMapping(wandsworthLocation.id, 1),
          NomisDpsLocationMapping(wandsworthLocation2.id, 2),
        )
      }
    }

    @Test
    fun `should check if Wandsworth locations are booked or not`() {
      activitiesAppointmentsClient.stub {
        on {
          getScheduledAppointments(
            prisonCode = WANDSWORTH,
            onDate = tomorrow(),
            locationIds = setOf(1, 2),
          )
        } doReturn listOf(
          searchResult(
            prisonCode = WANDSWORTH,
            date = tomorrow(),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            prisonerNumber = "123456",
            locationId = 1,
          ),
          searchResult(
            prisonCode = WANDSWORTH,
            date = tomorrow(),
            startTime = LocalTime.of(11, 0),
            endTime = LocalTime.of(12, 0),
            prisonerNumber = "123456",
            locationId = 2,
          ),
        )
      }

      val booked = service.findBooked(
        BookedLookup(
          prisonCode = WANDSWORTH,
          date = tomorrow(),
          locations = listOf(wandsworthLocation.toModel(), wandsworthLocation2.toModel()),
        ),
      )

      // location 1
      booked.isBooked(wandsworthLocation.toModel(), LocalTime.of(9, 0), LocalTime.of(10, 0)) isBool false
      booked.isBooked(wandsworthLocation.toModel(), LocalTime.of(9, 15), LocalTime.of(10, 15)) isBool true
      booked.isBooked(wandsworthLocation.toModel(), LocalTime.of(10, 0), LocalTime.of(11, 0)) isBool true
      booked.isBooked(wandsworthLocation.toModel(), LocalTime.of(10, 15), LocalTime.of(11, 15)) isBool true
      booked.isBooked(wandsworthLocation.toModel(), LocalTime.of(11, 0), LocalTime.of(12, 0)) isBool false

      // location 2
      booked.isBooked(wandsworthLocation2.toModel(), LocalTime.of(10, 0), LocalTime.of(11, 0)) isBool false
      booked.isBooked(wandsworthLocation2.toModel(), LocalTime.of(10, 15), LocalTime.of(11, 15)) isBool true
      booked.isBooked(wandsworthLocation2.toModel(), LocalTime.of(11, 0), LocalTime.of(12, 0)) isBool true
      booked.isBooked(wandsworthLocation2.toModel(), LocalTime.of(11, 45), LocalTime.of(12, 45)) isBool true
      booked.isBooked(wandsworthLocation2.toModel(), LocalTime.of(12, 0), LocalTime.of(13, 0)) isBool false
    }

    @Test
    fun `should check if Wandsworth locations are booked or not with matching BVLS booking appointment`() {
      whenever(videoBookingRepository.findById(1)) doReturn Optional.of(
        courtBooking().withMainCourtPrisonAppointment(
          date = tomorrow(),
          prisonCode = WANDSWORTH,
          prisonerNumber = "123456",
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
        ),
      )

      activitiesAppointmentsClient.stub {
        on {
          getScheduledAppointments(
            prisonCode = WANDSWORTH,
            onDate = tomorrow(),
            locationIds = setOf(1, 2),
          )
        } doReturn listOf(
          searchResult(
            prisonCode = WANDSWORTH,
            date = tomorrow(),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            prisonerNumber = "123456",
            locationId = 1,
          ),
          searchResult(
            prisonCode = WANDSWORTH,
            date = tomorrow(),
            startTime = LocalTime.of(11, 0),
            endTime = LocalTime.of(12, 0),
            prisonerNumber = "123456",
            locationId = 2,
          ),
        )
      }

      val booked = service.findBooked(
        BookedLookup(
          prisonCode = WANDSWORTH,
          date = tomorrow(),
          locations = listOf(wandsworthLocation.toModel(), wandsworthLocation2.toModel()),
          videoBookingIdToExclude = 1,
        ),
      )

      // location 1 times are all available when we have an existing matching BVLS appointment
      booked.isBooked(wandsworthLocation.toModel(), LocalTime.of(9, 0), LocalTime.of(10, 0)) isBool false
      booked.isBooked(wandsworthLocation.toModel(), LocalTime.of(9, 15), LocalTime.of(10, 15)) isBool false
      booked.isBooked(wandsworthLocation.toModel(), LocalTime.of(10, 0), LocalTime.of(11, 0)) isBool false
      booked.isBooked(wandsworthLocation.toModel(), LocalTime.of(10, 15), LocalTime.of(11, 15)) isBool false
      booked.isBooked(wandsworthLocation.toModel(), LocalTime.of(11, 0), LocalTime.of(12, 0)) isBool false

      // location 2
      booked.isBooked(wandsworthLocation2.toModel(), LocalTime.of(10, 0), LocalTime.of(11, 0)) isBool false
      booked.isBooked(wandsworthLocation2.toModel(), LocalTime.of(10, 15), LocalTime.of(11, 15)) isBool true
      booked.isBooked(wandsworthLocation2.toModel(), LocalTime.of(11, 0), LocalTime.of(12, 0)) isBool true
      booked.isBooked(wandsworthLocation2.toModel(), LocalTime.of(11, 45), LocalTime.of(12, 45)) isBool true
      booked.isBooked(wandsworthLocation2.toModel(), LocalTime.of(12, 0), LocalTime.of(13, 0)) isBool false
    }

    private fun searchResult(
      prisonCode: String,
      date: LocalDate,
      startTime: LocalTime,
      endTime: LocalTime,
      prisonerNumber: String,
      locationId: Long,
    ) = AppointmentSearchResult(
      appointmentType = AppointmentSearchResult.AppointmentType.INDIVIDUAL,
      startDate = date,
      startTime = startTime.toHourMinuteStyle(),
      endTime = endTime.toHourMinuteStyle(),
      isCancelled = false,
      isExpired = false,
      isEdited = false,
      appointmentId = 99,
      appointmentSeriesId = 1,
      appointmentName = "appointment name",
      attendees = listOf(AppointmentAttendeeSearchResult(1, prisonerNumber, 1)),
      category = AppointmentCategorySummary("VLB", "video link booking"),
      inCell = false,
      isRepeat = false,
      maxSequenceNumber = 1,
      prisonCode = prisonCode,
      sequenceNumber = 1,
      internalLocation = AppointmentLocationSummary(locationId, prisonCode, "VIDEO LINK"),
      timeSlot = AppointmentSearchResult.TimeSlot.AM,
    )
  }

  @Nested
  inner class ActivitiesAndAppointmentsNotRolledOut {

    @BeforeEach
    fun before() {
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(RISLEY)) doReturn false

      nomisMappingClient.stub {
        on { getNomisLocationMappingsBy(listOf(risleyLocation.id, risleyLocation2.id)) } doReturn listOf(
          NomisDpsLocationMapping(risleyLocation.id, 1),
          NomisDpsLocationMapping(risleyLocation2.id, 2),
        )
      }
    }

    @Test
    fun `should check if Risley locations are booked or not`() {
      prisonApiClient.stub {
        on {
          getScheduledAppointments(
            prisonCode = RISLEY,
            date = tomorrow(),
            locationId = setOf(1, 2),
          )
        } doReturn listOf(
          scheduledAppointment(
            prisonCode = RISLEY,
            date = tomorrow(),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            prisonerNumber = "123456",
            locationId = 1,
          ),
          scheduledAppointment(
            prisonCode = RISLEY,
            date = tomorrow(),
            startTime = LocalTime.of(11, 0),
            endTime = LocalTime.of(12, 0),
            prisonerNumber = "123456",
            locationId = 2,
          ),
        )
      }

      val booked = service.findBooked(
        BookedLookup(
          prisonCode = RISLEY,
          date = tomorrow(),
          locations = listOf(risleyLocation.toModel(), risleyLocation2.toModel()),
        ),
      )

      // location 1
      booked.isBooked(risleyLocation.toModel(), LocalTime.of(9, 0), LocalTime.of(10, 0)) isBool false
      booked.isBooked(risleyLocation.toModel(), LocalTime.of(9, 15), LocalTime.of(10, 15)) isBool true
      booked.isBooked(risleyLocation.toModel(), LocalTime.of(10, 0), LocalTime.of(11, 0)) isBool true
      booked.isBooked(risleyLocation.toModel(), LocalTime.of(10, 15), LocalTime.of(11, 15)) isBool true
      booked.isBooked(risleyLocation.toModel(), LocalTime.of(11, 0), LocalTime.of(12, 0)) isBool false

      // location 2
      booked.isBooked(risleyLocation2.toModel(), LocalTime.of(10, 0), LocalTime.of(11, 0)) isBool false
      booked.isBooked(risleyLocation2.toModel(), LocalTime.of(10, 15), LocalTime.of(11, 15)) isBool true
      booked.isBooked(risleyLocation2.toModel(), LocalTime.of(11, 0), LocalTime.of(12, 0)) isBool true
      booked.isBooked(risleyLocation2.toModel(), LocalTime.of(11, 45), LocalTime.of(12, 45)) isBool true
      booked.isBooked(risleyLocation2.toModel(), LocalTime.of(12, 0), LocalTime.of(13, 0)) isBool false
    }

    @Test
    fun `should check if Risley locations are booked or not with matching BVLS booking appointment`() {
      whenever(videoBookingRepository.findById(1)) doReturn Optional.of(
        courtBooking().withMainCourtPrisonAppointment(
          date = tomorrow(),
          prisonCode = RISLEY,
          prisonerNumber = "123456",
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
        ),
      )

      prisonApiClient.stub {
        on {
          getScheduledAppointments(
            prisonCode = RISLEY,
            date = tomorrow(),
            locationId = setOf(1, 2),
          )
        } doReturn listOf(
          scheduledAppointment(
            prisonCode = RISLEY,
            date = tomorrow(),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            prisonerNumber = "123456",
            locationId = 1,
          ),
          scheduledAppointment(
            prisonCode = RISLEY,
            date = tomorrow(),
            startTime = LocalTime.of(11, 0),
            endTime = LocalTime.of(12, 0),
            prisonerNumber = "123456",
            locationId = 2,
          ),
        )
      }

      val booked = service.findBooked(
        BookedLookup(
          prisonCode = RISLEY,
          date = tomorrow(),
          locations = listOf(risleyLocation.toModel(), risleyLocation2.toModel()),
          videoBookingIdToExclude = 1,
        ),
      )

      // location 1 times are all available when we have an existing matching BVLS appointment
      booked.isBooked(risleyLocation.toModel(), LocalTime.of(9, 0), LocalTime.of(10, 0)) isBool false
      booked.isBooked(risleyLocation.toModel(), LocalTime.of(9, 15), LocalTime.of(10, 15)) isBool false
      booked.isBooked(risleyLocation.toModel(), LocalTime.of(10, 0), LocalTime.of(11, 0)) isBool false
      booked.isBooked(risleyLocation.toModel(), LocalTime.of(10, 15), LocalTime.of(11, 15)) isBool false
      booked.isBooked(risleyLocation.toModel(), LocalTime.of(11, 0), LocalTime.of(12, 0)) isBool false

      // location 2
      booked.isBooked(risleyLocation2.toModel(), LocalTime.of(10, 0), LocalTime.of(11, 0)) isBool false
      booked.isBooked(risleyLocation2.toModel(), LocalTime.of(10, 15), LocalTime.of(11, 15)) isBool true
      booked.isBooked(risleyLocation2.toModel(), LocalTime.of(11, 0), LocalTime.of(12, 0)) isBool true
      booked.isBooked(risleyLocation2.toModel(), LocalTime.of(11, 45), LocalTime.of(12, 45)) isBool true
      booked.isBooked(risleyLocation2.toModel(), LocalTime.of(12, 0), LocalTime.of(13, 0)) isBool false
    }

    private fun scheduledAppointment(
      prisonCode: String,
      date: LocalDate,
      startTime: LocalTime,
      endTime: LocalTime,
      prisonerNumber: String,
      locationId: Long,
    ): ScheduledAppointment = ScheduledAppointment(
      id = locationId,
      agencyId = prisonerNumber,
      locationId = locationId,
      locationDescription = "location description $locationId",
      appointmentTypeCode = "VLB",
      appointmentTypeDescription = "appointment type description $locationId",
      startTime = date.atTime(startTime),
      endTime = date.atTime(endTime),
      offenderNo = prisonerNumber,
      firstName = "first name $locationId",
      lastName = "last name $locationId",
      createUserId = "user id",
    )
  }
}
