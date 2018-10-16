#!/usr/bin/env groovy
def call(label, Closure body){
    if (label == "docker") {
        podTemplate(name: 'jenkins-slave-docker', label: 'docker', namespace: 'cicd', idleMinutes: 30, containers: [
            containerTemplate(name: 'docker', image: 'docker:17.03-dind', 
                args: '--registry-mirror=https://registry.docker-cn.com -s=overlay2 --bip=172.30.1.1/24', privileged: true, ttyEnabled: true),
        ],
        volumes:[
            secretVolume(secretName: 'docker-config', mountPath: '/home/jenkins/.docker'),
        ]){
            node(label) {
                body()
            }
        }
    }
    if (label == "maven") {
        podTemplate(name: 'jenkins-slave-maven', namespace: 'kube-system', idleMinutes: 30, containers: [
            containerTemplate(name: 'maven', image: 'maven:3.5-alpine', command: 'cat', ttyEnabled: true),
            containerTemplate(name: 'docker', image: 'docker:17.03-dind', 
                args: '--registry-mirror=https://registry.docker-cn.com -s=overlay2 --bip=172.30.1.1/24', privileged: true, ttyEnabled: true),
            containerTemplate(name: 'k8s', image: 'hub.simpletour.com/infra/jenkins-k8s', command: 'cat', ttyEnabled: true), 
        ],
        volumes:[
            secretVolume(secretName: 'docker-config', mountPath: '/home/jenkins/.docker'),
            secretVolume(secretName: 'jenkins-maven-settings', mountPath: '/root/.m2'),
            secretVolume(secretName: 'k8s-context', mountPath: '/home/jenkins/.kube'),
            hostPathVolume(mountPath: '/maven/repository', hostPath: '/data/nfs/maven-repo'),
            // persistentVolumeClaim(claimName: 'maven-repo', mountPath: '/maven/repository'),
        ]){
            node(label) {
                body()
            }
        }
    }
    if (label == "k8s"){
        podTemplate(name: 'jenkins-slave-k8s', label: 'k8s', namespace: 'cicd', idleMinutes: 30, containers: [
            containerTemplate(name: 'k8s', image: 'hub.simpletour.com/infra/jenkins-k8s', command: 'cat', ttyEnabled: true), 
        ],
        volumes:[
            secretVolume(secretName: 'k8s-context', mountPath: '/home/jenkins/.kube'),
        ]){
            node(label) {
                body()
            }
        }
    }
}
