package org.osiam.addons.selfadministration.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.osiam.addons.selfadministration.plugin.api.Plugin;
import org.osiam.addons.selfadministration.plugin.api.RegistrationFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PluginHandler implements Plugin {
    @Value("${org.osiam.addon-self-administration.plugin.enabled}")
    private Boolean isPluginEnabled;
    
    @Value("${org.osiam.addon-self-administration.plugin.jar.path}")
    private String pluginJarPath;

    @Value("${org.osiam.addon-self-administration.plugin.classname}")
    private String pluginClass;

    private ClassLoader classLoader = null;
    private Plugin plugin = null;

    @Override
    public void performPreRegistrationCheck(String name) throws RegistrationFailedException {
        if(isPluginEnabled){
            getPlugin().performPreRegistrationCheck(name);
        }
    }
    
    private Plugin getPlugin() {
        if (plugin == null) {
            try {
                
                plugin = (Plugin) getClassLoader().loadClass(pluginClass).getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e); 
            }
        }

        return plugin;
    }

    private ClassLoader getClassLoader() {
        if(classLoader == null){
            try {
                classLoader = URLClassLoader.newInstance(new URL[]{new File(pluginJarPath).toURI().toURL()}, getClass().getClassLoader());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        
        return classLoader;
    }

}
