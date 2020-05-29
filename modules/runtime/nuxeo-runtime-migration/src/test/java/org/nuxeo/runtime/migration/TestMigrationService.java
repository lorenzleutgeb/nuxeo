/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nour AL KOTOB
 */

package org.nuxeo.runtime.migration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.kv")
@Deploy("org.nuxeo.runtime.cluster")
@Deploy("org.nuxeo.runtime.migration:OSGI-INF/migration-service.xml")
@Deploy("org.nuxeo.runtime.migration.tests:OSGI-INF/dummy-migration.xml")
public class TestMigrationService {

    @Inject
    protected MigrationService migrationService;

    @Test
    public void testGetMigration() {
        checkMigration(migrationService.getMigration("dummy-migration"));
    }

    @Test
    public void testGetMigrations() {
        var migrations = migrationService.getMigrations();
        assertEquals(1, migrations.size());
        checkMigration(migrations.get(0));
    }

    @Test
    public void testUnknownMigration() {
        assertEquals(null, migrationService.getMigration("fake"));
    }

    @Test
    public void testRunMigration() {
        String dummy = "dummy-migration";
        assertEquals("before", migrationService.getMigration(dummy).getStatus().getState());
        migrationService.probeAndRun(dummy);
        await().atMost(1, SECONDS)
               .until(() -> migrationService.getMigration(dummy).getStatus().getState().equals("after"));
        try {
            migrationService.probeAndRun(dummy);
            fail("should fail");
        } catch (java.lang.IllegalStateException e) {
            assertEquals("Migration dummy-migration needs to have exactly one step to run", e.getMessage());
        }
    }

    protected void checkMigration(Migration migration) {
        assertEquals("dummy-migration", migration.getId());
        assertEquals("Dummy Migration", migration.getDescription());
        assertEquals("migration.dummy", migration.getLabel());
        assertEquals("before", migration.getStatus().getState());
        assertEquals("Migrate dummy state from before to after",
                migration.getSteps().get("before-to-after").getDescription());
    }

}
