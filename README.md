# demo_microservice_api_user

Spring Boot 是构建单个微服务应用的理想选择，但是我们还需要以某种方式将它们互相联系起来。这就是 Spring Cloud Netflix 所要解决的问题。Netflix 它提供了各种组件，比如：Eureka服务发现与Ribbon客户端负载均衡的结合，为内部“微服务”提供通信支持。
本章介绍如何通过使用 Netflix Zuul 实现一个微服务API Gateway 来实现简单代理转发和过滤器功能。

API Gateway简介

API Gateway 是随着微服务（Microservice）这个概念一起兴起的一种架构模式，它用于解决微服务过于分散，没有一个统一的出入口进行流量管理的问题。

不同的微服务一般有不同的网络域名（或 IP地址），而通常情况下，在大规模分布式架构系统中，外部的客户端可能需要调用多个服务的接口才能完成一个业务逻辑。比如，在京东、淘宝上下单购买一个商品的场景，通常会去商品数据服务、订单服务、支付服务等。如果客户端直接单独和这些微服务进行通信，可能会存在如下的问题：

客户端会多次请求不同微服务，增加客户端的复杂性
存在跨域请求，在一定场景下处理相对复杂
认证复杂，每一个服务都需要独立认证

诸如上述问题，我们可以引入一个中间代理层—— API Gateway 来解决。API Gateway 是介于客户端和服务器端之间的中间层，所有的外部请求都会先经过微服务网关，架构图如下：



这样客户端只需要和API Gateway交互，而无需单独去调用特定微服务的接口，而且方便监控，易于认证，减少客户端和各个微服务之间的交互次数。


Zuul 简介

对于 API Gateway，常见的选型有基于 Openresty 的 Kong、基于 Go 的 Tyk 和基于 Java 的 Zuul。常规的选择我们会使用Nginx作为代理。但是Netflix带来了它自己的解决方案——智能路由Zuul。它带有许多有趣的功能，它可以用于身份验证、服务迁移、分级卸载以及各种动态路由选项。同时，它是使用Java编写的。

Zuul是Netflix开源的微服务网关，可以和Eureka,Ribbon,Hystrix等组件配合使用。Zuul 是netflix开源的一个API Gateway 服务器, 本质上是一个web servlet应用。Zuul 在云平台上提供动态路由，监控，弹性，安全等边缘服务的框架。Zuul 相当于是设备和 Netflix 流应用的 Web 网站后端所有请求的前门。

Netflix Zuul 中提供了服务发现 (Eureka), Circuit Breaker (Hystrix), 智能路由 (Zuul) 和客户端负载均衡 (Ribbon) 等功能。

Zuul可以简单理解为一个类似于 Servlet 中过滤器（Filter）的概念。和大部分基于Java的Web应用类似，Zuul也采用了servlet架构，因此Zuul处理每个请求的方式是针对每个请求是用一个线程来处理。通常情况下，为了提高性能，所有请求会被放到处理队列中，从线程池中选取空闲线程来处理该请求。这样的设计方式，足以应付一般的高并发场景。Zuul 是在云平台上提供动态路由，监控，弹性，安全等边缘服务的框架。

Zuul组件的核心是一系列的过滤器，它们可以完成以下功能：

身份认证和安全: 识别每一个资源的验证要求，并拒绝那些不符的请求。
审计和监控：实现对 API 调用过程的审计和监控，追踪有意义数据及统计结果，从而为我们带来准确的生产状态数据。
动态路由：动态将请求路由到不同后端集群。
压力测试：逐渐增加指向集群的流量，以了解系统的性能。
负载分配：为每一种负载类型分配对应容量，并弃用超出限定值的请求。
静态响应处理：边缘位置进行响应，避免转发到内部集群。
多区域弹性：跨域AWS Region进行请求路由，旨在实现ELB(ElasticLoad Balancing)使用多样化。

Zuul 提供了四种过滤器的 API，分别为前置（pre）、后置（post）、路由（route）和错误（error）四种处理方式。其生命周期如下图所示



一个请求会先按顺序通过所有的前置过滤器，之后在路由过滤器中转发给后端应用，得到响应后又会通过所有的后置过滤器，最后响应给客户端。在整个流程中如果发生了异常则会跳转到错误过滤器中。

