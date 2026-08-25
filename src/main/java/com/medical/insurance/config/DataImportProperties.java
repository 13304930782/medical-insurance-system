package com.medical.insurance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.data-import")
public class DataImportProperties {

    private boolean enabled;
    private String parameterFile;
    private String catalogFile;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getParameterFile() {
        return parameterFile;
    }

    public void setParameterFile(String parameterFile) {
        this.parameterFile = parameterFile;
    }

    public String getCatalogFile() {
        return catalogFile;
    }

    public void setCatalogFile(String catalogFile) {
        this.catalogFile = catalogFile;
    }
}
