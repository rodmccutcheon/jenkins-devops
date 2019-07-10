#!groovy
import java.util.logging.Logger
import jenkins.*
import jenkins.model.*
import hudson.model.*
import hudson.security.*
import hudson.security.csrf.DefaultCrumbIssuer;
import jenkins.security.s2m.AdminWhitelistRule
@Grapes([
    @Grab(group='org.yaml', module='snakeyaml', version='1.17')
])
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

Logger logger = Logger.getLogger("security.groovy")
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
securityConfig = yaml.load(configText).security

// Disable CLI remoting, because it's insecure.
Boolean cliEnabled = (Boolean) securityConfig.cli_enabled

if (!cliEnabled) {
    // disabled CLI access over TCP listener (separate port)
    def p = jenkins.AgentProtocol.all()
    p.each { x ->
        if (x.name && x.name.contains("CLI")) {
            p.remove(x)
        }
    }

    // disable CLI access over /cli URL
    def removal = { lst ->
        lst.each { x ->
            if (x.getClass().name.contains("CLIAction")) {
                lst.remove(x)
            }
        }
    }

    logger.info("Removing the Jenkins CLI subsystem")
    removal(jenkins.getExtensionList(RootAction.class))
    removal(jenkins.actions)
}

// Configure Agent security settings
agentConfig = securityConfig.agent_settings
jenkins.setSlaveAgentPort(agentConfig.jnlp_tcp_port)
Set<String> protocols = new HashSet<String>();
agentConfig.protocols.each { protocol ->
    protocols.add(protocol)
}
jenkins.setAgentProtocols(protocols)

// Configure CSRF protection
if (securityConfig.csrf_protection_enabled) {
    DefaultCrumbIssuer crumbIssuer = new DefaultCrumbIssuer(
        securityConfig.csrf_proxy_compatibility
    )
    jenkins.setCrumbIssuer(crumbIssuer)
}

def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
strategy.setAllowAnonymousRead(false)
jenkins.setAuthorizationStrategy(strategy)
 
jenkins.getInjector().getInstance(AdminWhitelistRule.class).setMasterKillSwitch(false)

jenkins.save()

logger.info("Successfully configured global security settings.")