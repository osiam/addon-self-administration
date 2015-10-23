Welcome to OSIAM!

Please have a look at the website of OSIAM: https://www.osiam.org
GitHub: https://github.com/osiam/addon-self-administration
Jira: https://jira.osiam.org

If you want to run this OSIAM addon, please have a look at the documentation:
https://github.com/osiam/addon-self-administration/blob/master/docs/README.md

If you find this file in the distribution of the OSIAM addon, please jump
to the installation instructions among

if not, just download the .zip or .tar.gz distribution file here:
https://maven-repo.evolvis.org/releases/org/osiam/addon-self-administration/

or you can run the following commands on your console:
$ git clone https://github.com/osiam/addon-self-administration.git
$ cd addon-self-administration
$ mvn clean install
$ cd target
$ gunzip addon-self-administration-${VERSION}-dist-distribution.tar.gz
OR
$ unzip addon-self-administration-${VERSION}-dist-distribution.zip
$ cd addon-self-administration-${VERSION}-dist-distribution
Now you could follow the instructions:

Just copy the .war file to the application server of your choice and
before you copy the configuration file (addon-self-administration.properties),
of the /configuration folder, please check the config values:
https://github.com/osiam/addon-self-administration/blob/master/docs/configuration.md#configuring-osiam

After that, copy all files of the /configuration folder to your shared classpath of the
application server of your choice, like here described:
https://github.com/osiam/osiam/blob/master/docs/detailed_reference_installation#starting-osiam

Then import the sql files to your already configured database (and referenced in the config files)
which are in the /sql folder:
https://github.com/osiam/addon-self-administration/blob/master/docs/configuration.md#database-setup

Now have fun with OSIAM.
