package ar.edu.itba.harmos

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HarmosApplication

fun main(args: Array<String>) {
	runApplication<HarmosApplication>(*args)
}
