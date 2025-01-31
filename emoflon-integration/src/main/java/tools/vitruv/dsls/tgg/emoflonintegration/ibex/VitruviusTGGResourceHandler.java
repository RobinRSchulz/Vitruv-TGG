package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;

import java.io.IOException;

public class VitruviusTGGResourceHandler extends TGGResourceHandler {
    protected static final Logger logger = Logger.getRootLogger();

    //duplicate to avoid possible conflicts. Resources should only be present after loadModels was called.
    private Resource sourceToBeLoaded;
    private Resource targetToBeLoaded;
    private Resource corrToBeLoaded;

    public VitruviusTGGResourceHandler(Resource source, Resource target, Resource corr) throws IOException {
        super();
        this.sourceToBeLoaded = source;
        this.targetToBeLoaded = target;
        this.corrToBeLoaded = corr;
        // load protocol from file.
    }

    /**
     * Load models not from File, but from given Resources
     */
    @Override
    public void loadModels() {
        source = sourceToBeLoaded;
        target = targetToBeLoaded;
        this.rs.getResources().add(source);
        this.rs.getResources().add(target);

        // for now, corr is also loaded from the ibex project (in the same way as the superclass does it
//        corr = corrToBeLoaded;
        corr = this.createResource(this.options.project.path() + "/instances/corr.xmi");

        // Load protocol in the same way as the superclass
        protocol = createResource(options.project.path() + "/instances/protocol.xmi");
    }

    @Override
    public void saveRelevantModels() throws IOException {
        // TODO do sth special(?)
        super.saveRelevantModels();
    }

    @Override
    public EPackage loadAndRegisterCorrMetamodel() throws IOException {
        logger.info("Called loadAndRegisterMetamodel");
        // TODO
        /*
            Correspondence Metamodel in eMoflon. This cannot be generated from Vitruvius, because it means a different thing.
            Here, the correspondence metamodel is defined by the Schema.tgg.
            This file has to be defined by the methodologist.
            Further, it seems to be converted to ecore somehow and somewhere...

            Result: In a huge clusterfuck, this is done by the Editor on save.
            TODO if zu viel Zeit and/or need or want for getting rid of eclipse: do it here, so the methodologist does not have to click "save" in the eclipse editor.
            TODO Download eclipse + 5 mrd IBeX-Addons + wurschtel the project hier irgendwie rein

         */
        EPackage corrMetamodel = super.loadAndRegisterCorrMetamodel();
        logger.info("loaded corr metamodel: " + corrMetamodel);
        logger.info("this.options.tgg.corrMetamodel(): " + this.options.tgg.corrMetamodel());
        return corrMetamodel;
//        return super.loadAndRegisterCorrMetamodel();
    }
}
