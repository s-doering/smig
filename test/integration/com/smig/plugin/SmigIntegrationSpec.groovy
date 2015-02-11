/*
 * Copyright (c) 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smig.plugin

import com.smig.interfaces.Migrate
import grails.plugin.spock.IntegrationSpec
import groovy.sql.Sql
import org.apache.commons.io.FileUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.hibernate.SessionFactory
import org.springframework.context.ApplicationContext

import javax.sql.DataSource

class SmigIntegrationSpec extends IntegrationSpec {

    SessionFactory sessionFactory
    GrailsApplication grailsApplication

    private Smig getSmig() {
        return new Smig(grailsApplication)
    }

    private ConfigObject createConfigFromString(String configContent) {
        File configFile = File.createTempFile('config', 'groovy')
        FileUtils.writeStringToFile(configFile, configContent)
        return new ConfigSlurper('test' /* test environment */).parse(configFile.toURI().toURL())
    }

    def 'constructor – the SmiG config will be read correctly'() {
        given:
        ConfigObject config = createConfigFromString('''
smig.more.config = 'more Config'

environments {
    test {
        smig {
            is.fantastic = true
        }
    }
}
''')

        and:
        GrailsApplication grailsApplicationMock = Mock()
        _ * grailsApplicationMock.getConfig() >> config
        0 * _

        when:
        Smig smig = new Smig(grailsApplicationMock)

        then:
        smig.grailsApplication == grailsApplicationMock
        smig.config['smig.is.fantastic'] == true
        smig.config['smig.enabled'] == false
        smig.config['smig.more.config'] == 'more Config'
        smig.config.findAll({ it.key.startsWith('smig') }).size() == 3
    }

    // testing a private method
    def 'getMigrationsToRun – some migrations already run'() {
        given:
        new MigrationPlugin(fullName: 'com.smig.plugin.MigrateClass1', shortName: 'MigrateClass1',
                migrationDate: new Date()).save()
        new MigrationPlugin(fullName: 'com.smig.other.MigrateClass1', shortName: 'MigrateClass1',
                migrationDate: new Date()).save()
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        and: "mock the grails application, the original application won't find the migration artefacts"
        GrailsApplication grailsApplicationMock = Mock()
        _ * grailsApplicationMock.getArtefacts('Migration') >> ([new DefaultMigrationClass(MigrateClass1), new DefaultMigrationClass(MigrateClass2), new DefaultMigrationClass(Feature177)] as GrailsClass[])
        _ * grailsApplicationMock.getConfig() >> new ConfigObject()
        0 * _

        and:
        Smig smig = new Smig(grailsApplicationMock)

        when:
        List<MigrationClass> migrations = smig.getMigrationsToRun()

        then:
        migrations.collect { it.clazz } == [Feature177.class, MigrateClass2.class]
    }

    def 'getMigrationsToRun – matching migration classes to run'() {
        given: "create a config with included migration pattern"
        ConfigObject config = createConfigFromString(configString)

        and:
        GrailsApplication grailsApplicationMock = Mock()
        _ * grailsApplicationMock.getArtefacts('Migration') >> ([new DefaultMigrationClass(MigrateClass1), new DefaultMigrationClass(MigrateClass2), new DefaultMigrationClass(Feature177)] as GrailsClass[])
        _ * grailsApplicationMock.getConfig() >> config
        0 * _

        and:
        Smig smig = new Smig(grailsApplicationMock)

        when:
        List<MigrationClass> migrations = smig.getMigrationsToRun()

        then:
        migrations.collect { it.clazz } == matchingMigrations

        where:
        configString                                                                             || matchingMigrations
        'smig.included.migrations = []'                                                          || []
        'smig.included.migrations = null' /* NULL implements any {} and delivers always false */ || []
        'smig.included.migrations = [new com.smig.plugin.Feature177()]'                          || []
        'smig.included.migrations = ["com.smig.plugin"]'                                         || [Feature177, MigrateClass1, MigrateClass2]
        'smig.included.migrations = ["1"]'                                                       || [Feature177, MigrateClass1]
        'smig.included.migrations = [~/2/]'                                                      || [MigrateClass2]
        'smig.included.migrations = [~/e.*e/]'                                                   || [Feature177]
        'smig.included.migrations = [~/e.*e/, "ass1"]'                                           || [Feature177, MigrateClass1]
    }

    def 'getMigrationsToRun – invalid migration pattern'() {
        given: "create the invalid config"
        ConfigObject config = createConfigFromString('smig.included.migrations = java.util.Locale.JAPAN')

        and:
        GrailsApplication grailsApplicationMock = Mock()
        _ * grailsApplicationMock.getArtefacts('Migration') >> ([] as GrailsClass[])
        _ * grailsApplicationMock.getConfig() >> config
        0 * _

        and:
        Smig smig = new Smig(grailsApplicationMock)

        when:
        smig.getMigrationsToRun()

        then:
        thrown(IllegalStateException)
    }

    // testing a private method
    def 'getSql – creates a new SQL instance'() {
        given:
        Smig smig = getSmig()

        and:
        ApplicationContext applicationContext = grailsApplication.mainContext

        when:
        Sql sql = smig.getSql(applicationContext)

        then:
        sql != null
        sql.class == Sql.class
    }

    // testing a private method
    def 'createAutowiredMigrateObject – the migration class will be autowired with available services'() {
        given:
        Smig smig = getSmig()

        and:
        def migrationClass1 = new DefaultMigrationClass(MigrateClass1)
        def migrationClass2 = new DefaultMigrationClass(MigrateClass2)

        and:
        ApplicationContext applicationContext = grailsApplication.mainContext

        when:
        Migrate migrate1 = smig.createAutowiredMigrateObject(migrationClass1.fullName, applicationContext)

        then:
        migrate1 != null
        migrate1.class == MigrateClass1
        ((MigrateClass1) migrate1).dataSource != null

        when:
        Migrate migrate2 = smig.createAutowiredMigrateObject(migrationClass2.fullName, applicationContext)

        then:
        migrate2 != null
        migrate2.class == MigrateClass2
    }

    // testing a private method
    def "skipMigrations – do not run the migration if they are disabled in Config"() {
        given:
        Smig smig = getSmig()

        and:
        smig.metaClass.getConfig = { String a1, Object a2 ->
            return configValue
        }

        when:
        boolean result = smig.skipMigrations()

        then:
        skip == result

        where:
        configValue || skip
        null        || true
        true        || false
        false       || true
        ['list']    || true
        smig        || true
    }

    def 'doWithApplicationContext – happy path'() {
        given:
        ApplicationContext applicationContext = grailsApplication.mainContext

        and:
        Date currentDate = new Date()

        and: 'create config which runs migrations in tests'
        ConfigObject config = createConfigFromString('smig.enabled = true')

        and: "mock the grails application, the original application won't find the migration artefacts"
        GrailsApplication grailsApplicationMock = Mock()
        _ * grailsApplicationMock.getArtefacts('Migration') >> ([new DefaultMigrationClass(MigrateClass1)] as GrailsClass[])
        _ * grailsApplicationMock.getConfig() >> config
        0 * _

        and:
        Smig smig = new Smig(grailsApplicationMock)

        when:
        smig.doWithApplicationContext(applicationContext)

        then:
        MigrationPlugin.count() == 3

        when:
        List<MigrationPlugin> migrations = MigrationPlugin.list([sort: 'migrationDate'])
        MigrationPlugin migration1 = migrations[0]
        MigrationPlugin migration2 = migrations[1]
        MigrationPlugin migration3 = migrations[2]

        then:
        migration1.fullName == 'created.with.sql'
        migration2.fullName == 'created.with.gorm'

        and:
        migration3.fullName == 'com.smig.plugin.MigrateClass1'
        migration3.shortName == 'MigrateClass1'
        migration3.migrationDate >= currentDate
        migration3.migrationDate <= new Date()
    }

    def 'doWithApplicationContext – unhappy path'() {
        given:
        ApplicationContext applicationContext = grailsApplication.mainContext

        and: 'create config which runs migrations in tests'
        ConfigObject config = createConfigFromString('smig.enabled = true')

        and: "mock the grails application, the original application won't find the migration artefacts"
        GrailsApplication grailsApplicationMock = Mock()
        _ * grailsApplicationMock.getArtefacts('Migration') >> ([new DefaultMigrationClass(MigrateClass2)] as GrailsClass[])
        _ * grailsApplicationMock.getConfig() >> config
        0 * _

        and:
        Smig smig = new Smig(grailsApplicationMock)

        when:
        smig.doWithApplicationContext(applicationContext)

        then:
        thrown(NumberFormatException)
        MigrationPlugin.count() == 0
    }
}

class MigrateClass1 implements Migrate {

    DataSource dataSource

    void migrate(Sql sql) {
        // this will check if this method is called – sql will be a mock and commit() will be the verify call
        sql.executeInsert('INSERT INTO MIGRATION_PLUGIN ("FULL_NAME", "SHORT_NAME", "MIGRATION_DATE") VALUES (?, ?, ?)',
                ['created.with.sql', 'sql', new Date() - 2])

        // this will check if there is an open hibernate session
        new MigrationPlugin(fullName: 'created.with.gorm', shortName: 'gorm', migrationDate: new Date() - 1).save()
    }
}

class MigrateClass2 implements Migrate {

    void migrate(Sql sql) {
        throw new NumberFormatException()
    }
}

class Feature177 implements Migrate {

    void migrate(Sql sql) {}
}
