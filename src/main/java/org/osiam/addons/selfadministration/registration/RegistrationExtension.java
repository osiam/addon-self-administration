package org.osiam.addons.selfadministration.registration;

import java.util.Map;

public class RegistrationExtension {

    private Map<String, String> fields;
    
    public RegistrationExtension() {
        
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }
}
