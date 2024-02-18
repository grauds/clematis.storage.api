pipeline {

    agent any

    stages {

        stage("Verify tooling") {
            steps {
                sh '''
              cd jenkins
              docker version
              docker info
              docker compose version
              curl --version
              jq --version
              docker compose ps
            '''
            }
        }

        stage('Get code') {
            steps {
                git branch: 'master', url: 'https://github.com/grauds/clematis.storage.api.git'
                sh 'chmod +x gradlew'
            }
        }

        stage('Gradle build') {
            steps {
                sh './gradlew clean build'
            }

        }

        stage('Dependency-Check') {
            steps {
                dependencyCheck additionalArguments: '''
                    -o "./"
                    -s "./"
                    -f "ALL"
                    --prettyPrint''', odcInstallation: 'Dependency Checker'

                dependencyCheckPublisher pattern: 'dependency-check-report.xml'
            }
        }

        stage('Publish tests') {
            steps {
                recordCoverage(tools: [[parser: 'JACOCO']],
                        id: 'jacoco', name: 'JaCoCo Coverage',
                        sourceCodeRetention: 'EVERY_BUILD',
                        qualityGates: [
                                [threshold: 60.0, metric: 'LINE', baseline: 'PROJECT', unstable: true],
                                [threshold: 60.0, metric: 'BRANCH', baseline: 'PROJECT', unstable: true]])
            }
        }

        stage('Build docker image') {
            steps {
                sh 'docker build -t clematis.storage.api .'
            }
        }

        stage("Build and start docker compose services") {
            environment {
                SPRING_DATASOURCE_MYSQL_PASSWORD = credentials('SPRING_DATASOURCE_MYSQL_PASSWORD')
                STORAGE_FILES_PATH = credentials('STORAGE_FILES_PATH')
            }
            steps {
                sh '''
                 cd jenkins
                 docker compose stop
                 docker stop clematis-storage-api || true && docker rm clematis-storage-api || true
                 docker compose build --build-arg SPRING_DATASOURCE_MYSQL_PASSWORD='$SPRING_DATASOURCE_MYSQL_PASSWORD' --build-arg STORAGE_FILES_PATH='STORAGE_FILES_PATH'
                 docker compose up -d 
              '''
            }
        }
    }

    post {
        always {
            junit '**/build/**/test-results/test/*.xml'
        }
    }
}
