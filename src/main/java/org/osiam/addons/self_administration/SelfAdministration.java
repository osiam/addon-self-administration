package org.osiam.addons.self_administration;

import java.util.Properties;

import javax.servlet.Filter;

import org.osiam.addons.self_administration.one_time_token.ScavengerTask;
import org.osiam.addons.self_administration.service.ConnectorBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.google.common.base.Strings;

@Configuration
@EnableWebMvc
@ComponentScan
public class SelfAdministration extends WebMvcConfigurerAdapter {

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
    public static PropertySourcesPlaceholderConfigurer properties() {
        PropertySourcesPlaceholderConfigurer propertySources = new PropertySourcesPlaceholderConfigurer();
        Resource[] resources = new ClassPathResource[] { new ClassPathResource("addon-self-administration.properties") };
        propertySources.setLocations(resources);
        propertySources.setIgnoreUnresolvablePlaceholders(true);
        return propertySources;
    }

    @Bean
    public Filter characterEncodingFilter() {
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding("UTF-8");
        characterEncodingFilter.setForceEncoding(true);
        return characterEncodingFilter;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations(
                "classpath:/addon-self-administration/resources/css/");
    }

    @Override
    public Validator getValidator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource());
        return validator;
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setBasenames("addon-self-administration/i18n/registration",
                "addon-self-administration/i18n/mail");
        return messageSource;
    }
}
