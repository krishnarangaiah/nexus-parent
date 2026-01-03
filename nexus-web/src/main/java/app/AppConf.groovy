package app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@ComponentScan(["app.conf", "app.controller", "app.dao", "app.service", "app.websocket"])
class AppConf {}