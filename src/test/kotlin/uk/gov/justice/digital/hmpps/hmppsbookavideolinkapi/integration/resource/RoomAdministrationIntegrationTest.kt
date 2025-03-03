package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import jakarta.persistence.EntityManagerFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateRoomScheduleRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import java.time.LocalTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage as EntityLocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage as ModelLocationUsage

class RoomAdministrationIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var locationAttributeRepository: LocationAttributeRepository

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var entityManagerFactory: EntityManagerFactory

  @Test
  fun `should add a scheduled row to a room schedule`() {
    val id = locationAttributeRepository.saveAndFlush(
      LocationAttribute(
        dpsLocationId = wandsworthLocation.id,
        prison = prisonRepository.findByCode(WANDSWORTH)!!,
        createdBy = "INTEGRATION TEST",
        locationUsage = EntityLocationUsage.SCHEDULE,
        prisonVideoUrl = "https://probation-url",
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
      wandsworthLocation.id,
      PROBATION_USER,
    )

    getLocationAttribute(id).schedule().size isEqualTo 1
  }

  // Using the entity manager to get round lazy loading of schedule(s)
  private fun getLocationAttribute(id: Long) = entityManagerFactory
    .createEntityManager()
    .createQuery("SELECT la from LocationAttribute la where la.locationAttributeId = $id")
    .singleResult as LocationAttribute

  private fun WebTestClient.createSchedule(request: CreateRoomScheduleRequest, dpsLocationId: UUID, user: ExternalUser) = this
    .post()
    .uri("/room-admin/$dpsLocationId/schedule")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isCreated
}
