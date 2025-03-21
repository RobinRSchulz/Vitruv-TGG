package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.eclipse.emf.ecore.EObject;
import org.emoflon.ibex.common.emf.EMFEdge;
import org.emoflon.ibex.tgg.operational.defaults.IbexRedInterpreter;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import org.emoflon.ibex.tgg.operational.strategies.OperationalStrategy;
import runtime.CorrespondenceNode;

import java.util.HashSet;
import java.util.Set;

/**
 * A {@link IbexRedInterpreter} that remembers what it has done.
 */
public class VitruviusTGGIbexRedInterpreter extends IbexRedInterpreter {

    private Set<ITGGMatch> revokedRules = new HashSet<>();
    private Set<CorrespondenceNode> revokedCorrs = new HashSet<>();
    private Set<EObject> revokedModelNodes = new HashSet<>();
    private Set<EMFEdge> revokedEMFEdges = new HashSet<>();

    public VitruviusTGGIbexRedInterpreter(OperationalStrategy operationalStrategy) {
        super(operationalStrategy);
    }

    @Override
    public void revokeOperationalRule(final ITGGMatch match) {
        super.revokeOperationalRule(match);
        revokedRules.add(match);
    }

    @Override
    public void revokeCorr(EObject corr, Set<EObject> nodesToRevoke, Set<EMFEdge> edgesToRevoke) {
        super.revokeCorr(corr, nodesToRevoke, edgesToRevoke);
        // nodesToRevoke and edgesToRevoke are not handled in the super method, but only filled!
        revokedCorrs.add((CorrespondenceNode) corr);
    }

    @Override
    public void revoke(Set<EObject> nodesToRevoke, Set<EMFEdge> edgesToRevoke) {
        super.revoke(nodesToRevoke, edgesToRevoke);
        revokedModelNodes.addAll(nodesToRevoke);
        revokedEMFEdges.addAll(edgesToRevoke);
    }

    public Set<CorrespondenceNode> getRevokedCorrs() {
        return revokedCorrs;
    }

    public Set<EObject> getRevokedModelNodes() {
        return revokedModelNodes;
    }

    public Set<ITGGMatch> getRevokedRules() {
        return revokedRules;
    }

    public Set<EMFEdge> getRevokedEMFEdges() {
        return revokedEMFEdges;
    }
}
