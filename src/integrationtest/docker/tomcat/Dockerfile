FROM tomcat:8-jre8

COPY addon-self-administration-${project.version} $CATALINA_HOME/webapps/addon-self-administration
COPY deploy/addon-self-administration $CATALINA_HOME/webapps/addon-self-administration/WEB-INF/classes/addon-self-administration
COPY test_assets/registration.html $CATALINA_HOME/webapps/addon-self-administration/WEB-INF/classes/addon-self-administration/templates/web/
COPY addon-self-administration.properties $CATALINA_HOME/webapps/addon-self-administration/WEB-INF/classes/

COPY addon-self-administration-plugin-example-tests.jar /install/plugin-example.jar
COPY selfadminexample.properties /install/

COPY osiam.yaml /var/lib/osiam/config/osiam.yaml
COPY osiam.xml /usr/local/tomcat/conf/Catalina/localhost/osiam.xml
COPY osiam.war $CATALINA_HOME/webapps/osiam.war

COPY manager $CATALINA_HOME/webapps/manager
COPY greenmail-webapp-1.4.1 $CATALINA_HOME/webapps/greenmail-webapp

RUN cat /install/selfadminexample.properties >> $CATALINA_HOME/conf/catalina.properties

RUN sed -i 's/8080/8480/g' $CATALINA_HOME/conf/server.xml
RUN echo "JPDA_ADDRESS=8000" > $CATALINA_HOME/bin/setenv.sh

RUN cp -p /usr/share/zoneinfo/${user.timezone} /etc/localtime
RUN echo "${user.timezone}" > /etc/timezone

EXPOSE 8480 11110 8000

CMD [ "catalina.sh", ${debug.pre.command.tomcat} "run"]

