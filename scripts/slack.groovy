#!groovy
import java.util.logging.Logger
import jenkins.model.Jenkins
import hudson.model.User
import hudson.security.HudsonPrivateSecurityRealm
import hudson.tasks.Mailer.UserProperty
@Grapes([
    @Grab(group='org.yaml', module='snakeyaml', version='1.17')
])
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

Logger logger = Logger.getLogger("slack.groovy")
Jenkins jenkins = Jenkins.getInstance()
Yaml yaml = new Yaml(new SafeConstructor())

String configPath = System.getenv("JENKINS_CONFIG_PATH")
String configText = ''
try {
    configText = new File("${configPath}/config.yml").text
} catch (FileNotFoundException e) {
    logger.severe("Cannot find config file path @ ${configPath}/config.yml")
    jenkins.doSafeExit(null)
    System.exit(1)
}

Map slackConfig = yaml.load(configText).slack

def slack = jenkins.getExtensionList(
        jenkins.plugins.slack.SlackNotifier.DescriptorImpl.class
    )[0]

slack.baseUrl = slackConfig.base_url
slack.teamDomain = slackConfig.team_subdomain
slack.tokenCredentialId = slackConfig.integration_token
slack.botUser = slackConfig.is_slack_bot
slack.room = slackConfig.room

slack.save()
jenkins.save()
logger.info("Successfully configured slack integration.")
