package org.osiam.addons.selfadministration.util;

import org.osiam.addons.selfadministration.plugin.api.Plugin;
import org.osiam.addons.selfadministration.plugin.api.RegistrationFailedException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by ukayan on 10/07/14.
 */
public class PluginHandler {
    private File pluginsJar;
    ClassLoader classLoader = null;
    

    public PluginHandler(String fileName, ClassLoader classLoaderParent) throws MalformedURLException {
        this.pluginsJar = new File(fileName);
        classLoader = URLClassLoader.newInstance(new URL[]{pluginsJar.toURL()}, classLoaderParent);

    }

    public void callPreRegCheck(String pluginName, String email) throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, RegistrationFailedException {
        Plugin plugin = (Plugin) classLoader.loadClass(pluginName).newInstance();
        plugin.performPreRegistrationCheck(email);

    }

}
