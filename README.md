# Clematis Storage API 
  
[![License](https://img.shields.io/badge/License-GPLv2%202.0-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.html)
[![CI to Docker Hub](https://github.com/grauds/clematis.storage.api/actions/workflows/CI_to_Docker_Hub.yml/badge.svg)](https://github.com/grauds/clematis.storage.api/actions/workflows/CI_to_Docker_Hub.yml)
[![Docker Image CI](https://github.com/grauds/clematis.storage.api/actions/workflows/docker-image.yml/badge.svg)](https://github.com/grauds/clematis.storage.api/actions/workflows/docker-image.yml)
[![Docker](https://github.com/grauds/clematis.storage.api/actions/workflows/docker-publish.yml/badge.svg)](https://github.com/grauds/clematis.storage.api/actions/workflows/docker-publish.yml)


## About

This is a Restful API service for anonymous file storage 

## Quick Start

Checkout the code
```
git clone https://github.com/grauds/clematis.storage.api
```                                                            

Set executable bit to gradlew
```
chmod +x gradlew
```

The following command builds the entire project
```
./gradlew clean build
```

To pack the application into a Docker container run the Docker build
```
docker build -t clematis.storage.api .
```
The application is meant to be deployed with Docker compose along with MySQL database as a dependency. The suggested Docker compose configuration can be found [here](https://github.com/grauds/clematis.storage.api/blob/master/jenkins/docker-compose.yml). There are many environment variables defined in the [.env](https://github.com/grauds/money.tracker.api/blob/master/jenkins/.env) file, but if another environment needs to be configured, just replace the file with the actual one.

