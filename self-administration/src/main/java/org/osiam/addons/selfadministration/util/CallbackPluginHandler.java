package org.osiam.addons.selfadministration.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.osiam.addons.selfadministration.plugin.api.CallbackPlugin;
import org.osiam.addons.selfadministration.plugin.exception.CallbackException;
import org.osiam.resources.scim.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CallbackPluginHandler implements CallbackPlugin {
    @Value("${org.osiam.addon-self-administration.plugin.enabled}")
    private Boolean isPluginEnabled;

    @Value("${org.osiam.addon-self-administration.plugin.jar.path}")
    private String pluginJarPath;

    @Value("${org.osiam.addon-self-administration.plugin.classname}")
    private String pluginClass;

    private ClassLoader classLoader = null;
    private CallbackPlugin plugin = null;

    @Override
    public void performPreRegistrationActions(User user) throws CallbackException {
        if (isPluginEnabled) {
            getPlugin().performPreRegistrationActions(user);
        }
    }

    private synchronized CallbackPlugin getPlugin() {
        if (plugin == null) {
            try {

                plugin = (CallbackPlugin) getClassLoader().loadClass(pluginClass).getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return plugin;
    }

    private ClassLoader getClassLoader() {
        if (classLoader == null) {
            try {
                classLoader = URLClassLoader.newInstance(new URL[] { new File(pluginJarPath).toURI().toURL() },
                        getClass().getClassLoader());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        return classLoader;
    }

    @Override
    public void performPostRegistrationActions(User user) throws CallbackException {
        if (isPluginEnabled) {
            getPlugin().performPostRegistrationActions(user);
        }
    }
}
