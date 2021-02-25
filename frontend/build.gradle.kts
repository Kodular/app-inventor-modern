plugins {
    kotlin("js") version "1.4.21"
}

kotlin {
    js {
        browser()
    }
}

dependencies {
    implementation(kotlin("stdlib-js"))

    //React, React DOM + Wrappers (chapter 3)
    implementation("org.jetbrains:kotlin-react:16.13.1-pre.110-kotlin-1.4.0")
    implementation("org.jetbrains:kotlin-react-dom:16.13.1-pre.110-kotlin-1.4.0")
    implementation(npm("react", "16.13.1"))
    implementation(npm("react-dom", "16.13.1"))

//    //Coroutines (chapter 8)
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
}