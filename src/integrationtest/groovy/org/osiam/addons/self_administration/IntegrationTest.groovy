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

package org.osiam.addons.self_administration

import com.github.springtestdbunit.DbUnitTestExecutionListener
import com.icegreen.greenmail.util.ServerSetup
import com.sun.mail.pop3.POP3Store
import org.osiam.client.OsiamConnector
import org.osiam.client.oauth.AccessToken
import org.osiam.client.oauth.Scope
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import spock.lang.Specification

import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session

/**
 * Base class for integration tests.
 */
@ContextConfiguration('classpath:/test-context.xml')
@TestExecutionListeners([DependencyInjectionTestExecutionListener, DbUnitTestExecutionListener])
abstract class IntegrationTest extends Specification {

    private static final String CLIENT_ID = 'example-client'
    private static final String CLIENT_SECRET = 'secret'
    protected static final String OSIAM_ENDPOINT
    protected static final String REGISTRATION_ENDPOINT
    protected static final String SELF_ADMIN_URN = 'urn:org.osiam:scim:extensions:addon-self-administration'
    protected static final OsiamConnector OSIAM_CONNECTOR

    private static String OSIAM_MAIL_HOST
    private static int OSIAM_MAIL_PORT

    private POP3Store store
    private Folder inbox

    protected AccessToken accessToken

    static {
        OsiamConnector.setConnectTimeout(Integer.parseInt(System.getProperty("connector.timeout", "-1")));
        OsiamConnector.setReadTimeout(Integer.parseInt(System.getProperty("connector.timeout", "-1")));

        OSIAM_MAIL_HOST = System.getProperty('osiam.mail.host', 'localhost')
        OSIAM_MAIL_PORT = System.getProperty('osiam.mail.port', '11110').toInteger()

        final String osiamHost = System.getProperty('osiam.test.host', 'http://localhost:8480')
        OSIAM_ENDPOINT = "${osiamHost}/osiam"
        REGISTRATION_ENDPOINT = "${osiamHost}/addon-self-administration"

        OSIAM_CONNECTOR = new OsiamConnector.Builder()
                .withEndpoint(OSIAM_ENDPOINT)
                .setClientId(CLIENT_ID)
                .setClientSecret(CLIENT_SECRET)
                .build()
    }

    def setupToken() {
        accessToken = OSIAM_CONNECTOR.retrieveAccessToken('marissa', 'koala', Scope.ADMIN)
    }

    def createAccessToken(String userName, String password) {
        OsiamConnector osiamConnector = new OsiamConnector.Builder()
                .withEndpoint(OSIAM_ENDPOINT)
                .setClientId(CLIENT_ID)
                .setClientSecret(CLIENT_SECRET)
                .build()
        osiamConnector.retrieveAccessToken(userName, password, Scope.ADMIN)
    }

    def fetchEmail(String userNameAndPassword) {
        final Properties properties = System.getProperties()
        final Session session = Session.getDefaultInstance(properties)
        store = (POP3Store) session.getStore(ServerSetup.PROTOCOL_POP3)
        store.connect(OSIAM_MAIL_HOST, OSIAM_MAIL_PORT, userNameAndPassword, userNameAndPassword)
        inbox = store.getFolder('INBOX')
        inbox.open(Folder.READ_WRITE)
        final Message[] messages = inbox.getMessages()

        // mark all emails as delete
        for (int i = 1; i <= inbox.getMessageCount(); i++) {
            inbox.getMessage(i).setFlag(Flags.Flag.DELETED, true)
        }

        messages
    }

    def cleanup() {
        if (inbox != null && store != null) {
            inbox.close(true)
            store.close()
        }
    }
}
