package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.web.PagedModel
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.CreateCourtBookingRequestBuilder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.CreateProbationBookingRequestBuilder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvillePrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.properties.Delegates

class PrisonerSchedulesIntegrationTest : IntegrationTestBase() {
  @Nested
  inner class GetCourtBookingIDs {
    private val pentonvilleCourtBooking = CreateCourtBookingRequestBuilder.builder {
      court(DERBY_JUSTICE_CENTRE)
      prisoner(pentonvillePrisoner)
      main(pentonvilleLocation, tomorrow(), LocalTime.of(1, 0), LocalTime.of(1, 30))
    }.build()

    private val wandsworthCourtBooking = CreateCourtBookingRequestBuilder.builder {
      court(DERBY_JUSTICE_CENTRE)
      prisoner(wandsworthPrisoner)
      main(wandsworthLocation, tomorrow().plusDays(1), LocalTime.of(1, 0), LocalTime.of(1, 30))
    }.build()

    private var pentonvilleBookingId by Delegates.notNull<Long>()
    private var wandsworthBookingId by Delegates.notNull<Long>()

    @BeforeEach
    fun before() {
      pentonvilleBookingId = run {
        stubGetPrisoner(pentonvillePrisoner)
        webTestClient.createBooking(pentonvilleCourtBooking, COURT_USER)
      }

      wandsworthBookingId = run {
        stubGetPrisoner(wandsworthPrisoner)
        webTestClient.createBooking(wandsworthCourtBooking, COURT_USER)
      }
    }

    @Test
    fun `should get on a single day`() {
      val ids = findBookingIds(tomorrow(), tomorrow())

      ids.single() isEqualTo pentonvilleBookingId
    }

    @Test
    fun `should get over multiple days`() {
      val ids = findBookingIds(tomorrow(), tomorrow().plusDays(1))

      ids containsExactlyInAnyOrder listOf(pentonvilleBookingId, wandsworthBookingId)
    }

    @Test
    fun `should get over multiple days by prison`() {
      val ids = findBookingIds(tomorrow(), tomorrow().plusDays(1), prisonCode = PENTONVILLE)

      ids.single() isEqualTo pentonvilleBookingId
    }

    @Test
    fun `should get over multiple days by prisoner`() {
      val ids = findBookingIds(tomorrow(), tomorrow().plusDays(1), prisonerNumber = wandsworthPrisoner.number)

      ids.single() isEqualTo wandsworthBookingId
    }

    @Test
    fun `should get over multiple days by location ID`() {
      val ids = findBookingIds(tomorrow(), tomorrow().plusDays(1), locationId = pentonvilleLocation.id)

      ids.single() isEqualTo pentonvilleBookingId
    }

    @Test
    fun `should paginate over multiple days`() {
      run {
        val ids = findBookingIds(tomorrow(), tomorrow().plusDays(1), page = 0, pageSize = 1)
        ids.single() isEqualTo pentonvilleBookingId
      }

      run {
        val ids = findBookingIds(tomorrow(), tomorrow().plusDays(1), page = 1, pageSize = 1)
        ids.single() isEqualTo wandsworthBookingId
      }
    }

    private fun findBookingIds(
      fromDate: LocalDate,
      toDate: LocalDate,
      prisonCode: String? = null,
      prisonerNumber: String? = null,
      locationId: UUID? = null,
      page: Int? = null,
      pageSize: Int? = null,
    ) = run {
      webTestClient.findBookingIds("court", fromDate, toDate, prisonCode, prisonerNumber, locationId, page, pageSize).content
    }
  }

