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

Logger logger = Logger.getLogger("users.groovy")
Jenkins jenkins = Jenkins.getInstance()
Yaml yaml = new Yaml(new SafeConstructor())

String configPath = System.getenv("JENKINS_CONFIG_PATH")
String configText = ''
try {
    configText = new File("${configPath}/users.yml").text
} catch (FileNotFoundException e) {
    logger.severe("Cannot find config file path @ ${configPath}/users.yml")
    jenkins.doSafeExit(null)
    System.exit(1)
}

userConfigs = yaml.load(configText)

HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(false)

int userCount = 0
userConfigs.each { userData ->

    Collection<User> allUsers = User.getAll().collect { u -> u.getId() }
    if (!allUsers.contains(userData.username)) {
       realm.createAccount(userData.username, userData.password)
    }
    User user = realm.getUser(userData.username)

    UserProperty emailAddressProperty = new UserProperty(userData.email_address)
    user.addProperty(emailAddressProperty)
    user.save()
    userCount += 1

}

jenkins.setSecurityRealm(realm)
jenkins.save()
logger.info("Successfully created ${userCount.toString()} users.")