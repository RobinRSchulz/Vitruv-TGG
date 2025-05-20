package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.emoflon.ibex.tgg.compiler.defaults.IRegistrationHelper;
import org.emoflon.ibex.tgg.operational.IBlackInterpreter;
import org.emoflon.ibex.tgg.operational.csp.constraints.factories.RuntimeTGGAttrConstraintFactory;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.defaults.options.RepairOptions;
import org.emoflon.ibex.tgg.operational.strategies.PropagationDirectionHolder;
import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.hipe.SimpleNameSupportingURLClassLoader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * IBeX class  that helps with registering models and metamodels and is used by Vitruv-TGG for  configuration via the builder pattern.
 */
public class VitruviusTGGChangePropagationRegistrationHelper implements IRegistrationHelper {
    protected static final Logger logger = Logger.getLogger(VitruviusTGGChangePropagationRegistrationHelper.class);

    private Resource source;
    private Resource target;

    private EPackage sourceMetamodelPackage;
    private EPackage targetMetamodelPackage;

    private String sourceMetamodelPlatformUri;
    private String targetMetamodelPlatformUri;

    private File ibexProjectPath;

    private IBlackInterpreter patternMatcher;

    private PropagationDirectionHolder.PropagationDirection propagationDirection;
    private boolean useShortcutRules = false;

    @Override
    public void registerMetamodels(ResourceSet resourceSet, IbexExecutable ibexExecutable) throws IOException {
        //the democles way
        logger.trace("Called registerMetamodels with " );
        resourceSet.getAllContents().forEachRemaining(content -> logger.trace("    - " + content));
        resourceSet.getPackageRegistry().put(sourceMetamodelPackage.getNsURI() + ".ecore", sourceMetamodelPackage);
        resourceSet.getPackageRegistry().put(targetMetamodelPackage.getNsURI() + ".ecore", targetMetamodelPackage);

        resourceSet.getPackageRegistry().put(sourceMetamodelPlatformUri, sourceMetamodelPackage);
        resourceSet.getPackageRegistry().put(targetMetamodelPlatformUri, targetMetamodelPackage);
        ibexExecutable.getResourceHandler().loadAndRegisterMetamodel(sourceMetamodelPlatformUri);
        ibexExecutable.getResourceHandler().loadAndRegisterMetamodel(targetMetamodelPlatformUri);
        logger.trace("After registerMetamodels");
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
                .repair.repairAttributes(true)
                .repair.useShortcutRules(useShortcutRules)
                .registrationHelper(this);
        tryToFindAndAddUserDefinedAttributeConstraints(ibexOptions);
        return ibexOptions;
    }

    public PropagationDirectionHolder.PropagationDirection getPropagationDirection() {
        return propagationDirection;
    }

    /**
     *
     * @param TRGModel the model that is an instance of the metamodel defined as target in the Schema.
     */
    public VitruviusTGGChangePropagationRegistrationHelper withTRGModel(Resource TRGModel) {
        this.target = TRGModel;
        return this;
    }

    /**
     *
     * @param SRCModel the model that is an instance of the metamodel defined as source in the Schema.
     */
    public VitruviusTGGChangePropagationRegistrationHelper withSRCModel(Resource SRCModel) {
        this.source = SRCModel;
        return this;
    }

    /**
     *
     * @param ibexProjectPath the path to the ibex project where the Schema, rules etc. are defined.
     */
    public VitruviusTGGChangePropagationRegistrationHelper withIbexProjectPath(File ibexProjectPath) {
        this.ibexProjectPath = ibexProjectPath;
        return this;
    }

    /**
     *
     * @param SRCMetamodelPackage the metamodel defined as source in the Schema.
     */
    public VitruviusTGGChangePropagationRegistrationHelper withSRCMetamodelPackage(EPackage SRCMetamodelPackage) {
        this.sourceMetamodelPackage = SRCMetamodelPackage;
        return this;
    }

    /**
     *
     * @param TRGMetamodelPackage the metamodel defined as target in the Schema.
     */
    public VitruviusTGGChangePropagationRegistrationHelper withTRGMetamodelPackage(EPackage TRGMetamodelPackage) {
        this.targetMetamodelPackage = TRGMetamodelPackage;
        return this;
    }

    /**
     *
     * @param SRCMetamodelPlatformUri the platform URI of the metamodel defined as source in the Schema. Can be found in the Schema file.
     */
    public VitruviusTGGChangePropagationRegistrationHelper withSRCMetamodelPlatformUri(String SRCMetamodelPlatformUri) {
        this.sourceMetamodelPlatformUri = SRCMetamodelPlatformUri;
        return this;
    }

    /**
     *
     * @param TRGMetamodelPlatformUri the platform URI of the metamodel defined as target in the Schema. Can be found in the Schema file.
     */
    public VitruviusTGGChangePropagationRegistrationHelper withTRGMetamodelPlatformUri(String TRGMetamodelPlatformUri) {
        this.targetMetamodelPlatformUri = TRGMetamodelPlatformUri;
        return this;
    }

    public VitruviusTGGChangePropagationRegistrationHelper withPatternMatcher(IBlackInterpreter patternMatcher) {
        this.patternMatcher = patternMatcher;
        return this;
    }

    /**
     *
     * @param propagationDirection the propagation direction, based on what is defined as source and target in the Schema.
     *                             {@link PropagationDirectionHolder.PropagationDirection#FORWARD} means that the propagation should be from SRC to TRG.
     */
    public VitruviusTGGChangePropagationRegistrationHelper withPropagationDirection(PropagationDirectionHolder.PropagationDirection propagationDirection) {
        this.propagationDirection = propagationDirection;
        return this;
    }

    public VitruviusTGGChangePropagationRegistrationHelper withUseShortcutRules(boolean useShortcutRules) {
        this.useShortcutRules = useShortcutRules;
        return this;
    }

    private void tryToFindAndAddUserDefinedAttributeConstraints(IbexOptions ibexOptions) {
        try {
            //class loader should have access to this CL's classes as well as the ibex project
            Class userDefinedConstraintFactoryClass =
                    SimpleNameSupportingURLClassLoader
                            .getInstance(new URL[]{new File(ibexProjectPath, "/bin").toURI().toURL()})
                    .loadClass("org.emoflon.ibex.tgg.operational.csp.constraints.factories." + ibexOptions.project.name().toLowerCase() + ".UserDefinedRuntimeTGGAttrConstraintFactory");

            ibexOptions.csp.userDefinedConstraints((RuntimeTGGAttrConstraintFactory) userDefinedConstraintFactoryClass.getConstructor().newInstance());
        } catch (MalformedURLException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            logger.warn("Couldn't load UserDefinedRuntimeTGGAttrConstraintFactory");
        }
    }
}
