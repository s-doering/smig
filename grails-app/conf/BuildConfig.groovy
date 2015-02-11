grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
//  legacyResolve false
    // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility
    repositories {
        grailsCentral()
        mavenCentral()
        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        //mavenLocal()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        // runtime 'mysql:mysql-connector-java:5.1.21'
    }

    plugins {
        // Spock and Hibernate were commented so it can be easily used with other projects
        // without configuring the plugins, just use "maven-install". This works with Grails
        // versions 2.1, 2.2, 2.3 and 2.4.
        // Uncomment this if you develop and test this plugin.
//        test(":hibernate:$grailsVersion")
//        test(":spock:0.7")

        build(":release:2.2.1")
        build(":rest-client-builder:1.0.3") {
            export = false
        }
    }
}
