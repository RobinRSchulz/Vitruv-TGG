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
                .repair.useShortcutRules(false) // TODO schon zwei TODOs im Shortcutrule-Code entdeckt, weeeiß ja net ob des so gut geht --> Im Zweifel Future work, bzw Evaluation...
                .registrationHelper(this);
        tryToFindAndAddUserDefinedAttributeConstraints(ibexOptions);
        return ibexOptions;
    }

    public PropagationDirectionHolder.PropagationDirection getPropagationDirection() {
        return propagationDirection;
    }

    public VitruviusTGGChangePropagationRegistrationHelper withTRGModel(Resource TRGModel) {
        this.target = TRGModel;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withSRCModel(Resource SRCModel) {
        this.source = SRCModel;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withIbexProjectPath(File ibexProjectPath) {
        this.ibexProjectPath = ibexProjectPath;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withSRCMetamodelPackage(EPackage SRCMetamodelPackage) {
        this.sourceMetamodelPackage = SRCMetamodelPackage;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withTRGMetamodelPackage(EPackage TRGMetamodelPackage) {
        this.targetMetamodelPackage = TRGMetamodelPackage;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withSRCMetamodelPlatformUri(String SRCMetamodelPlatformUri) {
        this.sourceMetamodelPlatformUri = SRCMetamodelPlatformUri;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withTRGMetamodelPlatformUri(String TRGMetamodelPlatformUri) {
        this.targetMetamodelPlatformUri = TRGMetamodelPlatformUri;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withPatternMatcher(IBlackInterpreter patternMatcher) {
        this.patternMatcher = patternMatcher;
        return this;
    }
    public VitruviusTGGChangePropagationRegistrationHelper withPropagationDirection(PropagationDirectionHolder.PropagationDirection propagationDirection) {
        this.propagationDirection = propagationDirection;
        return this;
    }

    private void tryToFindAndAddUserDefinedAttributeConstraints(IbexOptions ibexOptions) {
        try {
            //class loader should have access to this CL's classes as well as the ibex project
            Class userDefinedConstraintFactoryClass = new SimpleNameSupportingURLClassLoader(
                    new URL[]{new File(ibexProjectPath, "/bin").toURI().toURL()},
                    this.getClass().getClassLoader())
                    .loadClass("org.emoflon.ibex.tgg.operational.csp.constraints.factories." + ibexOptions.project.name().toLowerCase() + ".UserDefinedRuntimeTGGAttrConstraintFactory");

            ibexOptions.csp.userDefinedConstraints((RuntimeTGGAttrConstraintFactory) userDefinedConstraintFactoryClass.getConstructor().newInstance());
        } catch (MalformedURLException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            logger.warn("Couldn't load UserDefinedRuntimeTGGAttrConstraintFactory");
        }
    }
}
