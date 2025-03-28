package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.apache.log4j.Logger;
import org.emoflon.ibex.tgg.operational.IBlackInterpreter;
import org.emoflon.ibex.tgg.operational.benchmark.FullBenchmarkLogger;
import org.emoflon.ibex.tgg.operational.strategies.PropagationDirectionHolder;
import org.emoflon.ibex.tgg.operational.strategies.sync.SYNC;

import java.io.IOException;

/**
 * This is the entrypoint for the propagation of Vitruvius Changes with TGG rules.
 * The Pattern matcher and other configuration (metamodel and model information) is provided in the constructor
 */
public class VitruviusTGGChangePropagationIbexEntrypoint extends SYNC {
    protected static final Logger logger = Logger.getLogger(VitruviusTGGChangePropagationIbexEntrypoint.class);

    private final PropagationDirectionHolder.PropagationDirection propagationDirection;

    /**
     *
     * @param registrationHelper contains information about the pattern matcher, metamodel and model info of source and target,
     *                          ibex project (rule definition) location. See ${@link VitruviusTGGChangePropagationRegistrationHelper}
     * @throws IOException if something is wrong with some model files (e.g. the IbeX Project path)
     */
    public VitruviusTGGChangePropagationIbexEntrypoint(VitruviusTGGChangePropagationRegistrationHelper registrationHelper) throws IOException {
        super(registrationHelper.createIbexOptions());
        //override redInterpreter with our own stuff
        VitruviusTGGIbexRedInterpreter vitruviusTGGIbexRedInterpreter = new VitruviusTGGIbexRedInterpreter(this);
        this.registerRedInterpeter(vitruviusTGGIbexRedInterpreter);
        //override SeqRepair with our own stuff (needed to switch repairing attributes off and on...)
        this.repairer = new FlexibleSeqRepair(this, this.propagationDirectionHolder);

        this.propagationDirection = registrationHelper.getPropagationDirection();
        IBlackInterpreter patternMatcher = this.getOptions().blackInterpreter();
        if (patternMatcher instanceof VitruviusBackwardConversionTGGEngine vitruviusBackwardConversionTGGEngine) {
            // we need feedback about matches created...
            this.registerObserver(vitruviusBackwardConversionTGGEngine);
            // TODO currently only for debug purposes...
            vitruviusBackwardConversionTGGEngine.addObservedOperationalStrategy(this);
            vitruviusBackwardConversionTGGEngine.addVitruviusTGGIbexRedInterpreter(vitruviusTGGIbexRedInterpreter);
        }
    }

    /**
     * Propagate changes in the source model to the target model.
     * <br/>
     * Changes are expected to be recorded by the caller. See {@link tools.vitruv.change.propagation.impl.ChangePropagator}
     *
     * @return a map of correspondences that are newly created!
     */
    public VitruviusTGGChangePropagationResult propagateChanges() throws IOException {
        //TODO is this necessary?
        this.getOptions().tgg.tgg().setCorr(this.getOptions().tgg.flattenedTGG().getCorr());

        logger.info("Starting SYNC");
        this.options.debug.benchmarkLogger(new FullBenchmarkLogger());
        long tic = System.currentTimeMillis();
        //TODO needs to be backward, depending on the direction the rules were specified and what currently is source and target! The ChangePropagationSpecification's source and target change, the ibex's don't...
        if (propagationDirection.equals(PropagationDirectionHolder.PropagationDirection.FORWARD)) {
            this.forward();
        } else {
            this.backward();
        }

        long toc = System.currentTimeMillis();
        logger.info("Completed SYNC in: " + (toc - tic) + " ms");
        logger.debug("BENCHMARKLOGGER logs: ");
        logger.debug(this.options.debug.benchmarkLogger().toString());

        // TODO resourceHandler.saveRelevantModels probably needs to be overridden!
        this.saveModels();
        this.terminate();

        if (this.getOptions().blackInterpreter() instanceof VitruviusBackwardConversionTGGEngine vitruviusBackwardConversionTGGEngine) {
            VitruviusTGGIbexRedInterpreter vitruviusTGGIbexRedInterpreter = (VitruviusTGGIbexRedInterpreter) redInterpreter;
            return new VitruviusTGGChangePropagationResult(
                    vitruviusBackwardConversionTGGEngine.getNewlyAddedCorrespondences(),
                    vitruviusTGGIbexRedInterpreter.getRevokedCorrs(),
                    vitruviusTGGIbexRedInterpreter.getRevokedRuleMatches(),
                    vitruviusTGGIbexRedInterpreter.getRevokedModelNodes(),
                    vitruviusTGGIbexRedInterpreter.getRevokedEMFEdges());
        } else return  new VitruviusTGGChangePropagationResult();
    }
}