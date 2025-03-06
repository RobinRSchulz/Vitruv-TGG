package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;

import java.io.IOException;

public class VitruviusTGGResourceHandler extends TGGResourceHandler {
    protected static final Logger logger = Logger.getRootLogger();

    //duplicates to avoid possible conflicts. Resources should only be present after loadModels was called. So at construction time, we can only store them.
    private final Resource sourceToBeLoaded;
    private final Resource targetToBeLoaded;

    public VitruviusTGGResourceHandler(Resource source, Resource target) throws IOException {
        super();
        this.sourceToBeLoaded = source;
        this.targetToBeLoaded = target;
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

        // corr and protocol come from the ibex project (like it is done in the superclass)
        corr = this.createResource(this.options.project.path() + "/instances/corr.xmi");
        protocol = createResource(options.project.path() + "/instances/protocol.xmi");
    }

    @Override
    public void saveRelevantModels() throws IOException {
        // TODO do sth special(?)
        super.saveRelevantModels();
    }

    @Override
    public EPackage loadAndRegisterCorrMetamodel() throws IOException {
        /*
            Correspondence Metamodel in eMoflon. This cannot be generated from Vitruvius, because it means a different thing.
            Here, the correspondence metamodel is defined by the Schema.tgg in the IbeX eclipse project.
            This file has to be defined by the methodologist.

            Result: In an ununderstandable manner, the conversion Schema.tgg -> ecore file is done by the Eclipse Editor on save.
            If there is need or want for getting rid of eclipse: do it here, so the methodologist does not have to click "save" in the eclipse editor.
         */
        EPackage corrMetamodel = super.loadAndRegisterCorrMetamodel();
        logger.debug("loaded corr metamodel: " + corrMetamodel);
        return corrMetamodel;
    }
}
