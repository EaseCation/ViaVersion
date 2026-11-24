import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

plugins {
    id("via.base-conventions")
    id("maven-publish")
    id("com.gradleup.shadow")
}

tasks {
    named<Jar>("jar") {
        archiveClassifier.set("unshaded")
        from(project.rootProject.file("LICENSE"))
    }
    val shadowJar = named<ShadowJar>("shadowJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("")
        configureRelocations()
        configureExcludes()
    }
    named("build") {
        dependsOn(shadowJar)
    }
}

publishing {
    publications.create<MavenPublication>("mavenJava") {
        groupId = rootProject.group as String
        artifactId = project.name
        version = (rootProject.version as String).removeSuffix("-SNAPSHOT") + "-ec." + rootProject.property("forkRevision")
        pom.properties.set(mapOf("upstream.groupId" to "com.viaversion", "upstream.version" to rootProject.version as String))

        artifact(tasks["shadowJar"])
        artifact(tasks["sourcesJar"])
    }
    // EaseCation private repository (Aliyun Yunxiao Packages); publications always carry a release version.
    // Credentials: yunxiaoMavenUsername / yunxiaoMavenPassword in ~/.gradle/gradle.properties.
    repositories.maven {
        name = "yunxiaoMaven"
        url = uri("https://easecation-cn-hangzhou.devops.aliyuncs.com/packages/api/protocol/maven/14075-release-h64s7e")
        credentials(PasswordCredentials::class)
    }
}

fun ShadowJar.configureRelocations() {
    relocate("com.google.gson", "com.viaversion.viaversion.libs.gson")
    relocate("it.unimi.dsi.fastutil", "com.viaversion.viaversion.libs.fastutil")
    relocate("org.yaml.snakeyaml", "com.viaversion.viaversion.libs.snakeyaml")
}

fun ShadowJar.configureExcludes() {
    // FastUtil - we only want object, int, long-to-object, and certain reference maps
    // Object types
    exclude("it/unimi/dsi/fastutil/*/*2Reference*")
    exclude("it/unimi/dsi/fastutil/*/*Reference2Int*")
    exclude("it/unimi/dsi/fastutil/*/*Boolean*")
    exclude("it/unimi/dsi/fastutil/*/*Byte*")
    exclude("it/unimi/dsi/fastutil/*/*Short*")
    exclude("it/unimi/dsi/fastutil/*/*Float*")
    exclude("it/unimi/dsi/fastutil/*/*Double*")
    exclude("it/unimi/dsi/fastutil/*/*2Long*")
    exclude("it/unimi/dsi/fastutil/*/*Char*")
    // Map types
    exclude("it/unimi/dsi/fastutil/*/*Custom*")
    exclude("it/unimi/dsi/fastutil/*/*Tree*")
    exclude("it/unimi/dsi/fastutil/*/*Heap*")
    exclude("it/unimi/dsi/fastutil/*/*Queue*")
    exclude("it/unimi/dsi/fastutil/longs/*2Int*")
    exclude("it/unimi/dsi/fastutil/longs/*ArrayMap*")
    exclude("it/unimi/dsi/fastutil/longs/*LinkedOpenHashMap*")
    exclude("it/unimi/dsi/fastutil/longs/*SortedMap*")
    // Crossing fingers
    exclude("it/unimi/dsi/fastutil/*Big*")
    exclude("it/unimi/dsi/fastutil/*/*Big*")
    exclude("it/unimi/dsi/fastutil/*/*Synchronized*")
    exclude("it/unimi/dsi/fastutil/*/*Unmodifiable*")
    // More
    exclude("it/unimi/dsi/fastutil/io/TextIO.class")
    exclude("it/unimi/dsi/fastutil/io/BinIO.class")
}
