package com.easy.springboot.demo_microservice_api_user

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
class SessionController {
    val log = LoggerFactory.getLogger(SessionController::class.java)
    @RequestMapping(value = ["/session"])
    fun getSession(request: HttpServletRequest): SessionInfo {
        val session = request.session
        log.info(session.javaClass.canonicalName)
        log.info(session.id)

        val SessionInfo = SessionInfo()
        SessionInfo.id = session.id
        SessionInfo.creationTime = session.creationTime
        SessionInfo.lastAccessedTime = session.lastAccessedTime
        SessionInfo.maxInactiveInterval = session.maxInactiveInterval
        SessionInfo.isNew = session.isNew
        return SessionInfo
    }

    class SessionInfo {
        var id = ""
        var creationTime = 0L
        var lastAccessedTime = 0L
        var maxInactiveInterval = 0
        var isNew = false
    }
}
