package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.administration

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.videoRoomAttributesWithoutSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthPrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateDecoratedRoomRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus as EntityLocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage as EntityLocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationStatus as ModelLocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage as ModelLocationUsage

class CreateDecoratedRoomServiceTest {
  private val locationsService: LocationsService = mock()
  private val prisonRepository: PrisonRepository = mock()
  private val locationAttributeRepository: LocationAttributeRepository = mock()
  private val service = CreateDecoratedRoomService(
    locationAttributeRepository,
    locationsService,
    prisonRepository,
  )
  private var locationAttributeCaptor = argumentCaptor<LocationAttribute>()

  @Test
  fun `should create an active decorated location`() {
    whenever(locationsService.getLocationById(wandsworthLocation.id)) doReturn wandsworthLocation.toModel(attributes = null)
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn wandsworthPrison
    whenever(locationAttributeRepository.saveAndFlush(locationAttributeCaptor.capture())) doReturn videoRoomAttributesWithoutSchedule(
      WANDSWORTH,
      wandsworthLocation.id,
      EntityLocationStatus.ACTIVE,
      EntityLocationUsage.SHARED,
    )

    service.create(
      wandsworthLocation.id,
      CreateDecoratedRoomRequest(
        locationUsage = ModelLocationUsage.SHARED,
        locationStatus = ModelLocationStatus.ACTIVE,
        prisonVideoUrl = "shared-prison-video-url-1",
        allowedParties = setOf("DRBYMC", "DRBYCC"),
      ),
      PROBATION_USER,
    )

    verify(locationAttributeRepository).saveAndFlush(locationAttributeCaptor.capture())

    with(locationAttributeCaptor.firstValue) {
      locationUsage isEqualTo EntityLocationUsage.SHARED
      locationStatus isEqualTo EntityLocationStatus.ACTIVE
      prisonVideoUrl isEqualTo "shared-prison-video-url-1"
      allowedParties isEqualTo "DRBYMC,DRBYCC"
    }
  }

  @Test
  fun `should fail if DPS location not found`() {
    whenever(locationsService.getLocationById(wandsworthLocation.id)) doReturn null

    assertThrows<EntityNotFoundException> {
      service.create(
        wandsworthLocation.id,
        CreateDecoratedRoomRequest(ModelLocationUsage.SHARED, ModelLocationStatus.ACTIVE),
        PROBATION_USER,
      )
    }.message isEqualTo "DPS location with ID ${wandsworthLocation.id} not found."
  }

  @Test
  fun `should fail if prison not found for DPS location`() {
    whenever(locationsService.getLocationById(wandsworthLocation.id)) doReturn wandsworthLocation.toModel(attributes = null)
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn null

    assertThrows<EntityNotFoundException> {
      service.create(
        wandsworthLocation.id,
        CreateDecoratedRoomRequest(ModelLocationUsage.SHARED, ModelLocationStatus.ACTIVE),
        PROBATION_USER,
      )
    }.message isEqualTo "Matching prison code WWI not found for DPS location ID ${wandsworthLocation.id}."
  }
}
