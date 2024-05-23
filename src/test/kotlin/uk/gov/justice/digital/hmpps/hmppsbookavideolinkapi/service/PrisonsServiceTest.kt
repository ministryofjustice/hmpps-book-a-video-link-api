package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison as PrisonEntity

class PrisonsServiceTest {
  private val prisonRepository: PrisonRepository = mock()
  private val service = PrisonsService(prisonRepository)

  private fun prisonEntity(id: Long, code: String, name: String, enabled: Boolean = true, notes: String? = "notes") =
    PrisonEntity(id, code, name, name, enabled, notes, "name")

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `Should return a list of enabled prisons`() {
    val listOfEnabledPrisons = listOf(
      prisonEntity(1L, "BMI", "Birmingham"),
      prisonEntity(2L, "LEI", "Leeds"),
      prisonEntity(3L, "PVI", "Pentonville"),
    )

    whenever(prisonRepository.findAllByEnabledIsTrue()).thenReturn(listOfEnabledPrisons)

    assertThat(service.getListOfPrisons(true)).isEqualTo(
      listOfEnabledPrisons.toModel(),
    )

    verify(prisonRepository).findAllByEnabledIsTrue()
  }

  @Test
  fun `Should return a list of all prisons`() {
    val listOfAllPrisons = listOf(
      prisonEntity(1L, "BMI", "Birmingham"),
      prisonEntity(2L, "LEI", "Leeds"),
      prisonEntity(3L, "PVI", "Pentonville"),
    )

    whenever(prisonRepository.findAll()).thenReturn(listOfAllPrisons)

    assertThat(service.getListOfPrisons(false)).isEqualTo(
      listOfAllPrisons.toModel(),
    )

    verify(prisonRepository).findAll()
  }

  @Test
  fun `Should return an empty list when no prisons are enabled`() {
    whenever(prisonRepository.findAllByEnabledIsTrue()).thenReturn(emptyList())
    assertThat(service.getListOfPrisons(true)).isEmpty()
    verify(prisonRepository).findAllByEnabledIsTrue()
  }
}
