Hokan the Java IRC bot

 == HOW TO RUN

   By default bot will try to use parameters defined in src/main/resources/application.properties
   to connect database.

   Either modify values in application.properties file and re-build jar to apply or when running bot
   override with command line parameters:

   > java -jar target\hokan_ng_springboot-io-0.0.1-final.jar --spring.datasource.url=jdbc:mysql://<DATABASE_HOST>/<DATABASE_NAME>?autoReconnect=true

   All parameters in application.properties can be overriden same way ...

  1) Build with Maven

   > mvn package

   this will generate .jar file to target/ directory

  2) Init MariaDB

   > apply DatabaseInit/create_user.sql
   > apply DatabaseInit/init_database.sql

   First one only need to run once.
   Later one can also be used to reset database again to empty.

  3) Create initial configuration to connect IRC

   > java -jar target\hokan_ng_springboot-io-0.0.1-final.jar --ConfigInit=true

   This will ask Network name, IrcServerConfig and Channels to use when connecting IRC network.

  4) Start bot

   > java -jar target\hokan_ng_springboot-io-0.0.1-final.jar

   Now bot should try to connect IRC server defined in step 3) and then join channels.


