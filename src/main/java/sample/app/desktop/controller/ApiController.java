package sample.app.desktop.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    @PostMapping("/hello")
    public String sayHello(@RequestBody(required = false) String message) {
        log.info("Received message: {}", message);
        return "Hello from Spring Boot!";
    }
}