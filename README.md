KBootstrap is an alternative to https://github.com/FabricMC/fabric-language-kotlin that downloads up-to-date Kotlin libraries at runtime for mod developers to freely include it in their Kotlin mods so as to not bloat them.
The libraries are cached under the operating system's temporary directory.

Like Fabric Language Kotlin, KBootstrap provides a Kotlin language adapter named "kbootstrap".

By default, KBootstrap downloads only the standard library; mods may specify any of the additional modules listed below.

Use KBootstrap by applying its Gradle plugin: 
`settings.gradle`:
```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        
        maven {url = "https://maven.auoeke.net"}
    }
}
```

`build.gradle`:
```groovy
plugins {
    id("kbootstrap-plugin").version("latest.release") // Or choose a version from https://maven.auoeke.net/net/auoeke/kbootstrap-plugin.
}
```

The plugin will apply the plugin `org.jetbrains.kotlin.jvm` and configure its target JVM version to `targetCompatibility` as recommended.

To use additional Kotlin libraries, add their corresponding modules:
```groovy
// The standard library is always included.
// Pick from these modules those that you want.
module("coroutines", "reflect", "serialization")

// or
allModules()
```
