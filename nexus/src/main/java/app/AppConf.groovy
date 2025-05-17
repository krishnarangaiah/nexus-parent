package app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(["app.conf", "app.controller", "app.dao", "app.service", "app.websocket"])
class AppConf {}