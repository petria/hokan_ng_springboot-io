package org.freakz.hokan_ng_springboot.bot;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.freakz.hokan_ng_springboot.bot.enums.CommandLineArgs;
import org.freakz.hokan_ng_springboot.bot.util.CommandLineArgsParser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.jms.ConnectionFactory;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

@Configuration
@ComponentScan({"org.freakz.hokan_ng_springboot.bot", "org.freakz.hokan_ng_springboot.bot.jpa.repository.service"})
@EnableJpaRepositories({"org.freakz.hokan_ng_springboot.bot", "org.freakz.hokan_ng_springboot.bot.jpa.repository.service"})
@EnableAutoConfiguration
@EnableTransactionManagement
@EnableScheduling
@Slf4j
public class HokanNgSpringBootIo {

  private static String JMS_BROKER_URL = "tcp://localhost:61616";

  @Bean
  public ConnectionFactory connectionFactory() {
    return new ActiveMQConnectionFactory(JMS_BROKER_URL);
  }

  public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException {
    CommandLineArgsParser parser = new CommandLineArgsParser(args);
    Map<CommandLineArgs, String> parsed = parser.parseArgs();
    String url = parsed.get(CommandLineArgs.JMS_BROKER_URL);
    if (url != null) {
      JMS_BROKER_URL = url;
    }
    log.debug("JMS_BROKER_URL: {}", JMS_BROKER_URL);
    SpringApplication.run(HokanNgSpringBootIo.class, args);
  }

}
