package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class NomisMappingApiMockServer : MockServer(8096)

class NomisMappingApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val server = NomisMappingApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    server.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    server.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    server.stop()
  }
}
