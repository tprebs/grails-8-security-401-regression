package demo

import grails.testing.mixin.integration.Integration
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.springframework.beans.factory.annotation.Value
import spock.lang.Specification

/**
 * Requests a @Secured Grails controller over HTTP without credentials.
 *
 * With the Spring Security REST plugin the server answers 401. On Grails 8 it answers 500 instead, because
 * grails-spring-security-compat's AbstractSecurityInterceptor omits the null-Authentication guard that
 * Spring Security uses to raise AuthenticationCredentialsNotFoundException.
 */
@Integration
class UnauthenticatedRequestSpec extends Specification {

    @Value('${local.server.port}')
    Integer serverPort

    void "an unauthenticated request to a secured controller is answered with 401"() {
        given: "no Authorization header"
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:${serverPort}/secret")).GET().build()

        when: "the request is made"
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString())

        then: "the caller is told to authenticate, rather than that the server failed"
        response.statusCode() == 401
    }
}
