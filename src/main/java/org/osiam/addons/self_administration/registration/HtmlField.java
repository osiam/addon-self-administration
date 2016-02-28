package org.osiam.addons.self_administration.registration;

public class HtmlField {

    private String name;
    private boolean required = false;
    private String type = "text";
    private boolean extension = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isExtension() {
        return extension;
    }

    public void setExtension(boolean isExtension) {
        this.extension = isExtension;
    }
}
