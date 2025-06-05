package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.health

import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature.FEATURE_MASTER_PUBLIC_PRIVATE_NOTES
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches

@Component
class FeatureToggleContributor(private val featureSwitches: FeatureSwitches) : InfoContributor {
  override fun contribute(builder: Info.Builder) {
    builder.withDetails(
      mapOf(
        "featureToggles" to mapOf(
          FEATURE_MASTER_PUBLIC_PRIVATE_NOTES to featureSwitches.isEnabled(
            FEATURE_MASTER_PUBLIC_PRIVATE_NOTES,
          ).toString(),
        ),
      ),
    )
  }
}
