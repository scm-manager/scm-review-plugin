#!groovy

// Keep the version in sync with the one used in pom.xml in order to get correct syntax completion.
@Library('github.com/cloudogu/ces-build-lib@263fcfe')
import com.cloudogu.ces.cesbuildlib.*

node('docker') {

  mainBranch = "master"

  properties([
    // Keep only the last 10 build to preserve space
    buildDiscarder(logRotator(numToKeepStr: '10')),
    disableConcurrentBuilds()
  ])

  timeout(activity: true, time: 20, unit: 'MINUTES') {

    Git git = new Git(this)

    catchError {

      Maven mvn = setupMavenBuild()

      stage('Checkout') {
        deleteDir()
        checkout scm
      }

      stage('Build') {
        mvn 'clean install -DskipTests'
        archive 'target/*.smp'
      }

      stage('Unit Test') {
        mvn 'test -Dmaven.test.failure.ignore=true'
      }

      stage('SonarQube') {

        def sonarQube = new SonarCloud(this, [sonarQubeEnv: 'sonarcloud.io-scm'])

        sonarQube.analyzeWith(mvn)

        if (!sonarQube.waitForQualityGateWebhookToBeCalled()) {
          currentBuild.result ='UNSTABLE'
        }
      }
    }

    // Archive Unit and integration test results, if any
    junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/TEST-*.xml,**/target/surefire-reports/TEST-*.xml,**/target/jest-reports/TEST-*.xml'

    // Find maven warnings and visualize in job
    warnings consoleParsers: [[parserName: 'Maven']], canRunOnFailed: true

    mailIfStatusChanged(git.commitAuthorEmail)
  }
}

String mainBranch

Maven setupMavenBuild() {
  // Keep this version number in sync with .mvn/maven-wrapper.properties
  Maven mvn = new MavenInDocker(this, "3.5.2-jdk-8")
  mvn.additionalArgs += ' -Pci'

  if (isMainBranch()) {
    // Release starts javadoc, which takes very long, so do only for certain branches
    mvn.additionalArgs += ' -DperformRelease'
    // JDK8 is more strict, we should fix this before the next release. Right now, this is just not the focus, yet.
    mvn.additionalArgs += ' -Dmaven.javadoc.failOnError=false'
  }
  return mvn
}

boolean isMainBranch() {
  return mainBranch.equals(env.BRANCH_NAME)
}
