package org.osiam.addons.self_administration.util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.osiam.addons.self_administration.plugin_api.CallbackException;
import org.osiam.addons.self_administration.plugin_api.CallbackPlugin;
import org.osiam.resources.scim.User;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
public class CallbackPluginHandler implements CallbackPlugin, InitializingBean {

    @Value("${org.osiam.addon-self-administration.plugin.enabled:false}")
    private Boolean isPluginEnabled;

    @Value("${org.osiam.addon-self-administration.plugin.jar.path:}")
    private String pluginJarPath;

    @Value("${org.osiam.addon-self-administration.plugin.classname:}")
    private String pluginClass;

    private CallbackPlugin plugin;

    @Override
    public void performPreRegistrationActions(User user) throws CallbackException {
        plugin.performPreRegistrationActions(user);
    }

    @Override
    public void performPostRegistrationActions(User user) throws CallbackException {
        plugin.performPostRegistrationActions(user);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!isPluginEnabled) {
            plugin = new NoopPlugin();
            return;
        }

        if (Strings.isNullOrEmpty(pluginJarPath)) {
            throw new IllegalStateException(
                    "property 'org.osiam.addon-self-administration.plugin.jar.path' cannot be empty " +
                            "if 'plugin.enabled' is true.");
        }

        if (Strings.isNullOrEmpty(pluginClass)) {
            throw new IllegalStateException(
                    "property 'org.osiam.addon-self-administration.plugin.classname' cannot be empty " +
                            "if 'plugin.enabled' is true.");
        }

        plugin = loadPlugin();
    }

    private CallbackPlugin loadPlugin() {
        try {
            final URL[] jarLocation = { new File(pluginJarPath).toURI().toURL() };
            final URLClassLoader classLoader = URLClassLoader.newInstance(jarLocation, getClass().getClassLoader());
            return plugin = (CallbackPlugin) classLoader.loadClass(pluginClass).getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException
                | ClassNotFoundException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
