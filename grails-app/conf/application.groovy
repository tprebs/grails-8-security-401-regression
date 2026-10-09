// The filter chain this reproduction is about.
//
// This mirrors a production application: a stateless REST API where the catch-all chain deliberately
// carries no anonymous authentication filter. The Spring Security Core plugin documents composing a chain
// with JOINED_FILTERS and names prefixed with "-", so this is ordinary, supported configuration.
//
// An unauthenticated caller therefore reaches the authorisation interceptor with a null Authentication.
grails.plugin.springsecurity.filterChain.chainMap = [
        [
                pattern: '/**',
                filters: 'JOINED_FILTERS,-anonymousAuthenticationFilter,-exceptionTranslationFilter,' +
                        '-authenticationProcessingFilter,-securityContextPersistenceFilter,' +
                        '-rememberMeAuthenticationFilter'
        ],
]

grails.plugin.springsecurity.securityConfigType = 'Annotation'

// Spring Security REST: answer API callers with a status code instead of redirecting to a login page.
grails.plugin.springsecurity.rest.token.validation.useBearerToken = true
grails.plugin.springsecurity.rest.token.storage.jwt.secret = 'a-secret-long-enough-for-the-jwt-signer-to-accept-it'
