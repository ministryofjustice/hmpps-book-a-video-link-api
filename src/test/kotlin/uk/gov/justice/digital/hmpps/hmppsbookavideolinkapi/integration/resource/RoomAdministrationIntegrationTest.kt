package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import jakarta.persistence.EntityManagerFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation3
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateDecoratedRoomRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateRoomScheduleRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage as EntityLocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationStatus as ModelLocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage as ModelLocationUsage

class RoomAdministrationIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var locationAttributeRepository: LocationAttributeRepository

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var entityManagerFactory: EntityManagerFactory

  @Test
  fun `should create an active shared decorated room`() {
    locationsInsidePrisonApi().stubGetLocationById(wandsworthLocation)

    val decoratedRoom = webTestClient.createDecoratedRoom(
      CreateDecoratedRoomRequest(
        locationUsage = ModelLocationUsage.SHARED,
        locationStatus = ModelLocationStatus.ACTIVE,
        prisonVideoUrl = "shared-prison-video-url-1",
      ),
      wandsworthLocation.toModel(),
      PROBATION_USER,
    )

    with(decoratedRoom.extraAttributes!!) {
      locationUsage.name isEqualTo ModelLocationUsage.SHARED.name
      locationStatus.name isEqualTo ModelLocationStatus.ACTIVE.name
      allowedParties.isEmpty() isBool true
      prisonVideoUrl isEqualTo "shared-prison-video-url-1"
    }
  }

  @Test
  fun `should create an inactive probation decorated room`() {
    locationsInsidePrisonApi().stubGetLocationById(wandsworthLocation2)

    val decoratedRoom = webTestClient.createDecoratedRoom(
      CreateDecoratedRoomRequest(
        locationUsage = ModelLocationUsage.PROBATION,
        locationStatus = ModelLocationStatus.INACTIVE,
        prisonVideoUrl = "shared-prison-video-url-2",
      ),
      wandsworthLocation2.toModel(),
      PROBATION_USER,
    )

    with(decoratedRoom.extraAttributes!!) {
      locationUsage.name isEqualTo ModelLocationUsage.PROBATION.name
      locationStatus.name isEqualTo ModelLocationStatus.INACTIVE.name
      allowedParties.isEmpty() isBool true
      prisonVideoUrl isEqualTo "shared-prison-video-url-2"
    }
  }

  @Test
  fun `should add a scheduled row to a room schedule`() {
    val id = locationAttributeRepository.saveAndFlush(
      LocationAttribute.decoratedRoom(
        dpsLocationId = wandsworthLocation.id,
        prison = prisonRepository.findByCode(WANDSWORTH)!!,
        createdBy = PROBATION_USER,
        locationUsage = EntityLocationUsage.SCHEDULE,
        prisonVideoUrl = "https://probation-url",
        allowedParties = emptySet(),
        locationStatus = LocationStatus.ACTIVE,
      ),
    ).locationAttributeId

    getLocationAttribute(id).schedule().size isEqualTo 0

    webTestClient.createSchedule(
      CreateRoomScheduleRequest(
        locationUsage = ModelLocationUsage.PROBATION,
        startDayOfWeek = 1,
        endDayOfWeek = 7,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(12, 0),
        notes = "some notes for the schedule",
      ),
      wandsworthLocation.toModel(),
      PROBATION_USER,
    )

    getLocationAttribute(id).schedule().size isEqualTo 1
  }

  @Test
  fun `should get a decorated location by its DPS identifier`() {
    locationsInsidePrisonApi().stubGetLocationById(wandsworthLocation3)

    val undecorated = webTestClient.getLocation(wandsworthLocation3.toModel(), PROBATION_USER)

    undecorated.extraAttributes isEqualTo null

    webTestClient.createDecoratedRoom(
      CreateDecoratedRoomRequest(
        locationUsage = ModelLocationUsage.PROBATION,
        locationStatus = ModelLocationStatus.ACTIVE,
        prisonVideoUrl = "shared-prison-video-url-3",
      ),
      wandsworthLocation3.toModel(),
      PROBATION_USER,
    )

    val decorated = webTestClient.getLocation(wandsworthLocation3.toModel(), PROBATION_USER)

    with(decorated.extraAttributes!!) {
      locationUsage.name isEqualTo LocationUsage.PROBATION.name
      locationStatus.name isEqualTo ModelLocationStatus.ACTIVE.name
      allowedParties.isEmpty() isBool true
      prisonVideoUrl isEqualTo "shared-prison-video-url-3"
    }
  }

  // Using the entity manager to get round lazy loading of schedule(s)
  private fun getLocationAttribute(id: Long) = entityManagerFactory
    .createEntityManager()
    .createQuery("SELECT la from LocationAttribute la where la.locationAttributeId = $id")
    .singleResult as LocationAttribute

  private fun WebTestClient.getLocation(location: Location, user: ExternalUser) = this
    .get()
    .uri("/room-admin/${location.dpsLocationId}")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(Location::class.java)
    .returnResult().responseBody!!

  private fun WebTestClient.createDecoratedRoom(request: CreateDecoratedRoomRequest, location: Location, user: ExternalUser) = this
    .post()
    .uri("/room-admin/${location.dpsLocationId}")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(Location::class.java)
    .returnResult().responseBody!!

  private fun WebTestClient.createSchedule(request: CreateRoomScheduleRequest, location: Location, user: ExternalUser) = this
    .post()
    .uri("/room-admin/${location.dpsLocationId}/schedule")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isCreated
}