一般来说，如果需要在请求到达后端应用前就进行处理的话，会选择前置过滤器，例如鉴权、请求转发、增加请求参数等行为。在请求完成后需要处理的操作放在后置过滤器中完成，例如统计返回值和调用时间、记录日志、增加跨域头等行为。路由过滤器一般只需要选择 Zuul 中内置的即可，错误过滤器一般只需要一个，这样可以在 Gateway 遇到错误逻辑时直接抛出异常中断流程，并直接统一处理返回结果。



Spring Cloud 对 Zuul 进行了整合和增强。目前，Zuul使用的默认是Apache的HTTP Client。也可以通过设置ribbon.restclient.enabled=true 来使用Rest Client。在 Zuul 中，每一个后端应用都称为一个 Route，为了避免一个 Route 抢占了太多资源影响到其他 Route 的情况出现，Zuul 使用 Hystrix 对每一个 Route 都做了隔离和限流。





提示：更多关于 Zuul 的内容参考 https://github.com/Netflix/zuul 。


项目实战

本节介绍如何使用Spring Boot 集成 Zuul 来实现 API Gateway。


1.创建项目
首先我们来创建基于 Kotlin、Gradle 的 Spring Boot 项目。使用的Kotlin、Spring Boot、Spring Cloud的版本号分别配置如下

buildscript {
  ext {
    kotlinVersion = '1.2.20'
    springBootVersion = '2.0.1.RELEASE'
  }

  dependencies {
    classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
    classpath("org.jetbrains.kotlin:kotlin-allopen:${kotlinVersion}")
  }
}
...
ext {
  springCloudVersion = 'Finchley.M9'
}
...
dependencyManagement {
  imports {
    mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
  }
}


2.添加Zuul依赖

通常Zuul需要注册到Eureka上。这里我们为了简单演示，只实现一个单机版的 API Gateway。在 build.gradle 中添加 spring-cloud-starter-netflix-zuul 如下

repositories {
  mavenCentral()
  maven { url "https://repo.spring.io/milestone" }
}

ext {
  springCloudVersion = 'Finchley.M9'
}

dependencies {
  compile('org.springframework.cloud:spring-cloud-starter-netflix-zuul')
  ...
}

dependencyManagement {
  imports {
    mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
  }
}


3.启动类加上注解 @EnableZuulProxy

@EnableZuulProxy注解默认加上了@EnableCircuitBreaker。它的定义如下

@EnableCircuitBreaker
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ZuulProxyMarkerConfiguration.class)
public @interface EnableZuulProxy

在 Spring Boot 启动类上添加注解 @EnableZuulProxy代码如下

@SpringBootApplication
@EnableZuulProxy
open class DemoZuulApplication

fun main(args: Array<String>) {
    runApplication<DemoZuulApplication>(*args)
}

其中，@EnableZuulProxy简单理解为@EnableZuulServer的增强版，当Zuul与Eureka、Ribbon等组件配合使用时，我们使用@EnableZuulProxy。

4.配置application.properties

zuul.routes.book_api.url=http://127.0.0.1:9000
zuul.routes.user_api.url=http://127.0.0.1:9001
server.port=8000

其中， book_api 是微服务 Book 的服务 API 地址标识，user_api 是微服务 User 的服务 API 地址标识。这个请求流程可以简单如下图所示


5.启动测试微服务应用

http://localhost:9000/book/1

{
  "id": 1,
  "title": "Spring Boot 2.0 极简教程",
  "author": "陈光剑"
}

http://localhost:9001/user/1

{
  "id": 1,
  "username": "user",
  "password": "123456"
}

6.启动 API Gateway 服务

访问http://127.0.0.1:8000/user_api/user/1，可以得到输出

{
  "id": 1,
  "username": "user",
  "password": "123456"
}

访问http://127.0.0.1:8000/book_api/book/1，可以得到输出

{
  "id": 1,
  "title": "Spring Boot 2.0 极简教程",
  "author": "陈光剑"
}



7.编写Zuul过滤器

我们只需要继承抽象类ZuulFilter过滤器即可，让该过滤器打印请求日志

package com.easy.springboot.demo_zuul

