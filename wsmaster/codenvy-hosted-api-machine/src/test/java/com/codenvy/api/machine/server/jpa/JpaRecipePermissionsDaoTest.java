/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.machine.server.jpa;

import com.codenvy.api.machine.server.recipe.RecipePermissionsImpl;
import com.codenvy.api.permission.server.PermissionsModule;
import com.codenvy.api.permission.server.jpa.SystemPermissionsJpaModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.persist.jpa.JpaPersistModule;

import org.eclipse.che.api.core.jdbc.jpa.eclipselink.EntityListenerInjectionManagerInitializer;
import org.eclipse.che.api.core.jdbc.jpa.guice.JpaInitializer;
import org.eclipse.che.api.machine.server.event.BeforeRecipeRemovedEvent;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.commons.test.tck.repository.JpaTckRepository;
import org.eclipse.che.commons.test.tck.repository.TckRepository;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.TRANSACTION_TYPE;
import static org.testng.Assert.assertTrue;

/**
 * @author Max Shaposhnik
 */
public class JpaRecipePermissionsDaoTest {

    private EntityManager manager;

    private JpaRecipePermissionsDao dao;


    private JpaRecipePermissionsDao.RemovePermissionsBeforeRecipeRemovedEventSubscriber removePermissionsBeforeRecipeRemovedEventSubscriber;


    RecipePermissionsImpl[] permissionses;

    UserImpl[] users;

    RecipeImpl[] recipes;

    @BeforeClass
    public void setupEntities() throws Exception {
        permissionses = new RecipePermissionsImpl[] {new RecipePermissionsImpl("user1", "recipe1", Arrays.asList("read", "use", "run")),
                                                     new RecipePermissionsImpl("user2", "recipe1", Arrays.asList("read", "use")),
                                                     new RecipePermissionsImpl("user1", "recipe2", Arrays.asList("read", "run")),
                                                     new RecipePermissionsImpl("user2", "recipe2",
                                                                               Arrays.asList("read", "use", "run", "configure"))};

        users = new UserImpl[] {new UserImpl("user1", "user1@com.com", "usr1"),
                                new UserImpl("user2", "user2@com.com", "usr2")};

        recipes = new RecipeImpl[] {
                new RecipeImpl("recipe1", "rc1", null, null, null, null, null),
                new RecipeImpl("recipe2", "rc2", null, null, null, null, null)};

        Injector injector =
                Guice.createInjector(new TestModule(), new OnPremisesJpaMachineModule(), new PermissionsModule(),
                                     new SystemPermissionsJpaModule());
        manager = injector.getInstance(EntityManager.class);
        dao = injector.getInstance(JpaRecipePermissionsDao.class);
        removePermissionsBeforeRecipeRemovedEventSubscriber = injector.getInstance(
                JpaRecipePermissionsDao.RemovePermissionsBeforeRecipeRemovedEventSubscriber.class);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        manager.getTransaction().begin();
        for (UserImpl user : users) {
            manager.persist(user);
        }

        for (RecipeImpl recipe : recipes) {
            manager.persist(recipe);
        }

        for (RecipePermissionsImpl recipePermissions : permissionses) {
            manager.persist(recipePermissions);
        }
        manager.getTransaction().commit();
        manager.clear();
    }

    @AfterMethod
    public void cleanup() {
        manager.getTransaction().begin();

        manager.createQuery("SELECT p FROM RecipePermissions p", RecipePermissionsImpl.class)
               .getResultList()
               .forEach(manager::remove);

        manager.createQuery("SELECT r FROM Recipe r", RecipeImpl.class)
               .getResultList()
               .forEach(manager::remove);

        manager.createQuery("SELECT u FROM Usr u", UserImpl.class)
               .getResultList()
               .forEach(manager::remove);
        manager.getTransaction().commit();
    }

    @AfterClass
    public void shutdown() throws Exception {
        manager.getEntityManagerFactory().close();
    }

