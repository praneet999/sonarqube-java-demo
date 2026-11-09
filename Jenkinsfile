
pipeline {

    agent {
        label 'agent1'
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

                sh 'mvn clean verify'

            }
        }


        // STAGE 3: SONARQUBE ANALYSIS

        stage('Sonar Analysis') {

            steps {

                withSonarQubeEnv('SonarQube-Server') {

                    sh '''

                        mvn org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar \
                        -Dsonar.projectKey=DevOps-Java-App \
                        -Dsonar.projectName=DevOps-Java-App \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

                    '''
                }

                timeout(time: 5, unit: 'MINUTES') {

                    script {

                        def qg = waitForQualityGate()

                        if (qg.status != 'OK') {

                            error "Pipeline aborted: Quality Gate FAILED!"

                        }

                        echo "Quality Gate PASSED!"

                    }
                }
            }
        }


        // STAGE 4: BUILD WAR

        stage('Build') {

            steps {

                sh 'mvn clean package -DskipTests'

            }
        }


        // STAGE 5: UPLOAD WAR TO NEXUS

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


        // STAGE 6: DEPLOY TO TOMCAT

        stage('Deploy to Tomcat') {

            steps {

                withCredentials([

                    usernamePassword(
                        credentialsId: 'nexus-creds',
                        usernameVariable: 'NEXUS_USERNAME',
                        passwordVariable: 'NEXUS_PASSWORD'
                    ),

                    sshUserPrivateKey(
                        credentialsId: 'tomcat-ssh',
                        keyFileVariable: 'SSH_KEY',
                        usernameVariable: 'SSH_USER'
                    )

                ]) {

                    sh '''

                        set +x
                        set -eu
                        umask 077

                        SETTINGS=$(mktemp)

                        trap 'rm -f "$SETTINGS"' EXIT

                        cat > "$SETTINGS" <<'EOF'

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

                        # Remove previous local snapshot cache

                        rm -rf "$HOME/.m2/repository/com/example/sonarqube-java-demo/1.0-SNAPSHOT"

                        # Download WAR from Nexus

                        mvn -s "$SETTINGS" \
                          org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get \
                          -Dartifact=com.example:sonarqube-java-demo:1.0-SNAPSHOT:war \
                          -Dtransitive=false \
                          -DremoteRepositories=nexus-snapshots::default::http://172.31.10.242:8081/repository/maven-snapshots/

                        # Locate WAR

                        WAR="$HOME/.m2/repository/com/example/sonarqube-java-demo/1.0-SNAPSHOT/sonarqube-java-demo-1.0-SNAPSHOT.war"

                        test -f "$WAR"

                        echo "WAR downloaded successfully"

                        # Transfer WAR to Tomcat

                        scp -i "$SSH_KEY" \
                            -o StrictHostKeyChecking=accept-new \
                            "$WAR" \
                            "$SSH_USER@172.31.9.142:/tmp/sonarqube-java-demo.war"

                        # Deploy WAR on Tomcat

                        ssh -i "$SSH_KEY" \
                            -o StrictHostKeyChecking=accept-new \
                            "$SSH_USER@172.31.9.142" <<'REMOTE'

set -e

sudo systemctl stop tomcat10

sudo rm -rf /var/lib/tomcat10/webapps/sonarqube-java-demo

sudo install -o tomcat -g tomcat -m 0644 \
    /tmp/sonarqube-java-demo.war \
    /var/lib/tomcat10/webapps/sonarqube-java-demo.war

sudo rm -f /tmp/sonarqube-java-demo.war

sudo systemctl start tomcat10

sudo systemctl status tomcat10 --no-pager

REMOTE

                        echo "Deployment completed successfully"

                    '''
                }
            }
        }

    }

    post {

        success {
            echo 'CI/CD Pipeline completed successfully!'
        }

        failure {
            echo 'CI/CD Pipeline failed. Check the stage logs.'
        }

    }
}

