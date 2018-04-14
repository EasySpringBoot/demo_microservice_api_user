package com.easy.springboot.demo_microservice_api_user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class RedisTemplateController {
    @Autowired lateinit var stringRedisTemplate: StringRedisTemplate

    @RequestMapping(value = ["/redis/{key}/{value}"], method = [RequestMethod.GET])
    fun redisSave(@PathVariable key: String, @PathVariable value: String): String {

        val redisValue = stringRedisTemplate.opsForValue().get(key)

        if (StringUtils.isEmpty(redisValue)) {
            stringRedisTemplate.opsForValue().set(key, value)
            return String.format("设置[key=%s,value=%s]成功！", key, value)
        }

        if (redisValue != value) {
            stringRedisTemplate.opsForValue().set(key, value)
            return String.format("更新[key=%s,value=%s]成功！", key, value)
        }

        return String.format("redis中已存在[key=%s,value=%s]的数据！", key, value)
    }

    @RequestMapping(value = ["/redis/{key}"], method = [RequestMethod.GET])
    fun redisGet(@PathVariable key: String): String? {
        return stringRedisTemplate.opsForValue().get(key)
    }

    @RequestMapping(value = ["/redisHash/{key}/{field}"], method = [RequestMethod.GET])
    fun redisHashGet(@PathVariable key: String, @PathVariable field: String): String? {
        return stringRedisTemplate.opsForHash<String, String>().get(key, field)
    }
}