    @Test
    public void shouldRemoveWorkersWhenWorkspaceIsRemoved() throws Exception {
        BeforeRecipeRemovedEvent event = new BeforeRecipeRemovedEvent(recipes[0]);
        removePermissionsBeforeRecipeRemovedEventSubscriber.onEvent(event);
        assertTrue(dao.getByInstance("recipe1").isEmpty());
    }

    @Test
    public void shouldGetRecipePermissionByInstanceIdAndWildcard() throws Exception {
        manager.getTransaction().begin();
        manager.persist(new RecipePermissionsImpl(null, "recipe1", asList("read", "use", "run")));
        manager.getTransaction().commit();

        RecipePermissionsImpl result = dao.get("*", "recipe1");
        assertEquals(result.getInstanceId(), "recipe1");
        assertEquals(result.getUserId(), null);
    }


    @Test
    public void shouldGetRecipePermissionByInstanceIdAndUserIdIfPublicPermissionExistsWithSameInstanceId() throws Exception {
        manager.getTransaction().begin();
        manager.persist(new RecipePermissionsImpl(null, "recipe1", asList("read", "use", "run")));
        manager.getTransaction().commit();

        RecipePermissionsImpl result = dao.get("user1", "recipe1");
        assertEquals(result.getInstanceId(), "recipe1");
        assertEquals(result.getUserId(), "user1");
    }

    @Test
    public void shouldStoreRecipePublicPermission() throws Exception {
        final RecipePermissionsImpl publicPermission = new RecipePermissionsImpl("*",
                                                                                 "recipe1",
                                                                                 asList("read", "use", "run"));
        dao.store(publicPermission);

        assertTrue(dao.getByInstance(publicPermission.getInstanceId())
                      .contains(publicPermission));
    }

    @Test
    public void shouldUpdateExistingRecipePublicPermissions() throws Exception {
        final RecipePermissionsImpl publicPermission = new RecipePermissionsImpl("*",
                                                                                 "recipe1",
                                                                                 asList("read", "use", "run"));
        dao.store(publicPermission);
        dao.store(publicPermission);

        List<RecipePermissionsImpl> byInstance = dao.getByInstance(publicPermission.getInstanceId());
        assertTrue(byInstance.contains(publicPermission));
        assertTrue(byInstance.stream().filter(p -> "*".equals(p.getUserId())).count() == 1);
    }

    @Test
    public void shouldRemoveRecipePublicPermission() throws Exception {
        final RecipePermissionsImpl publicPermission = new RecipePermissionsImpl("*",
                                                                                 "recipe1",
                                                                                 asList("read", "use", "run"));
        dao.store(publicPermission);
        dao.remove(publicPermission.getUserId(), publicPermission.getInstanceId());

        List<RecipePermissionsImpl> byInstance = dao.getByInstance(publicPermission.getInstanceId());
        assertTrue(byInstance.stream().filter(p -> "*".equals(p.getUserId())).count() == 0);
    }


    private class TestModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(new TypeLiteral<TckRepository<RecipePermissionsImpl>>() {}).toInstance(new JpaTckRepository<>(RecipePermissionsImpl.class));
            bind(new TypeLiteral<TckRepository<UserImpl>>() {}).toInstance(new JpaTckRepository<>(UserImpl.class));
            bind(new TypeLiteral<TckRepository<RecipeImpl>>() {}).toInstance(new JpaTckRepository<>(RecipeImpl.class));

            Map properties = new HashMap();
            if (System.getProperty("jdbc.driver") != null) {
                properties.put(TRANSACTION_TYPE,
                               PersistenceUnitTransactionType.RESOURCE_LOCAL.name());

                properties.put(JDBC_DRIVER, System.getProperty("jdbc.driver"));
                properties.put(JDBC_URL, System.getProperty("jdbc.url"));
                properties.put(JDBC_USER, System.getProperty("jdbc.user"));
                properties.put(JDBC_PASSWORD, System.getProperty("jdbc.password"));
            }

            JpaPersistModule main = new JpaPersistModule("main");
            main.properties(properties);
            install(main);
            bind(JpaInitializer.class).asEagerSingleton();
            bind(EntityListenerInjectionManagerInitializer.class).asEagerSingleton();
        }
    }

}
