package org.motechproject.mds.service.impl;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.eclipse.gemini.blueprint.util.OsgiBundleUtils;
import org.motechproject.bundle.extender.MotechOsgiConfigurableApplicationContext;
import org.motechproject.mds.MDSDataProvider;
import org.motechproject.mds.builder.MDSConstructor;
import org.motechproject.mds.domain.ClassData;
import org.motechproject.mds.domain.EntityInfo;
import org.motechproject.mds.ex.MdsException;
import org.motechproject.mds.javassist.JavassistHelper;
import org.motechproject.mds.javassist.MotechClassPool;
import org.motechproject.mds.repository.MetadataHolder;
import org.motechproject.mds.service.BaseMdsService;
import org.motechproject.mds.service.JarGeneratorService;
import org.motechproject.mds.util.ClassName;
import org.motechproject.osgi.web.util.BundleHeaders;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.velocity.VelocityEngineUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static java.util.jar.Attributes.Name;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.motechproject.mds.util.Constants.BundleNames.MDS_ENTITIES_SYMBOLIC_NAME;
import static org.motechproject.mds.util.Constants.Manifest.BUNDLE_MANIFESTVERSION;
import static org.motechproject.mds.util.Constants.Manifest.BUNDLE_NAME_SUFFIX;
import static org.motechproject.mds.util.Constants.Manifest.MANIFEST_VERSION;
import static org.motechproject.mds.util.Constants.Manifest.SYMBOLIC_NAME_SUFFIX;

/**
 * Default implementation of {@link org.motechproject.mds.service.JarGeneratorService} interface.
 */
