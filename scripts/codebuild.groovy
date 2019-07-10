#!groovy
import java.util.logging.Logger
import dev.lsegal.jenkins.codebuilder.CodeBuilderCloud
import jenkins.model.Jenkins
@Grapes([
    @Grab(group='org.yaml', module='snakeyaml', version='1.17')
])
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

Logger logger = Logger.getLogger("codebuild.groovy")
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

Map codeBuildConfig = yaml.load(configText).codebuild

// Remove previous cloud instances
jenkins = Jenkins.getInstance()
prevInstances = jenkins.clouds.findAll { c -> c instanceof CodeBuilderCloud }
jenkins.clouds.removeAll(prevInstances)

// Re-add this cloud
cbc = new CodeBuilderCloud(null, codeBuildConfig.project_name, codeBuildConfig.credentials_id, codeBuildConfig.region)
cbc.setLabel(codeBuildConfig.label)
cbc.setJenkinsUrl(codeBuildConfig.jenkins_url)
cbc.setJnlpImage(codeBuildConfig.jnlp_image)
cbc.setComputeType(codeBuildConfig.compute_type)

jenkins.clouds.add(cbc);
jenkins.save()

logger.info("Successfully configured AWS CodeBuild integration.")