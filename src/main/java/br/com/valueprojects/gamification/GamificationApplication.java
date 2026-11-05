package br.com.valueprojects.gamification;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // dispatcher da outbox (módulo evento)
public class GamificationApplication {

    /** Relógio injetável: os testes controlam o tempo para verificar o backoff sem esperar. */
    @Bean
    public Clock relogio() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(GamificationApplication.class, args);
    }
}
