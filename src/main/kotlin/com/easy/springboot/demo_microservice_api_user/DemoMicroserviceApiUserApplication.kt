package com.easy.springboot.demo_microservice_api_user

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class DemoMicroserviceApiUserApplication

fun main(args: Array<String>) {
    runApplication<DemoMicroserviceApiUserApplication>(*args)
}
