package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

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
