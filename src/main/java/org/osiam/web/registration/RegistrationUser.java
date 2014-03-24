package org.osiam.web.registration;

import org.osiam.resources.scim.User;


public class RegistrationUser extends User {

    private String confimPassword = "";
    private String userName02;
    private String userusername;
    

    
    
    @Override
    public String toString(){
        return userusername;
    }
    public RegistrationUser(){
        super();
    }

    public String getConfirmPassword(){
        return confimPassword;
    }
    public String getUserName02() {
        return userName02;
    }
    public void setUserName02(String userName02) {
        this.userName02 = userName02;
    }
    public String getUserusername() {
        return userusername;
    }

    

}
