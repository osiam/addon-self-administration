/*
 * Copyright (C) 2013 tarent AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.osiam.addons.self_administration;

import java.util.*;

import javax.servlet.*;

import org.osiam.addons.self_administration.one_time_token.*;
import org.osiam.client.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.context.web.*;
import org.springframework.context.annotation.*;
import org.springframework.mail.javamail.*;
import org.springframework.scheduling.concurrent.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.filter.*;
import org.springframework.web.servlet.config.annotation.*;

import com.fasterxml.jackson.core.*;
import com.google.common.base.*;

@SpringBootApplication
@EnableWebMvc
@RestController
public class SelfAdministration extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(SelfAdministration.class, args);
    }

    @RequestMapping(value = "/")
    public ServiceInfo getServiceInfo() throws JsonProcessingException {
        String version = getClass().getPackage().getImplementationVersion();
        String name = getClass().getPackage().getImplementationTitle();
        if (Strings.isNullOrEmpty(version)) {
            version = "Version not found";
        }
        if (Strings.isNullOrEmpty(name)) {
            name = "addon-self-administration";
        }
        return new ServiceInfo(name, version);
    }

    @Autowired
    public void createOneTokenScavengers(final Config config) {
        if (!config.isOneTimeTokenScavengerEnabled()) {
            return;
        }

        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();

        new ScavengerTask(taskScheduler, osiamConnector(config), config.getActivationTokenTimeout(),
                Config.EXTENSION_URN, Config.ACTIVATION_TOKEN_FIELD)
                .start();

        new ScavengerTask(taskScheduler, osiamConnector(config), config.getConfirmationTokenTimeout(),
                Config.EXTENSION_URN, Config.CONFIRMATION_TOKEN_FIELD, Config.TEMP_EMAIL_FIELD)
                .start();

        new ScavengerTask(taskScheduler, osiamConnector(config), config.getOneTimePasswordTimeout(),
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

    @Bean
    public OsiamConnector osiamConnector(Config config) {
        OsiamConnector.Builder oConBuilder = new OsiamConnector.Builder()
                .setAuthServerEndpoint(config.getAuthServerHome())
                .setResourceServerEndpoint(config.getResourceServerHome())
                .setClientId(config.getClientId())
                .setClientSecret(config.getClientSecret());
        return oConBuilder.build();
    }

    private static class ServiceInfo {

        private String name;
        private String version;

        public ServiceInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }
    }
}
