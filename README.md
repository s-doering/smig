# Simple migrations in Grails. #

## Introduction ##

A simple but yet powerful migration plugin for Grails going a more lightweight direction than the standard "Grails Database Migration Plugin".

This plugin leaves the database creation and update process in hibernates (more or less) skillful hands, but having an easy way to add, change, or delete data on applications startup. Need to access your services? No problem. Use them as you are used to it in Grails.  

Simply create a migration file for every feature / task / story / fairy tale you develop, put your changes there and on next startup the changes will be made – once.  
No need to maintain (and merge) a changes file.


## Usage ##

1. Add this line to the plugins section of your applications BuildConfig.groovy __(not that easy yet, see below*)__

        plugins {

            ..

            runtime ":smig:1.2.0"

            ..
            ..
        }

2. Refresh your dependencies

        grails refresh-dependencies

3. After refreshing, there will be a new folder in your project

        ./grails-app/migrations
   
   Create a new groovy file in that new folder. You can use package structure if you like.  
   This new file has to implement the **Migrate** interface (com.smig.interfaces.Migrate). Your IDE will generate the **migrate(Sql)** method you'll need to implement.  
   Here is an example of what this migration file can look like.
   
        class FeatureXmas implements Migrate {
    
            UserService userService

            void migrate(Sql sql) {
    
                User.list().each {
                    boolean friendly = userService.isUserFriendly(it)
                    sql.executeUpdate('UPDATE user SET xmas_gifts=? WHERE id=?', [friendly, it.id])
                }
            }
        }
    
You can add as many migration files as you like. All _new_ migrations will run on next application startup.


## Roadmap ##

##### _v1.0 (implemented)_ #####

_Initial running and working release._

##### _v1.1 (implemented)_ #####

_Exclude migration from running in certain environments via config._  
_Disable migrations running in TEST environment by default._

##### _v1.2 (implemented)_ #####

_Specify packages in config to run in certain environments._

##### __v1.3 (next)__ #####

Enhance Grails compability down to version Grails 2.0 – currently Grails 2.2.  
Internal: Make Spock test framework to work within plugin tests.  
Internal: Inventing a cool smig-ish icon to enhance recognition value.

##### v2.0 #####

Add possibility of a depending migration running before an other migration. _The "migration 2" dependsOn "migration 1"._


## Detailed information ##

#### Exclude environments from running migrations ####

By default migrations run in every environment except _TEST_. You can change this behaviour by adding a config. You can make it environment specific, e.g.:

        smig.enabled = true

        environments {

            develop {

                smig.enabled = false

            }
        }

This will run migrations in every environment except _DEVELOP_.

#### Restrict migrations to specific environments ####

By default all found, not yet run migrations will be executed. You can change this behaviour by specifying some "patterns" applying to the full migration class names.

        smig.included.migrations = ['com.project.migrations.production']

        environments {

            test {

                smig.enabled = true // is disabled by default
                smig.included.migrations = ['com.project.migrations.integration']

            }

            custom-env {

                // example with regex pattern
                smig.included.migrations = ['com.project.migrations.database', ~/[Cc]ustom/]

            }
        }

This config specifies the packages for the migrations to run. Using package names is just a recommendation. All migrations will be found

1. containing _(not beginning)_ the specified Strings

2. finding _(not matching)_ the specified Pattern (java.util.regex.Pattern)

__Beware:__  
The config _smig.included.migrations_ has to be a collection. Any other kind of object will fail and abort the application with a meaningful error.  
Setting _null_ or an empty list will result in never finding any migrations.

#### Logging Migrations ####

There is logging for migrations as well. Add this line to your log4j config.

        log4j = {

            ..

            info 'com.smig.plugin'

            ..
            ..
        }

"debug" is also possible to squeeze some more information out of it.

#### Order of executed migrations ####

If there is more than one migration file the executed order results from sorting the full qualified class name.

#### __*__ Add the plugin to your local repository ####

Unfortunately it's not that easy yet. Unless this plugin is not hosted on Grails Centrals repository, you have to clone this git repository by doing the following commands:

        git clone https://github.com/s-doering/smig.git
        cd smig.git
        grails clean
        grails compile
        grails maven-install

This will generate the plugin on your local harddrive.

If you have an artifactory yourself. You can add it there, too.