@Service
public class JarGeneratorServiceImpl extends BaseMdsService implements JarGeneratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JarGeneratorServiceImpl.class);

    private final Object generationLock = new Object();

    private BundleHeaders bundleHeaders;
    private BundleContext bundleContext;
    private MetadataHolder metadataHolder;
    private MDSConstructor mdsConstructor;
    private VelocityEngine velocityEngine;
    private MDSDataProvider mdsDataProvider;

    @Override
    @Transactional
    public void regenerateMdsDataBundle(boolean buildDDE) {
        LOGGER.info("Regenerating the mds entities bundle");

        mdsConstructor.constructEntities(buildDDE);
        mdsDataProvider.updateDataProvider();

        File dest = new File(bundleLocation());
        if (dest.exists()) {
            // proceed when the bundles context is ready, we want the context processors to finish
            waitForEntitiesContext();
        }

        File tmpBundleFile;

        try {
            tmpBundleFile = generate();
        } catch (IOException | NotFoundException | CannotCompileException e) {
            throw new MdsException("Unable to generate entities bundle", e);
        }

        synchronized (generationLock) {
            Bundle dataBundle = OsgiBundleUtils.findBundleBySymbolicName(bundleContext, createSymbolicName());

            try (InputStream in = new FileInputStream(tmpBundleFile)) {
                FileUtils.deleteQuietly(dest);
                FileUtils.moveFile(tmpBundleFile, dest);

                if (dataBundle == null) {
                    LOGGER.info("Creating the entities bundle");
                    dataBundle = bundleContext.installBundle(bundleLocation(), in);
                } else {
                    LOGGER.info("Updating the entities bundle");
                    dataBundle.stop();
                    dataBundle.update(in);
                }

                LOGGER.info("Starting the entities bundle");
                dataBundle.start();
            } catch (IOException e) {
                throw new MdsException("Unable to read temporary entities bundle", e);
            } catch (BundleException e) {
                throw new MdsException("Unable to start the entities bundle", e);
            }
        }
    }

    @Override
    @Transactional
    public File generate() throws IOException, NotFoundException, CannotCompileException {
        Path tempDir = Files.createTempDirectory("mds");
        Path tempFile = Files.createTempFile(tempDir, "mds-entities", ".jar");

        java.util.jar.Manifest manifest = createManifest();
        FileOutputStream fileOutput = new FileOutputStream(tempFile.toFile());

        try (JarOutputStream output = new JarOutputStream(fileOutput, manifest)) {
            List<EntityInfo> information = new ArrayList<>();

            for (ClassData classData : MotechClassPool.getEnhancedClasses(false)) {
                String className = classData.getClassName();
                EntityInfo info = new EntityInfo();
                info.setClassName(className);

                // insert entity class, only for EUDE, note that this can also be a generated enum class
                if (!classData.isDDE()) {
                    addEntry(output, JavassistHelper.toClassPath(className), classData.getBytecode());
                }

                // insert history and trash classes, these classes will not be present for enums
                ClassData historyClassData = MotechClassPool.getHistoryClassData(className);
                if (historyClassData != null) {
                    addEntry(output, JavassistHelper.toClassPath(historyClassData.getClassName()),
                            historyClassData.getBytecode());
                }

                ClassData trashClassData = MotechClassPool.getTrashClassData(className);
                if (trashClassData != null) {
                    addEntry(output, JavassistHelper.toClassPath(trashClassData.getClassName()),
                           trashClassData.getBytecode());
                }

                // insert repository
                String repositoryName = MotechClassPool.getRepositoryName(className);
                if (addInfrastructure(output, repositoryName)) {
                    info.setRepository(repositoryName);
                }

                // insert service implementation
                String serviceName = MotechClassPool.getServiceImplName(className);
                if (addInfrastructure(output, serviceName)) {
                    info.setServiceName(serviceName);
                }

                // insert the interface
                String interfaceName = MotechClassPool.getInterfaceName(className);
                if (addInfrastructure(output, interfaceName)) {
                    info.setInterfaceName(interfaceName);
                }

                information.add(info);
            }

            String blueprint = mergeTemplate(information, BLUEPRINT_TEMPLATE);
            String context = mergeTemplate(information, MDS_ENTITIES_CONTEXT_TEMPLATE);

            addEntry(output, PACKAGE_JDO, metadataHolder.getJdoMetadata().toString().getBytes());
            addEntry(output, BLUEPRINT_XML, blueprint.getBytes());
            addEntry(output, MDS_ENTITIES_CONTEXT, context.getBytes());
            addEntry(output, MDS_COMMON_CONTEXT);
            addEntry(output, DATANUCLEUS_PROPERTIES);
            addEntry(output, MOTECH_MDS_PROPERTIES);

            return tempFile.toFile();
        }
    }

    private boolean addInfrastructure(JarOutputStream output, String name) {
        CtClass clazz = MotechClassPool.getDefault().getOrNull(name);
        boolean added = false;

        if (null != clazz) {
            try {
                addEntry(output, JavassistHelper.toClassPath(name), clazz.toBytecode());
                added = true;
            } catch (IOException | CannotCompileException e) {
                LOGGER.error("There were problems with adding entry: ", e);
                added = false;
            }
        }

        return added;
    }

    private void addEntry(JarOutputStream output, String name) throws IOException {
        addEntry(output, name, null);
    }

    private void addEntry(JarOutputStream output, String name, byte[] bytes) throws IOException {
        JarEntry entry = new JarEntry(name);

        output.putNextEntry(entry);

        if (null != bytes) {
            output.write(bytes);
        } else {
            writeResourceToStream(name, output);
        }

        output.closeEntry();
    }

    private String mergeTemplate(List<EntityInfo> information, String templatePath) {
        StringWriter writer = new StringWriter();
        Map<String, Object> model = new HashMap<>();
        model.put("StringUtils", StringUtils.class);
        model.put("list", information);

        try {
            VelocityEngineUtils.mergeTemplate(velocityEngine, templatePath, model, writer);
        } catch (Exception e) {
            LOGGER.error("An exception occurred, while trying to load" + templatePath + " template and merge it with data", e);
        }

        return writer.toString();
    }

    private java.util.jar.Manifest createManifest() throws IOException {
        java.util.jar.Manifest manifest = new java.util.jar.Manifest();
        Attributes attributes = manifest.getMainAttributes();

        String exports = createExportPackage(org.motechproject.mds.util.Constants.PackagesGenerated.ENTITY, org.motechproject.mds.util.Constants.PackagesGenerated.SERVICE);

        // standard attributes
        attributes.put(Name.MANIFEST_VERSION, MANIFEST_VERSION);

        // osgi attributes
        attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, BUNDLE_MANIFESTVERSION);
        attributes.putValue(Constants.BUNDLE_NAME, createName());
        attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, createSymbolicName());
        attributes.putValue(Constants.BUNDLE_VERSION, bundleHeaders.getVersion());
        attributes.putValue(Constants.EXPORT_PACKAGE, exports);
        attributes.putValue(Constants.IMPORT_PACKAGE, getImports());
        return manifest;
    }

    private String createName() {
        return String.format("%s%s", bundleHeaders.getName(), BUNDLE_NAME_SUFFIX);
    }

    private String createSymbolicName() {
        return String.format("%s%s", bundleHeaders.getSymbolicName(), SYMBOLIC_NAME_SUFFIX);
    }

    private String bundleLocation() {
        Path path = FileSystems.getDefault().getPath(System.getProperty("user.home"), ".motech/bundles", "mds-entities.jar");
        return path.toAbsolutePath().toString();
    }

    private String createExportPackage(String... packages) {
        StringBuilder builder = new StringBuilder();
        String prefix = "";

        for (String pack : packages) {
            builder.append(prefix);
            builder.append(pack);
            builder.append(";");
            builder.append(Constants.VERSION_ATTRIBUTE);
            builder.append("=");
            builder.append(bundleHeaders.getVersion());

            if (isBlank(prefix)) {
                prefix = ",";
            }
        }

        return builder.toString();
    }

    private String loadResourceAsString(String resource) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            return IOUtils.toString(in);
        }
    }

    private void writeResourceToStream(String resource, OutputStream output) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            IOUtils.copy(in, output);
        }
    }

    private String getImports() throws IOException {
        // first load the standard imports
        String stdImports = loadResourceAsString(BUNDLE_IMPORTS).replaceAll("\\r|\\n", "");

        // we want to prevent duplicate imports
        StringBuilder sb = new StringBuilder(stdImports);
        Set<String> alreadyImported = new HashSet<>();

        // add imports for DDE classes
        for (ClassData classData : MotechClassPool.getEnhancedClasses(false)) {
            if (classData.isDDE()) {
                String pkg = ClassName.getPackage(classData.getClassName());
                if (!alreadyImported.contains(pkg)) {
                    sb.append(',').append(pkg);
                    alreadyImported.add(pkg);
                }
            }
        }

        return sb.toString();
    }

    private void waitForEntitiesContext() {
        MotechOsgiConfigurableApplicationContext entitiesContext = null;

        try {
            ServiceReference[] references =
                    bundleContext.getAllServiceReferences(ApplicationContext.class.getName(), null);

            for (ServiceReference ref : references) {
                if (MDS_ENTITIES_SYMBOLIC_NAME.equals(ref.getBundle().getSymbolicName())) {
                    Object ctx = bundleContext.getService(ref);
                    if (ctx instanceof MotechOsgiConfigurableApplicationContext) {
                        entitiesContext = (MotechOsgiConfigurableApplicationContext) ctx;
                        break;
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Invalid syntax expression when retrieving the entities context", e);
        }

        if (entitiesContext != null) {
            entitiesContext.waitForContext(5000);
        }
    }

    @Autowired
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.bundleHeaders = new BundleHeaders(bundleContext);
    }

    @Autowired
    public void setMetadataHolder(MetadataHolder metadataHolder) {
        this.metadataHolder = metadataHolder;
    }

    @Autowired
    public void setMdsConstructor(MDSConstructor mdsConstructor) {
        this.mdsConstructor = mdsConstructor;
    }

    @Resource(name = "mdsVelocityEngine")
    public void setVelocityEngine(VelocityEngine velocityEngine) {
        this.velocityEngine = velocityEngine;
    }

    @Autowired
    public void setMdsDataProvider(MDSDataProvider mdsDataProvider) {
        this.mdsDataProvider = mdsDataProvider;
    }
}