  @Nested
  inner class GetProbationBookingIDs {
    private val pentonvilleProbationBooking = CreateProbationBookingRequestBuilder.builder {
      probationTeam(BLACKPOOL_MC_PPOC)
      prisoner(pentonvillePrisoner)
      main(pentonvilleLocation, tomorrow(), LocalTime.of(1, 0), LocalTime.of(1, 30))
    }.build()

    private val wandsworthProbationBooking = CreateProbationBookingRequestBuilder.builder {
      probationTeam(BLACKPOOL_MC_PPOC)
      prisoner(wandsworthPrisoner)
      main(wandsworthLocation, tomorrow().plusDays(1), LocalTime.of(1, 0), LocalTime.of(1, 30))
    }.build()

    private var pentonvilleBookingId by Delegates.notNull<Long>()
    private var wandsworthBookingId by Delegates.notNull<Long>()

    @BeforeEach
    fun before() {
      pentonvilleBookingId = run {
        stubGetPrisoner(pentonvillePrisoner)
        webTestClient.createBooking(pentonvilleProbationBooking, PROBATION_USER)
      }

      wandsworthBookingId = run {
        stubGetPrisoner(wandsworthPrisoner)
        webTestClient.createBooking(wandsworthProbationBooking, PROBATION_USER)
      }
    }

    @Test
    fun `should get on a single day`() {
      val ids = findBookingIds(tomorrow(), tomorrow())

      ids.single() isEqualTo pentonvilleBookingId
    }

    @Test
    fun `should get over multiple days`() {
      val ids = findBookingIds(tomorrow(), tomorrow().plusDays(1))

      ids containsExactlyInAnyOrder listOf(pentonvilleBookingId, wandsworthBookingId)
    }

    @Test
    fun `should get over multiple days by prison`() {
      val ids = findBookingIds(tomorrow(), tomorrow().plusDays(1), prisonCode = PENTONVILLE)

      ids.single() isEqualTo pentonvilleBookingId
    }

    @Test
    fun `should get over multiple days by prisoner`() {
      val ids = findBookingIds(tomorrow(), tomorrow().plusDays(1), prisonerNumber = wandsworthPrisoner.number)

      ids.single() isEqualTo wandsworthBookingId
    }

    @Test
    fun `should get over multiple days by location ID`() {
      val ids = findBookingIds(tomorrow(), tomorrow().plusDays(1), locationId = wandsworthLocation.id)

      ids.single() isEqualTo wandsworthBookingId
    }

    @Test
    fun `should paginate over multiple days`() {
      run {
        val ids = findBookingIds(tomorrow(), tomorrow().plusDays(1), page = 0, pageSize = 1)
        ids.single() isEqualTo pentonvilleBookingId
      }

      run {
        val ids = findBookingIds(tomorrow(), tomorrow().plusDays(1), page = 1, pageSize = 1)
        ids.single() isEqualTo wandsworthBookingId
      }
    }

    private fun findBookingIds(
      fromDate: LocalDate,
      toDate: LocalDate,
      prisonCode: String? = null,
      prisonerNumber: String? = null,
      locationId: UUID? = null,
      page: Int? = null,
      pageSize: Int? = null,
    ) = run {
      webTestClient.findBookingIds("probation", fromDate, toDate, prisonCode, prisonerNumber, locationId, page, pageSize).content
    }
  }

  private fun WebTestClient.findBookingIds(
    bookingTypePrefix: String,
    fromDate: LocalDate,
    toDate: LocalDate,
    prisonCode: String? = null,
    prisonerNumber: String? = null,
    locationId: UUID? = null,
    page: Int? = 0,
    pageSize: Int? = 20,
  ) = run {
    get()
      .uri(
        UriComponentsBuilder.fromPath("/integrations/schedules/$bookingTypePrefix-bookings/ids")
          .queryParam("fromDate", fromDate.toIsoDate())
          .queryParam("toDate", toDate.toIsoDate())
          .queryParam("page", page)
          .queryParam("size", pageSize)
          .maybeQueryParam("prisonCode", prisonCode)
          .maybeQueryParam("prisonerNumber", prisonerNumber)
          .maybeQueryParam("locationId", locationId?.toString())
          .build()
          .toUri().toString(),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = "prisoner schedules user", roles = listOf("ROLE_PRISONER_SCHEDULES__RO")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<PaginatedResponse>()
      .returnResult().responseBody!!
  }

  private fun UriComponentsBuilder.maybeQueryParam(param: String, value: String?) = if (value != null) queryParam(param, value) else this

  data class PaginatedResponse(
    val content: List<Long>,
    val page: PagedModel.PageMetadata,
  )
}
