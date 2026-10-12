package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.BooleanFeature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.StringFeature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.InboundEventsListener
import uk.gov.justice.hmpps.sqs.HmppsQueueService

class FeatureSwitchesTest : IntegrationTestBase() {

  // Beans are mocked out so as not to interfere with the running of the tests. We do not want/need them on the context.
  @MockitoBean
  private lateinit var hmppsQueueService: HmppsQueueService

  @MockitoBean
  private lateinit var listener: InboundEventsListener

  @TestPropertySource(
    properties = [
      "feature.placeholder.example=true",
      "feature.public.private.comments.sync=true",
      "feature.court.only.prisons=PVI",
      "feature.probation.only.prisons=MDI",
    ],
  )
  @Nested
  @DisplayName("Features are enabled/present when set")
  inner class EnabledFeatures(@Autowired val featureSwitches: FeatureSwitches) {
    @Test
    fun `boolean features are enabled`() {
      BooleanFeature.entries.forEach {
        assertThat(featureSwitches.isEnabled(it)).withFailMessage("${it.label} not enabled").isTrue
      }
    }

    @Test
    fun `string features are set`() {
      val features =
        mapOf(StringFeature.FEATURE_COURT_ONLY_PRISONS to "PVI", StringFeature.FEATURE_PROBATION_ONLY_PRISONS to "MDI")

      features.size isEqualTo StringFeature.entries.size

      features.forEach { (key, value) ->
        assertThat(featureSwitches.getValue(key)).withFailMessage("${key.label} not set").isEqualTo(value)
      }
    }
  }

  @Nested
  @DisplayName("Boolean features are disabled by default")
  inner class DisabledFeatures(@Autowired val featureSwitches: FeatureSwitches) {
    @Test
    fun `features are disabled by default`() {
      BooleanFeature.entries.forEach {
        assertThat(featureSwitches.isEnabled(it)).withFailMessage("${it.label} enabled").isFalse
      }
    }
  }

  @Nested
  @DisplayName("Features can be defaulted when not present")
  inner class DefaultedFeatures(@Autowired val featureSwitches: FeatureSwitches) {
    @Test
    fun `boolean feature types can be defaulted `() {
      featureSwitches.isEnabled(BooleanFeature.FEATURE_PLACEHOLDER, true) isBool true
    }

    @Test
    fun `string feature types can be defaulted `() {
      featureSwitches.getValue(StringFeature.FEATURE_COURT_ONLY_PRISONS, "default value") isEqualTo "default value"
    }
  }
}
