package org.motechproject.mds.domain;

import org.motechproject.mds.dto.SettingOptions;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * The <code>SettingOptionsMapping</code> contains single setting option for given {@link org.motechproject.mds.domain.TypeSettingsMapping}. This class is
 * related with table in database with the same name.
 */
@PersistenceCapable(identityType = IdentityType.DATASTORE, detachable = "true")
public class SettingOptionsMapping {

    @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
    @PrimaryKey
    private Long id;

    @Persistent
    private String name;

    @Persistent
    private TypeSettingsMapping typeSettings;

    public SettingOptionsMapping(SettingOptions option) {
        this(option.name());
    }

    public SettingOptionsMapping(String name) {
        this.name = name;
    }

    public SettingOptions toDto() {
        return SettingOptions.valueOf(name);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TypeSettingsMapping getTypeSettings() {
        return typeSettings;
    }

    public void setTypeSettings(TypeSettingsMapping typeSettings) {
        this.typeSettings = typeSettings;
    }

    public SettingOptionsMapping copy() {
        return new SettingOptionsMapping(name);
    }
}
