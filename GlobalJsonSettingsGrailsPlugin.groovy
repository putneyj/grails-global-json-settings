import grails.converters.JSON
import org.codehaus.groovy.grails.commons.GrailsApplication
import com.putneyj.CustomObjectMarshaller

class GlobalJsonSettingsGrailsPlugin {
    def version = "0.1.1"
    def grailsVersion = "2.3 > *"
    def title = "Global JSON Settings"
    def description = '''\
Allows you to setup global JSON setting in your Config.groovy file.\
You can exclude properties by name, prevent null properties from rendering, and ignore the version property.
'''

    def documentation = "http://grails.org/plugin/global-json-exclusions"

    def license = "APACHE"
    def developers = [[name: "Jonathan Putney", email: "jonathan.putney@me.com"]]
    def issueManagement = [system: "GITHUB", url: 'https://github.com/putneyj/grails-global-json-settings/issues']
    def scm = [url: 'https://github.com/putneyj/grails-global-json-settings']

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
