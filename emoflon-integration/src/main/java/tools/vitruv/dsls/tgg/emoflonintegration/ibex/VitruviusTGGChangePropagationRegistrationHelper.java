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

    private Resource source;
    private Resource target;

    private EPackage sourceMetamodelPackage;
    private EPackage targetMetamodelPackage;

    private String sourceMetamodelPlatformUri;
    private String targetMetamodelPlatformUri;

    private File ibexProjectPath;

    private IBlackInterpreter patternMatcher;

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

    public VitruviusTGGChangePropagationRegistrationHelper withTargetModel(Resource targetModel) {
        this.target = targetModel;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withSourceModel(Resource sourceModel) {
        this.source = sourceModel;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withIbexProjectPath(File ibexProjectPath) {
        this.ibexProjectPath = ibexProjectPath;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withSourceMetamodelPackage(EPackage sourceMetamodelPackage) {
        this.sourceMetamodelPackage = sourceMetamodelPackage;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withTargetMetamodelPackage(EPackage targetMetamodelPackage) {
        this.targetMetamodelPackage = targetMetamodelPackage;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withSourceMetamodelPlatformUri(String sourceMetamodelPlatformUri) {
        this.sourceMetamodelPlatformUri = sourceMetamodelPlatformUri;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withTargetMetamodelPlatformUri(String targetMetamodelPlatformUri) {
        this.targetMetamodelPlatformUri = targetMetamodelPlatformUri;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withPatternMatcher(IBlackInterpreter patternMatcher) {
        this.patternMatcher = patternMatcher;
        return this;
    }
}
