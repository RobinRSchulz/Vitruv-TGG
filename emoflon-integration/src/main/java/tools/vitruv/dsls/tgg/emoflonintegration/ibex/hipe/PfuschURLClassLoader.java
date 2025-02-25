package tools.vitruv.dsls.tgg.emoflonintegration.ibex.hipe;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * This is not part of the thesis and it is unsure if this is even used in the evaluation...
 *
 * A classloader to maybe solve a problem that probably never existed. TODO try out and maybe remove
 */
public class PfuschURLClassLoader extends URLClassLoader {
    private final Map<String, Class<?>> packagelessClassNameToClass = new HashMap<>();
    public PfuschURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public PfuschURLClassLoader(URL[] urls) {
        super(urls);
    }

    public PfuschURLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    public PfuschURLClassLoader(String name, URL[] urls, ClassLoader parent) {
        super(name, urls, parent);
    }

    public PfuschURLClassLoader(String name, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(name, urls, parent, factory);
    }

    @Override
    public Class<?> loadClass(String name) {
        if (packagelessClassNameToClass.containsKey(name)) {
            return packagelessClassNameToClass.get(name);
        } else {
            // fill the map
            Class<?> loadedClass = null;
            try {
                loadedClass = super.loadClass(name);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            packagelessClassNameToClass.put(loadedClass.getSimpleName(), loadedClass);
            return loadedClass;
        }
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) {
        if (packagelessClassNameToClass.containsKey(name)) {
            return packagelessClassNameToClass.get(name);
        } else {
            // fill the map
            Class<?> loadedClass = null;
            try {
                loadedClass = super.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            packagelessClassNameToClass.put(loadedClass.getSimpleName(), loadedClass);
            return loadedClass;
        }

    }
}
