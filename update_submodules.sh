#!/bin/bash
git submodule init
git submodule update
cd saiku-query
git checkout master
git pull origin master
cd ../saiku-ui
git checkout dev 
git pull origin dev
