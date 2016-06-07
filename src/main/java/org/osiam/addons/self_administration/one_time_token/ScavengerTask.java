package org.osiam.addons.self_administration.one_time_token;

import com.google.common.base.Strings;
import org.osiam.client.OsiamConnector;
import org.osiam.client.exception.ConnectionInitializationException;
import org.osiam.client.exception.OsiamClientException;
import org.osiam.client.oauth.AccessToken;
import org.osiam.client.oauth.Scope;
import org.osiam.client.query.Query;
import org.osiam.client.query.QueryBuilder;
import org.osiam.resources.scim.Extension;
import org.osiam.resources.scim.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ScavengerTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ScavengerTask.class);

    private TaskScheduler taskScheduler;
    private final OsiamConnector osiamConnector;
    private final long timeout;
    private final String urn;
    private final String tokenField;
    private final String[] fieldsToDelete;

    public ScavengerTask(final TaskScheduler taskScheduler, final OsiamConnector osiamConnector, final long timeout,
            final String urn, final String tokenField, final String... fieldsToDelete) {

        this.taskScheduler = taskScheduler;
        this.osiamConnector = osiamConnector;
        this.timeout = timeout;
        this.urn = urn;
        this.tokenField = tokenField;
        this.fieldsToDelete = fieldsToDelete.clone();
    }

    @Override
    public void run() {
        final Query qb = new QueryBuilder()
                .filter(urn + "." + tokenField + " pr")
                .count(Integer.MAX_VALUE)
                .build();

        AccessToken accessToken;
        try {
            accessToken = osiamConnector.retrieveAccessToken(Scope.ADMIN);
        } catch (ConnectionInitializationException e) {
            LOG.warn(e.getMessage());
            // let it fail and try again next time
            return;
        }
        final List<User> users = osiamConnector.searchUsers(qb, accessToken).getResources();

        for (User user : users) {
            Extension extension = user.getExtension(urn);
            final OneTimeToken storedConfirmationToken = OneTimeToken.fromString(
                    extension.getFieldAsString(tokenField));

            if (storedConfirmationToken.isExpired(timeout)) {
                final User.Builder userBuilder = new User.Builder(user);
                userBuilder.removeExtension(urn);
                Extension.Builder extensionBuilder = new Extension.Builder(extension);
                extensionBuilder.removeField(tokenField);

                for (String fieldToDelete : fieldsToDelete) {
                    if (!Strings.isNullOrEmpty(fieldToDelete)) {
                        extensionBuilder.removeField(fieldToDelete);
                    }
                }

                userBuilder.addExtension(extensionBuilder.build());

                try {
                    osiamConnector.replaceUser(user.getId(), userBuilder.build(), accessToken);
                } catch (OsiamClientException e) {
                    // let it fail and try again next time
                    continue;
                }
            }
        }
    }

    public void start() {
        final Date startTime = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
        taskScheduler.scheduleWithFixedDelay(this, startTime, timeout);
    }
}
