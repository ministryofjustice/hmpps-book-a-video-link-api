package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentAttendeeSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisMappingClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.findByCourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.findByProbationMeetingType
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class ManageExternalAppointmentsServiceTest {

  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val referenceCodeRepository: ReferenceCodeRepository = mock()
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val nomisMappingClient: NomisMappingClient = mock()
  private val birminghamLocation = Location(
    locationId = 123456,
    locationType = "VLB",
    "VIDEO LINK",
    BIRMINGHAM,
  )
  private val wandsworthLocation = birminghamLocation.copy(agencyId = WANDSWORTH)
  private val courtBooking = courtBooking()
  private val courtAppointment = appointment(
    booking = courtBooking,
    prisonCode = BIRMINGHAM,
    prisonerNumber = "123456",
    appointmentType = "VLB_COURT_PRE",
    date = LocalDate.of(2100, 1, 1),
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(11, 30),
    locationId = UUID.randomUUID(),
  )
  private val probationBooking = probationBooking()
  private val probationAppointment = appointment(
    booking = probationBooking,
    prisonCode = WANDSWORTH,
    prisonerNumber = "654321",
    appointmentType = "VLB_PROBATION",
    date = LocalDate.of(2100, 1, 1),
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(11, 30),
    locationId = UUID.randomUUID(),
  )
  private val service =
    ManageExternalAppointmentsService(
      prisonAppointmentRepository,
      activitiesAppointmentsClient,
      prisonApiClient,
      prisonerSearchClient,
      referenceCodeRepository,
      nomisMappingClient,
    )

  @Test
  fun `should create court appointment via activities client when appointments rolled out`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn true
    whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
    whenever(referenceCodeRepository.findByCourtHearingType("TRIBUNAL")) doReturn courtHearingType("Tribunal")

    service.createAppointment(1)

    verify(nomisMappingClient).getNomisLocationMappingBy(courtAppointment.prisonLocationId)

    verify(activitiesAppointmentsClient).createAppointment(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startDate = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      internalLocationId = 123456,
      comments = "Video booking for court hearing type Tribunal at $DERBY_JUSTICE_CENTRE\n\nCourt hearing comments",
    )
  }

  @Test
  fun `should create court appointment without comments via activities client when appointments rolled out`() {
    val courtAppointmentWithoutComments = appointment(
      booking = courtBooking.apply { comments = null },
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_PRE",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = UUID.randomUUID(),
    )

    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointmentWithoutComments)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn true
    whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointmentWithoutComments.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)

    service.createAppointment(1)

    verify(nomisMappingClient).getNomisLocationMappingBy(courtAppointmentWithoutComments.prisonLocationId)

    verify(activitiesAppointmentsClient).createAppointment(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startDate = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      internalLocationId = 123456,
      comments = null,
    )
  }

  @Test
  fun `should not create court appointment via activities client when appointment already exists`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn true
    whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
    whenever(
      activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
        prisonCode = courtAppointment.prisonCode(),
        prisonerNumber = courtAppointment.prisonerNumber,
        onDate = courtAppointment.appointmentDate,
        birminghamLocation.locationId,
      ),
    ) doReturn listOf(
      AppointmentSearchResult(
        appointmentType = AppointmentSearchResult.AppointmentType.INDIVIDUAL,
        startDate = courtAppointment.appointmentDate,
        startTime = courtAppointment.startTime.toHourMinuteStyle(),
        endTime = courtAppointment.endTime.toHourMinuteStyle(),
        isCancelled = false,
        isExpired = false,
        isEdited = false,
        appointmentId = 99,
        appointmentSeriesId = 1,
        appointmentName = "appointment name",
        attendees = listOf(AppointmentAttendeeSearchResult(1, courtAppointment.prisonerNumber, 1)),
        category = AppointmentCategorySummary("VLB", "video link booking"),
        inCell = false,
        isRepeat = false,
        maxSequenceNumber = 1,
        prisonCode = courtAppointment.prisonCode(),
        sequenceNumber = 1,
        internalLocation = AppointmentLocationSummary(
          birminghamLocation.locationId,
          courtAppointment.prisonCode(),
          "VIDEO LINK",
        ),
        timeSlot = AppointmentSearchResult.TimeSlot.AM,
      ),
    )

    service.createAppointment(1)

    inOrder(nomisMappingClient, activitiesAppointmentsClient) {
      verify(nomisMappingClient).getNomisLocationMappingBy(courtAppointment.prisonLocationId)
      verify(activitiesAppointmentsClient).getPrisonersAppointmentsAtLocations(
        prisonCode = courtAppointment.prisonCode(),
        prisonerNumber = courtAppointment.prisonerNumber,
        onDate = courtAppointment.appointmentDate,
        birminghamLocation.locationId,
      )
    }

    verify(activitiesAppointmentsClient, never()).createAppointment(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should create probation appointment via activities client when appointments rolled out`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(probationAppointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(WANDSWORTH)) doReturn true
    whenever(nomisMappingClient.getNomisLocationMappingBy(probationAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = wandsworthLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
    whenever(referenceCodeRepository.findByProbationMeetingType("PSR")) doReturn probationMeetingType("Pre-sentence report")

    service.createAppointment(1)

    verify(nomisMappingClient).getNomisLocationMappingBy(probationAppointment.prisonLocationId)

    verify(activitiesAppointmentsClient).createAppointment(
      prisonCode = WANDSWORTH,
      prisonerNumber = "654321",
      startDate = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      internalLocationId = 123456,
      comments = "Video booking for probation meeting type Pre-sentence report at probation team description\n\nProbation meeting comments",
    )
  }

  @Test
  fun `should create court appointment via prison api client when appointments not rolled out`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn false
    whenever(prisonerSearchClient.getPrisoner(courtAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
      prisonerNumber = courtAppointment.prisonerNumber,
      prisonCode = courtAppointment.prisonCode(),
      bookingId = 1,
    )
    whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
    whenever(referenceCodeRepository.findByCourtHearingType("TRIBUNAL")) doReturn courtHearingType("Tribunal")

    service.createAppointment(1)

    verify(activitiesAppointmentsClient, never()).createAppointment(any(), any(), any(), any(), any(), any(), any())
    verify(prisonApiClient).createAppointment(
      bookingId = 1,
      locationId = 123456,
      appointmentDate = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      comments = "Video booking for court hearing type Tribunal at $DERBY_JUSTICE_CENTRE\n\nCourt hearing comments",
    )
  }

  @Test
  fun `should create court appointment without comments via prison api client when appointments not rolled out`() {
    val courtAppointmentWithoutComments = appointment(
      booking = courtBooking.apply { comments = null },
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_PRE",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = UUID.randomUUID(),
    )

    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointmentWithoutComments)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn false
    whenever(prisonerSearchClient.getPrisoner(courtAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
      prisonerNumber = courtAppointment.prisonerNumber,
      prisonCode = courtAppointment.prisonCode(),
      bookingId = 1,
    )
    whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointmentWithoutComments.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)

    service.createAppointment(1)

    verify(activitiesAppointmentsClient, never()).createAppointment(any(), any(), any(), any(), any(), any(), any())
    verify(prisonApiClient).createAppointment(
      bookingId = 1,
      locationId = 123456,
      appointmentDate = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      comments = null,
    )
  }

  @Test
  fun `should not create court appointment via prison api client when appointment already exists`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn false
    whenever(prisonerSearchClient.getPrisoner(courtAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
      prisonerNumber = courtAppointment.prisonerNumber,
      prisonCode = courtAppointment.prisonCode(),
      bookingId = 1,
    )
    whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
    whenever(
      prisonApiClient.getPrisonersAppointmentsAtLocations(
        courtAppointment.prisonCode(),
        courtAppointment.prisonerNumber,
        courtAppointment.appointmentDate,
        birminghamLocation.locationId,
      ),
    ) doReturn listOf(
      PrisonerSchedule(
        offenderNo = courtAppointment.prisonerNumber,
        locationId = 99,
        firstName = "Bob",
        lastName = "Builder",
        eventId = 99,
        event = "VLB",
        startTime = courtAppointment.appointmentDate.atTime(courtAppointment.startTime),
        endTime = courtAppointment.appointmentDate.atTime(courtAppointment.endTime),
      ),
    )

    service.createAppointment(1)

    inOrder(nomisMappingClient, prisonApiClient) {
      verify(nomisMappingClient).getNomisLocationMappingBy(courtAppointment.prisonLocationId)
      verify(prisonApiClient).getPrisonersAppointmentsAtLocations(
        prisonCode = courtAppointment.prisonCode(),
        prisonerNumber = courtAppointment.prisonerNumber,
        onDate = courtAppointment.appointmentDate,
        birminghamLocation.locationId,
      )
    }

    verify(prisonApiClient, never()).createAppointment(any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should create probation appointment via prison api client when appointments not rolled out`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(probationAppointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(WANDSWORTH)) doReturn false
    whenever(prisonerSearchClient.getPrisoner(probationAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
      prisonerNumber = probationAppointment.prisonerNumber,
      prisonCode = probationAppointment.prisonCode(),
      bookingId = 1,
    )
    whenever(nomisMappingClient.getNomisLocationMappingBy(probationAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = wandsworthLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
    whenever(referenceCodeRepository.findByProbationMeetingType("PSR")) doReturn probationMeetingType("Pre-sentence report")

    service.createAppointment(1)

    verify(activitiesAppointmentsClient, never()).createAppointment(any(), any(), any(), any(), any(), any(), any())
    verify(prisonApiClient).createAppointment(
      bookingId = 1,
      locationId = 123456,
      appointmentDate = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      comments = "Video booking for probation meeting type Pre-sentence report at probation team description\n\nProbation meeting comments",
    )
  }

  @Test
  fun `should be no-op when appointment not found`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.empty()

    service.createAppointment(1)

    verifyNoInteractions(activitiesAppointmentsClient)
    verifyNoInteractions(prisonApiClient)
  }

  @Test
  fun `should cancel appointment via activities client when appointments rolled out`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn true
    whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
    whenever(
      activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
        courtAppointment.prisonCode(),
        courtAppointment.prisonerNumber,
        courtAppointment.appointmentDate,
        birminghamLocation.locationId,
      ),
    ) doReturn listOf(
      AppointmentSearchResult(
        appointmentType = AppointmentSearchResult.AppointmentType.INDIVIDUAL,
        startDate = courtAppointment.appointmentDate,
        startTime = courtAppointment.startTime.toHourMinuteStyle(),
        endTime = courtAppointment.endTime.toHourMinuteStyle(),
        isCancelled = false,
        isExpired = false,
        isEdited = false,
        appointmentId = 99,
        appointmentSeriesId = 1,
        appointmentName = "appointment name",
        attendees = listOf(AppointmentAttendeeSearchResult(1, courtAppointment.prisonerNumber, 1)),
        category = AppointmentCategorySummary("VLB", "video link booking"),
        inCell = false,
        isRepeat = false,
        maxSequenceNumber = 1,
        prisonCode = courtAppointment.prisonCode(),
        sequenceNumber = 1,
        internalLocation = AppointmentLocationSummary(
          birminghamLocation.locationId,
          courtAppointment.prisonCode(),
          "VIDEO LINK",
        ),
        timeSlot = AppointmentSearchResult.TimeSlot.AM,
      ),
    )

    service.cancelCurrentAppointment(1)

    verify(activitiesAppointmentsClient).cancelAppointment(99)
  }

  @Test
  fun `should not cancel appointment via activities client when appointments rolled out but matching appointment not found`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn true
    whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
    whenever(
      activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
        courtAppointment.prisonCode(),
        courtAppointment.prisonerNumber,
        courtAppointment.appointmentDate,
        birminghamLocation.locationId,
      ),
    ) doReturn listOf(
      AppointmentSearchResult(
        appointmentType = AppointmentSearchResult.AppointmentType.INDIVIDUAL,
        startDate = courtAppointment.appointmentDate,
        startTime = courtAppointment.startTime.plusMinutes(1).toHourMinuteStyle(),
        endTime = courtAppointment.endTime.toHourMinuteStyle(),
        isCancelled = false,
        isExpired = false,
        isEdited = false,
        appointmentId = 99,
        appointmentSeriesId = 1,
        appointmentName = "appointment name",
        // TIME DOES NOT MATCH
        attendees = listOf(AppointmentAttendeeSearchResult(1, courtAppointment.prisonerNumber, 1)),
        category = AppointmentCategorySummary("VLB", "video link booking"),
        inCell = false,
        isRepeat = false,
        maxSequenceNumber = 1,
        prisonCode = courtAppointment.prisonCode(),
        sequenceNumber = 1,
        internalLocation = AppointmentLocationSummary(
          birminghamLocation.locationId,
          courtAppointment.prisonCode(),
          "VIDEO LINK",
        ),
        timeSlot = AppointmentSearchResult.TimeSlot.AM,
      ),
    )

    service.cancelCurrentAppointment(1)

    verify(activitiesAppointmentsClient, never()).cancelAppointment(anyLong())
  }

  @Test
  fun `should cancel appointment via prison api client when appointments not rolled out`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn false
    whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
    whenever(
      prisonApiClient.getPrisonersAppointmentsAtLocations(
        courtAppointment.prisonCode(),
        courtAppointment.prisonerNumber,
        courtAppointment.appointmentDate,
        birminghamLocation.locationId,
      ),
    ) doReturn listOf(
      PrisonerSchedule(
        offenderNo = courtAppointment.prisonerNumber,
        locationId = 99,
        firstName = "Bob",
        lastName = "Builder",
        eventId = 99,
        event = "VLB",
        startTime = courtAppointment.appointmentDate.atTime(courtAppointment.startTime),
        endTime = courtAppointment.appointmentDate.atTime(courtAppointment.endTime),
      ),
    )

    service.cancelCurrentAppointment(1)

    verify(prisonApiClient).cancelAppointment(99)
  }

  @Test
  fun `should not cancel appointment via prison api client when appointments not rolled out but matching appointment not found`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn false
    whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
    whenever(
      prisonApiClient.getPrisonersAppointmentsAtLocations(
        courtAppointment.prisonCode(),
        courtAppointment.prisonerNumber,
        courtAppointment.appointmentDate,
        birminghamLocation.locationId,
      ),
    ) doReturn listOf(
      PrisonerSchedule(
        offenderNo = courtAppointment.prisonerNumber,
        locationId = 99,
        firstName = "Bob",
        lastName = "Builder",
        eventId = 99,
        event = "VLB",
        // TIME DOES NOT MATCH
        startTime = courtAppointment.appointmentDate.atTime(courtAppointment.startTime.plusMinutes(1)),
        endTime = courtAppointment.appointmentDate.atTime(courtAppointment.endTime),
      ),
    )

    service.cancelCurrentAppointment(1)

    verify(prisonApiClient, never()).cancelAppointment(anyLong())
  }

  @Test
  fun `should not cancel appointment appointment not found`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.empty()

    service.cancelCurrentAppointment(1)

    verifyNoInteractions(activitiesAppointmentsClient)
    verifyNoInteractions(prisonApiClient)
  }

  @Test
  fun `Cancel previous - should cancel previous appointment via A&A API when A&A is rolled out`() {
    val bookingHistory = buildFakeBookingHistory()

    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn true

    whenever(
      activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
        courtAppointment.prisonCode(),
        courtAppointment.prisonerNumber,
        courtAppointment.appointmentDate,
        birminghamLocation.locationId,
      ),
    ) doReturn listOf(
      AppointmentSearchResult(
        appointmentType = AppointmentSearchResult.AppointmentType.INDIVIDUAL,
        startDate = courtAppointment.appointmentDate,
        startTime = courtAppointment.startTime.toHourMinuteStyle(),
        endTime = courtAppointment.endTime.toHourMinuteStyle(),
        isCancelled = false,
        isExpired = false,
        isEdited = false,
        appointmentId = 99,
        appointmentSeriesId = 1,
        appointmentName = "appointment name",
        attendees = listOf(AppointmentAttendeeSearchResult(1, courtAppointment.prisonerNumber, 1)),
        category = AppointmentCategorySummary("VLB", "video link booking"),
        inCell = false,
        isRepeat = false,
        maxSequenceNumber = 1,
        prisonCode = courtAppointment.prisonCode(),
        sequenceNumber = 1,
        internalLocation = AppointmentLocationSummary(
          birminghamLocation.locationId,
          courtAppointment.prisonCode(),
          "VIDEO LINK",
        ),
        timeSlot = AppointmentSearchResult.TimeSlot.AM,
      ),
    )

    whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)

    service.cancelPreviousAppointment(bookingHistory.appointments().first())

    verify(activitiesAppointmentsClient).cancelAppointment(99)
    verify(prisonApiClient, times(0)).cancelAppointment(anyLong())
  }

  @Test
  fun `Cancel previous - should cancel previous appointment via Prison API when A&A is not rolled out`() {
    val bookingHistory = buildFakeBookingHistory()

    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn false

    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
    whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
    whenever(
      prisonApiClient.getPrisonersAppointmentsAtLocations(
        courtAppointment.prisonCode(),
        courtAppointment.prisonerNumber,
        courtAppointment.appointmentDate,
        birminghamLocation.locationId,
      ),
    ) doReturn listOf(
      PrisonerSchedule(
        offenderNo = courtAppointment.prisonerNumber,
        locationId = 99,
        firstName = "Bob",
        lastName = "Builder",
        eventId = 99,
        event = "VLB",
        startTime = courtAppointment.appointmentDate.atTime(courtAppointment.startTime),
        endTime = courtAppointment.appointmentDate.atTime(courtAppointment.endTime),
      ),
    )

    service.cancelPreviousAppointment(bookingHistory.appointments().first())

    verify(activitiesAppointmentsClient, times(0)).cancelAppointment(anyLong())
    verify(prisonApiClient).cancelAppointment(99)
  }

  private fun buildFakeBookingHistory(): BookingHistory {
    val bookingHistory = BookingHistory(
      bookingHistoryId = 1L,
      videoBookingId = courtBooking.videoBookingId,
      historyType = HistoryType.CREATE,
      courtId = courtBooking.court?.courtId,
      hearingType = courtBooking.hearingType,
      createdBy = courtBooking.createdBy,
    )

    val bookingHistoryAppointment = BookingHistoryAppointment(
      bookingHistoryAppointmentId = 1L,
      prisonCode = courtAppointment.prisonCode(),
      prisonerNumber = courtAppointment.prisonerNumber,
      appointmentDate = courtAppointment.appointmentDate,
      appointmentType = courtAppointment.appointmentType,
      prisonLocationId = courtAppointment.prisonLocationId,
      startTime = courtAppointment.startTime,
      endTime = courtAppointment.endTime,
      bookingHistory = bookingHistory,
    )

    bookingHistory.addBookingHistoryAppointments(listOf(bookingHistoryAppointment))

    return bookingHistory
  }
}
