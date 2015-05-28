#!/bin/bash
git submodule update
cd saiku-query
git checkout master
git pull origin master
mvn install
cd ..
cd saiku-core
mvn clean install  
cd ..
cd saiku-webapp
mvn clean install
cd ..
git submodule init
git submodule update
cd saiku-ui
git pull
git checkout dev 
git pull origin dev
mvn clean package install:install-file -Dfile=target/saiku-ui-3.0-PSTOELLBERGER-SNAPSHOT.war  -DgroupId=org.saiku -DartifactId=saiku-ui -Dversion=3.0-PSTOELLBERGER-SNAPSHOT -Dpackaging=war
cd ../saiku-server
mvn clean package
#cd ../saiku-bi-platform-plugin
#mvn clean package
cd ../saiku-bi-platform-plugin-p5
mvn clean package
