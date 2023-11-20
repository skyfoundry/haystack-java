# README #

The haystack-java library provides a java implementation of the haystack data model. It also includes a haystack 3.0 compliant client, and reference server.

### Building ###

The project is built with [Gradle](http://gradle.org/). It makes uses of the [gradle wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) so that you don't have to actually install Gradle yourself. You should use the `gradlew` script to run all gradle tasks for this project.

After cloning the repository, run the following command to build and test the library.

`./gradlew build` (Unix)

`./gradlew.bat build` (Windows)

It is highly recommended to enable the [gradle daemon](https://docs.gradle.org/current/userguide/gradle_daemon.html) so builds go faster.

### Gradle Dependency ###

If you have Gradle-based java projects that depend on this library, you can configure your build scripts to get the artifact from BinTray/JCenter.

```
repositories {
    jcenter()
}

dependencies {
    implementation 'org.projecthaystack:haystack-java:3.0.7'
}
```

### Maven Dependency ###

If you have Maven-based java projects that depend on this library, you can configure your build scripts to get the artifact from BinTray/JCenter.

```
<repositories>
    <repository>
        <id>bintray</id>
        <name>Bintray</name>
        <url>https://jcenter.bintray.com/</url>
    </repository>
</repositories>

<dependency>
  <groupId>org.projecthaystack</groupId>
  <artifactId>haystack-java</artifactId>
  <version>3.0.7</version>
</dependency>
```
