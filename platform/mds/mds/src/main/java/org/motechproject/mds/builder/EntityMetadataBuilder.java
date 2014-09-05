package org.motechproject.mds.builder;

import org.motechproject.mds.domain.ClassData;
import org.motechproject.mds.domain.Entity;
import org.motechproject.mds.domain.EntityType;

import javax.jdo.metadata.JDOMetadata;

/**
 * The <code>EntityMetadataBuilderImpl</code> class is responsible for building jdo metadata for an
 * entity class.
 */
public interface EntityMetadataBuilder {

    /**
     * Adds information about package and class name to a {@link javax.jdo.metadata.JDOMetadata}
     * instance.
     *
     * @param jdoMetadata a empty instance of {@link javax.jdo.metadata.JDOMetadata}.
     * @param entity      a instance of {@link org.motechproject.mds.domain.Entity}
     */
    void addEntityMetadata(JDOMetadata jdoMetadata, Entity entity);

    /**
     * Adds base information about package and class name to a
     * {@link javax.jdo.metadata.JDOMetadata} instance.
     *
     * @param jdoMetadata an empty instance of {@link javax.jdo.metadata.JDOMetadata}.
     * @param classData   an instance of {@link org.motechproject.mds.domain.ClassData}
     * @param entityType type of the entity(regular, history or trash)
     */
    void addBaseMetadata(JDOMetadata jdoMetadata, ClassData classData, EntityType entityType);

    /**
     * Creates metadata with basic information about package and class name to the
     * {@link javax.jdo.metadata.JDOMetadata} instance. Additionally, fetches fields
     * from passed entites and adds metadata for fields, if it's necessary. If entity
     * is null, it will work just like <code>addBaseMetadata(JDOMetadata, ClassData)</code>
     * and won't add any metadata for fields.
     *
     * @param jdoMetadata an empty instance of {@link javax.jdo.metadata.JDOMetadata}.
     * @param classData   an instance of {@link org.motechproject.mds.domain.ClassData}
     * @param entity      an entity to fetch fields from
     * @param entityType type of the entity(history or trash)
     */
    void addHelperClassMetadata(JDOMetadata jdoMetadata, ClassData classData, Entity entity,
                                EntityType entityType);

    /**
     * This updates the metadata after enhancement. Nucleus makes some "corrections" which do not work with
     * the runtime enhancer.
     */
    void fixEnhancerIssuesInMetadata(JDOMetadata jdoMetadata);
}
