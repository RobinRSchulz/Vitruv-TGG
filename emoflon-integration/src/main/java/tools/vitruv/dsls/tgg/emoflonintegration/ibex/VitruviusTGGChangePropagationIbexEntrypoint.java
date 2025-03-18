package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import language.TGGRule;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EPackage;
import org.emoflon.ibex.tgg.compiler.defaults.IRegistrationHelper;
import org.emoflon.ibex.tgg.operational.IBlackInterpreter;
import org.emoflon.ibex.tgg.operational.benchmark.FullBenchmarkLogger;
import org.emoflon.ibex.tgg.operational.strategies.sync.SYNC;
import runtime.CorrespondenceNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * This is the entrypoint for the propagation of Vitruvius Changes with TGG rules.
 * The Pattern matcher and other configuration (metamodel and model information) is provided in the constructor
 */
public class VitruviusTGGChangePropagationIbexEntrypoint extends SYNC {
    protected static final Logger logger = Logger.getLogger(VitruviusTGGChangePropagationIbexEntrypoint.class);

    /**
     *
     * @param registrationHelper contains information about the pattern matcher, metamodel and model info of source and target,
     *                          ibex project (rule definition) location. See ${@link VitruviusTGGChangePropagationRegistrationHelper}
     * @throws IOException if something is wrong with some model files (e.g. the IbeX Project path)
     */
    public VitruviusTGGChangePropagationIbexEntrypoint(VitruviusTGGChangePropagationRegistrationHelper registrationHelper) throws IOException {
        super(registrationHelper.createIbexOptions());
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
     *
     * @return a map of correspondences that are newly created!
     */
    public Set<CorrespondenceNode> propagateChanges() throws IOException {
        //TODO is this necessary?
        this.getOptions().tgg.tgg().setCorr(this.getOptions().tgg.flattenedTGG().getCorr());

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
        if (this.getOptions().blackInterpreter() instanceof VitruviusBackwardConversionTGGEngine vitruviusBackwardConversionTGGEngine) {
            return vitruviusBackwardConversionTGGEngine.getNewlyAddedCorrespondences();
        } else return new HashSet<>();
    }
}