
pipeline {

    agent {
        label 'agent1'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {

        // SonarQube
        SONAR_PROJECT_KEY = 'sonarqube-java-demo'

        // Nexus
        NEXUS_URL = 'http://172.31.10.242:8081'

        // Tomcat
        TOMCAT_PRIVATE_IP = 'REPLACE_WITH_TOMCAT_PRIVATE_IP'
        TOMCAT_USER = 'ubuntu'
        TOMCAT_WEBAPPS = '/opt/tomcat/webapps'
        TOMCAT_SERVICE = 'tomcat'

    }

    stages {

        // STAGE 1: CHECKOUT

        stage('Checkout') {

            steps {

                git branch: 'main',
                    url: 'https://github.com/praneet999/sonarqube-java-demo.git'

            }
        }


        // STAGE 2: BUILD AND TEST

        stage('Build and Test') {

            steps {

                sh '''
                    mvn clean verify
                '''

            }
        }


        // STAGE 3: SONARQUBE ANALYSIS

        stage('Sonar Analysis') {

            steps {

                withCredentials([
                    string(
                        credentialsId: 'sonar-token',
                        variable: 'SONAR_TOKEN'
                    )
                ]) {

                    withSonarQubeEnv('SonarQube') {

                        sh '''
                            mvn sonar:sonar \
                            -Dsonar.projectKey=$SONAR_PROJECT_KEY \
                            -Dsonar.token=$SONAR_TOKEN
                        '''

                    }
                }
            }
        }


        // STAGE 4: UPLOAD WAR TO NEXUS

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

                        cat > "$SETTINGS" <<'EOF'
<settings>
    <servers>
        <server>
            <id>nexus-snapshots</id>
            <username>${env.NEXUS_USERNAME}</username>
            <password>${env.NEXUS_PASSWORD}</password>
        </server>
    </servers>
</settings>
EOF

                        mvn -s "$SETTINGS" deploy -DskipTests
                    '''

                }
            }
        }


        // STAGE 5: DEPLOY TO TOMCAT

        stage('Deploy to Tomcat') {

            steps {

                sshagent(credentials: ['tomcat-ssh']) {

                    sh '''

                        set -eu

                        WAR_FILE="target/sonarqube-java-demo.war"

                        if [ ! -f "$WAR_FILE" ]; then
                            echo "WAR file not found!"
                            exit 1
                        fi

                        scp -o StrictHostKeyChecking=no \
                            "$WAR_FILE" \
                            ${TOMCAT_USER}@${TOMCAT_PRIVATE_IP}:/tmp/sonarqube-java-demo.war

                        ssh -o StrictHostKeyChecking=no \
                            ${TOMCAT_USER}@${TOMCAT_PRIVATE_IP} \
                            "sudo cp /tmp/sonarqube-java-demo.war ${TOMCAT_WEBAPPS}/ROOT.war && sudo systemctl restart ${TOMCAT_SERVICE}"

                        echo "Application deployed successfully!"

                    '''

                }
            }
        }

    }


    // POST BUILD ACTIONS

    post {

        success {
            echo 'CI/CD Pipeline completed successfully!'
        }

        failure {
            echo 'CI/CD Pipeline failed. Check the stage logs.'
        }

        always {
            echo 'Pipeline execution finished.'
        }

    }

}
