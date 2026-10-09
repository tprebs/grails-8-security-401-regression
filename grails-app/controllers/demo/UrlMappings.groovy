package demo

class UrlMappings {

    static mappings = {
        "/secret"(controller: 'secret', action: 'index')
        "/"(view: '/index')
        "500"(view: '/error')
    }
}
