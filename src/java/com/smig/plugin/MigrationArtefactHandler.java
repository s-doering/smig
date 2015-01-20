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
package com.smig.plugin;

import com.smig.interfaces.Migrate;
import org.codehaus.groovy.grails.commons.ArtefactHandlerAdapter;

import java.lang.reflect.Constructor;

class MigrationArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "Migration";

    public MigrationArtefactHandler() {
        super(TYPE, MigrationClass.class, DefaultMigrationClass.class, null);
    }

    public boolean isArtefactClass(Class clazz) {

        // class shouldn't be null
        if (clazz == null) {
            return false;
        }

        // class should implement Migrate interface
        if ((Migrate.class.isAssignableFrom(clazz)) == false) {
            return false;
        }

        // class should only have a none argument constructor
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length != 1 || constructors[0].getParameterTypes().length > 0) {
            System.out.println("The migration class will be ignored. " +
                    "An migration plugin should only have the default constructor.");
            return false;
        }

        // this class is a migration artefact
        return true;
    }
}
