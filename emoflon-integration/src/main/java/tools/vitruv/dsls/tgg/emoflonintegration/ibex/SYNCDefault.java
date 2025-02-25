package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXModel;
import org.emoflon.ibex.tgg.compiler.defaults.IRegistrationHelper;
import org.emoflon.ibex.tgg.compiler.patterns.PatternType;
import org.emoflon.ibex.tgg.compiler.transformations.patterns.ContextPatternTransformation;
import org.emoflon.ibex.tgg.operational.benchmark.FullBenchmarkLogger;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.strategies.modules.MatchDistributor;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;
import org.emoflon.ibex.tgg.operational.strategies.sync.SYNC;
import tools.vitruv.change.composite.description.VitruviusChange;

import java.io.IOException;

public class SYNCDefault extends SYNC {

    private IRegistrationHelper registrationHelper;

    // reg helper sets config, e.g. project paths and BlackInterpreter, which is the Pattern Matcher
    public SYNCDefault(IRegistrationHelper registrationHelper) throws IOException {
        super(registrationHelper.createIbexOptions());
        this.registrationHelper = registrationHelper;
    }

    /**
     * Propagate changes in the source model to the target model.
     * <br/>
     * Changes are expected to be recorded by the caller. See {@link tools.vitruv.change.propagation.impl.ChangePropagator}
     */
    public void propagateChanges() throws IOException {
        /* TODO here, we ignore the VitruviusChange.
            1. Make schön! Ideas:
                * Create a superclass which defines this method.
                * Use strategy pattern, where the pattern matcher is chosen

            2. Load the resources (see SYNC_App.java =>
         */
        // this is already done by the resource handler!
////        System.out.println(TGGResourceHandler.class.descriptorString());
//        new TGGResourceHandler() {
//            @Override
//            public void loadModels() throws IOException {
//
//                // TODO THAT is the way SYNC_App does it
////                source = createResource(options.project.path() + "/instances/src.xmi");
////                target = createResource(options.project.path() + "/instances/trg.xmi");
////                corr = createResource(options.project.path() + "/instances/corr.xmi");
////                protocol = createResource(options.project.path() + "/instances/protocol.xmi");
//
//                // TODO Ideally, this is what we can do (we got ecore, not xmi...).
//                //      Might be we need to buffer the models to other files?
//
//                source = sourceModel;
//                target = targetModel;
//                this.
//                // TODO these two need to come from somewhere. Need to reinwurschtel in the Vitruvius resource handling.
//                //      for now: let it be gotten from the TGGChangePropagationSpecification
//                //      maybe corr should be generated from the Vitruvius Correspondence model.
//                //      (no doppelte Datenhaltung)?
//                corr = createResource(options.project.path() + "/instances/corr.xmi");
//                protocol = createResource(options.project.path() + "/instances/protocol.xmi");
//            }
//        };
        //TODO is this necessary?
        this.getOptions().tgg.tgg().setCorr(this.getOptions().tgg.flattenedTGG().getCorr());
        printTGG();

        logger.info("Debugging SYNC");
        logger.info(
                " src= " + this.resourceHandler.getSourceResource() +
                ", trg= " + this.resourceHandler.getTargetResource());
        logger.info("  model resource set= " + this.options.resourceHandler().getModelResourceSet());
        logger.info("  operational patterns:");
        this.getRelevantOperationalPatterns().forEach(logger::info);
//        this.options.blackInterpreter().initialise();
        logger.info("  RULES: ");
        this.greenFactories.getAll().forEach( (key, greenFactory) -> {
            logger.info("  - Fagtory [" + key + " -> " + greenFactory + "]");
            logger.info("    " + greenFactory.create(PatternType.FWD));
        });

//        this.matchDistributor.

        //TODO geht noch nisch, bei bedarf Democles-Dependencies einbinden
//        ContextPatternTransformation compiler = new ContextPatternTransformation(options, (MatchDistributor)matchObserver);
//        IBeXModel ibexModel = compiler.transform();
//        this.ibexPatterns = ibexModel.getPatternSet();
//        this.patternToRuleMap = compiler.getPatternToRuleMap();
        logger.info("this.options.tgg.getFlattenedConcreteTGGRules() -> ");





        this.options.patterns.greenPatternFactories().getAll().entrySet().stream().forEach( patternFactoryEntry -> {
            logger.info(  "options.patterns.greenPatternFactory: [" + patternFactoryEntry.getKey() + " -> " + patternFactoryEntry.getValue());
        });
//        this.options.blackInterpreter().initialise();

        logger.info("Starting SYNC");
        this.options.debug.benchmarkLogger(new FullBenchmarkLogger());
        long tic = System.currentTimeMillis();
        //TODO needs to be backward, depending on the direction the rules were specified and what currently is source and target! The ChangePropagationSpecification's source and target change, the ibex's don't...
        this.forward();

        long toc = System.currentTimeMillis();
        logger.info("Completed SYNC in: " + (toc - tic) + " ms");
        logger.debug("BENCHMARKLOGGER logs: ");
        logger.debug(this.options.debug.benchmarkLogger().toString());

        // TODO resourceHandler.saveRelevantModels probably needs to be overridden!
        this.saveModels();
        this.terminate();
        printTGG();

    }

    private void printTGG() {
        logger.info("Printing TGG[" + this.getTGG().getName() + "]");
        logger.info("  - src: List[EPackage, size=" + this.getTGG().getSrc().size() + "]");

        this.getTGG().getSrc().forEach(pkg ->  {
            logger.info("    - EPackage[" + pkg + "]");
        });
        logger.info("  - trg: ");
        this.getTGG().getTrg().forEach(pkg ->  {
            logger.info("    - EPackage[" + pkg + "]");
        });
        logger.info("  - corr: " +  this.getTGG().getCorr());
        logger.debug("  - corr from ibexOptions: " + this.getOptions().tgg.corrMetamodel());
        logger.debug("  - corr from flattenedTGG: " + this.getOptions().tgg.flattenedTGG().getCorr());

        this.getTGG();
        logger.info("  - rules: ");
        this.getTGG().getRules().forEach(pkg ->  {
            logger.info("    - EPackage[" + pkg + "]");
        });
    }

}
//public class SYNCDefault {
//
//}