package de.fdpg.dataportal_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DataportalBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(DataportalBackendApplication.class, args);
  }

}
