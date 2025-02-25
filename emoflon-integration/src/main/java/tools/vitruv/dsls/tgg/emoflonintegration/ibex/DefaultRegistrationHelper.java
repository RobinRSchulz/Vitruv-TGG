package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.emoflon.ibex.tgg.compiler.defaults.IRegistrationHelper;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.strategies.modules.IbexExecutable;
import org.emoflon.ibex.tgg.runtime.democles.DemoclesTGGEngine;
import org.emoflon.ibex.tgg.runtime.hipe.HiPETGGEngine;
import tools.vitruv.change.composite.description.VitruviusChange;

import java.io.File;
import java.io.IOException;

public class DefaultRegistrationHelper implements IRegistrationHelper {
    protected static final Logger logger = Logger.getRootLogger();

    private Resource source;
    private Resource target;
    private Resource corr;

    private EPackage sourceMetamodel;
    private EPackage targetMetamodel;

    private File ibexProjectPath;

    private VitruviusChange vitruviusChange;

    public DefaultRegistrationHelper(EPackage sourceMetamodel, EPackage targetMetamodel, Resource source, Resource target, Resource corr, VitruviusChange vitruviusChange, File ibexProjectPath) {
        this.sourceMetamodel = sourceMetamodel;
        this.targetMetamodel = targetMetamodel;
        this.source = source;
        this.target = target;
        this.corr = corr;
        this.vitruviusChange = vitruviusChange;
        this.ibexProjectPath = ibexProjectPath;
    }
    @Override
    public void registerMetamodels(ResourceSet resourceSet, IbexExecutable ibexExecutable) throws IOException {
        //the democles way
        logger.info("Called registerMetamodels with " );
//        logger.info("  - resources: " + resourceSet);
        resourceSet.getAllContents().forEachRemaining(content -> logger.info("    - " + content));
//        logger.info("  - packages: " +  resourceSet.getPackageRegistry());
//        resourceSet.getPackageRegistry().entrySet().stream().forEach(a -> {
//            logger.info("    - " + a);
//        });
//        logger.info("Trying to register " + sourceMetamodel.getNsURI() + " AND " + targetMetamodel.getNsURI());
        resourceSet.getPackageRegistry().put(sourceMetamodel.getNsURI() + ".ecore", sourceMetamodel);
        resourceSet.getPackageRegistry().put(targetMetamodel.getNsURI() + ".ecore", targetMetamodel);
        //TODO this is required. de-hardcode!
        resourceSet.getPackageRegistry().put("platform:/resource/tools.vitruv.methodologisttemplate.model/src/main/ecore/model.ecore", sourceMetamodel);
        resourceSet.getPackageRegistry().put("platform:/resource/tools.vitruv.methodologisttemplate.model/src/main/ecore/model2.ecore", targetMetamodel);

        //TODO necessary?
        ibexExecutable.getResourceHandler().loadAndRegisterMetamodel("platform:/resource/tools.vitruv.methodologisttemplate.model/src/main/ecore/model.ecore");
        ibexExecutable.getResourceHandler().loadAndRegisterMetamodel("platform:/resource/tools.vitruv.methodologisttemplate.model/src/main/ecore/model2.ecore");

        // TODO: Here,  metamodels are registered twice, under different uris. (from DemoclesRegistrationHelper in an eMoflon test). Maybe we need to do that also??
//        rs.getPackageRegistry().put("http://de.ubt.ai1.bw.qvt.examples.gantt.ecore", ganttPack);
//        rs.getPackageRegistry().put("http://de.ubt.ai1.bw.qvt.examples.cpm.ecore", cpmPack);
//        rs.getPackageRegistry().put("platform:/resource/Gantt/model/Gantt.ecore", ganttPack);
//        rs.getPackageRegistry().put("platform:/resource/CPM/model/CPM.ecore", cpmPack);
        logger.info("After registerMetamodels");
    }

    @Override
    public IbexOptions createIbexOptions() {
        IbexOptions ibexOptions = new IbexOptions();
        // Handle resources with Vitruvius resources, not actual files.
        try {
            ibexOptions.resourceHandler(new VitruviusTGGResourceHandler(source, target));
        } catch (IOException e) { throw new RuntimeException("Couldn't load TGG resources by using the models already loaded by Vitruvius! "); }

        ibexOptions // TODO here, insert the pattern matcher
//                .blackInterpreter(new DemoclesTGGEngine()) // can't get it to find matches despite it having the patterns and resources...
//                .blackInterpreter(new HiPETGGEngine())  // doesnt work, creats path that is not reproducible -> use overridden class
                .blackInterpreter(new VitruviusHiPETGGEngine())
                .blackInterpreter(new VitruviusBackwardConversionTGGEngine(vitruviusChange))
//                .project.name(ibexProjectPath.getName()) //TODO maybe solve that via some strategy, e.g. TGGChangePropagationSpecification has to be extended for each new consistency relation and must explicitly specify
                .project.name("Something2Else") //TODO de-hardcode
                .project.workspacePath(ibexProjectPath.getParentFile().getAbsolutePath())
//                .project.path(ibexProjectPath.getAbsolutePath())
                .project.path("Something2Else")
                .debug.ibexDebug(true)  //TODO change back
                .csp.userDefinedConstraints(new UserDefinedRuntimeTGGAttrConstraintFactory())
                .registrationHelper(this);
        return ibexOptions;
    }
}
