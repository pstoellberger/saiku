#!/bin/bash
git submodule init
git submodule update
cd saiku-query
git checkout master
git pull origin master
cd ..
cd saiku-ui
git pull
git checkout dev 
git pull origin dev
cd ..
echo "Building Everything"
mvn clean package
#cd ../saiku-bi-platform-plugin-p5
#mvn clean package
