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
import grails.util.Environment
import groovy.sql.Sql
import org.apache.commons.lang.time.StopWatch
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext

import javax.sql.DataSource

class Smig {

    private static final Logger log = Logger.getLogger(Smig.class)

    private GrailsApplication grailsApplication
    private Map<String, Object> config

    Smig(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
        this.config = enhanceApplicationConfigWithSmigDefaults(grailsApplication)
    }

    private Map<String, Object> enhanceApplicationConfigWithSmigDefaults(GrailsApplication grailsApplication) {

        log.debug('Loading config for migrations.')

        // load the default Smig config
        // inspired by Grails Quartz Plugin: http://grails.org/plugin/quartz
        ConfigObject config = grailsApplication.config.flatten(null)

        GroovyClassLoader classLoader = new GroovyClassLoader(getClass().classLoader)
        ConfigObject newConfig = new ConfigSlurper(Environment.getCurrent().getName())
                .parse(classLoader.loadClass('DefaultSmigConfig')).flatten(null)

        // Overwrite defaults with what Config.groovy has supplied, perhaps from external files
        return newConfig.merge(config)
    }

    void doWithApplicationContext(ApplicationContext applicationContext) {

        log.info('Starting migrations.')

        // check if the migrations should be skipped – depending on current Grails environment
        if (skipMigrations()) {
            log.info('The migrations will be skipped.')
            return
        }

        // the sql will be given into every migration instance
        Sql sql = getSql(applicationContext)

        // run each not yet executed migration
        List<MigrationClass> migrations = getMigrationsToRun()
        migrations.each { MigrationClass migration ->
            try {
                log.info("Running migration: ${migration.fullName}")
                StopWatch stopWatch = new StopWatch()

                // create a migration instance, the services will be injected
                Migrate migrate = createAutowiredMigrateObject(migration.fullName, applicationContext)

                // run the migration
                MigrationPlugin.withNewSession {
                    migrate.migrate(sql)
                }

                // the migration was successful, save it as executed – so it won't be executed
                // on next application start up
                Class<?> clazz = migrate.class
                MigrationPlugin.withTransaction {
                    new MigrationPlugin(fullName: clazz.name, shortName: clazz.simpleName,
                            migrationDate: new Date()).save(failOnError: true)
                }
                log.info("Migration \"${clazz.simpleName}\" done: ${stopWatch}")
            }
            catch (Exception e) {
                log.error('There was an error during migration.', e)
                throw e
            }
        }

        // thankfully all went well
        log.info('Finished migrations.')
    }

    private List<MigrationClass> getMigrationsToRun() {
        // get all already executed migrations – they won't be executed again
        List<String> executedMigrations = MigrationPlugin.withCriteria {
            projections { property('fullName') }
        }

        List<MigrationClass> migrations = grailsApplication.getArtefacts(MigrationArtefactHandler.TYPE)
                .findAll({ executedMigrations.contains(it.fullName) == false })
                .sort({ it.fullName })
        return migrations
    }

    private Sql getSql(ApplicationContext applicationContext) {
        DataSource dataSource = applicationContext.getBean('dataSource', DataSource)
        Sql sql = Sql.newInstance(dataSource)
        return sql
    }

    private Migrate createAutowiredMigrateObject(String fullName, ApplicationContext applicationContext) {
        ClassLoader clazzLoader = Thread.currentThread().getContextClassLoader()
        Class clazz = Class.forName(fullName, true, clazzLoader)
        Migrate migrate = applicationContext.autowireCapableBeanFactory.autowire(clazz,
                AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
        return migrate
    }

    private boolean skipMigrations() {
        Object config = getConfig('smig.enabled', true)
        return (toBoolean(config) == false)
    }

    private boolean toBoolean(Boolean value) {
        return value
    }

    private boolean toBoolean(Object ignored) {
        // always false – any Object other than Boolean is not allowed for a boolean config
        return false
    }

    private <T> T getConfig(String key, Object defaultValue) {
        return (config.containsKey(key) ? config[key] : defaultValue)
    }
}
