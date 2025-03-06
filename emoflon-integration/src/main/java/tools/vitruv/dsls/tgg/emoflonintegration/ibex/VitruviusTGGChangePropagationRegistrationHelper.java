package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.emoflon.ibex.tgg.compiler.defaults.IRegistrationHelper;
import org.emoflon.ibex.tgg.operational.IBlackInterpreter;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;

import java.io.File;
import java.io.IOException;

public class VitruviusTGGChangePropagationRegistrationHelper implements IRegistrationHelper {
    protected static final Logger logger = Logger.getRootLogger();

    private final Resource source;
    private final Resource target;

    private final EPackage sourceMetamodelPackage;
    private final EPackage targetMetamodelPackage;

    private final String sourceMetamodelPlatformUri;
    private final String targetMetamodelPlatformUri;

    private final File ibexProjectPath;

    private final IBlackInterpreter patternMatcher;

    /**
     *
     * @param sourceMetamodelPackage
     * @param targetMetamodelPackage
     * @param sourceMetamodelPlatformUri eclipse/ ibex need that. Look in your ibex project to find it...
     * @param targetMetamodelPlatformUri eclipse/ ibex need that. Look in your ibex project to find it...
     * @param source source model where the changes occur
     * @param target target model where the source changes are to be propagated to.
     * @param ibexProjectPath path to the eclipse ibex project where the TGG rules have been defined and compiled(!) (see README).
     * @param patternMatcher the pattern matcher which should be used to find forward and broken matches of TGG rules.
     */
    public VitruviusTGGChangePropagationRegistrationHelper(EPackage sourceMetamodelPackage, EPackage targetMetamodelPackage,
                                                           String sourceMetamodelPlatformUri, String targetMetamodelPlatformUri,
                                                           Resource source, Resource target,
                                                           File ibexProjectPath, IBlackInterpreter patternMatcher) {
        this.sourceMetamodelPackage = sourceMetamodelPackage;
        this.targetMetamodelPackage = targetMetamodelPackage;
        this.sourceMetamodelPlatformUri = sourceMetamodelPlatformUri;
        this.targetMetamodelPlatformUri = targetMetamodelPlatformUri;
        this.source = source;
        this.target = target;
        this.ibexProjectPath = ibexProjectPath;
        this.patternMatcher = patternMatcher;
    }
    @Override
    public void registerMetamodels(ResourceSet resourceSet, IbexExecutable ibexExecutable) throws IOException {
        //the democles way
        logger.debug("Called registerMetamodels with " );
        resourceSet.getAllContents().forEachRemaining(content -> logger.debug("    - " + content));
        resourceSet.getPackageRegistry().put(sourceMetamodelPackage.getNsURI() + ".ecore", sourceMetamodelPackage);
        resourceSet.getPackageRegistry().put(targetMetamodelPackage.getNsURI() + ".ecore", targetMetamodelPackage);

        resourceSet.getPackageRegistry().put(sourceMetamodelPlatformUri, sourceMetamodelPackage);
        resourceSet.getPackageRegistry().put(targetMetamodelPlatformUri, targetMetamodelPackage);
        ibexExecutable.getResourceHandler().loadAndRegisterMetamodel(sourceMetamodelPlatformUri);
        ibexExecutable.getResourceHandler().loadAndRegisterMetamodel(targetMetamodelPlatformUri);
        logger.debug("After registerMetamodels");
    }

    @Override
    public IbexOptions createIbexOptions() {
        IbexOptions ibexOptions = new IbexOptions();
        // Handle resources with Vitruvius resources, not actual files.
        try {
            ibexOptions.resourceHandler(new VitruviusTGGResourceHandler(source, target));
        } catch (IOException e) { throw new RuntimeException("Couldn't load TGG resources by using the models already loaded by Vitruvius! "); }

        ibexOptions
                .blackInterpreter(patternMatcher)
                .project.name(ibexProjectPath.getName())
                .project.workspacePath(ibexProjectPath.getParentFile().getAbsolutePath())
                .project.path(ibexProjectPath.getName())
                .debug.ibexDebug(true)
                .registrationHelper(this);
        return ibexOptions;
    }
}
