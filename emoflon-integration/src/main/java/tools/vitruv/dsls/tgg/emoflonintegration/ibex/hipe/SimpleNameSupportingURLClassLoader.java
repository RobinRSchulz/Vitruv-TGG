package tools.vitruv.dsls.tgg.emoflonintegration.ibex.hipe;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * A classloader to enable loading Classes without the package name.
 */
public class SimpleNameSupportingURLClassLoader extends URLClassLoader {
    private final Map<String, Class<?>> packagelessClassNameToClass = new HashMap<>();

    public SimpleNameSupportingURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public Class<?> loadClass(String name) {
        if (packagelessClassNameToClass.containsKey(name)) {
            return packagelessClassNameToClass.get(name);
        } else {
            // fill the map
            Class<?> loadedClass;
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
            Class<?> loadedClass;
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
