/*
 * Copyright (c) 2011 the original author or authors.
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
import groovy.sql.Sql
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.hibernate.SessionFactory
import org.junit.Test
import org.springframework.context.ApplicationContext

import javax.sql.DataSource

class SmigIntegrationTests extends GroovyTestCase {

    Smig smig = new Smig()

    GrailsApplication grailsApplication
    SessionFactory sessionFactory

    // testing a private method
    @Test
    void 'getMigrationsToRun'() {
        // given
        new MigrationPlugin(fullName: 'com.smig.plugin.MigrateClass1', shortName: 'MigrateClass1',
                migrationDate: new Date()).save()
        new MigrationPlugin(fullName: 'com.smig.other.MigrateClass1', shortName: 'MigrateClass1',
                migrationDate: new Date()).save()
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        // and: "mock the grails application, the original application won't find the migration artefacts"
        GrailsApplication grailsApplication = [
                getArtefacts: { String type ->
                    assert type == 'Migration'
                    return [new DefaultMigrationClass(MigrateClass1), new DefaultMigrationClass(MigrateClass2), new DefaultMigrationClass(Feature177)] as GrailsClass[]
                },
        ] as GrailsApplication

        // when
        List<MigrationClass> migrations = smig.getMigrationsToRun(grailsApplication)

        // then
        assert migrations.collect { it.clazz } == [Feature177.class, MigrateClass2.class]
    }

    // testing a private method
    @Test
    void 'getSql'() {
        // given
        ApplicationContext applicationContext = grailsApplication.mainContext

        // when
        Sql sql = smig.getSql(applicationContext)

        // then
        assert sql != null
        assert sql.class == Sql.class
    }

    // testing a private method
    @Test
    void 'createAutowiredMigrateObject'() {
        // given
        def migrationClass1 = new DefaultMigrationClass(MigrateClass1)
        def migrationClass2 = new DefaultMigrationClass(MigrateClass2)

        // and
        ApplicationContext applicationContext = grailsApplication.mainContext

        // when
        Migrate migrate1 = smig.createAutowiredMigrateObject(migrationClass1.fullName, applicationContext)

        // then
        assert migrate1 != null
        assert migrate1.class == MigrateClass1
        assert ((MigrateClass1) migrate1).dataSource != null

        // when
        Migrate migrate2 = smig.createAutowiredMigrateObject(migrationClass2.fullName, applicationContext)

        // then
        assert migrate2 != null
        assert migrate2.class == MigrateClass2
    }

    @Test
    void 'doWithApplicationContext – happy path'() {
        // given
        ApplicationContext applicationContext = grailsApplication.mainContext

        // and: "mock the grails application, the original application won't find the migration artefacts"
        GrailsApplication grailsApplication = [
                getArtefacts: { String type ->
                    return [new DefaultMigrationClass(MigrateClass1)] as GrailsClass[]
                },
        ] as GrailsApplication

        // and
        Date currentDate = new Date()

        // when
        smig.doWithApplicationContext(grailsApplication, applicationContext)

        // then
        assert MigrationPlugin.count() == 3

        // when
        List<MigrationPlugin> migrations = MigrationPlugin.list([sort: 'migrationDate'])
        MigrationPlugin migration1 = migrations[0]
        MigrationPlugin migration2 = migrations[1]
        MigrationPlugin migration3 = migrations[2]

        // then:
        assert migration1.fullName == 'created.with.sql'
        assert migration2.fullName == 'created.with.gorm'

        // and
        assert migration3.fullName == 'com.smig.plugin.MigrateClass1'
        assert migration3.shortName == 'MigrateClass1'
        assert migration3.migrationDate >= currentDate
        assert migration3.migrationDate <= new Date()
    }

    @Test
    void 'doWithApplicationContext – unhappy path'() {
        // given
        ApplicationContext applicationContext = grailsApplication.mainContext

        // and: "mock the grails application, the original application won't find the migration artefacts"
        GrailsApplication grailsApplication = [
                getArtefacts: { String type ->
                    return [new DefaultMigrationClass(MigrateClass2)] as GrailsClass[]
                },
        ] as GrailsApplication

        // when
        shouldFail(NumberFormatException) {
            smig.doWithApplicationContext(grailsApplication, applicationContext)
        }

        then:
        assert MigrationPlugin.count() == 0
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
