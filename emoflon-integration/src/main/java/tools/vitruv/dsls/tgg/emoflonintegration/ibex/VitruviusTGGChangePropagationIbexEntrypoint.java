package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.emoflon.ibex.tgg.compiler.defaults.IRegistrationHelper;
import org.emoflon.ibex.tgg.operational.IBlackInterpreter;
import org.emoflon.ibex.tgg.operational.benchmark.FullBenchmarkLogger;
import org.emoflon.ibex.tgg.operational.strategies.sync.SYNC;

import java.io.IOException;

/**
 * This is the entrypoint for the propagation of Vitruvius Changes with TGG rules.
 * The Pattern matcher and other configuration (metamodel and model information) is provided in the constructor
 */
public class VitruviusTGGChangePropagationIbexEntrypoint extends SYNC {

    private IRegistrationHelper registrationHelper;

    /**
     *
     * @param registrationHelper contains information about the pattern matcher, metamodel and model info of source and target,
     *                          ibex project (rule definition) location. See ${@link VitruviusTGGChangePropagationRegistrationHelper}
     * @throws IOException if something is wrong with some model files (e.g. the IbeX Project path)
     */
    public VitruviusTGGChangePropagationIbexEntrypoint(VitruviusTGGChangePropagationRegistrationHelper registrationHelper) throws IOException {
        super(registrationHelper.createIbexOptions());
        this.registrationHelper = registrationHelper;
        IBlackInterpreter patternMatcher = this.getOptions().blackInterpreter();
        if (patternMatcher instanceof VitruviusBackwardConversionTGGEngine vitruviusBackwardConversionTGGEngine) {
            // we need feedback about matches created...
            this.registerObserver(vitruviusBackwardConversionTGGEngine);
            // TODO currently only for debug purposes...
            vitruviusBackwardConversionTGGEngine.addObservedOperationalStrategy(this);
        }

    }

    /**
     * Propagate changes in the source model to the target model.
     * <br/>
     * Changes are expected to be recorded by the caller. See {@link tools.vitruv.change.propagation.impl.ChangePropagator}
     */
    public void propagateChanges() throws IOException {
        //TODO is this necessary?
        this.getOptions().tgg.tgg().setCorr(this.getOptions().tgg.flattenedTGG().getCorr());

        logger.info("Starting SYNC");
        this.options.debug.benchmarkLogger(new FullBenchmarkLogger());
        long tic = System.currentTimeMillis();
        //TODO needs to be backward, depending on the direction the rules were specified and what currently is source and target! The ChangePropagationSpecification's source and target change, the ibex's don't...
//        try {
//            this.forward();
//        } catch (Exception e) {
//            logger.error("Problem while propagating changes: " + e.getMessage());
//            logger.warn("DELETE THIS CRAP ASAP!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//        }
        this.forward();
        this.getResourceHandler().getCorrCaching();

        long toc = System.currentTimeMillis();
        logger.info("Completed SYNC in: " + (toc - tic) + " ms");
        logger.debug("BENCHMARKLOGGER logs: ");
        logger.debug(this.options.debug.benchmarkLogger().toString());

        // TODO resourceHandler.saveRelevantModels probably needs to be overridden!
        this.saveModels();
        this.terminate();
        printTGG();
    }

    /**
     * [DEBUG helper]
     */
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