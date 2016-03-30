# addon-self-administration [![Circle CI](https://circleci.com/gh/osiam/addon-self-administration.svg?style=svg)](https://circleci.com/gh/osiam/addon-self-administration) [![Codacy Badge](https://api.codacy.com/project/badge/grade/ad2f264b04c846949878779e7e8101b7)](https://www.codacy.com/app/OSIAM/addon-self-administration) [![Codacy Coverage Badge](https://api.codacy.com/project/badge/coverage/ad2f264b04c846949878779e7e8101b7)](https://www.codacy.com/app/OSIAM/addon-self-administration)

A self administration for OSIAM.

Learn how to install and configure this add-on for production in the
[documentation](docs/README.md).

## Snapshots

To use the latest snapshot of the OSIAM self-administration just download it
from the oss jfrog repository:
https://oss.jfrog.org/artifactory/repo/org/osiam/addon-self-administration

## Run the integration-tests

### Configure Docker

The integration-tests use the [docker-maven-plugin](https://github.com/alexec/docker-maven-plugin),
which utilizes [docker-java](https://github.com/docker-java/docker-java).
In order to run the integration-tests, you need to ensure that your docker daemon
listens on the TCP port `2375`.

How exactly this works depends on your operating system, but

    echo 'DOCKER_OPTS="-H tcp://127.0.0.1:2375 -H unix:///var/run/docker.sock' >> /etc/default/docker

is a good starting point. For further information, please refer to  the
[docker-java README](https://github.com/docker-java/docker-java#build-with-maven)
and the official Docker documentation.

### Run

Run the integration-tests

    $ mvn clean verify -P integration-tests

### Run with Debugging

If you want to debug the running Self Administration, then just add the `debug`
profile when you run Maven:

    $ mvn clean verify -P integration-tests,debug

You can connect to the debugging agent using `localhost:8000`.

### Run in your IDE

To run the integration-tests in your IDE against the started containers

    $ mvn clean pre-integration-test -P integration-tests

If you also want to debug the running Self Administration, add the `debug`
profile when you run Maven:

    $ mvn clean pre-integration-test -P integration-tests,debug

You can connect to the debugging agent using `localhost:8000`.

If you are on mac or want to run them in a VM, just checkout the
[OSIAM vagrant VM](https://github.com/osiam/vagrant). It's pretty easy to setup.
Just run the above mentioned command in the OSIAM vagrant VM and then the
integration-tests against the VM.

### Run against remote docker host

If you like to run the tests against a remote docker host, you nedd to set the
following system properties:

Docker:
- `docker.host`
  The URL of the docker daemon. Default: `http://localhost:2375`

OSIAM:
- `osiam.host.protocol`
  The protocol of the OSIAM host. Default: `http`
- `osiam.host`
  The host where OSIAM is running. Default: `localhost`
- `osiam.port`
  The port where OSIAM is running. Default: `8480`
- `osiam.database.host`
  The host where the postgres for OSIAM is running. Default: `localhost`
- `osiam.database.port`
  The port where the postgres for OSIAM is running. Default: `45432`
- `osiam.mail.host`
  The mail host where OSIAM is connecting to. Default: `localhost`
- `osiam.mail.port`
  The mail port where OSIAM is connecting to. Default: `11110`

Here is an example when docker running in a boot2docker vm:

    $ mvn verify -P integration-tests -Ddocker.host=https://192.168.99.100:2376 -Dosiam.host=192.168.99.100
