package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component

@Component
class FeatureToggleContributor(
  @Value("\${FEATURE_GREY_RELEASE_PRISONS:default}") private val greyReleasePrisons: String,
  @Value("\${FEATURE_MASTER_PUBLIC_PRIVATE_NOTES:default}") private val masterPublicPrivateNotes: String,
) : InfoContributor {
  override fun contribute(builder: Info.Builder) {
    builder.withDetails(
      mapOf(
        "featureToggles" to mapOf(
          "FEATURE_GREY_RELEASE_PRISONS" to greyReleasePrisons,
          "FEATURE_MASTER_PUBLIC_PRIVATE_NOTES" to masterPublicPrivateNotes,
        ),
      ),
    )
  }
}
