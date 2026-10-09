# Grails 8: a `@Secured` controller answers 500 instead of 401

A standard Grails application with Spring Security. One controller, one `@Secured` annotation, one request
with no credentials.

**Expected: `401 Unauthorized`. Actual on Grails 8: `500 Internal Server Error`.**

## Running it

    ./runGrails7.sh     # passes - the request is answered properly
    ./runGrails8.sh     # fails  - the server answers 500

Requires JDK 21. Each script sets the Grails version and the Gradle wrapper, runs
the integration test, and restores both on exit.

| | Grails | Gradle | unauthenticated `GET /secret` |
|---|---|---|---|
| `./runGrails7.sh` | 7.2.3 | 8.11.1 | **401** - correct |
| `./runGrails8.sh` | 8.0.0 | 9.8.0 | **500** - `AssertionError` |

## The whole application

`grails-app/controllers/demo/SecretController.groovy`:

```groovy
class SecretController {

    @Secured(['ROLE_USER'])
    def index() {
        render(contentType: 'application/json', text: '{"secret":"only for authenticated callers"}')
    }
}
```

`grails-app/conf/application.groovy`:

```groovy
grails.plugin.springsecurity.filterChain.chainMap = [
        [
                pattern: '/**',
                filters: 'JOINED_FILTERS,-anonymousAuthenticationFilter,-exceptionTranslationFilter,' +
                        '-authenticationProcessingFilter,-securityContextPersistenceFilter,' +
                        '-rememberMeAuthenticationFilter'
        ],
]

grails.plugin.springsecurity.rest.token.validation.useBearerToken = true
```

The application uses the Spring Security REST plugin, and the chain above is taken from a production
codebase: a stateless API whose catch-all chain deliberately carries no anonymous authentication filter.

## What happens

Grails logs the chain it built with no anonymous authentication filter, exactly as configured:

```
Creating filter chain: Ant [pattern='/**'],
  [SecurityRequestHolderFilter, SecurityContextPersistenceFilter, MutableLogoutFilter,
   GrailsUsernamePasswordAuthenticationFilter, SecurityContextHolderAwareRequestFilter,
   GrailsRememberMeAuthenticationFilter, UpdateRequestContextHolderExceptionTranslationFilter,
   FilterSecurityInterceptor]
```

An unauthenticated request reaches `FilterSecurityInterceptor` with a null `Authentication`, and the server log shows:

```
ERROR ... Servlet.service() ... threw exception [Filter execution threw an exception] with root cause
java.lang.AssertionError: authentication cannot be null. Expression: authentication. Values: authentication = null
    at grails.plugin.springsecurity.web.access.expression.WebExpressionVoter.vote(WebExpressionVoter.groovy:47)
ERROR ... Exception Processing [ErrorPage[errorCode=500, location=/error]]
```

## Why

Spring Security 7 removed `FilterSecurityInterceptor`. Grails 8 includes `org.apache.grails:grails-spring-security-compat` to re-implement them in Spring Security's own package namespace which is why the stack frame above reads `FilterSecurityInterceptor.groovy`.

The re-implementation omits the null-`Authentication` guard. Spring Security's
`AbstractSecurityInterceptor.authenticateIfRequired()` begins:

```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
if (authentication == null) {
    throw new AuthenticationCredentialsNotFoundException(
        "An Authentication object was not found in the SecurityContext");
}
```

`AuthenticationCredentialsNotFoundException` extends `AuthenticationException`, so
`ExceptionTranslationFilter` catches it and returns 401.

In `grails-spring-security-compat:8.0.0`:

- `AbstractSecurityInterceptor` has no `authenticateIfRequired` method
- `AuthenticationCredentialsNotFoundException` is not referenced in the compat jar

With the guard gone the null reaches `WebExpressionVoter`, whose Groovy `assert` throws an `AssertionError`.
That is an `Error`, not an `AuthenticationException`, so `ExceptionTranslationFilter` does not catch it and
the request fails as a 500.

## Confirmed against Grails 7

`./runGrails7.sh` runs this same application on Grails 7.2.3 and the test passes - the unauthenticated
request is answered with 401.

The same contrast was observed on a production codebase. One specification, run on both branches with
identical security configuration, the same JDK 21 and the same machine:

| Grails | result |
|---|---|
| 7.2.3 | 6 tests, **0 failures** - 401 |
| 8.0.0 | 6 tests, **1 failure** - 500 |
