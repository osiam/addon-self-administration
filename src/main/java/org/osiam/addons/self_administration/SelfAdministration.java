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
                config.getExtensionUrn(), config.getActivationTokenField())
                .start();

        new ScavengerTask(taskScheduler, connectorBuilder.createConnector(), config.getConfirmationTokenTimeout(),
                config.getExtensionUrn(), config.getConfirmationTokenField(), config.getTempEmailField())
                .start();

        new ScavengerTask(taskScheduler, connectorBuilder.createConnector(), config.getOneTimePasswordTimeout(),
                config.getExtensionUrn(), config.getOneTimePasswordField())
                .start();
    }

}
