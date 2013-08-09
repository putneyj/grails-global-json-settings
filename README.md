## Global JSON Settings

This plugin allows you to setup global JSON setting in your Config.groovy file.

You can exclude properties by name, prevent null properties from rendering, and ignore the version property by setting the following properties in your Config.groovy file:

grails.plugin.globalJsonSettings.renderNull = true

grails.plugin.globalJsonSettings.includeVersion = false

grails.plugin.globalJsonSettings.exclusions = "class, password"
