package ar.edu.itba.harmos

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = ["ar.edu.itba.harmos"])
@EntityScan(basePackages = ["ar.edu.itba.harmos.models"])
@EnableJpaRepositories(basePackages = ["ar.edu.itba.harmos.persistence"])
class HarmosApplication

fun main(args: Array<String>) {
	runApplication<HarmosApplication>(*args)
} 