#!/usr/bin/env groovy

node {
    try {

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

        stage('build') {
            sh "cd fetcher-java-graph;../mvnw docker:build"
        }

        slackSend(color: '#00FF00', message: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
    } catch (e) {
        currentBuild.result = 'FAILURE'
        slackSend(color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})\n${e}")
        throw e
    }

}
