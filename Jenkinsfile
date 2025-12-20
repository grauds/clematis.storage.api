pipeline {
    agent any
    environment {
        REMOTE_HOST = "192.168.1.118"
        REMOTE_USER = "anton"
        SSH_DEST = "${REMOTE_USER}@${REMOTE_HOST}"
        REMOTE_APP_DIR = "/home/anton/deploy/storage/api"
    }

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
                sh './gradlew clean build --no-daemon'
            }

        }

        stage('Dependency-Check') {
            steps {
                timeout(time: 1, unit: 'MINUTES') {
                    dependencyCheck additionalArguments: '''
                        -o "./"
                        -s "./"
                        -f "ALL"
                        --prettyPrint''', nvdCredentialsId: 'NVD_API_Key', odcInstallation: 'Dependency Checker'
                    dependencyCheckPublisher pattern: 'dependency-check-report.xml'
                    catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                       sh "exit 1"
                    }
                }
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

       stage('Export Docker Images') {
          steps {
            sh '''
              mkdir -p docker_export
              docker save clematis.storage.api > docker_export/clematis.storage.api.tar
            '''
          }
        }

        stage('Transfer Files to Yoda') {
          steps {
            sshagent (credentials: ['yoda-anton-key']) {
              sh '''
                [ -d ~/.ssh ] || mkdir ~/.ssh && chmod 0700 ~/.ssh
                scp -o StrictHostKeyChecking=no docker_export/*.tar "${SSH_DEST}:${REMOTE_APP_DIR}/"
                scp -o StrictHostKeyChecking=no "jenkins/docker-compose.yml" "${SSH_DEST}:${REMOTE_APP_DIR}/"
                scp -o StrictHostKeyChecking=no "jenkins/.env" "${SSH_DEST}:${REMOTE_APP_DIR}/"
               '''
            }
          }
        }

        stage('Deploy on Yoda') {
          environment {
            SPRING_DATASOURCE_PASSWORD = credentials('SPRING_DATASOURCE_PASSWORD')
            STORAGE_FILES_PATH = credentials('STORAGE_FILES_PATH')
          }
          steps {
            sshagent (credentials: ['yoda-anton-key']) {
                sh """
                  ssh ${SSH_DEST} '
                    cd ${REMOTE_APP_DIR} && \
                    docker rm -f rm -f clematis-storage-api clematis-storage-mysql-db 2>/dev/null || true && \
                    export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD}" && \
                    export STORAGE_FILES_PATH="${STORAGE_FILES_PATH}" && \
                    docker load < clematis.storage.api.tar && \
                    docker compose -f docker-compose.yml build --build-arg STORAGE_FILES_PATH="${STORAGE_FILES_PATH}" --build-arg SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD}" && \
                    docker compose -f docker-compose.yml up -d clematis-storage-db && \
                    docker compose -f docker-compose.yml up -d --no-deps --build clematis-storage-api
                  '
                """
            }
          }
        }
    }

    post {
        always {
            junit '**/build/**/test-results/test/*.xml'
            sh '''
               rm -rf docker_export
            '''
        }
    }
}
