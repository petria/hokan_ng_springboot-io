= Hokan the Java IRC bot =

 == BOT MODULES

   Bot is divided in three modules

    - HokanIO
    - HokanEngine
    - HokanServices

    Each is independent Java SpringBoot application.
    Applications communicates using JMS/ActiveMQ.
    Applications store data using JPA.

       |------- ActiveMQ ----------|
       |            |              |
       |           JMS             |
       |            |              |
    HokanIO    HokanEngine    HokanServices
       \            |              /
        \           |             /
         \----------|------------/
                    |
                   JPA
                    |
        MariaDB: hokan_ng_springboot

    All components MUST use same database instance.

    All of the modules must be run same time Bot to operate fully. Starting HokanIO module will connect
    Bot to IRC but alone it does nothing else but joins channels and accepts Admin Token command.

 == JMS/ActiveMQ

   By default all components try to use ActiveMQ from "tcp://localhost:61616". This can be override either
   with command line parameter --JmsBrokerUrl=<anotherActiveMq:XXXX> or by changing application.properties
   and building jar again.

   Download latest Apache ActiveMQ http://activemq.apache.org/download.html and extract package.

   ActiveMQ can be started by running bin/activemq.bat (windows) or bin/activemq.sh (linux) with start parameter:

   bin/activemq start

   No further configuration is needed.

 == DEFAULT PARAMETERS

   By default bot will try to use parameters defined in src/main/resources/application.properties
   to connect database.

   Either modify values in application.properties file and re-build jar to apply or when running bot
   override with command line parameters:

   java -jar target\hokan_ng_springboot-io-0.0.1-final.jar --spring.datasource.url=jdbc:mysql://DATABASE_HOST/DATABASE_NAME?autoReconnect=true

   All parameters in application.properties can be override same way ...

 == HOW TO INITIALIZE DATABASE

  1) Build with Maven

   mvn package

   this will generate .jar file to target/ directory

  2) Init MariaDB

   mysql < DatabaseInit/create_user.sql
   mysql < DatabaseInit/init_database.sql

   First one only need to run once.
   Later one can also be used to reset database again to empty.

  3) Create initial configuration to connect IRC

   NOTE: This step should only be done once after the DB has been initialized in step 2)
         If needed, reset the DB as in step 2) and then do step 3) again.

   java -jar target\hokan_ng_springboot-io-0.0.1-final.jar --ConfigInit

   This will ask Network name, IrcServerConfig and Channels to use when connecting IRC network.


   NOTE: when run with --ConfigInit also AdminUserToken will be generated:

    ***************************************************
    *           !!!!!!  IMPORTANT !!!!!!              *
    *                                                 *
      ADMIN USER TOKEN IS: <XXXX>
      DO: /msg HokanBot @AdminUserToken <XXXX>
      TO GET ADMIN RIGHTS
    *                                                 *
    *           !!!!!!  IMPORTANT !!!!!!              *
    ***************************************************

    By sending bot message: @AdminUserToken <XXXX> the sender of message will be granted Admin rights.
    Token can only be used once.

  4) Starting HokanIO module

    java -jar target\hokan_ng_springboot-io-0.0.1-final.jar

    Now bot should try to connect IRC server defined in step 3) and then join channels.

  5) Starting other modules

