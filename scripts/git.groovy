#!groovy
import java.util.logging.Logger
import jenkins.model.Jenkins
@Grapes([
    @Grab(group='org.yaml', module='snakeyaml', version='1.17')
])
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

Logger logger = Logger.getLogger("git.groovy")
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

Map gitConfig = yaml.load(configText).git

def gitScm = jenkins.getDescriptorByType(
                hudson.plugins.git.GitSCM.DescriptorImpl.class
             )

gitScm.setGlobalConfigName(gitConfig.name)
gitScm.setGlobalConfigEmail(gitConfig.email)

jenkins.save()