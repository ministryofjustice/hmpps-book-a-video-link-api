package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.administration

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthPrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendDecoratedRoomRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus as EntityLocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage as EntityLocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationStatus as ModelLocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage as ModelLocationUsage

class AmendDecoratedLocationsServiceTest {
  private val locationsService: LocationsService = mock()
  private val locationAttributeRepository: LocationAttributeRepository = mock()
  private val locationScheduleRepository: LocationScheduleRepository = mock()
  private val service = AmendDecoratedLocationService(locationsService, locationAttributeRepository, locationScheduleRepository)
  private val decoratedRoom = LocationAttribute.decoratedRoom(
    dpsLocationId = wandsworthLocation.id,
    prison = wandsworthPrison,
    locationUsage = EntityLocationUsage.COURT,
    locationStatus = EntityLocationStatus.INACTIVE,
    allowedParties = setOf("COURT"),
    prisonVideoUrl = "prison-video-url",
    notes = "some comments",
    createdBy = COURT_USER,
  )

  @Test
  fun `should amend already decorated room`() {
    whenever(locationsService.getLocationById(wandsworthLocation.id)) doReturn wandsworthLocation.toModel()

    locationAttributeRepository.stub {
      on { findByDpsLocationId(wandsworthLocation.id) } doReturn decoratedRoom
    }

    with(decoratedRoom) {
      locationUsage isEqualTo EntityLocationUsage.COURT
      locationStatus isEqualTo EntityLocationStatus.INACTIVE
      prisonVideoUrl isEqualTo "prison-video-url"
      allowedParties isEqualTo "COURT"
      notes isEqualTo "some comments"
    }

    service.amend(
      wandsworthLocation.id,
      AmendDecoratedRoomRequest(
        locationUsage = ModelLocationUsage.SHARED,
        locationStatus = ModelLocationStatus.ACTIVE,
        allowedParties = emptySet(),
        prisonVideoUrl = "different-prison-video-url",
        comments = "amended comments",
      ),
      COURT_USER,
    )

    with(decoratedRoom) {
      locationUsage isEqualTo EntityLocationUsage.SHARED
      locationStatus isEqualTo EntityLocationStatus.ACTIVE
      prisonVideoUrl isEqualTo "different-prison-video-url"
      allowedParties isEqualTo null
      notes isEqualTo "amended comments"
    }

    verify(locationAttributeRepository).saveAndFlush(decoratedRoom)
  }

  @Test
  fun `should fail to amend decorated location when not found`() {
    locationAttributeRepository.stub {
      on { findByDpsLocationId(wandsworthLocation.id) } doReturn null
    }

    assertThrows<EntityNotFoundException> {
      service.amend(wandsworthLocation.id, mock(), COURT_USER)
    }.message isEqualTo "Existing room decoration for DPS location ID ${wandsworthLocation.id} not found."
  }
}
