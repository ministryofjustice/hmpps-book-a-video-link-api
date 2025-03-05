package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.administration

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvillePrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateRoomScheduleRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import java.time.LocalTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationScheduleUsage as EntityLocationScheduleUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage as EntityLocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationScheduleUsage as ModelLocationScheduleUsage

class CreateLocationScheduleServiceTest {
  private val locationAttributeRepository: LocationAttributeRepository = mock()
  private val service = CreateLocationScheduleService(locationAttributeRepository)

  @Test
  fun `should add schedule row to existing empty schedule`() {
    val roomAttributes = LocationAttribute.decoratedRoom(
      dpsLocationId = UUID.randomUUID(),
      prison = pentonvillePrison,
      locationStatus = LocationStatus.ACTIVE,
      locationUsage = EntityLocationUsage.SCHEDULE,
      createdBy = PROBATION_USER,
      allowedParties = emptySet(),
      notes = null,
      prisonVideoUrl = null,
    )

    roomAttributes.schedule().isEmpty() isBool true

    val dpsLocationId = UUID.randomUUID()

    whenever(locationAttributeRepository.findByDpsLocationId(dpsLocationId)) doReturn roomAttributes

    service.create(
      dpsLocationId,
      CreateRoomScheduleRequest(
        locationUsage = ModelLocationScheduleUsage.SHARED,
        startDayOfWeek = 1,
        endDayOfWeek = 7,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(11, 0),
        allowedParties = setOf("DRBYMC"),
        notes = "Some notes",
      ),
      PROBATION_USER,
    )

    with(roomAttributes.schedule().single()) {
      locationUsage isEqualTo EntityLocationScheduleUsage.SHARED
      startDayOfWeek isEqualTo 1
      endDayOfWeek isEqualTo 7
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(11, 0)
      allowedParties isEqualTo "DRBYMC"
      notes isEqualTo "Some notes"
    }

    verify(locationAttributeRepository).saveAndFlush(roomAttributes)
  }

  @Test
  fun `should add schedule row to existing populated schedule`() {
    val roomAttributes = LocationAttribute.decoratedRoom(
      dpsLocationId = UUID.randomUUID(),
      prison = pentonvillePrison,
      locationStatus = LocationStatus.ACTIVE,
      locationUsage = EntityLocationUsage.SCHEDULE,
      createdBy = PROBATION_USER,
      allowedParties = emptySet(),
      notes = null,
      prisonVideoUrl = null,
    ).apply {
      addSchedule(
        usage = EntityLocationScheduleUsage.PROBATION,
        startDayOfWeek = 1,
        endDayOfWeek = 2,
        startTime = LocalTime.of(13, 0),
        endTime = LocalTime.of(17, 0),
        allowedParties = emptySet(),
        notes = "Some notes",
        createdBy = "TEST",
      )
    }

    roomAttributes.schedule() hasSize 1

    val dpsLocationId = UUID.randomUUID()

    whenever(locationAttributeRepository.findByDpsLocationId(dpsLocationId)) doReturn roomAttributes

    service.create(
      dpsLocationId,
      CreateRoomScheduleRequest(
        locationUsage = ModelLocationScheduleUsage.SHARED,
        startDayOfWeek = 1,
        endDayOfWeek = 7,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(11, 0),
        allowedParties = setOf("DRBYMC"),
        notes = "Some more notes",
      ),
      PROBATION_USER,
    )

    roomAttributes.schedule() hasSize 2

    with(roomAttributes.schedule()[0]) {
      locationUsage isEqualTo EntityLocationScheduleUsage.PROBATION
      startDayOfWeek isEqualTo 1
      endDayOfWeek isEqualTo 2
      startTime isEqualTo LocalTime.of(13, 0)
      endTime isEqualTo LocalTime.of(17, 0)
      allowedParties isEqualTo null
      notes isEqualTo "Some notes"
    }

    with(roomAttributes.schedule()[1]) {
      locationUsage isEqualTo EntityLocationScheduleUsage.SHARED
      startDayOfWeek isEqualTo 1
      endDayOfWeek isEqualTo 7
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(11, 0)
      allowedParties isEqualTo "DRBYMC"
      notes isEqualTo "Some more notes"
    }

    verify(locationAttributeRepository).saveAndFlush(roomAttributes)
  }

  @Test
  fun `should fail if no matching DPS location identifier`() {
    val dpsLocationId = UUID.fromString("34d336e8-7f25-4861-9c00-015cfa33fc42")

    whenever(locationAttributeRepository.findByDpsLocationId(dpsLocationId)) doReturn null

    assertThrows<EntityNotFoundException> {
      service.create(
        dpsLocationId,
        CreateRoomScheduleRequest(
          locationUsage = ModelLocationScheduleUsage.SHARED,
          startDayOfWeek = 1,
          endDayOfWeek = 7,
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(11, 0),
          allowedParties = setOf("DRBYMC"),
          notes = "Some more notes",
        ),
        PROBATION_USER,
      )
    }.message isEqualTo "Location attribute with DPS location ID 34d336e8-7f25-4861-9c00-015cfa33fc42 not found"
  }
}
