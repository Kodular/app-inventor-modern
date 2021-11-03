// -*- mode: java; c-basic-offset: 2; -*-
// Copyright © 2017 Massachusetts Institute of Technology, All rights reserved.
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver.util

import java.io.File

/**
 * AARLibraries implements a set of [AARLibrary] and performs additional bookkeeping by
 * tracking the various classes, resources, assets, etc. packaged within each of the .aar files.
 *
 * @author ewpatton@mit.edu (Evan W. Patton)
 */
class AARLibraries(generated: File) : HashSet<AARLibrary?>() {
    /**
     * Absolute path to where generated source files will be written.
     */
    private val generated: String

    /**
     * The output directory where compiled class files will be written.
     */
    private var outputDir: File? = null

    /**
     * The classes.dex file present in the .aar files, if any.
     */
    private val classes: Set<File> = HashSet()

    /**
     * The resources files present in the .aar files, if any.
     */
    private val resources: Set<File> = HashSet()

    /**
     * The assets present in the .aar files, if any.
     */
    private val assets: Set<File> = HashSet()

    /**
     * The libraries (.jar) files present in the .aar files, if any.
     */
    private val libraries: Set<File> = HashSet()

    /**
     * The native libraries (.so) files present in the .aar files, if any.
     */
    private val natives: Set<File> = HashSet()

    /**
     * Maps the package name for the dependency to any symbols it declares.
     */
    private val symbols: Multimap<String, SymbolLoader> = HashMultimap.create()
    @Override
    fun add(e: AARLibrary): Boolean {
        if (super.add(e)) {
            val packageName: String = e.getPackageName()
            classes.add(e.getClassesJar())
            resources.addAll(e.getResources())
            assets.addAll(e.getAssets())
            libraries.addAll(e.getLibraries())
            natives.addAll(e.getNatives())
            try {
                if (e.getRTxt() != null) {
                    val loader = SymbolLoader(e.getRTxt(), LOG)
                    loader.load()
                    symbols.put(packageName, loader)
                }
            } catch (ex: IOException) {
                throw IllegalArgumentException("IOException merging resources", ex)
            }
            return true
        }
        return false
    }

    @Override
    fun remove(o: Object?): Boolean {
        // we don't support removing AAR libraries during compilation
        throw UnsupportedOperationException()
    }

    fun getClasses(): Set<File> {
        return classes
    }

    fun getResources(): Set<File> {
        return resources
    }

    fun getAssets(): Set<File> {
        return assets
    }

    fun getLibraries(): Set<File> {
        return libraries
    }

    fun getNatives(): Set<File> {
        return natives
    }

    val outputDirectory: File?
        get() = outputDir

    /**
     * Gets a list of resource sets loaded from the AAR libraries in the collection. Note that this
     * is computed on every call (results are not cached), so it is recommended that the caller only
     * call this after all AAR libraries of interest have been added.
     * @return  the list of all resource sets available across the AAR libraries.
     */
    private val resourceSets: List<Any>
        private get() {
            val resourceSets: List<ResourceSet> = ArrayList()
            for (library in this) {
                if (library.getResDirectory() != null) {
                    val resourceSet = ResourceSet(library.getDirectory().getName())
                    resourceSet.addSource(library.getResDirectory())
                    resourceSets.add(resourceSet)
                }
            }
            return resourceSets
        }

    /**
     * Merges the resources from all of the dependent AAR libraries into the main resource bundle for
     * the compiling app.
     *
     * @param outputDir the output directory to write the R.java files.
     * @param mainResDir the resource directory where the resource descriptors for the app reside.
     * @param cruncher configured PNG cruncher utility for reducing the size of PNG assets.
     * @return true if the merge was successful, otherwise false.
     */
    fun mergeResources(outputDir: File?, mainResDir: File?, cruncher: PngCruncher?): Boolean {
        val resourceSets: List<ResourceSet> = resourceSets
        val mainResSet = ResourceSet("main")
        mainResSet.addSource(mainResDir)
        resourceSets.add(mainResSet)
        val merger = ResourceMerger()
        return try {
            for (resourceSet in resourceSets) {
                resourceSet.loadFromFiles(LOG)
                merger.addDataSet(resourceSet)
            }
            val writer = MergedResourceWriter(outputDir, cruncher, false, false, null)
            writer.setInsertSourceMarkers(true)
            merger.mergeData(writer, false)
            true
        } catch (e: MergingException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Writes out the R.class files for all of the ARR libraries as well as an R.class file for the
     * main app being compiled.
     *
     * @param outputDir the output directory to write the R.class files to.
     * @param appPackageName The package name of the currently compiling app.
     * @param appRTxt The app's R.txt file containing a list of resources.
     * @return 0 if the operation completes successfully, or 1 if an error occurs.
     * @throws IOException if the program is unable to read any R.txt files or write R.java or
     * R.class files
     * @throws InterruptedException if the compiler thread is interrupted.
     */
    @Throws(IOException::class, InterruptedException::class)
    fun writeRClasses(outputDir: File, appPackageName: String?, appRTxt: File?): Int {
        this.outputDir = outputDir
        val baseSymbolTable = SymbolLoader(appRTxt, LOG)
        baseSymbolTable.load()

        // aggregate symbols into one writer per package
        val writers: Map<String, SymbolWriter> = HashMap()
        for (packageName in symbols.keys()) {
            val loaders: Collection<SymbolLoader> = symbols.get(packageName)
            val writer = SymbolWriter(generated, packageName, baseSymbolTable)
            for (loader in loaders) {
                writer.addSymbolsToWrite(loader)
            }
            writers.put(packageName, writer)
            writer.write()
        }

        // construct compiler command line
        val args: List<String> = ArrayList()
        args.add("-1.7")
        args.add("-d")
        args.add(outputDir.getAbsolutePath())
        args.add(generated)

        // compile R classes using ECJ batch compiler
        val out = PrintWriter(System.out)
        val err = PrintWriter(System.err)
        if (BatchCompiler.compile(args.toArray(arrayOfNulls<String>(0)), out, err, NOPCompilationProgress())) {
            return 0
        } else {
            return 1
        }
    }

    companion object {
        private const val serialVersionUID = -5005733968228085856L
        private val LOG: ILogger = BaseLogger()
    }

    /**
     * Construct a new AARLibraries collection.
     *
     * @param generated  directory where the generated, intermediate R.java files for each AAR
     * library will be written.
     */
    init {
        this.generated = generated.getAbsolutePath()
    }
}