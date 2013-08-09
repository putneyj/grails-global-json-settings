import com.putneyj.CustomObjectMarshaller
import grails.converters.JSON
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.converters.marshaller.json.DomainClassMarshaller
import org.springframework.context.ApplicationContext

class GlobalJsonSettingsGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.2 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Global JSON Exclusions" // Headline display name of the plugin
    def author = "Jonathan Putney"
    def authorEmail = "jonathan.putney@me.com"
    def description = '''\
This plugin allows you to setup global JSON setting in your Config.groovy file.\
You can exclude properties by name, prevent null properties from rendering, and ignore the version property.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/global-json-exclusions"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
    def developers = [ [ name: "Jonathan Putney", email: "jonathan.putney@me.com" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    def doWithApplicationContext = { ctx ->
        configCustomMarshaller(application)
    }

    def onConfigChange = { event ->
        configCustomMarshaller(application)
    }

    private void configCustomMarshaller(GrailsApplication application) {
        GroovyClassLoader classLoader = new GroovyClassLoader(getClass().getClassLoader())
        ConfigObject config
        try {
            // Class containing our default exclusions - 'grails-app/conf/GlobalJsonSettingsDefaultConfig.groovy'
            config = new ConfigSlurper().parse(classLoader.loadClass('GlobalJsonSettingsDefaultConfig'))
        } catch (Exception e) {  /* do nothing */ }

        // Grab our default exclusions
        String defaultExclusions = config.globalJsonSettings.exclusions

        // Grab our user-defined exclusions
        String userExclusions = application.config.grails.plugin.globalJsonSettings.exclusions

        def configGlobalExclusions = defaultExclusions + ', ' + userExclusions

        // Check if 'version' should be included and create custom marshaller
        CustomObjectMarshaller customObjectMarshaller =
            new CustomObjectMarshaller(application)

        // Should be include the version property?
        boolean globalIncludeVersion = application.config.grails.plugin.globalJsonSettings.includeVersion
        customObjectMarshaller.includeVersion = globalIncludeVersion != null ?
            globalIncludeVersion : config.globalJsonSettings.includeVersion

        // Should we render properties with 'null' value?
        boolean globalRenderNull = application.config.grails.plugin.globalJsonSettings.renderNull
        customObjectMarshaller.renderNull = globalRenderNull != null ? globalRenderNull : config.globalJsonSettings.renderNull


        // Create array from excluded field names and pass it to our CustomObjectMarshaller
        customObjectMarshaller.excludes = configGlobalExclusions.split(/,\s*/).collect { it }

        // Register our custom marshaller with JSON
        JSON.registerObjectMarshaller(customObjectMarshaller)
    }
}
