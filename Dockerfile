FROM amazoncorretto:19-alpine-jdk
VOLUME /tmp
ARG JAR_FILE
ARG AGENT_ARG
ENV AGENT_ENV=${AGENT_ARG}
ARG NEWSGEARS_DEVELOPMENT
ARG NEWSGEARS_SINGLEUSERMODE
ARG NEWSGEARS_APPURL
ARG NEWSGEARS_ORIGINURL
ARG NEWSGEARS_BROKERURL
ARG NEWSGEARS_BROKERCLAIM
ARG SPRING_DATASOURCE_URL
ARG SPRING_DATASOURCE_USERNAME
ARG SPRING_DATASOURCE_PASSWORD
ARG SPRING_SQL_INIT_MODE
ARG SPRING_REDIS_HOST
ARG SPRING_REDIS_PASSWORD
ARG SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENTID
ARG SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENTSECRET
ARG SPRING_MAIL_HOST
ARG SPRING_MAIL_USERNAME
ARG SPRING_MAIL_PASSWORD
ARG TOKEN_SERVICE_SECRET
ARG NEWS_API_KEY
ARG NEWS_API_DISABLED
ARG NEWS_API_DEBUG_SOURCES
ARG RCMD_SERVICE_URL
ARG RCMD_SERVICE_API_KEY
COPY ${JAR_FILE} app.jar
ENV NEWSGEARS_DEVELOPMENT ${NEWSGEARS_DEVELOPMENT}
ENV NEWSGEARS_SINGLEUSERMODE ${NEWSGEARS_SINGLEUSERMODE}
ENV NEWSGEARS_APPURL ${NEWSGEARS_APPURL}
ENV NEWSGEARS_ORIGINURL ${NEWSGEARS_ORIGINURL}
ENV NEWSGEARS_BROKERURL ${NEWSGEARS_BROKERURL}
ENV NEWSGEARS_BROKERCLAIM ${NEWSGEARS_BROKERCLAIM}
ENV SPRING_DATASOURCE_URL ${SPRING_DATASOURCE_URL}
ENV SPRING_DATASOURCE_USERNAME ${SPRING_DATASOURCE_USERNAME}
ENV SPRING_DATASOURCE_PASSWORD ${SPRING_DATASOURCE_PASSWORD}
ENV SPRING_SQL_INIT_MODE ${SPRING_SQL_INIT_MODE}
ENV SPRING_REDIS_HOST ${SPRING_REDIS_HOST}
ENV SPRING_REDIS_PASSWORD ${SPRING_REDIS_PASSWORD}
ENV SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENTID ${SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENTID}
ENV SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENTSECRET ${SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENTSECRET}
ENV SPRING_MAIL_HOST ${SPRING_MAIL_HOST}
ENV SPRING_MAIL_USERNAME ${SPRING_MAIL_USERNAME}
ENV SPRING_MAIL_PASSWORD ${SPRING_MAIL_PASSWORD}
ENV TOKEN_SERVICE_SECRET ${TOKEN_SERVICE_SECRET}
ENV NEWS_API_KEY ${NEWS_API_KEY}
ENV NEWS_API_DISABLED ${NEWS_API_DISABLED}
ENV NEWS_API_DEBUG_SOURCES ${NEWS_API_DEBUG_SOURCES}
ENTRYPOINT java ${AGENT_ENV} -Djava.security.egd=file:/dev/./urandom -jar /app.jar
