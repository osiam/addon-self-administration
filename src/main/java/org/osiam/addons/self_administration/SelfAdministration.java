package org.osiam.addons.self_administration;

import org.osiam.addons.self_administration.one_time_token.ScavengerTask;
import org.osiam.addons.self_administration.service.ConnectorBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SelfAdministration {

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

}
