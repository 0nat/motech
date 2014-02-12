package org.motechproject.mds.builder;

import org.apache.commons.beanutils.PropertyUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.motechproject.commons.date.model.Time;
import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.mds.BaseIT;
import org.motechproject.mds.domain.Entity;
import org.motechproject.mds.dto.EntityDto;
import org.motechproject.mds.javassist.MotechClassPool;
import org.motechproject.mds.repository.AllEntities;
import org.motechproject.mds.repository.MetadataHolder;
import org.motechproject.mds.repository.MotechDataRepository;
import org.motechproject.mds.service.impl.DefaultMotechDataService;
import org.motechproject.mds.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jdo.Query;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.motechproject.mds.testutil.FieldTestHelper.field;

public class MDSConstructorIT extends BaseIT {
    private static final String SIMPLE_NAME = "Constructor";
    private static final String CLASS_NAME = String.format("%s.%s", Constants.PackagesGenerated.ENTITY, SIMPLE_NAME);
    private static final String REPOSITORY_NAME = String.format("%s.All%ss", Constants.PackagesGenerated.REPOSITORY, SIMPLE_NAME);
    private static final String INTERFACE_NAME = String.format("%s.%sService", Constants.PackagesGenerated.SERVICE, SIMPLE_NAME);
    private static final String SERVICE_NAME = String.format("%s.%sServiceImpl", Constants.PackagesGenerated.SERVICE_IMPL, SIMPLE_NAME);
    private static final String ENTITY_WITH_FIELDS = String.format("%s.EntityWithFields", Constants.PackagesGenerated.ENTITY);

    @Autowired
    private MDSConstructor constructor;

    @Autowired
    private AllEntities allEntities;

    @Autowired
    private MetadataHolder metadataHolder;

    @Before
    public void setUp() throws Exception {
        allEntities.create(new EntityDto(CLASS_NAME));
        metadataHolder.reloadMetadata();
    }

    @After
    public void tearDown() throws Exception {
        clearDB();
        MotechClassPool.unregisterEnhancedData(CLASS_NAME);
        MotechClassPool.unregisterEnhancedData(ENTITY_WITH_FIELDS);
    }

    @Test
    public void testConstructEntity() throws Exception {
        Entity temp = new Entity();
        temp.setClassName(CLASS_NAME);

        constructor.constructEntity(temp);

        MDSClassLoader mdsClassLoader = MDSClassLoader.getInstance();

        Class<?> entityClass = mdsClassLoader.loadClass(CLASS_NAME);
        Class<?> repositoryClass = mdsClassLoader.loadClass(REPOSITORY_NAME);
        Class<?> interfaceClass = mdsClassLoader.loadClass(INTERFACE_NAME);
        Class<?> serviceClass = mdsClassLoader.loadClass(SERVICE_NAME);

        assertNotNull(entityClass);
        assertNotNull(repositoryClass);
        assertNotNull(interfaceClass);
        assertNotNull(serviceClass);

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // This is a hack for this test, normally the metadata lives along the class in a separate bundle
            // but for this test we use the current PersistenceManagerFactory for persisting
            Thread.currentThread().setContextClassLoader(mdsClassLoader);
            getPersistenceManagerFactory().registerMetadata(metadataHolder.getJdoMetadata());

            MotechDataRepository repository = (MotechDataRepository) repositoryClass.newInstance();
            DefaultMotechDataService service = (DefaultMotechDataService) serviceClass.newInstance();

            repository.setPersistenceManagerFactory(getPersistenceManagerFactory());
            service.setRepository(repository);

            Object Voucher = service.create(entityClass.newInstance());
            assertEquals(1, service.retrieveAll().size());

            service.delete(Voucher);
            assertTrue(service.retrieveAll().isEmpty());
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Test
    public void shouldConstructEntityWithFields() throws Exception {
        Entity entity = new Entity(ENTITY_WITH_FIELDS);
        entity.setFields(asList(field("fieldInt", Integer.class), field("fieldStr", String.class),
                field("fieldDouble", Double.class), field("fieldBool", Boolean.class),
                field("fieldDate", Date.class), field("fieldDateTime", DateTime.class),
                field("fieldTime", Time.class), field("fieldList", List.class)));

        constructor.constructEntity(entity);

        MDSClassLoader mdsClassLoader = MDSClassLoader.getInstance();

        Class<?> entityClass = mdsClassLoader.loadClass(ENTITY_WITH_FIELDS);
        Object instance = entityClass.newInstance();

        Date date = new Date();
        DateTime dateTime = DateUtil.now();

        PropertyUtils.setProperty(instance, "fieldInt", 17);
        PropertyUtils.setProperty(instance, "fieldDouble", 3.14);
        PropertyUtils.setProperty(instance, "fieldStr", "Hello world!");
        PropertyUtils.setProperty(instance, "fieldBool", true);
        PropertyUtils.setProperty(instance, "fieldDate", date);
        PropertyUtils.setProperty(instance, "fieldDateTime", dateTime);
        PropertyUtils.setProperty(instance, "fieldTime", new Time(17, 19));
        PropertyUtils.setProperty(instance, "fieldList", asList("one", "two"));

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // This is a hack for this test, normally the metadata lives along the class in a separate bundle
            // but for this test we use the current PersistenceManagerFactory for persisting
            Thread.currentThread().setContextClassLoader(mdsClassLoader);
            getPersistenceManagerFactory().registerMetadata(metadataHolder.getJdoMetadata());

            getPersistenceManager().makePersistent(instance);

            Query query = getPersistenceManager().newQuery(entityClass);
            query.setUnique(true);
            Object fromDb = query.execute();

            assertNotNull(fromDb);
            assertEquals(entityClass, fromDb.getClass());

            assertEquals(17, PropertyUtils.getProperty(fromDb, "fieldInt"));
            assertEquals(3.14, PropertyUtils.getProperty(fromDb, "fieldDouble"));
            assertEquals("Hello world!", PropertyUtils.getProperty(fromDb, "fieldStr"));
            assertEquals(true, PropertyUtils.getProperty(fromDb, "fieldBool"));
            assertEquals(date, PropertyUtils.getProperty(fromDb, "fieldDate"));
            assertEquals(dateTime, PropertyUtils.getProperty(fromDb, "fieldDateTime"));
            assertEquals(new Time(17, 19), PropertyUtils.getProperty(fromDb, "fieldTime"));
            assertEquals(asList("one", "two"), PropertyUtils.getProperty(fromDb, "fieldList"));
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
}