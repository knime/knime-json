#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        // knime-base -> knime-javasnippet -> knime-json
        upstream("knime-javasnippet/${env.BRANCH_NAME.replaceAll('/', '%2F')}")
    ]),
    parameters(workflowTests.getConfigurationsAsParameters() + fsTests.getFSConfigurationsAsParameters()),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

SSHD_IMAGE = "${dockerTools.ECR}/knime/sshd:alpine3.11"

try {
    knimetools.defaultTychoBuild('org.knime.update.json')
    configs = [
        "Workflowtests" : {
            workflowTests.runTests(
                dependencies: [
                    repositories: ['knime-json', 'knime-xml', 'knime-filehandling', 'knime-jep', 'knime-productivity-oss', 'knime-reporting'],
                    ius: ['org.knime.json.tests']
                ]
            )
        },
        "Filehandlingtests" : {
            workflowTests.runFilehandlingTests (
                dependencies: [
                    repositories: [
                        'knime-json'
                    ]
                ],
            )
        }
    ]

    parallel configs

    stage('Sonarqube analysis') {
        env.lastStage = env.STAGE_NAME
        workflowTests.runSonar()
    }
    
} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}
/* vim: set shiftwidth=4 expandtab smarttab: */
