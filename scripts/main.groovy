#!groovy
import java.util.logging.Logger
import jenkins.*
import jenkins.model.*
import hudson.model.*
import hudson.tasks.Shell
import hudson.slaves.EnvironmentVariablesNodeProperty
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry
import hudson.model.Node.Mode
import hudson.markup.RawHtmlMarkupFormatter
import hudson.markup.EscapedMarkupFormatter
@Grapes([
    @Grab(group='org.yaml', module='snakeyaml', version='1.17')
])
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

Logger logger = Logger.getLogger("main.groovy")
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

Map mainConfig = yaml.load(configText).main
// Map propertiesConfig = yaml.load(configText).global_properties
Map locationConfig = mainConfig.location
Map shellConfig = mainConfig.shell
Map formatterConfig = mainConfig.formatter
Map setupWizardConfig = mainConfig.setup_wizard

logger.info("Configuring basic Jenkins options")
try {
    jenkins.setSystemMessage(mainConfig.system_message)
    jenkins.setNumExecutors(mainConfig.number_of_executers)
    //jenkins.setLabelString(mainConfig.labels.join(' '))
    jenkins.setQuietPeriod(mainConfig.quiet_period)
    jenkins.setScmCheckoutRetryCount(mainConfig.scm_retry_count)
    jenkins.setDisableRememberMe(mainConfig.disable_remember_me)
    if (mainConfig.usage == 'NORMAL') {
        jenkins.setMode(Mode.NORMAL)
    } else if (mainConfig.usage == 'EXCLUSIVE') {
        jenkins.setMode(Mode.EXCLUSIVE)
    }
    else {
        logger.severe('Invalid value specified for USAGE')
        logger.severe('Exiting')
        jenkins.doSafeExit(null)
        System.exit(1)
    }
} catch (MissingMethodException e) {
    logger.severe("Invalid value specified for main configuration in ${configPath}/config.yml")
    jenkins.doSafeExit(null)
    System.exit(1)
}

// logger.info("Adding global environment variables to Jenkins")
// List<Entry> envVarList = new ArrayList<Entry>()
// propertiesConfig.ENVIRONMENT_VARIABLES.each { envVar ->
//                 try {
//                     envVarList.add(new Entry(envVar.NAME, envVar.VALUE))
//                 } catch (MissingMethodException e) {
//                     logger.severe("Invalid value specified for environment variables in ${configPath}/config.yml")
//                     jenkins.doSafeExit(null)
//                     System.exit(1)
//                 }
// }

// jenkins.getGlobalNodeProperties().replaceBy(
//     Collections.singleton(
//         new EnvironmentVariablesNodeProperty(envVarList)
//     )
// )

logger.info("Setting up the Jenkins URL")
JenkinsLocationConfiguration location = jenkins.getExtensionList(jenkins.model.JenkinsLocationConfiguration).get(0)
try {
    location.setUrl(locationConfig.url)
    location.setAdminAddress(locationConfig.admin_email)
} catch (MissingMethodException e) {
    logger.severe("Invalid value in the location configuration of ${configPath}/config.yml")
    jenkins.doSafeExit(null)
    System.exit(1)
}

logger.info("Setting the default shell used by Jenkins")
Process p = "stat ${shellConfig.executable}".execute()
p.waitForOrKill(1000)
if (p.exitcode != 0) {
    logger.severe("Executable ${shellConfig.executable} not present on system")
    jenkins.doSafeExit(null)
    System.exit(1)
}
Shell.DescriptorImpl shell = jenkins.getExtensionList(Shell.DescriptorImpl.class).get(0)
shell.setShell(shellConfig.EXECUTABLE)
shell.save()

// Configure the markup formatter for build and job descriptions
if (formatterConfig.type.toLowerCase() == 'rawhtml') {
    RawHtmlMarkupFormatter markupFormatter = new RawHtmlMarkupFormatter(
        formatterConfig.disable_syntax_highlighting
    )
    jenkins.setMarkupFormatter(markupFormatter)
} else if (formatterConfig.type.toLowerCase() == 'plain') {
    EscapedMarkupFormatter markupFormatter = new EscapedMarkupFormatter()
    jenkins.setMarkupFormatter(markupFormatter)
} else {
    logger.severe("Invalid value in the formatter configuration of ${configPath}/config.yml")
    jenkins.doSafeExit(null)
    System.exit(1)
}

// By default the setup wizard is enabled, disable if desired
// Boolean setupWizardEnabled = (Boolean) setupWizardConfig.enabled
// if (!setupWizardEnabled) {
//     if (!jenkins.installState.isSetupComplete()) {
//         jenkins.install.InstallState.INITIAL_SETUP_COMPLETED.initializeState()
//     }
// }

jenkins.save()
logger.info("Finished configuring the main Jenkins options.")