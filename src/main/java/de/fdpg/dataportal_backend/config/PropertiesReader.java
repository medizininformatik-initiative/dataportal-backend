package de.fdpg.dataportal_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration
@PropertySource("classpath:maven.properties")
public class PropertiesReader {

  private final Environment env;

  public PropertiesReader(Environment env) {
    this.env = env;
  }

  public String getValue(String key) {
    return env.getProperty(key);
  }
}