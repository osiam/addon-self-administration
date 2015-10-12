# OSIAM addon-self-administration

## 2.0 - Unreleased

### Changes

- Remove usage of old, method-based OAuth scopes

## 1.7 - 2015-10-09

### Features

- Rollback User Creation on MailException

### Updates

- OSIAM connector4java 1.7
- Spring Boot 1.2.6
- Joda Time 2.8.2

## 1.6 - 2015-06-18

### Changes

- Bump connector to make use of more concurrent HTTP connections
- Bump plugin API to 1.4
- Bump Jackson version

## 1.5 - 2015-06-02

### Changes

- Add sensible default values for most configuration properties

    See docs for details

- Remove possibility to configure the requested scopes

    The configuration parameter wasn't used anyway

- Remove possibility to configure the extension's attributes
- Remove field `expiry` from OAuth client in SQL scripts

    The field `expiry` has been removed from the auth-server
    (osiam/auth-server#9) and must be removed from the SQL scripts too.

- Change some attributes of OAuth client

    - Remove unnecessary grants: authorisation code grant, refresh token grant,
      resource owner credentials grant
    - Decrease access token validity to 300 seconds

- Rename SQL scripts for installing client and extension

    Use `client.sql` to create the OAuth client in the `auth-server`'s database
    and `extension.sql` to create the SCIM extension in the `resource-server`'s
    database. The old files are still in place for compatibility reasons, but
    will receive no further updates and be eventually removed in a future
    version. All users are encouraged to update to the new files.

- Bump dependencies

### Fixes

- If no address field is set, the user should have no address

### Other

- Introduce Spring Boot
- Switch from xml to java configuration

## 1.4 - 2015-05-11

### Features

This is a full feature set to handle the expiration of all tokens. For more
information have a look at the [configuration]
(docs/configuration.md#orgosiamaddon-self-administrationregistrationactivation-token-timeout).

- expiration of one time passwords
- expiration of confirmation tokens
- expiration of activation tokens
- scavenge expired tokens

### Changes

- use latest plugin api release: Version 1.3.2
- switch to latest connector release: Version 1.4
- bump dependencies and cleanup pom
- move documentation from wiki to repo

### Fixes

- handle missing extension field gracefully
- client database id may lead to problems with other clients

## 1.3.2 - 2014-11-24
- release because of fixes in addon-administration

## 1.3.1 - 2014-10-27
- [fix] using the already in scim-schema defined email validation
- https://github.com/osiam/addon-self-administration/issues/61

- [fix] fixed passwords-not-equal error-message response
- https://github.com/osiam/addon-self-administration/issues/74

- [fix] return forbidden, if you try to change the password more than one time
- https://github.com/osiam/addon-self-administration/issues/63

- [refactoring] revert the escaping for register a new user

## 1.3 - 2014-10-17
- [fix] translation property files parsed as UTF-8
  Before this fix, the translation messages were parsed as ISO-8859-1
- [cleanup] cleanup logging handling in all controllers
  Before this cleanup JUL was used to log messages, now the slf4j logger will be used.
  The main concept is the following: When an error occurred which happen because of a
  bad request (e.g. missing authorization token) the message will be logged as warn,
  when it occurred because of an internal error (e.g. the mail server is not available
  or the requested template file is missing) the message will be logged as error.
- [fix] fixed https://github.com/osiam/addon-self-administration/issues/63
  When the password reset was already done by the user a FORBIDDEN with an error
  description will be returned. Before an exception occurred and it wasn't caught,
  so an internal error/500 was returned with a servlet container specific HTML error
  page.
- [fix] fixed validation when register a user
  Before this fix, the validation wasn't working for the JSR-330 Bean Validation
  annotations, but only for the custom user validator. So an exception occurred when
  a malformed e-mail-address was send, which response with an internal error and a
  servlet container specific HTML error page. This fix switch to only JSR-330
  Bean Validation annotations and returns with an error message prepared HTML form.

## 1.2 - 2014-09-30
- [feature] the users password can also changed by clients
  Before this feature, the password can only changed with the users access token.
  Now it's also possible to use the addon-self-administrations as an oauth client
  to change the password of a specified user.
- [fix] the plugin api don't need to be configured
  Before this fix, the system fails on startup, because the properties for the
  plugin api need to configured also if no plugin is provided

## 1.1 - 2014-09-22
- introduced the plugin-api: https://github.com/osiam/addon-self-administration-plugin-api
- introduced AccountManagement: Added account deletion and deactivation action

- added sql example script to import a client for the self-administration
- cleanup properties

- fixed BT-52: https://jira.osiam.org/browse/BT-52
- fixed BT-53: https://jira.osiam.org/browse/BT-53

## 1.0 - 2014-05-13
- finally released version 1.0 of the addon-self-administration together with the OSIAM server!
