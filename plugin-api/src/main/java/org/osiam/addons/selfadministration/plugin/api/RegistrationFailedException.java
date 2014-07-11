package org.osiam.addons.selfadministration.plugin.api;

/**
 * Created by ukayan on 11/07/14.
 */
public class RegistrationFailedException extends Exception{
    private String errorMessage;

    public RegistrationFailedException(String reason){
        this.errorMessage = reason;
    }

    public void setErrorMessage(String msg){
        this.errorMessage = msg;
    }

    public String getErrorMessage(){
        return this.errorMessage;
    }
}
