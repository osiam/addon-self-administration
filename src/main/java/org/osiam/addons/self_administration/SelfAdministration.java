package org.osiam.addons.self_administration;

import java.util.Properties;

import javax.servlet.Filter;

import org.osiam.addons.self_administration.one_time_token.ScavengerTask;
import org.osiam.addons.self_administration.service.ConnectorBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.google.common.base.Strings;

@SpringBootApplication
@EnableWebMvc
@PropertySource("classpath:addon-self-administration.properties")
public class SelfAdministration extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(SelfAdministration.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SelfAdministration.class);
    }

    @Autowired
    public void createOneTokenScavengers(final ConnectorBuilder connectorBuilder, final Config config) {
        if (!config.isOneTimeTokenScavengerEnabled()) {
            return;
        }

        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();

        new ScavengerTask(taskScheduler, connectorBuilder.createConnector(), config.getActivationTokenTimeout(),
                Config.EXTENSION_URN, Config.ACTIVATION_TOKEN_FIELD)
                .start();

        new ScavengerTask(taskScheduler, connectorBuilder.createConnector(), config.getConfirmationTokenTimeout(),
                Config.EXTENSION_URN, Config.CONFIRMATION_TOKEN_FIELD, Config.TEMP_EMAIL_FIELD)
                .start();

        new ScavengerTask(taskScheduler, connectorBuilder.createConnector(), config.getOneTimePasswordTimeout(),
                Config.EXTENSION_URN, Config.ONETIME_PASSWORD_FIELD)
                .start();
    }

    @Bean
    @Autowired
    public JavaMailSender mailSender(Config config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getMailServerHost());
        sender.setPort(config.getMailServerPort());
        sender.setDefaultEncoding("utf8");

        if (!Strings.isNullOrEmpty(config.getMailServerUserName())) {
            sender.setUsername(config.getMailServerUserName());
        }

        if (!Strings.isNullOrEmpty(config.getMailServerPassword())) {
            sender.setPassword(config.getMailServerPassword());
        }

        Properties properties = new Properties();
        properties.put("mail.transport.protocol", config.getMailServerProtocol());
        properties.put("mail.smtp.auth", config.isMailServerAuthenticationEnabled());
        properties.put("mail.smtp.starttls.enable", config.isMailServerStartTlsEnabled());
        sender.setJavaMailProperties(properties);

        return sender;
    }

    @Bean
    public Filter characterEncodingFilter() {
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding("UTF-8");
        characterEncodingFilter.setForceEncoding(true);
        return characterEncodingFilter;
    }
}
