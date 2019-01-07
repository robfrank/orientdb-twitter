#!/usr/bin/env groovy

node {
    try {

        properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '10', artifactNumToKeepStr: '10', daysToKeepStr: '10', numToKeepStr: '10')), disableConcurrentBuilds()])

        cleanWs()

        stage('checkout') {
            checkout scm
        }

        stage('check java') {
            sh "java -version"
        }

        stage('clean') {
            sh "chmod +x mvnw"
            sh "./mvnw clean "
        }


        stage('install') {
            try {
                sh "./mvnw test install"
            } catch (err) {
                throw err
            } finally {
                junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'
            }
        }

        stage('docker') {
            sh "cd fetcher-java-graph;../mvnw docker:build"
        }

        slackSend(color: '#00FF00', message: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
    } catch (e) {
        currentBuild.result = 'FAILURE'
        slackSend(color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})\n${e}")
        throw e
    }

}
