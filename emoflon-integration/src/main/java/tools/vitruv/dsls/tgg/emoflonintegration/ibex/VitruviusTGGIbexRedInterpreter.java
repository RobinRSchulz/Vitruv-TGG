package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.eclipse.emf.ecore.EObject;
import org.emoflon.ibex.common.emf.EMFEdge;
import org.emoflon.ibex.common.emf.EMFManipulationUtils;
import org.emoflon.ibex.tgg.operational.IBlackInterpreter;
import org.emoflon.ibex.tgg.operational.defaults.IbexRedInterpreter;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import org.emoflon.ibex.tgg.operational.strategies.OperationalStrategy;
import runtime.CorrespondenceNode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link IbexRedInterpreter} that remembers what it has done.
 */
public class VitruviusTGGIbexRedInterpreter extends IbexRedInterpreter {

    private Set<ITGGMatch> revokedRules = new HashSet<>();
    private Set<RevokedCorrespondenceNodeWrapper> revokedCorrs = new HashSet<>();
    private Set<EObject> revokedModelNodes = new HashSet<>();
    private Set<EMFEdge> revokedEMFEdges = new HashSet<>();

    private VitruviusBackwardConversionTGGEngine patternMatcher;

    public VitruviusTGGIbexRedInterpreter(OperationalStrategy operationalStrategy, VitruviusBackwardConversionTGGEngine patternMatcher) {
        super(operationalStrategy);
        this.patternMatcher = patternMatcher;
    }

    @Override
    public void revokeOperationalRule(final ITGGMatch match) {
        super.revokeOperationalRule(match);
        revokedRules.add(match);
    }

    @Override
    public void revokeCorr(EObject corr, Set<EObject> nodesToRevoke, Set<EMFEdge> edgesToRevoke) {
        // nodesToRevoke and edgesToRevoke are not handled in the super method, but only filled!
        // We do this before calling revokeCorr so that we can copy source and target before their removal.
        revokedCorrs.add(new RevokedCorrespondenceNodeWrapper((CorrespondenceNode) corr));
        super.revokeCorr(corr, nodesToRevoke, edgesToRevoke);
    }

    @Override
    public void revoke(Set<EObject> nodesToRevoke, Set<EMFEdge> edgesToRevoke) {
        // we need this, but cannot leave it activated all the time since it breaks edge cases in serialization!
        patternMatcher.setNeeds_paranoid_modificiations(true);
        super.revoke(nodesToRevoke, edgesToRevoke);
        patternMatcher.setNeeds_paranoid_modificiations(false);


        revokedModelNodes.addAll(nodesToRevoke.stream().filter(eObject -> !(eObject instanceof CorrespondenceNode)).collect(Collectors.toSet())); // only model nodes!
        revokedEMFEdges.addAll(edgesToRevoke);
    }

    public Set<RevokedCorrespondenceNodeWrapper> getRevokedCorrs() {
        return revokedCorrs;
    }

    public Set<EObject> getRevokedModelNodes() {
        return revokedModelNodes;
    }

    public Set<ITGGMatch> getRevokedRuleMatches() {
        return revokedRules;
    }

    public Set<EMFEdge> getRevokedEMFEdges() {
        return revokedEMFEdges;
    }

    public static class RevokedCorrespondenceNodeWrapper {
        private final CorrespondenceNode correspondenceNode;
        private final EObject sourceEObject;
        private final EObject targetEObject;

        public RevokedCorrespondenceNodeWrapper(CorrespondenceNode correspondenceNode) {
            this.correspondenceNode = correspondenceNode;
            this.sourceEObject = (EObject) correspondenceNode.eGet(correspondenceNode.eClass().getEStructuralFeature("source"));
            this.targetEObject = (EObject) correspondenceNode.eGet(correspondenceNode.eClass().getEStructuralFeature("target"));
        }

        public CorrespondenceNode getCorrespondenceNode() {
            return correspondenceNode;
        }

        public EObject getSourceEObject() {
            return sourceEObject;
        }

        public EObject getTargetEObject() {
            return targetEObject;
        }
    }
}
