package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

class EndpointSecurityCheck {

  @Test
  fun `Ensure checks are working by referencing fake unprotected controller`() {
    getAllUnprotectedControllers().map(ControllerInfo::clazz) containsExactlyInAnyOrder listOf(FakeUnprotectedController::class.java)
  }

  @Test
  fun `Ensure endpoints are secured`() {
    val controllers = getAllUnprotectedControllers().filterNot { it.clazz == FakeUnprotectedController::class.java }

    if (controllers.isNotEmpty()) {
      fail("The following controllers are not secured: ${controllers.joinToString("\n")}\n")
    }
  }

  private fun getAllUnprotectedControllers() = ClassPathScanningCandidateComponentProvider(false)
    .apply { addIncludeFilter(AnnotationTypeFilter(RestController::class.java)) }
    .findCandidateComponents(this::class.java.`package`.name)
    .map { Class.forName(it.beanClassName) }
    .filterNot(Class<*>::isProtected)
    .map(::ControllerInfo)
    .filter { it.unprotectedEndpoints.isNotEmpty() }
}

private data class EndpointInfo(val method: String, val hasEndpointLevelProtection: Boolean) {
  constructor(method: Method) : this(method.toString(), method.isProtected())
}

private fun AnnotatedElement.isProtected() =
  getAnnotation(PreAuthorize::class.java)?.let { it.value.contains("hasAnyRole") || it.value.contains("hasRole") } == true ||
    getAnnotation(ProtectedByIngress::class.java) != null

private data class ControllerInfo(val clazz: Class<*>, val controller: String, val unprotectedEndpoints: List<EndpointInfo>) {

  constructor(clazz: Class<*>) : this(clazz, clazz.toString(), clazz.getUnprotectedEndpoints())

  override fun toString() =
    "\n$controller:".plus(unprotectedEndpoints.joinToString(separator = "\n * ", prefix = "\n * ") { it.method })
}

private fun Class<*>.getUnprotectedEndpoints() =
  methods.filter { it.isEndpoint() }.map(::EndpointInfo).filterNot(EndpointInfo::hasEndpointLevelProtection)

private fun Method.isEndpoint() =
  this.annotations.any { it.annotationClass.qualifiedName!!.startsWith("org.springframework.web.bind.annotation") }

@RestController
@RequestMapping("/unprotected", produces = [MediaType.APPLICATION_JSON_VALUE])
internal class FakeUnprotectedController {
  @GetMapping
  fun getNothing(): Nothing = TODO()
}
