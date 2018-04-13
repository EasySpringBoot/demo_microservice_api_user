package com.easy.springboot.demo_microservice_api_user

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController {
    @RequestMapping(value = "/user/1")
    fun getOne(): User {
        val user = User()
        user.id = 1
        user.username = "user"
        user.password = "123456"
        return user
    }


    class User {
        var id: Long = 0
        var username: String = ""
        var password: String = ""
    }
}
