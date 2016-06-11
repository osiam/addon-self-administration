## Preconditions

This add-on needs a pre-installed mail server on your system. The configuration
is up to the operator, apart from the important parts for this add-on. These are
the SMTP-port and the hostname. More information on the configuration in the
following chapters.

## The operating system

Before you install the addon-self-administration, make sure you already
installed the latest release version of [OSIAM]
(https://github.com/osiam/osiam/blob/master/docs/detailed-reference-installation.md).

We recommend to choose the latest OSIAM addon-self-administration release
version:

 * Release Repository: https://dl.bintray.com/osiam/downloads/addon-self-administration/
 * GitHub Release Tags: https://github.com/osiam/addon-self-administration/releases

The setup of the addon-self-administration is really easy with the distribution
.zip or .tar.gz, which you can find here:

    https://dl.bintray.com/osiam/downloads/addon-self-administration/<VERSION>/addon-self-administration-<VERSION>-distribution.zip or .tar.gz

Unpack the distribution and copy the war file to you application server (e.g.
Tomcat). All files and folders inside the /configuration folder have to copy to
the shared loader folder of your app server, like like described [here]
(https://github.com/osiam/osiam/blob/master/docs/detailed-reference-installation.md#starting-osiam).
The sql file in the /sql folder have be to imported before you start you server,
please check also the migration files, if you already installed the
self-administration before. It is highly recommended to update the [client.sql]
(../src/main/sql/client.sql).

If you like to check the sources, you download and unpack the sources:

    $ unzip addon-self-administration-<VERSION>-sources.jar

or you just fetch the latest version [here]
(https://github.com/osiam/addon-self-administration.git) from GitHub with

    $ git clone https://github.com/osiam/addon-self-administration.git

and import the project as Maven Project into your favorite IDE.

## Deployment into the application server

To deploy the addon-self-administration into your Tomcat the downloaded .war
files need to be renamed and moved into Tomcat's webapp directory:

    $ sudo mv addon-self-administration-<VERSION>.war /var/lib/tomcat7/webapps/addon-self-administration.war

For further information on Tomcat's configuration and how to add the /etc/osiam
folder to the classpath, [read this section]
(https://github.com/osiam/osiam/blob/master/docs/detailed-reference-installation.md#starting-osiam):
