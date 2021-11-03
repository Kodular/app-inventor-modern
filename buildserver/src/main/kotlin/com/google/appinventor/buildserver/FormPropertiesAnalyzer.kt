// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver

import org.codehaus.jettison.json.JSONArray

/**
 * Methods for analyzing the contents of a Young Android Form file.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
object FormPropertiesAnalyzer {
    private const val FORM_PROPERTIES_PREFIX = "#|\n"
    private const val FORM_PROPERTIES_SUFFIX = "\n|#"

    // Logging support
    private val LOG: Logger = Logger.getLogger(FormPropertiesAnalyzer::class.java.getName())

    /**
     * Parses a complete source file and return the properties as a JSONObject.
     *
     * @param source a complete source file
     * @return the properties as a JSONObject
     */
    fun parseSourceFile(source: String): JSONObject {
        var source = source
        source = source.replaceAll("\r\n", "\n")
        // First, locate the beginning of the $JSON section.
        // Older files have a $Properties before the $JSON section and we need to make sure we skip
        // that.
        val jsonSectionPrefix = """
             $FORM_PROPERTIES_PREFIX${"$"}JSON
             
             """.trimIndent()
        var beginningOfJsonSection: Int = source.lastIndexOf(jsonSectionPrefix)
        if (beginningOfJsonSection == -1) {
            throw IllegalArgumentException(
                "Unable to parse file - cannot locate beginning of \$JSON section"
            )
        }
        beginningOfJsonSection += jsonSectionPrefix.length()

        // Then, locate the end of the $JSON section
        val jsonSectionSuffix = FORM_PROPERTIES_SUFFIX
        val endOfJsonSection: Int = source.lastIndexOf(jsonSectionSuffix)
        if (endOfJsonSection == -1) {
            throw IllegalArgumentException(
                "Unable to parse file - cannot locate end of \$JSON section"
            )
        }
        val jsonPropertiesString: String = source.substring(
            beginningOfJsonSection,
            endOfJsonSection
        )
        return try {
            JSONObject(jsonPropertiesString)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Unable to parse file - invalid \$JSON section syntax")
        }
    }

    /**
     * Returns the Set of component types used in the given form file source.
     */
    fun getComponentTypesFromFormFile(source: String): Set<String> {
        val componentTypes: Set<String> = HashSet<String>()
        val propertiesObject: JSONObject = parseSourceFile(source)
        try {
            collectComponentTypes(propertiesObject.getJSONObject("Properties"), componentTypes)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Unable to parse file - invalid \$JSON section syntax")
        }
        return componentTypes
    }

    @Throws(JSONException::class)
    private fun collectComponentTypes(
        componentProperties: JSONObject,
        componentTypes: Set<String>
    ) {
        val componentType: String = componentProperties.getString("\$Type")
        componentTypes.add(componentType)

        // Recursive call to collect nested components.
        if (componentProperties.has("\$Components")) {
            val components: JSONArray = componentProperties.getJSONArray("\$Components")
            for (i in 0 until components.length()) {
                collectComponentTypes(components.getJSONObject(i), componentTypes)
            }
        }
    }

    /**
     * Extracts a mapping from component to set of blocks used from the Form's
     * Scheme (.scm) file, which is actually a JSON dictionary contained within
     * a block comment. Any property that is expressed in the properties section
     * (i.e., not the default value) is considered used by the function.
     *
     * @param source Source contents of the Scheme file
     * @return A mapping of component type names to sets of blocks used
     */
    fun getComponentBlocksFromSchemeFile(source: String): Map<String, Set<String>> {
        val result: Map<String, Set<String>> = HashMap()
        var propertiesObject: JSONObject = parseSourceFile(source)
        try {
            val toProcess: Queue<JSONObject> = LinkedList<JSONObject>()
            toProcess.add(propertiesObject.getJSONObject("Properties"))
            while (toProcess.poll().also { propertiesObject = it } != null) {
                val type: String = propertiesObject.getString("\$Type")
                if (!result.containsKey(type)) {
                    result.put(type, HashSet<String>())
                }
                val typeProps = result[type]!!
                val it: Iterator<String> = propertiesObject.keys()
                while (it.hasNext()) {
                    val key = it.next()
                    if (!key.startsWith("$")) {
                        typeProps.add(key)
                    }
                }
                if (propertiesObject.has("\$Components")) {
                    val components: JSONArray = propertiesObject.getJSONArray("\$Components")
                    for (i in 0 until components.length()) {
                        toProcess.add(components.getJSONObject(i))
                    }
                }
            }
        } catch (e: JSONException) {
            throw IllegalArgumentException("Unable to parse file - invalid \$JSON section syntax")
        }
        return result
    }

    /**
     * Extracts a mapping from component to set of blocks used from the Form's
     * Blocks (.bky) file. This method will **EXCLUDE** any blocks
     * that are marked disabled. This allows a developer to keep the old variation
     * of any blocks that would cause complications in their project for reference.
     *
     * @param source Source contents of the Blockly (XML) file.
     * @return A mapping of component type names to sets of blocks used
     */
    fun getComponentBlocksFromBlocksFile(source: String?): Map<String, Set<String>> {
        val result: Map<String, Set<String>> = HashMap()
        if (source == null || source.isEmpty()) {
            return result
        }
        try {
            val reader: XMLReader = XMLReaderFactory.createXMLReader()
            reader.setContentHandler(object : DefaultHandler() {
                private var skipBlocksCounter = 0
                private var blockType: String? = null
                @Override
                @Throws(SAXException::class)
                fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
                    if ("block".equals(qName)) {
                        if ("true".equals(attributes.getValue("disabled")) || skipBlocksCounter > 0) {
                            skipBlocksCounter++
                        }
                        blockType = attributes.getValue("type")
                    } else if ("next".equals(qName) && skipBlocksCounter == 1) {
                        skipBlocksCounter = 0
                    } else if (skipBlocksCounter == 0 && "mutation".equals(qName)) {
                        var blockName: String? = null
                        if ("component_event".equals(blockType)) {
                            blockName = attributes.getValue("event_name")
                        } else if ("component_method".equals(blockType)) {
                            blockName = attributes.getValue("method_name")
                        } else if ("component_set_get".equals(blockType)) {
                            blockName = attributes.getValue("property_name")
                        }
                        if (blockName != null) {
                            val componentType: String = attributes.getValue("component_type")
                            if (!result.containsKey(componentType)) {
                                result.put(componentType, HashSet<String>())
                            }
                            result[componentType].add(blockName)
                        }
                    }
                    super.startElement(uri, localName, qName, attributes)
                }

                @Override
                @Throws(SAXException::class)
                fun endElement(uri: String?, localName: String?, qName: String?) {
                    if ("block".equals(qName) && skipBlocksCounter > 0) {
                        skipBlocksCounter--
                    }
                    super.endElement(uri, localName, qName)
                }
            })
            reader.parse(InputSource(ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8))))
        } catch (e: SAXException) {
            throw IllegalStateException(e)
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
        return result
    }
}