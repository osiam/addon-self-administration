- [Database setup](#database-setup)
- [Configuring OSIAM](#configuring-osiam)
 - [Configuration values](#configuration-values)
    - [org.osiam.mail.from](#orgosiammailfrom)
    - [org.osiam.mail.*.linkprefix](#orgosiammaillinkprefix)
    - [org.osiam.html.*.url](#orgosiamhtmlurl)
    - [org.osiam.mail.server.*](#orgosiammailserver)
    - [org.osiam.addon-self-administration.client.id](#orgosiamaddon-self-administrationclientid)
    - [org.osiam.addon-self-administration.client.secret](#orgosiamaddon-self-administrationclientsecret)
    - [org.osiam.addon-self-administration.registration.activation-token-timeout](#orgosiamaddon-self-administrationregistrationactivation-token-timeout)
    - [org.osiam.addon-self-administration.change-email.confirmation-token-timeout](#orgosiamaddon-self-administrationchange-emailconfirmation-token-timeout)
    - [org.osiam.addon-self-administration.lost-password.one-time-password-timeout](#orgosiamaddon-self-administrationlost-passwordone-time-password-timeout)
    - [org.osiam.addon-self-administration.one-time-token-scavenger.enabled](#orgosiamaddon-self-administrationone-time-token-scavengerenabled)
    - [org.osiam.html.form.usernameEqualsEmail](user-registration.md#orgosiamhtmlformusernameequalsemail)
    - [org.osiam.html.form.password.length](user-registration.md#orgosiamhtmlformpasswordlength)
    - [org.osiam.html.form.fields](user-registration.md#orgosiamhtmlformfields)
    - [org.osiam.html.form.extensions](user-registration.md#orgosiamhtmlformextensions)

## Database setup

**PRECONDITION**
You need to import the sql script into your postgres database which you will find in the OSIAM project!

For the self-administration you need to add some extension fields into the database otherwise it will not work.
The extension is configured with it's own namespace and will not conflict user defined extensions (extension.sql).
You need also to add a specific client for self-administration in OSIAM's database (client.sql).

Start the database commandline:

    $ sudo -u postgres psql

Now insert it as user osiam while being in the directory where you unpacked the sources by calling

    $ psql -f ./sql/extension.sql -U osiam

and

    $ psql -f ./sql/client.sql -U osiam

but update the client.sql before you import it and sync the data with the addon-self-administration.properties!

## Configuring OSIAM

This add-on needs some configuration values. Create the file

    /etc/osiam/addon-self-administration.properties

with content based on this [example](../src/main/deploy/addon-self-administration.properties)

### Configuration values

### org.osiam.home

The home location of OSIAM.

Default: http://localhost:8080/osiam

#### org.osiam.mail.from

The sender address from where the emails will be send to the user.

#### org.osiam.mail.*.linkprefix
(*changeemail, *lostpassword)

The controller action URL on the client side where the link will point to.
This must be on client side and should not point directly to the osiam registration module due to security issues.
The URL must end with either a '?' or a '&' character. This depends on whether you already have some parameters or not.
If you don't have any url parameters add the '?' character otherwise the '&' character.

The registration link will be generated and set by the add-on webapp and link to itself.

Here some examples:
 * http://localhost:1234/client/action?
 * http://localhost:1234/client/action?someParameter=value&

#### org.osiam.html.*.url
(*changeemail, *lostpassword)

The controller action URL on the client side where the call arrives, submitted by the HTML from.
This must be a URL on client side and should not point directly to the osiam registration module due to security issues.

#### org.osiam.mail.server.*
* *.smtp.port=25 (default)
* *.host.name=localhost (default)
* *.username=username (example)
* *.password=password (example)
* *.smtp.starttls.enable=false (default)
* *.smtp.auth=false (default)
* *.transport.protocol=smtp (default)

#### org.osiam.addon-self-administration.client.id

The id of the self administration client that also has to be imported in the
database.

Default: addon-self-administration-client

#### org.osiam.addon-self-administration.client.secret

The secret of the self administration client, needs to be set!

#### org.osiam.addon-self-administration.registration.activation-token-timeout

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

#### org.osiam.addon-self-administration.change-email.confirmation-token-timeout

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

#### org.osiam.addon-self-administration.lost-password.one-time-password-timeout

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

#### org.osiam.addon-self-administration.one-time-token-scavenger.enabled

Enable the scavenging of expired one time tokens. Affects all three kinds of
tokens, i.e. change email, activation after registration, and lost password.

Default: true
