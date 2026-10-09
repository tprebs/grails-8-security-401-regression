package demo

import grails.plugin.springsecurity.annotation.Secured

/**
 * An ordinary Grails controller that requires an authenticated user.
 */
class SecretController {

    @Secured(['ROLE_USER'])
    def index() {
        render(contentType: 'application/json', text: '{"secret":"only for authenticated callers"}')
    }
}
