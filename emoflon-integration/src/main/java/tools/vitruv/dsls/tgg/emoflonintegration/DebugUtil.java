package tools.vitruv.dsls.tgg.emoflonintegration;

import org.eclipse.emf.ecore.resource.Resource;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;
import org.emoflon.smartemf.persistence.JDOMXmiParser;
import org.emoflon.smartemf.persistence.XmiParserUtil;

import java.io.*;
import java.lang.reflect.Field;

/**
 * Utility methods for debugging like easier reflection access and such.
 */
public class DebugUtil {

    /**
     * This debugs SmartEMFResource::load, because that method has a bug in exception handling making it undebuggable.
     * You need to provide a Resource which is created by emoflon via ${@link TGGResourceHandler#createResource(String)}
     */
    public void smartEMFResourceLoadDEBUG(Resource resource) throws IOException {
        String path = XmiParserUtil.resolveURIToPath(
                (org.eclipse.emf.common.util.URI) getPrivateSuperclassField("uri", resource),
                (String) getPrivateClassField("workspacePath", resource));
        if (path == null) {
            throw new FileNotFoundException("No valid xmi file present at: " + getPrivateSuperclassField("uri", resource));
        } else {
            File file = new File(path);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                smartEMFResourceLoadDEBUG(resource, (InputStream)fis);
                fis.close();
            } else {
                throw new FileNotFoundException("No valid xmi file present at: " + path);
            }
        }
    }

    private static void smartEMFResourceLoadDEBUG(Resource resource, InputStream inputStream) throws IOException {
        JDOMXmiParser parser = new JDOMXmiParser((String) getPrivateClassField("workspacePath", resource));
        try {
            parser.load(inputStream, resource);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
            // the original ::load does this, which is the whole reason we're doing this here...
//            throw new IOException(e.getMessage(), e.getCause());
        }
    }

    /**
     *
     * @param fieldName a field of the Superclass of object
     * @return what object.fieldname would return if it were accessible.
     */
    public static Object getPrivateSuperclassField(String fieldName, Object object) {
        try {
            Field field = object.getClass().getSuperclass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Reflection-Exception: " + e);
        }
    }

    /**
     *
     * @param fieldName a field of the class of object
     * @return what object.fieldname would return if it were accessible.
     */
    public static Object getPrivateClassField(String fieldName, Object object) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Reflection-Exception: " + e);
        }
    }
}
