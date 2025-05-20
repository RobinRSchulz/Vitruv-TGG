package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import language.TGGRule;
import org.apache.log4j.Logger;
import org.emoflon.ibex.tgg.operational.IBlackInterpreter;
import org.emoflon.ibex.tgg.operational.benchmark.FullBenchmarkLogger;
import org.emoflon.ibex.tgg.operational.strategies.PropagationDirectionHolder;
import org.emoflon.ibex.tgg.operational.strategies.sync.SYNC;
import tools.vitruv.dsls.tgg.emoflonintegration.Timer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This is the entrypoint for the propagation of Vitruvius Changes with TGG rules. Also, since it extends {@link SYNC},
 * this class acts as the counterpart for {@link VitruviusBackwardConversionTGGEngine}.
 * To enable that, certain customizations are made, giving the pattern matcher more information that it would normally have and fixing some bugs.
 *
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
        this.propagationDirection = registrationHelper.getPropagationDirection();
        IBlackInterpreter patternMatcher = this.getOptions().blackInterpreter();

        //further initialize this SYNC instance and the pattern matcher
        if (patternMatcher instanceof VitruviusBackwardConversionTGGEngine vitruviusBackwardConversionTGGEngine) {
            // custom RedInterpreter for being able to remeber revoked matches. That is required by the pattern matcher.
            VitruviusTGGIbexRedInterpreter vitruviusTGGIbexRedInterpreter = new VitruviusTGGIbexRedInterpreter(this, (VitruviusBackwardConversionTGGEngine) this.getOptions().blackInterpreter());
            this.registerRedInterpeter(vitruviusTGGIbexRedInterpreter);

            //override SeqRepair with our own stuff (needed to switch repairing attributes off and on...)
            this.repairer = new FlexibleSeqRepair(this, this.propagationDirectionHolder);

            // we need feedback about matches created...
            this.registerObserver(vitruviusBackwardConversionTGGEngine);

            // enable access to "consistency matches" (protocol)
            vitruviusBackwardConversionTGGEngine.addObservedOperationalStrategy(this);

            // enable deleting broken consistency matches (not done automatically by ibex..) and getting feedback on which reported broken matches have been deleted.
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
        this.getOptions().tgg.tgg().setCorr(this.getOptions().tgg.flattenedTGG().getCorr());

        logger.info("Starting SYNC");
        this.options.debug.benchmarkLogger(new FullBenchmarkLogger());

        Timer timer = new Timer();
        timer.start();

        if (propagationDirection.equals(PropagationDirectionHolder.PropagationDirection.FORWARD)) {
            this.forward();
        } else {
            this.backward();
        }

        timer.stop();
        logger.info("Completed SYNC in: " + timer.getTime(TimeUnit.MILLISECONDS) + " ms");
        logger.debug("BENCHMARKLOGGER logs: ");
        logger.debug(this.options.debug.benchmarkLogger().toString());

        this.saveModels();
        this.terminate();

        if (this.getOptions().blackInterpreter() instanceof VitruviusBackwardConversionTGGEngine vitruviusBackwardConversionTGGEngine) {
            VitruviusTGGIbexRedInterpreter vitruviusTGGIbexRedInterpreter = (VitruviusTGGIbexRedInterpreter) redInterpreter;
            Map<String, Timer> allTimeMeasurements = new HashMap<>();
            allTimeMeasurements.putAll(vitruviusBackwardConversionTGGEngine.getTimeMeasurements());
            allTimeMeasurements.put("total change propagation time", timer);

            return new VitruviusTGGChangePropagationResult(
                    this.getIntactRules(),
                    this.getCorruptRules(),
                    vitruviusBackwardConversionTGGEngine.getAppliedMatches(),
                    vitruviusBackwardConversionTGGEngine.getNewlyAddedCorrespondences(),
                    vitruviusTGGIbexRedInterpreter.getRevokedCorrs(),
                    vitruviusTGGIbexRedInterpreter.getRevokedRuleMatches(),
                    vitruviusTGGIbexRedInterpreter.getRevokedModelNodes(),
                    vitruviusTGGIbexRedInterpreter.getRevokedEMFEdges(),
                    allTimeMeasurements
            );
        } else return new VitruviusTGGChangePropagationResult()
                .setTimeMeasurements(Map.of("total change propagation time", timer));
    }

    private Set<TGGRule> getCorruptRules() {
        return this.getOptions().tgg.flattenedTGG().getRules().stream()
                .filter(tggRule -> !tggRule.isAbstract())
                .filter(rule ->
                        rule.getNodes().stream().anyMatch(ruleNode -> ruleNode.getType().eIsProxy())
                                || rule.getEdges().stream().anyMatch(ruleEdge -> ruleEdge.getType().eIsProxy())
                ).collect(Collectors.toSet());
    }

    private Set<TGGRule> getIntactRules() {
        return this.getOptions().tgg.flattenedTGG().getRules().stream()
                .filter(tggRule -> !tggRule.isAbstract())
                .filter(rule ->
                        rule.getNodes().stream().noneMatch(ruleNode -> ruleNode.getType().eIsProxy())
                                && rule.getEdges().stream().noneMatch(ruleEdge -> ruleEdge.getType().eIsProxy())
                ).collect(Collectors.toSet());
    }
}