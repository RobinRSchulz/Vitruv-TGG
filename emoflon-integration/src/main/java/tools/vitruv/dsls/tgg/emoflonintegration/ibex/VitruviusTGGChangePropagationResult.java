package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.eclipse.emf.ecore.EObject;
import org.emoflon.ibex.common.emf.EMFEdge;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import runtime.CorrespondenceNode;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents the results of our tgg pattern matching.
 */
public class VitruviusTGGChangePropagationResult {

    private final Set<CorrespondenceNode> addedCorrespondences;

    private final Set<CorrespondenceNode> revokedCorrespondences;
    private final Set<ITGGMatch> revokedMatches;
    private final Set<EObject> revokedModelNodes;
    private final Set<EMFEdge> revokedEMFEdges;

    /**
     * Empty Result
     */
    public VitruviusTGGChangePropagationResult() {
        addedCorrespondences = new HashSet<>();
        revokedCorrespondences = new HashSet<>();
        revokedMatches = new HashSet<>();
        revokedModelNodes = new HashSet<>();
        revokedEMFEdges = new HashSet<>();
    }


    public VitruviusTGGChangePropagationResult(Set<CorrespondenceNode> newlyAddedCorrespondences,
                                               Set<CorrespondenceNode> revokedCorrs,
                                               Set<ITGGMatch> revokedRules,
                                               Set<EObject> revokedModelNodes,
                                               Set<EMFEdge> revokedEMFEdges) {
        this.addedCorrespondences = newlyAddedCorrespondences;
        this.revokedCorrespondences = revokedCorrs;
        this.revokedMatches = revokedRules;
        this.revokedModelNodes = revokedModelNodes;
        this.revokedEMFEdges = revokedEMFEdges;
    }

    public Set<CorrespondenceNode> getAddedCorrespondences() {
        return addedCorrespondences;
    }

    public Set<CorrespondenceNode> getRevokedCorrespondences() {
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
}
