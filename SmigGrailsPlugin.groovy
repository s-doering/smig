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
import com.smig.plugin.MigrationArtefactHandler
import com.smig.plugin.Smig
import org.springframework.context.ApplicationContext

class SmigGrailsPlugin {

    def version = "1.0.0"

    def grailsVersion = "2.2 > *"

    def pluginExcludes = [
            "grails-app/migrations/**"
    ]

    def title = "Simple migrations in Grails"
    def author = "Steffen Döring"
    def authorEmail = "s.doering@wortzwei.de"
    def description = '''
A simple but yet powerful migration plugin for Grails going a more lightweight
direction than the standard "Grails Database Migration Plugin".
This plugin leaves the database creation and update process in hibernates
skillful hands, but having an easy way to add, change, or delete data on
applications startup. Need to access your services? No problem. Use them as
you are used to it in Grails.

Simply create a migration file for every feature / task / story / fairy tale
you develop, put your changes there and on next startup the changes will be
made – once.
No need to maintain (and merge) a changes file.
'''

    // URL to the plugin's documentation
    def documentation = "http://smig.com/plugin/smig"

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    def artefacts = [new MigrationArtefactHandler()]

    def doWithApplicationContext = { ApplicationContext applicationContext ->

        // start the migration procedure
        // (!!): outsourced to a class to make it testable
        new Smig().doWithApplicationContext(application, applicationContext)
    }
}
