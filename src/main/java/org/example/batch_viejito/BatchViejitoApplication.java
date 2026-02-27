package org.example.batch_viejito;

import service.CampaniaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"dao", "service", "dto", "org.example.batch_viejito"})
public class BatchViejitoApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchViejitoApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(CampaniaService service) {
        return args -> service.ejecutarProcesoCampania();
    }
}