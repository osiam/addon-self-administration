FROM tomcat:8-jre8

COPY addon-self-administration-${project.version} $CATALINA_HOME/webapps/addon-self-administration

COPY addon-self-administration-plugin-example.jar /install/plugin-example.jar
COPY selfadminexample.properties /install/

COPY osiam.yaml /var/lib/osiam/config/osiam.yaml
COPY osiam.xml /usr/local/tomcat/conf/Catalina/localhost/osiam.xml
COPY osiam.war $CATALINA_HOME/webapps/osiam.war

COPY manager $CATALINA_HOME/webapps/manager
COPY greenmail-webapp-1.4.1 $CATALINA_HOME/webapps/greenmail-webapp

RUN cat /install/selfadminexample.properties >> $CATALINA_HOME/conf/catalina.properties

RUN sed -i 's/8080/8480/g' $CATALINA_HOME/conf/server.xml

RUN cp -p /usr/share/zoneinfo/${user.timezone} /etc/localtime
RUN echo "${user.timezone}" > /etc/timezone

EXPOSE 8480 11110

CMD [ "catalina.sh", "run"]
