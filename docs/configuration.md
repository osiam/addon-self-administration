- [Database setup](#database-setup)
- [Configuration File](#configuration-file)
- [Connection to OSIAM](#connection-to-osiam)
    - [OSIAM 3.x](#osiam-3x)
        - [org.osiam.home](#orgosiamhome)
    - [OSIAM 2.x](#osiam-2x)
        - [org.osiam.auth-server.home](#orgosiamauth-serverhome)
        - [org.osiam.resource-server.home](#orgosiamresource-serverhome)
- [E-Mail](#e-mail)
    - [org.osiam.mail.from](#orgosiammailfrom)
    - [org.osiam.mail.*.linkprefix](#orgosiammaillinkprefix)
    - [org.osiam.html.*.url](#orgosiamhtmlurl)
    - [org.osiam.mail.server.*](#orgosiammailserver)
- [Client](#client)
    - [org.osiam.addon-self-administration.client.id](#orgosiamaddon-self-administrationclientid)
    - [org.osiam.addon-self-administration.client.secret](#orgosiamaddon-self-administrationclientsecret)
- [Tokens](#tokens)
      - [org.osiam.addon-self-administration.registration.activation-token-timeout](#orgosiamaddon-self-administrationregistrationactivation-token-timeout)
      - [org.osiam.addon-self-administration.change-email.confirmation-token-timeout](#orgosiamaddon-self-administrationchange-emailconfirmation-token-timeout)
      - [org.osiam.addon-self-administration.lost-password.one-time-password-timeout](#orgosiamaddon-self-administrationlost-passwordone-time-password-timeout)
      - [org.osiam.addon-self-administration.one-time-token-scavenger.enabled](#orgosiamaddon-self-administrationone-time-token-scavengerenabled)

# Database setup

For the self-administration you need to add some extension fields into the database otherwise it will not work.
The extension is configured with it's own namespace and will not conflict user defined extensions (extension.sql).
You need also to add a specific client for self-administration in OSIAM's database (client.sql).

**Note for users of OSIAM 2.x:** the `extension.sql` must be run against the resource-server's database and the
`client.sql` must be run against the auth-server's database.

Start the database commandline:

    $ sudo -u postgres psql

Now insert it as user osiam while being in the directory where you unpacked the sources by calling

    $ psql -f ./sql/extension.sql -U osiam

and

    $ psql -f ./sql/client.sql -U osiam

but update the client.sql before you import it and sync the data with the addon-self-administration.properties!

# Configuration File

This add-on needs some configuration values. Create the file

    /etc/osiam/addon-self-administration.properties

with content based on this [example](../src/main/deploy/addon-self-administration.properties).

# Connection to OSIAM

The self administration needs to know where it can find OSIAM. This configuration depends on whether
you use OSIAM 3.x or OSIAM 2.x.

## OSIAM 3.x

OSIAM 3.x now comes as a single server, so you have to configure only one endpoint.

### org.osiam.home

The home location of OSIAM

Default: none

## OSIAM 2.x

OSIAM 2.x is split into auth-server and resource-server. Hence you have to configure both endpoints.

### org.osiam.auth-server.home

The home location of the auth server

Default: `http://localhost:8080/osiam-auth-server`

### org.osiam.resource-server.home

The home location of the resource server

Default: `http://localhost:8080/osiam-resource-server`

### org.osiam.connector.legacy-schemas

Enable the use of legacy schemas, i.e. schemas that were defined before SCIM 2 draft 09

Default: `false`

# E-Mail

The self administration sends E-Mails for certain operations, like registration. Thus a valid SMTP
server must be configured.

## org.osiam.mail.from

The sender address from where the emails will be send to the user.

## org.osiam.mail.*.linkprefix
(*changeemail, *lostpassword)

The controller action URL on the client side where the link will point to.
This must be on client side and should not point directly to the osiam registration module due to security issues.
The URL must end with either a '?' or a '&' character. This depends on whether you already have some parameters or not.
If you don't have any url parameters add the '?' character otherwise the '&' character.

The registration link will be generated and set by the add-on webapp and link to itself.

Here some examples:
 * http://localhost:1234/client/action?
 * http://localhost:1234/client/action?someParameter=value&

## org.osiam.html.*.url
(*changeemail, *lostpassword)

The controller action URL on the client side where the call arrives, submitted by the HTML from.
This must be a URL on client side and should not point directly to the osiam registration module due to security issues.

## org.osiam.mail.server.*
* *.smtp.port=25 (default)
* *.host.name=localhost (default)
* *.username=username (example)
* *.password=password (example)
* *.smtp.starttls.enable=false (default)
* *.smtp.auth=false (default)
* *.transport.protocol=smtp (default)

# Client

The self administration performs background tasks, like cleaning up expired one-time tokens, but
also foreground operations, like registering a new user. Therefore it needs an own OAuth 2 client
to authenticate against OSIAM.

## org.osiam.addon-self-administration.client.id

The id of the self administration client that also has to be imported in the
database.

Default: addon-self-administration-client

## org.osiam.addon-self-administration.client.secret

The secret of the self administration client, needs to be set!

# Tokens

The self administration generates tokens for registration, changing a user's email address and
resetting a user's password. These tokens have a limited lifespan, that can be configured separately
for each operation. You can also disable the cleanup of tokens completely.

## org.osiam.addon-self-administration.registration.activation-token-timeout

Specify how long the activation token is valid before the user has to request
a new one. Also using this property to configure the delay to perform the
cleanup task of the expired activation tokens. For example, when setting this
property to `24h` the scavenger task runs on startup and every 24 hours.

The timeout can be configured with the following time units:

- d: days
- h: hours
- m: minutes
- s: seconds

Example: 2d 12h 30m 5s

Default: 24h

## org.osiam.addon-self-administration.change-email.confirmation-token-timeout

Specify how long the confirmation token is valid before the user has to request
a new one. Also using this property to configure the delay to perform the
cleanup task of the expired confirmation tokens. For example, when setting this
property to `24h` the scavenger task runs on startup and every 24 hours.

The timeout can be configured with the following time units:

- d: days
- h: hours
- m: minutes
- s: seconds

Example: 2d 12h 30m 5s

Default: 24h

## org.osiam.addon-self-administration.lost-password.one-time-password-timeout

Specify how long the one time password is valid before the user has to request
a new one. Also using this property to configure the delay to perform the
cleanup of the expired one time passwords. For example, when setting this
property to `24h` the scavenger task runs on startup and every 24 hours.

The timeout can be configured with the following time units:

- d: days
- h: hours
- m: minutes
- s: seconds

Example: 2d 12h 30m 5s

Default: 24h

## org.osiam.addon-self-administration.one-time-token-scavenger.enabled

Enable the scavenging of expired one time tokens. Affects all three kinds of
tokens, i.e. change email, activation after registration, and lost password.

Default: true
