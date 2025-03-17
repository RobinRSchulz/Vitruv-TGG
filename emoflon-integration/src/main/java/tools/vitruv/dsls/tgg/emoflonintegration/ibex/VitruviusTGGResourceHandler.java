package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.emoflon.ibex.common.emf.EMFSaveUtils;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;
import org.emoflon.smartemf.persistence.JDOMXmiParser;
import org.emoflon.smartemf.persistence.XmiParserUtil;
import org.moflon.core.utilities.MoflonUtil;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class VitruviusTGGResourceHandler extends TGGResourceHandler {
    protected static final Logger logger = Logger.getLogger(VitruviusTGGResourceHandler.class);

    //duplicates to avoid possible conflicts. Resources should only be present after loadModels was called. So at construction time, we can only store them.
    private final Resource sourceToBeLoaded;
    private final Resource targetToBeLoaded;
    // remember the former resourceset, as we hand resources over to the ibex' resource set and need to hand it back...
    private final ResourceSet vitruvResourceSet;

    public VitruviusTGGResourceHandler(Resource source, Resource target) throws IOException {
        super();
        this.sourceToBeLoaded = source;
        this.targetToBeLoaded = target;
        this.vitruvResourceSet = source.getResourceSet();
        // load protocol from file.
    }

    /**
     * Load models not from File, but from given Resources
     */
    @Override
    public void loadModels() {
        logger.debug("Loading models");
        source = sourceToBeLoaded;
        target = targetToBeLoaded;
        logger.debug("  moving source and target resources to iBeX' resource set");
        this.rs.getResources().add(source);
        this.rs.getResources().add(target);

        // corr and protocol come from the ibex project (like it is done in the superclass)
        try {
            if (!source.isLoaded()) {
                logger.warn("source not loaded, loading...");
                source.load(null);
            }
            if (!target.isLoaded()) {
                logger.warn("source not loaded, loading...");
                target.load(null);
            }

            corr = loadResource(options.project.path() + "/instances/corr.xmi");
            protocol = loadResource(options.project.path() + "/instances/protocol.xmi");
        } catch (IOException e) {
            e.printStackTrace();
            Throwable t = e.getCause();

            logger.info("---- CAUSE ? " + t); // this doesnt work as there is a Bug in SmartEMFResource:142 (as a cause they take e.getCause() instead of just e...)
            while (t != null) {
                logger.info("---- CAUSE ----");
                t.printStackTrace();
                t = t.getCause();
            }
            throw new RuntimeException("Resource could not be loaded: If the following message is an Indexing error, " +
                    "the problem most likely is that some resource has not been loaded." +
                    "Use the debugger and set a breakpoint in JDOMXmiParser::indexForeignResource. Original message:" + e.getMessage()
                    + " -- stacktrace above");
        }
//        corr = createResource(this.options.project.path() + "/instances/corr.xmi");
//        protocol = createResource(options.project.path() + "/instances/protocol.xmi");
    }


    /**
     * Only load the resource from file if it is present.
     * Else, behave like {@link VitruviusTGGResourceHandler#createResource(String)}
     */
    @Override
    public Resource loadResource(String workspaceRelativePath) throws IOException {
        Resource res = this.createResource(workspaceRelativePath);
        String absolutePath = options.project.workspacePath() + "/" + workspaceRelativePath;
        if (!Files.exists(Path.of(absolutePath))) {
            logger.info("Resource not found. Not loading. Path=" + absolutePath);
            return res;
        }
        logger.info("Resource found. Now trying to load resource " + res.getURI() + " which is located at " + absolutePath);
        try {
//            DebugUtil.smartEMFResourceLoadDEBUG(res);
            res.load((Map)null);
        } catch (FileNotFoundException e) {
            throw new TGGFileNotFoundException(e, res.getURI());
        }

        EMFSaveUtils.resolveAll(res);
        return res;
    }

    @Override
    public void saveRelevantModels() throws IOException {
        logger.debug("Saving relevant models");
        super.saveRelevantModels();
        logger.debug("  switching source and target resources back to vitruvius resource set");
        this.vitruvResourceSet.getResources().add(source);
        this.vitruvResourceSet.getResources().add(target);
    }

    /**
        Correspondence Metamodel in eMoflon. This cannot be generated from Vitruvius, because it means a different thing.
        Here, the correspondence metamodel is defined by the Schema.tgg in the IbeX eclipse project.
        This file has to be defined by the methodologist.

        Result: In an ununderstandable manner, the conversion Schema.tgg -> ecore file is done by the Eclipse Editor on save.
        If there is need or want for getting rid of eclipse: do it here, so the methodologist does not have to click "save" in the eclipse editor.
     */
    @Override
    public EPackage loadAndRegisterCorrMetamodel() throws IOException {

        String relativePath = MoflonUtil.lastCapitalizedSegmentOf(options.project.name()) + "/model/"
                + MoflonUtil.lastCapitalizedSegmentOf(options.project.name()) + ".ecore";
        EPackage corrMetamodelEPackage = loadAndRegisterMetamodel("platform:/resource/" + relativePath);

        //also register under platform:/plugin
        String corrMetamodelPluginNSUri = "platform:/plugin/" + relativePath;
        logger.trace("Try registering metamodel under " + corrMetamodelPluginNSUri);
        this.specificationRS.getPackageRegistry().put(corrMetamodelPluginNSUri, corrMetamodelEPackage);
        logger.trace("Package not registered??:      EPackage.Registry.INSTANCE.getEPackage(pluginNSUri)=" + EPackage.Registry.INSTANCE.getEPackage(corrMetamodelPluginNSUri));
        EPackage.Registry.INSTANCE.put(corrMetamodelPluginNSUri, corrMetamodelEPackage);
        logger.trace("Package STILLnot registered??: EPackage.Registry.INSTANCE.getEPackage(pluginNSUri)=" + EPackage.Registry.INSTANCE.getEPackage(corrMetamodelPluginNSUri));

        options.tgg.corrMetamodel(corrMetamodelEPackage);
        return corrMetamodelEPackage;
    }
}
