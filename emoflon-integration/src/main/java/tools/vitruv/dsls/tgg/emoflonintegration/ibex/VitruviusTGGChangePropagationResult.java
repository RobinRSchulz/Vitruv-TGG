package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import language.TGGRule;
import org.eclipse.emf.ecore.EObject;
import org.emoflon.ibex.common.emf.EMFEdge;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import runtime.CorrespondenceNode;
import tools.vitruv.dsls.tgg.emoflonintegration.Timer;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.VitruviusTGGIbexRedInterpreter.RevokedCorrespondenceNodeWrapper;
import tools.vitruv.dsls.tgg.emoflonintegration.patternmatching.VitruviusBackwardConversionMatch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents the results of our tgg pattern matching.
 */
public class VitruviusTGGChangePropagationResult {

    private final Set<CorrespondenceNode> addedCorrespondences;

    private final Set<RevokedCorrespondenceNodeWrapper> revokedCorrespondences;
    private final Set<ITGGMatch> revokedMatches;
    private final Set<EObject> revokedModelNodes;
    private final Set<EMFEdge> revokedEMFEdges;

    private final Set<VitruviusBackwardConversionMatch> appliedMatches;
    private final Set<TGGRule> intactRules;
    private final Set<TGGRule> corruptRules;

    private Map<String, Timer> timeMeasurements;

    /**
     * Empty Result
     */
    public VitruviusTGGChangePropagationResult() {
        intactRules = new HashSet<>();
        corruptRules = new HashSet<>();
        appliedMatches = new HashSet<>();
        addedCorrespondences = new HashSet<>();
        revokedCorrespondences = new HashSet<>();
        revokedMatches = new HashSet<>();
        revokedModelNodes = new HashSet<>();
        revokedEMFEdges = new HashSet<>();
        timeMeasurements = new HashMap<>();
    }


    public VitruviusTGGChangePropagationResult(
            Set<TGGRule> intactRules,
            Set<TGGRule> corruptRules,
            Set<VitruviusBackwardConversionMatch> appliedMatches,
            Set<CorrespondenceNode> newlyAddedCorrespondences,
                                               Set<RevokedCorrespondenceNodeWrapper> revokedCorrs,
                                               Set<ITGGMatch> revokedRules,
                                               Set<EObject> revokedModelNodes,
                                               Set<EMFEdge> revokedEMFEdges,
            Map<String, Timer> timeMeasurements) {

        this.intactRules = intactRules;
        this.corruptRules = corruptRules;
        this.appliedMatches = appliedMatches;
        this.addedCorrespondences = newlyAddedCorrespondences;
        this.revokedCorrespondences = revokedCorrs;
        this.revokedMatches = revokedRules;
        this.revokedModelNodes = revokedModelNodes;
        this.revokedEMFEdges = revokedEMFEdges;
        this.timeMeasurements = timeMeasurements;
    }

    public VitruviusTGGChangePropagationResult setTimeMeasurements(Map<String, Timer> timeMeasurements) {
        this.timeMeasurements = timeMeasurements;
        return this;
    }

    public Set<TGGRule> getIntactRules() {
        return intactRules;
    }

    public Set<TGGRule> getCorruptRules() {
        return corruptRules;
    }

    public Set<VitruviusBackwardConversionMatch> getAppliedMatches() {
        return appliedMatches;
    }

    public Set<CorrespondenceNode> getAddedCorrespondences() {
        return addedCorrespondences;
    }

    public Set<RevokedCorrespondenceNodeWrapper> getRevokedCorrespondences() {
        return revokedCorrespondences;
    }

    public Set<ITGGMatch> getRevokedMatches() {
        return revokedMatches;
    }

    public Set<EObject> getRevokedModelNodes() {
        return revokedModelNodes;
    }

    public Set<EMFEdge> getRevokedEMFEdges() {
        return revokedEMFEdges;
    }

    public Map<String, Timer> getTimeMeasurements() {
        return timeMeasurements;
    }
}