import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SimpleFilter : ZuulFilter() {
    private val log = LoggerFactory.getLogger(SimpleFilter::class.java)
    override fun run(): Any? {
        val ctx = RequestContext.getCurrentContext()
        val request = ctx.request

        log.info(String.format("%s request to %s", request.method, request.requestURL.toString()))
        log.info(String.format("LocalAddr: %s", request.localAddr))
        log.info(String.format("LocalName: %s", request.localName))
        log.info(String.format("LocalPort: %s", request.localPort))

        log.info(String.format("RemoteAddr: %s", request.remoteAddr))
        log.info(String.format("RemoteHost: %s", request.remoteHost))
        log.info(String.format("RemotePort: %s", request.remotePort))

        return null
    }

    override fun shouldFilter(): Boolean {
        // 判断是否需要过滤
        return true
    }

    override fun filterType(): String {
        // 过滤器类型
        return "pre"
    }

    override fun filterOrder(): Int {
        // 过滤器的优先级，越大越靠后执行
        return 1
    }

}


其中，fun filterType()指定过滤器类型为"pre" 。下面是 Zuul 提供的几种标准的过滤器类型：
pre：这种过滤器在请求到达Origin Server之前调用。比如身份验证，在集群中选择请求的Origin Server，记log等。
route：在这种过滤器中把用户请求发送给Origin Server。发送给Origin Server的用户请求在这类过滤器中build。并使用Apache HttpClient或者Netfilx Ribbon发送给Origin Server。
post：这种过滤器在用户请求从Origin Server返回以后执行。比如在返回的response上面加response header，做各种统计等。并在该过滤器中把response返回给客户。
error：在其他阶段发生错误时执行该过滤器。
客户定制：支持自定义静态响应的"静态"类型, 请参见 StaticResponseFilter类。通过调用 FilterProcessor.runFilters (类型) 来创建或添加并运行任何 filterType。

这些过滤器的核心处理逻辑在ZuulServlet类中。关键代码说明如下

public class ZuulServlet extends HttpServlet {
    ...
    @Override
    public void service(...) throws ServletException, IOException {
        try {
            // 初始化请求响应对象
            init((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
            ...
            RequestContext context = RequestContext.getCurrentContext();
            context.setZuulEngineRan();

            try {
                preRoute();// "pre"过滤器
            } catch (ZuulException e) {
                error(e);
                postRoute();
                return;
            }
            try {
                route();// "route" 过滤器
            } catch (ZuulException e) {
                error(e);
                postRoute();
                return;
            }
            try {
                postRoute();// "post"过滤器
            } catch (ZuulException e) {
                error(e);
                return;
            }

        } catch (Throwable e) {
            // "error" 过滤器
            error(new ZuulException(e, 500, "UNHANDLED_EXCEPTION_" + e.getClass().getName()));
        } finally {
            RequestContext.getCurrentContext().unset();
        }
        ...
    }
}





8.测试SimpleFilter过滤器效果
重启应用，再次分别请求 http://127.0.0.1:8000/user_api/user/1 和 http://127.0.0.1:8000/book_api/book/1，我们可以在API Gateway 应用的控制台后端看到类似下面的请求日志

GET request to http://127.0.0.1:8000/user_api/user/1
LocalAddr: 127.0.0.1
LocalName: localhost
LocalPort: 8000
RemoteAddr: 127.0.0.1
RemoteHost: 127.0.0.1
RemotePort: 61747
GET request to http://127.0.0.1:8000/book_api/book/1
LocalAddr: 127.0.0.1
LocalName: localhost
LocalPort: 8000
RemoteAddr: 127.0.0.1
RemoteHost: 127.0.0.1
RemotePort: 61747

提示：API Gateway 工程源代码：https://github.com/EasySpringBoot/demo_zuul
Book 微服务工程源代码：https://github.com/EasySpringBoot/demo_microservice_api_book
User 微服务工程源代码：https://github.com/EasySpringBoot/demo_microservice_api_user


本章小结

使用API Gateway 我们将"1对N"问题 转换成了"1对1”问题，同时在请求到达真正的服务之前，可以做一些预处理工作。API Gateway 的可以完成诸如鉴权、流量控制、系统监控、页面缓存等功能。使用 Spring Boot 加上 Spring Cloud “全家桶”来实现微服务架构无疑是一种相当不错的选择。




