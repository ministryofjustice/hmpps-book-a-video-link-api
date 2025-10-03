package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import jakarta.persistence.EntityManagerFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.norwichLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation3
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.RoomSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendDecoratedRoomRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendRoomScheduleRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateDecoratedRoomRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateRoomScheduleRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus as EntityLocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage as EntityLocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationScheduleUsage as ModelLocationScheduleUsage
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
  fun `should create temporarily blocked probation decorated room`() {
    locationsInsidePrisonApi().stubGetLocationById(wandsworthLocation2)

    val decoratedRoom = webTestClient.createDecoratedRoom(
      CreateDecoratedRoomRequest(
        locationUsage = ModelLocationUsage.PROBATION,
        locationStatus = ModelLocationStatus.TEMPORARILY_BLOCKED,
        blockedFrom = today(),
        blockedTo = tomorrow(),
      ),
      wandsworthLocation2.toModel(),
      PROBATION_USER,
    )

    with(decoratedRoom.extraAttributes!!) {
      locationUsage.name isEqualTo ModelLocationUsage.PROBATION.name
      locationStatus.name isEqualTo ModelLocationStatus.TEMPORARILY_BLOCKED.name
      blockedFrom isEqualTo today()
      blockedTo isEqualTo tomorrow()
    }
  }

  @Test
  fun `should amend temporarily blocked probation decorated room`() {
    locationsInsidePrisonApi().stubGetLocationById(wandsworthLocation2)

    webTestClient.createDecoratedRoom(
      CreateDecoratedRoomRequest(
        locationUsage = ModelLocationUsage.PROBATION,
        locationStatus = ModelLocationStatus.TEMPORARILY_BLOCKED,
        blockedFrom = today(),
        blockedTo = tomorrow(),
      ),
      wandsworthLocation2.toModel(),
      PROBATION_USER,
    )

    val amendedRoom = webTestClient.amendDecoratedRoom(
      AmendDecoratedRoomRequest(
        locationUsage = ModelLocationUsage.PROBATION,
        locationStatus = ModelLocationStatus.TEMPORARILY_BLOCKED,
        blockedFrom = today(),
        blockedTo = tomorrow().plusDays(1),
      ),
      wandsworthLocation2.toModel(),
      PROBATION_USER,
    )

    with(amendedRoom.extraAttributes!!) {
      locationUsage.name isEqualTo ModelLocationUsage.PROBATION.name
      locationStatus.name isEqualTo ModelLocationStatus.TEMPORARILY_BLOCKED.name
      blockedFrom isEqualTo today()
      blockedTo isEqualTo tomorrow().plusDays(1)
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
        notes = null,
        locationStatus = EntityLocationStatus.ACTIVE,
      ),
    ).locationAttributeId

    getLocationAttribute(id).schedule().size isEqualTo 0

    webTestClient.createSchedule(
      CreateRoomScheduleRequest(
        locationUsage = ModelLocationScheduleUsage.PROBATION,
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
      locationUsage.name isEqualTo ModelLocationUsage.PROBATION.name
      locationStatus.name isEqualTo ModelLocationStatus.ACTIVE.name
      allowedParties.isEmpty() isBool true
      prisonVideoUrl isEqualTo "shared-prison-video-url-3"
    }
  }

  @Test
  fun `should create and amend an existing decorated location with a long prison video url`() {
    locationsInsidePrisonApi().stubGetLocationById(wandsworthLocation3)

    webTestClient.createDecoratedRoom(
      CreateDecoratedRoomRequest(
        locationUsage = ModelLocationUsage.PROBATION,
        locationStatus = ModelLocationStatus.INACTIVE,
        allowedParties = setOf("PROBATION"),
        prisonVideoUrl = "x".repeat(300),
        comments = "some comments",
      ),
      wandsworthLocation3.toModel(),
      PROBATION_USER,
    )

    val amendedRoom = webTestClient.amendDecoratedRoom(
      AmendDecoratedRoomRequest(
        locationUsage = ModelLocationUsage.COURT,
        locationStatus = ModelLocationStatus.ACTIVE,
        allowedParties = setOf("COURT"),
        prisonVideoUrl = "v".repeat(300),
        comments = "amended comments",
      ),
      wandsworthLocation3.toModel(),
      COURT_USER,
    )

    with(amendedRoom.extraAttributes!!) {
      locationUsage isEqualTo ModelLocationUsage.COURT
      locationStatus isEqualTo ModelLocationStatus.ACTIVE
      allowedParties containsExactlyInAnyOrder setOf("COURT")
      prisonVideoUrl isEqualTo "v".repeat(300)
      notes isEqualTo "amended comments"
    }

    with(locationAttributeRepository.findByDpsLocationId(amendedRoom.dpsLocationId)!!) {
      locationUsage isEqualTo EntityLocationUsage.COURT
      locationStatus isEqualTo EntityLocationStatus.ACTIVE
      allowedParties isEqualTo "COURT"
      prisonVideoUrl isEqualTo "v".repeat(300)
      notes isEqualTo "amended comments"
      amendedBy isEqualTo COURT_USER.username
      amendedTime isCloseTo LocalDateTime.now()
    }
  }

  @Test
  fun `should delete an existing decorated location with schedule`() {
    locationsInsidePrisonApi().stubGetLocationById(risleyLocation)

    webTestClient.createDecoratedRoom(
      CreateDecoratedRoomRequest(
        locationUsage = ModelLocationUsage.SCHEDULE,
        locationStatus = ModelLocationStatus.INACTIVE,
      ),
      risleyLocation.toModel(),
      PROBATION_USER,
    )

    webTestClient.createSchedule(
      CreateRoomScheduleRequest(
        locationUsage = ModelLocationScheduleUsage.PROBATION,
        startDayOfWeek = 1,
        endDayOfWeek = 7,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(12, 0),
        notes = "some notes for the schedule",
      ),
      risleyLocation.toModel(),
      PROBATION_USER,
    )

    val persistedLocationAttributeId = locationAttributeRepository.findByDpsLocationId(risleyLocation.id)!!.locationAttributeId

    getLocationAttribute(persistedLocationAttributeId).schedule() hasSize 1

    webTestClient.deleteDecoratedRoom(risleyLocation.id, PROBATION_USER)

    locationAttributeRepository.findByDpsLocationId(risleyLocation.id) isEqualTo null
  }

  @Test
  fun `should delete an existing schedule from a decorated location`() {
    locationsInsidePrisonApi().stubGetLocationById(risleyLocation2)

    webTestClient.createDecoratedRoom(
      CreateDecoratedRoomRequest(
        locationUsage = ModelLocationUsage.SCHEDULE,
        locationStatus = ModelLocationStatus.INACTIVE,
      ),
      risleyLocation2.toModel(),
      PROBATION_USER,
    )

    webTestClient.createSchedule(
      CreateRoomScheduleRequest(
        locationUsage = ModelLocationScheduleUsage.PROBATION,
        startDayOfWeek = 1,
        endDayOfWeek = 7,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(12, 0),
        notes = "some notes for the schedule",
      ),
      risleyLocation2.toModel(),
      PROBATION_USER,
    )

    val persistedLocationAttributeId = locationAttributeRepository.findByDpsLocationId(risleyLocation2.id)!!.locationAttributeId
    val decoratedLocationWithSchedule = getLocationAttribute(persistedLocationAttributeId)

    webTestClient.deleteSchedule(risleyLocation2.id, decoratedLocationWithSchedule.schedule().single().locationScheduleId, PROBATION_USER)

    getLocationAttribute(persistedLocationAttributeId).schedule().isEmpty() isBool true
  }

  @Test
  fun `should amend a scheduled row on a decorated room schedule`() {
    locationsInsidePrisonApi().stubGetLocationById(norwichLocation)

    webTestClient.createDecoratedRoom(
      CreateDecoratedRoomRequest(
        locationUsage = ModelLocationUsage.SCHEDULE,
        locationStatus = ModelLocationStatus.INACTIVE,
      ),
      norwichLocation.toModel(),
      PROBATION_USER,
    )

    val schedule = webTestClient.createSchedule(
      CreateRoomScheduleRequest(
        locationUsage = ModelLocationScheduleUsage.COURT,
        startDayOfWeek = 1,
        endDayOfWeek = 7,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(12, 0),
        notes = "some notes for the initial schedule",
      ),
      norwichLocation.toModel(),
      COURT_USER,
    )

    val roomSchedule = webTestClient.amendSchedule(
      norwichLocation.id,
      schedule.scheduleId,
      AmendRoomScheduleRequest(
        locationUsage = ModelLocationScheduleUsage.PROBATION,
        startDayOfWeek = 2,
        endDayOfWeek = 5,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(12, 0),
        allowedParties = setOf("PROBATION"),
        notes = "some notes for the amended schedule",
      ),
      PROBATION_USER,
    )

    with(roomSchedule) {
      locationUsage isEqualTo ModelLocationScheduleUsage.PROBATION
      startDayOfWeek isEqualTo DayOfWeek.of(2)
      endDayOfWeek isEqualTo DayOfWeek.of(5)
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(12, 0)
      allowedParties containsExactly setOf("PROBATION")
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

  private fun WebTestClient.amendDecoratedRoom(request: AmendDecoratedRoomRequest, location: Location, user: ExternalUser) = this
    .put()
    .uri("/room-admin/${location.dpsLocationId}")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(Location::class.java)
    .returnResult().responseBody!!

  private fun WebTestClient.deleteDecoratedRoom(dpsLocationId: UUID, user: ExternalUser) = this
    .delete()
    .uri("/room-admin/$dpsLocationId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isNoContent

  private fun WebTestClient.createSchedule(request: CreateRoomScheduleRequest, location: Location, user: ExternalUser) = this
    .post()
    .uri("/room-admin/${location.dpsLocationId}/schedule")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(RoomSchedule::class.java)
    .returnResult().responseBody!!

  private fun WebTestClient.deleteSchedule(dpsLocationId: UUID, scheduleId: Long, user: ExternalUser) = this
    .delete()
    .uri("/room-admin/$dpsLocationId/schedule/$scheduleId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isNoContent

  private fun WebTestClient.amendSchedule(dpsLocationId: UUID, scheduleId: Long, request: AmendRoomScheduleRequest, user: ExternalUser) = this
    .put()
    .uri("/room-admin/$dpsLocationId/schedule/$scheduleId")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(RoomSchedule::class.java)
    .returnResult().responseBody!!
}
