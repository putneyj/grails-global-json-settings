package com.putneyj

import grails.converters.JSON
import org.codehaus.groovy.grails.commons.ClassPropertyFetcher
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.IncludeExcludeSupport
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.converters.ConverterUtil
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.marshaller.IncludeExcludePropertyMarshaller
import org.codehaus.groovy.grails.web.json.JSONWriter
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl

class CustomObjectMarshaller extends IncludeExcludePropertyMarshaller<JSON> {

    private ProxyHandler proxyHandler
    private GrailsApplication application

    private List<String> excludes = []

    boolean includeVersion = false
    boolean renderNull = true

    CustomObjectMarshaller(GrailsApplication application) {
        this(false, new DefaultProxyHandler(), application)
    }

    CustomObjectMarshaller(boolean includeVersion, ProxyHandler proxyHandler, GrailsApplication application) {
        this.includeVersion = includeVersion
        this.proxyHandler = proxyHandler
        this.application = application
    }

    boolean supports(object) {
        return object instanceof GroovyObject
    }

    void marshalObject(value, JSON json) throws ConverterException {
        JSONWriter writer = json.getWriter()
        value = proxyHandler.unwrapIfProxy(value)
        Class<?> clazz = value.getClass()
        List<String> includes = json.getIncludes(clazz)
        IncludeExcludeSupport<String> includeExcludeSupport = new IncludeExcludeSupport<String>()

        GrailsDomainClass domainClass = application.getDomainClass(ConverterUtil.trimProxySuffix(clazz.name))
        BeanWrapper beanWrapper = new BeanWrapperImpl(value)

        writer.object()

        if(shouldInclude(includeExcludeSupport, includes, excludes, value, "class")) {
            writer.key("class").value(domainClass.getClazz().getName())
        }

        GrailsDomainClassProperty id = domainClass.getIdentifier()

        if(shouldInclude(includeExcludeSupport, includes, excludes, value, id.getName())) {
            Object idValue = extractValue(value, id)
            json.property(GrailsDomainClassProperty.IDENTITY, idValue)
        }

        if (shouldInclude(includeExcludeSupport, includes, excludes, value, GrailsDomainClassProperty.VERSION) && isIncludeVersion()) {
            Object version = extractValue(value, domainClass.version)
            json.property(GrailsDomainClassProperty.VERSION, version)
        }

        for (GrailsDomainClassProperty property : domainClass.persistentProperties) {
            if(!shouldInclude(includeExcludeSupport, includes, excludes, value, property.getName())) continue
            if(beanWrapper.getPropertyValue(property.getName()) == null && !renderNull) continue

            writer.key(property.getName())
            if (!property.isAssociation()) {
                // Write non-relation property
                Object val = beanWrapper.getPropertyValue(property.getName())
                json.convertAnother(val)
            }
            else {
                Object referenceObject = beanWrapper.getPropertyValue(property.getName())
                if (isRenderDomainClassRelations()) {
                    if (referenceObject == null) {
                        writer.value(null)
                    }
                    else {
                        referenceObject = proxyHandler.unwrapIfProxy(referenceObject)
                        if (referenceObject instanceof SortedMap) {
                            referenceObject = new TreeMap((SortedMap) referenceObject)
                        }
                        else if (referenceObject instanceof SortedSet) {
                            referenceObject = new TreeSet((SortedSet) referenceObject)
                        }
                        else if (referenceObject instanceof Set) {
                            referenceObject = new HashSet((Set) referenceObject)
                        }
                        else if (referenceObject instanceof Map) {
                            referenceObject = new HashMap((Map) referenceObject)
                        }
                        else if (referenceObject instanceof Collection) {
                            referenceObject = new ArrayList((Collection) referenceObject)
                        }
                        json.convertAnother(referenceObject)
                    }
                }
                else {
                    if (referenceObject == null) {
                        json.value(null)
                    }
                    else {
                        GrailsDomainClass referencedDomainClass = property.getReferencedDomainClass()

                        // Embedded are now always fully rendered
                        if (referencedDomainClass == null || property.isEmbedded() || property.getType().isEnum()) {
                            json.convertAnother(referenceObject)
                        }
                        else if (property.isOneToOne() || property.isManyToOne() || property.isEmbedded()) {
                            asShortObject(referenceObject, json, referencedDomainClass.getIdentifier(), referencedDomainClass)
                        }
                        else {
                            GrailsDomainClassProperty referencedIdProperty = referencedDomainClass.getIdentifier()
                            if (referenceObject instanceof Collection) {
                                writer.array()
                                for (el in referenceObject) {
                                    asShortObject(el, json, referencedIdProperty, referencedDomainClass)
                                }
                                writer.endArray()
                            }
                            else if (referenceObject instanceof Map) {
                                referenceObject.each { k, v ->
                                    writer.object()
                                    writer.key(String.valueOf(k))
                                    asShortObject(v, json, referencedIdProperty, referencedDomainClass)
                                    writer.endObject()
                                }
                            }
                        }
                    }
                }
            }
        }
        writer.endObject()
    }

    private boolean isRenderDomainClassRelations() {
        return false
    }

    private boolean shouldInclude(IncludeExcludeSupport<String> includeExcludeSupport, List<String> includes, List<String> excludes, object, String propertyName) {
        return includeExcludeSupport.shouldInclude(includes,excludes,propertyName) && shouldInclude(object,propertyName)
    }

    protected void asShortObject(refObj, JSON json, GrailsDomainClassProperty idProperty, GrailsDomainClass referencedDomainClass) throws ConverterException {

        Object idValue

        if (proxyHandler instanceof EntityProxyHandler) {
            idValue = proxyHandler.getProxyIdentifier(refObj)
            if (idValue == null) {
                idValue = extractValue(refObj, idProperty)
            }
        }
        else {
            idValue = extractValue(refObj, idProperty)
        }
        JSONWriter writer = json.getWriter()
        writer.object()
        writer.key("class").value(referencedDomainClass.getName())
        writer.key("id").value(idValue)
        writer.endObject()
    }

    protected extractValue(domainObject, GrailsDomainClassProperty property) {
        if(domainObject instanceof GroovyObject) {
            return domainObject.getProperty(property.getName())
        }

        ClassPropertyFetcher propertyFetcher = ClassPropertyFetcher.forClass(domainObject.getClass())
        return propertyFetcher.getPropertyValue(domainObject, property.getName())
    }
}
