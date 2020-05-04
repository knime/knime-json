#!groovy
def BN = BRANCH_NAME == "master" || BRANCH_NAME.startsWith("releases/") ? BRANCH_NAME : "master"

library "knime-pipeline@$BN"

properties([
    pipelineTriggers([
        upstream('knime-javasnippet/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
        upstream('knime-base/' + env.BRANCH_NAME.replaceAll('/', '%2F')),
    ]),
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

try {
    knimetools.defaultTychoBuild('org.knime.update.json')

    workflowTests.runTests(
        dependencies: [
            repositories: ['knime-json', 'knime-xml', 'knime-filehandling', 'knime-jep', 'knime-productivity-oss', 'knime-reporting'],
            ius: ['org.knime.json.tests']
        ]
    )
    stage('Sonarqube analysis') {
        env.lastStage = env.STAGE_NAME
        workflowTests.runSonar()
    }
} catch (ex) {
    currentBuild.result = 'FAILED'
    throw ex
} finally {
    notifications.notifyBuild(currentBuild.result);
}
/* vim: set shiftwidth=4 expandtab smarttab: */