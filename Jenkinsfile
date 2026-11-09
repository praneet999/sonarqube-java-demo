
pipeline {

    agent {
        label 'agent1'
    }

    options {
        timestamps()
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
    }

    environment {

        // Application configuration
        APP_NAME = 'sonarqube-java-demo'
        GROUP_ID = 'com.example'
        APP_VERSION = '1.0-SNAPSHOT'
        WAR_FILE = 'target/sonarqube-java-demo.war'

        // GitHub
        GIT_URL = 'https://github.com/praneet999/sonarqube-java-demo.git'
        GIT_BRANCH = 'main'

        // SonarQube
        SONAR_PROJECT_KEY = 'sonarqube-java-demo'

        // Nexus
        NEXUS_URL = 'http://172.31.10.242:8081'
        NEXUS_REPOSITORY = 'maven-snapshots'
        NEXUS_SERVER_ID = 'nexus-snapshots'

        // Tomcat
        TOMCAT_HOST = '172.31.9.142'
        TOMCAT_USER = 'ubuntu'
        TOMCAT_WEBAPPS = '/var/lib/tomcat10/webapps'
        TOMCAT_SERVICE = 'tomcat10'
    }

    stages {

        // STAGE 1: CHECKOUT

        stage('Checkout') {
            steps {
                echo 'Checking out source code from GitHub'

                git branch: "${GIT_BRANCH}",
                    url: "${GIT_URL}"
            }
        }


        // STAGE 2: BUILD AND TEST

        stage('Build and Test') {
            steps {
                echo 'Building Java application using Maven'

                sh '''
                    set -e

                    java -version
                    mvn -version

                    mvn clean verify
                '''

                archiveArtifacts artifacts: 'target/*.war',
                    fingerprint: true
            }
        }


        // STAGE 3: SONARQUBE ANALYSIS

        stage('SonarQube Analysis') {
            steps {

                withSonarQubeEnv('SonarQube') {

                    withCredentials([
                        string(
                            credentialsId: 'sonar-token',
                            variable: 'SONAR_TOKEN'
                        )
                    ]) {

                        sh '''
                            set -e
                            set +x

                            mvn \
                            org.sonarsource.scanner.maven:sonar-maven-plugin:5.2.0.4988:sonar \
                            -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                            -Dsonar.token=${SONAR_TOKEN} \
                            -Dsonar.java.binaries=target/classes \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }


        // STAGE 4: QUALITY GATE

        stage('Quality Gate') {
            steps {

                timeout(time: 5, unit: 'MINUTES') {

                    waitForQualityGate abortPipeline: true

                }
            }
        }


        // STAGE 5: UPLOAD ARTIFACT TO NEXUS

        stage('Upload WAR to Nexus') {
            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-creds',
                        usernameVariable: 'NEXUS_USERNAME',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {

                    sh '''
                        set -eu
                        set +x
                        umask 077

                        SETTINGS=$(mktemp)

                        trap 'rm -f "$SETTINGS"' EXIT

                        cat > "$SETTINGS" <<EOF
<settings>
    <servers>
        <server>
            <id>nexus-snapshots</id>
            <username>${NEXUS_USERNAME}</username>
            <password>${NEXUS_PASSWORD}</password>
        </server>
    </servers>
</settings>
EOF

                        mvn -s "$SETTINGS" deploy -DskipTests
                    '''
                }
            }
        }


        // STAGE 6: DOWNLOAD ARTIFACT FROM NEXUS

        stage('Download WAR from Nexus') {
            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-creds',
                        usernameVariable: 'NEXUS_USERNAME',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {

                    sh '''
                        set -eu
                        set +x
                        umask 077

                        SETTINGS=$(mktemp)

                        trap 'rm -f "$SETTINGS"' EXIT

                        cat > "$SETTINGS" <<EOF
<settings>
    <servers>
        <server>
            <id>nexus-snapshots</id>
            <username>${NEXUS_USERNAME}</username>
            <password>${NEXUS_PASSWORD}</password>
        </server>
        <server>
            <id>nexus-download</id>
            <username>${NEXUS_USERNAME}</username>
            <password>${NEXUS_PASSWORD}</password>
        </server>
    </servers>
</settings>
EOF

                        mvn -s "$SETTINGS" \
                        org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get \
                        -Dartifact=${GROUP_ID}:${APP_NAME}:${APP_VERSION}:war \
                        -Dtransitive=false \
                        -DremoteRepositories=nexus-download::default::${NEXUS_URL}/repository/${NEXUS_REPOSITORY}/

                        mkdir -p downloaded-artifact

                        cp "$HOME/.m2/repository/com/example/sonarqube-java-demo/1.0-SNAPSHOT/sonarqube-java-demo-1.0-SNAPSHOT.war" \
                        downloaded-artifact/sonarqube-java-demo.war

                        echo "Downloaded artifact from Nexus:"
                        ls -lh downloaded-artifact/
                    '''
                }

                archiveArtifacts artifacts: 'downloaded-artifact/*.war',
                    fingerprint: true
            }
        }


        // STAGE 7: DEPLOY TO TOMCAT

        stage('Deploy to Tomcat') {
            steps {

                sshagent(credentials: ['tomcat-ssh']) {

                    sh '''
                        set -eu

                        echo "Deploying application to Tomcat"

                        test -f downloaded-artifact/sonarqube-java-demo.war

                        scp -o StrictHostKeyChecking=no \
                        downloaded-artifact/sonarqube-java-demo.war \
                        ${TOMCAT_USER}@${TOMCAT_HOST}:/tmp/sonarqube-java-demo.war

                        ssh -o StrictHostKeyChecking=no \
                        ${TOMCAT_USER}@${TOMCAT_HOST} \
                        "sudo mv /tmp/sonarqube-java-demo.war ${TOMCAT_WEBAPPS}/sonarqube-java-demo.war && sudo systemctl restart ${TOMCAT_SERVICE}"

                        echo "Application deployed successfully"
                    '''
                }
            }
        }
    }


    // POST BUILD ACTIONS

    post {

        success {
            echo 'CI/CD Pipeline completed successfully!'
            echo 'Application deployed to Tomcat.'
        }

        failure {
            echo 'CI/CD Pipeline failed. Check the failed stage logs.'
        }

        always {
            echo 'Pipeline execution finished.'
        }
    }
}
