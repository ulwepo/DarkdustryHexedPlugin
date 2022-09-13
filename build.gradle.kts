import groovy.json.JsonSlurper

plugins {
    java
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    val json = JsonSlurper().parseText(file("src/main/resources/plugin.json").readText()) as Map<*, *>
    val mindustryVersion = json["minGameVersion"]!!
    project.version = json["version"]!!

    compileOnly("com.github.Anuken.Arc:arc-core:v$mindustryVersion")
    compileOnly("com.github.Anuken.Mindustry:core:v$mindustryVersion") {
        exclude("com.github.Anuken.Arc", "flabel")
        exclude("com.github.Anuken.Arc", "freetype")
        exclude("com.github.Anuken.Arc", "fx")
        exclude("com.github.Anuken.Arc", "g3d")
    }
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
