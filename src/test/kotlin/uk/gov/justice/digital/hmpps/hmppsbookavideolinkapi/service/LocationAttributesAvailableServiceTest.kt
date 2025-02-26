package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AvailabilityStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import java.time.LocalDateTime
import java.util.Optional

class LocationAttributesAvailableServiceTest {
  private val court: Court = mock()
  private val probationTeam: ProbationTeam = mock()
  private val locationAttribute: LocationAttribute = mock()
  private val now = LocalDateTime.now()
  private val locationAttributeRepository: LocationAttributeRepository = mock()
  private val courtRepository: CourtRepository = mock()
  private val probationTeamRepository: ProbationTeamRepository = mock()
  private val service = LocationAttributesAvailableService(locationAttributeRepository, courtRepository, probationTeamRepository)

  @Test
  fun `should fail if location attributes not found`() {
    whenever(locationAttributeRepository.findById(1)) doReturn Optional.empty()

    assertThrows<EntityNotFoundException> { service.isLocationAvailableFor(LocationAvailableRequest.court(1, "COURT", now)) }
      .message isEqualTo "Location attribute 1 not found"

    assertThrows<EntityNotFoundException> { service.isLocationAvailableFor(LocationAvailableRequest.probation(1, "PROBATION", now)) }
      .message isEqualTo "Location attribute 1 not found"
  }

  @Test
  fun `should fail if court code not found`() {
    whenever(locationAttributeRepository.findById(1)) doReturn mock()
    whenever(courtRepository.findByCode("COURT")) doReturn null

    assertThrows<EntityNotFoundException> { service.isLocationAvailableFor(LocationAvailableRequest.court(1, "COURT", now)) }
      .message isEqualTo "Court code COURT not found"

    verify(courtRepository).findByCode("COURT")
  }

  @Test
  fun `should fail if probation team not found`() {
    whenever(locationAttributeRepository.findById(1)) doReturn mock()
    whenever(probationTeamRepository.findByCode("PROBATION")) doReturn null

    assertThrows<EntityNotFoundException> { service.isLocationAvailableFor(LocationAvailableRequest.probation(1, "PROBATION", now)) }
      .message isEqualTo "Probation team PROBATION not found"

    verify(probationTeamRepository).findByCode("PROBATION")
  }

  @Test
  fun `should return true when location attribute is available for court`() {
    whenever(locationAttributeRepository.findById(1)) doReturn Optional.of(locationAttribute)
    whenever(courtRepository.findByCode("COURT")) doReturn court
    whenever(locationAttribute.isAvailableFor(court, now)) doReturn AvailabilityStatus.COURT

    service.isLocationAvailableFor(LocationAvailableRequest.court(1, "COURT", now)) isEqualTo AvailabilityStatus.COURT
  }

  @Test
  fun `should return false when location attribute is not available for court`() {
    whenever(locationAttributeRepository.findById(1)) doReturn Optional.of(locationAttribute)
    whenever(courtRepository.findByCode("COURT")) doReturn court
    whenever(locationAttribute.isAvailableFor(court, now)) doReturn AvailabilityStatus.NONE

    service.isLocationAvailableFor(LocationAvailableRequest.court(1, "COURT", now)) isEqualTo AvailabilityStatus.NONE
  }

  @Test
  fun `should return true when location attribute is available for probation team`() {
    whenever(locationAttributeRepository.findById(1)) doReturn Optional.of(locationAttribute)
    whenever(probationTeamRepository.findByCode("PROBATION")) doReturn probationTeam
    whenever(locationAttribute.isAvailableFor(probationTeam, now)) doReturn AvailabilityStatus.PROBATION

    service.isLocationAvailableFor(LocationAvailableRequest.probation(1, "PROBATION", now)) isEqualTo AvailabilityStatus.PROBATION
  }

  @Test
  fun `should return false when location attribute is not available for probation team`() {
    whenever(locationAttributeRepository.findById(1)) doReturn Optional.of(locationAttribute)
    whenever(probationTeamRepository.findByCode("PROBATION")) doReturn probationTeam
    whenever(locationAttribute.isAvailableFor(probationTeam, now)) doReturn AvailabilityStatus.NONE

    service.isLocationAvailableFor(LocationAvailableRequest.probation(1, "PROBATION", now)) isEqualTo AvailabilityStatus.NONE
  }
}
