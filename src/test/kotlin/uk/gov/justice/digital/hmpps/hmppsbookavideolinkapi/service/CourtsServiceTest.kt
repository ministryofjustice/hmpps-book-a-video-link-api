package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.user
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.SetCourtPreferencesRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.SetCourtPreferencesResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.UserCourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court as CourtEntity
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.UserCourt as UserCourtEntity

class CourtsServiceTest {
  private val courtRepository: CourtRepository = mock()
  private val userCourtRepository: UserCourtRepository = mock()
  private val service = CourtsService(courtRepository, userCourtRepository)
  private fun courtEntity(id: Long, code: String, desc: String, enabled: Boolean = true, notes: String? = "notes") =
    CourtEntity(id, code, desc, enabled, notes, "name")

  private fun userCourtEntity(id: Long, court: CourtEntity, username: String) =
    UserCourtEntity(id, court, username, username)

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `Should return a list of enabled courts`() {
    val listOfEnabledCourts = listOf(
      courtEntity(1L, "COURT1", "One"),
      courtEntity(2L, "COURT2", "Two"),
      courtEntity(3L, "COURT3", "Three"),
    )

    whenever(courtRepository.findAllByEnabledIsTrue()).thenReturn(listOfEnabledCourts)

    assertThat(service.getEnabledCourts()).isEqualTo(
      listOfEnabledCourts.toModel(),
    )

    verify(courtRepository).findAllByEnabledIsTrue()
  }

  @Test
  fun `Should return an empty list when no courts are enabled`() {
    whenever(courtRepository.findAllByEnabledIsTrue()).thenReturn(emptyList())
    assertThat(service.getEnabledCourts()).isEmpty()
    verify(courtRepository).findAllByEnabledIsTrue()
  }

  @Test
  fun `Should get user-court preferences for a user`() {
    val listOfCourtsForUser = listOf(
      courtEntity(1L, "COURT1", "One"),
      courtEntity(2L, "COURT2", "Two"),
      courtEntity(3L, "COURT3", "Three"),
    )

    whenever(courtRepository.findCourtsByUsername("user")).thenReturn(listOfCourtsForUser)

    assertThat(service.getUserCourtPreferences(user("user"))).isEqualTo(
      listOfCourtsForUser.toModel(),
    )

    verify(courtRepository).findCourtsByUsername("user")
  }

  @Test
  fun `Should set court preferences for a user`() {
    val user = user("user")
    val existingCourts = listOf("COURT1", "COURT2")
    val requestedCourts = listOf("COURT3", "COURT4")
    val request = SetCourtPreferencesRequest(courtCodes = requestedCourts)

    // Mock response for the current UserCourt selections to remove
    val existingPreferences = listOf(
      userCourtEntity(1L, courtEntity(1, existingCourts.first(), "One"), user.username),
      userCourtEntity(2L, courtEntity(2, existingCourts.last(), "Two"), user.username),
    )

    // Mock response for requested courts
    val newCourts = listOf(
      courtEntity(3L, "COURT3", "Three"),
      courtEntity(4L, "COURT4", "Four"),
    )

    whenever(userCourtRepository.findAllByUsername(user.username)).thenReturn(existingPreferences)
    whenever(courtRepository.findAllByCodeIn(requestedCourts)).thenReturn(newCourts)

    assertThat(service.setUserCourtPreferences(request, user))
      .isEqualTo(SetCourtPreferencesResponse(courtsSaved = 2))

    verify(userCourtRepository).findAllByUsername(user.username)
    verify(userCourtRepository, times(2)).delete(any())
    verify(userCourtRepository, times(2)).saveAndFlush(any())
    verify(courtRepository).findAllByCodeIn(requestedCourts)
  }

  @Test
  fun `Should set court preferences for a user only for enabled courts`() {
    val user = user("user")
    val existingCourts = listOf("COURT1")
    val requestedCourts = listOf("COURT3", "COURT4")
    val request = SetCourtPreferencesRequest(courtCodes = requestedCourts)

    // Mock response for the current UserCourt selections to remove
    val existingPreferences = listOf(
      userCourtEntity(1L, courtEntity(1, existingCourts.first(), "One"), user.username),
    )

    // Mock response for requested courts
    val newCourts = listOf(
      courtEntity(3L, "COURT3", "Three", enabled = false),
      courtEntity(4L, "COURT4", "Four"),
    )

    whenever(userCourtRepository.findAllByUsername(user.username)).thenReturn(existingPreferences)
    whenever(courtRepository.findAllByCodeIn(requestedCourts)).thenReturn(newCourts)

    assertThat(service.setUserCourtPreferences(request, user))
      .isEqualTo(SetCourtPreferencesResponse(courtsSaved = 1))

    verify(userCourtRepository).findAllByUsername(user.username)
    verify(userCourtRepository).delete(any())
    verify(userCourtRepository).saveAndFlush(any())
    verify(courtRepository).findAllByCodeIn(requestedCourts)
  }
}
