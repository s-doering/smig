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
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import groovy.sql.Sql
import org.junit.Test

@TestMixin(GrailsUnitTestMixin)
class MigrationArtefactHandlerTests {

    def artefactHandler = new MigrationArtefactHandler()

    @Test
    void 'isArtefactClass'() {

        // Test:
        assert false == artefactHandler.isArtefactClass(null)

        // Test:
        assert false == artefactHandler.isArtefactClass(TestClassNoMigrateInterface)

        // Test:
        assert false == artefactHandler.isArtefactClass(TestClassNoDefaultConstructor)

        // Test:
        assert false == artefactHandler.isArtefactClass(TestClassMultipleConstructors)

        // Test:
        assert false == artefactHandler.isArtefactClass(TestClassPrivateConstructor)

        // Test:
        assert true == artefactHandler.isArtefactClass(TestClassGoodMigrate1)

        // Test:
        assert true == artefactHandler.isArtefactClass(TestClassGoodMigrate2)
    }
}

class TestClassNoMigrateInterface {
}

class TestClassNoDefaultConstructor implements Migrate {

    TestClassNoDefaultConstructor(Sql sql) {}

    void migrate(Sql sql) {}
}

class TestClassMultipleConstructors implements Migrate {

    TestClassMultipleConstructors() {}

    TestClassMultipleConstructors(Sql sql) {}

    void migrate(Sql sql) {}
}

class TestClassPrivateConstructor implements Migrate {

    private TestClassPrivateConstructor() {}

    void migrate(Sql sql) {}
}

class TestClassGoodMigrate1 implements Migrate {
    void migrate(Sql sql) {}
}

class TestClassGoodMigrate2 implements Migrate {

    TestClassGoodMigrate2() {}

    void migrate(Sql sql) {}
}
