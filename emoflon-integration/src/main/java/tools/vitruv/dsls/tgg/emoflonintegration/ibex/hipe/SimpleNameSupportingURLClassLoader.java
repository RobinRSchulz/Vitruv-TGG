package tools.vitruv.dsls.tgg.emoflonintegration.ibex.hipe;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * A singleton classloader to enable loading classes without the package name.
 * The singleton mechanism is used for preventing ambiguities when a set of classes is to be loaded by different classes,
 * as is done in the evaluation project.
 */
public class SimpleNameSupportingURLClassLoader extends URLClassLoader {
    private final Map<String, Class<?>> packagelessClassNameToClass = new HashMap<>();

    private static SimpleNameSupportingURLClassLoader instance;

    /**
     *
     * @param urls the urls to be loaded on first getInstance call. On the second, they are ignored.
     * @return this singleton instance, instanciated with the first urls.
     */
    public static SimpleNameSupportingURLClassLoader getInstance(URL[] urls) {
        if (instance == null) {
            instance = new SimpleNameSupportingURLClassLoader(urls,
                    SimpleNameSupportingURLClassLoader.class.getClassLoader());
        }
        return instance;
    }

    private SimpleNameSupportingURLClassLoader(URL[] urls, ClassLoader parent) {
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

    @Override
    public String toString() {
        return super.toString() + "parent: " + getParent();
    }
}
