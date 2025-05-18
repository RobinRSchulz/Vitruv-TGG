package tools.vitruv.dsls.tgg.emoflonintegration.patternmatching;

import edu.kit.ipd.sdq.commons.util.java.Pair;
import language.*;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.emoflon.ibex.tgg.operational.strategies.PropagationDirectionHolder.PropagationDirection;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.CreateEObject;
import tools.vitruv.change.atomic.eobject.EobjectFactory;
import tools.vitruv.change.atomic.feature.reference.InsertEReference;
import tools.vitruv.change.atomic.feature.reference.ReferenceFactory;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.atomic.root.RootFactory;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Broken matches, that remain unrepaired by either shortcut rules not being sufficiently applicable or unreliable and thus unused,
 * must be repaired by creating new additive matches from what's broken
 */
public class UnrepairedBrokenMatchOldChangesRetriever {
    protected static final Logger logger = Logger.getLogger(UnrepairedBrokenMatchOldChangesRetriever.class);

    private Set<VitruviusConsistencyMatch> brokenAndUnrepairedMatches;
    private TGGResourceHandler resourceHandler;
    private Set<VitruviusConsistencyMatch> intactMatchesFromProtocol;
    private Collection<TGGRule> rules;
    private PropagationDirection propagationDirection;

    public UnrepairedBrokenMatchOldChangesRetriever(TGGResourceHandler resourceHandler, Collection<TGGRule> rules, Set<VitruviusConsistencyMatch> brokenAndUnrepairedMatches, PropagationDirection propagationDirection) {
        this.brokenAndUnrepairedMatches = brokenAndUnrepairedMatches;
        this.resourceHandler = resourceHandler;
        this.rules = rules;
        this.intactMatchesFromProtocol = Util.getTGGRuleApplicationsWithRules(resourceHandler, rules).entrySet().stream()
                .map(entry -> new VitruviusConsistencyMatch(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());
        this.propagationDirection = propagationDirection;
    }

    public List<EChange<EObject>> createNewChangeSequence() {
        List<VitruviusConsistencyMatch> allBrokenMatches = fixpointIterateBrokenMatches();
        List<EChange<EObject>> newChanges = new LinkedList<>();
        allBrokenMatches.forEach(match -> newChanges.addAll(createEChangesForMatch(match)));
        return newChanges;
    }

    /**
     *
     * @return the fixpoint iteration of the given broken matches, meaning all matches that are broken because the given matches are broken.
     */
    private List<VitruviusConsistencyMatch> fixpointIterateBrokenMatches() {
        Queue<Pair<TGGRuleNode, EObject>> createNodesWorklist = brokenAndUnrepairedMatches.stream()
                .map(VitruviusConsistencyMatch::getMatchedCreateNodes)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toCollection(LinkedList::new));
        List<VitruviusConsistencyMatch> brokenMatches = new LinkedList<>(brokenAndUnrepairedMatches);
        while (!createNodesWorklist.isEmpty()) {
            Pair<TGGRuleNode, EObject> currentCreateNode = createNodesWorklist.remove();
            Set<VitruviusConsistencyMatch> newBrokenMatches = intactMatchesFromProtocol.stream()
                    // EVERY match that contains a CONTEXT node that has been CREATEd in a broken match is also broken!
                    .filter(intactMatch ->
                            intactMatch.getMatchedContextNodes().stream()
                                    .map(Pair::get1)
                                    .collect(Collectors.toSet())
                                    .contains(currentCreateNode.get1()))
                    .collect(Collectors.toSet());
            brokenMatches.addAll(newBrokenMatches);
            createNodesWorklist.addAll(
                    newBrokenMatches.stream()
                            .map(VitruviusConsistencyMatch::getMatchedCreateNodes)
                            .flatMap(Collection::stream)
                            .filter(ruleNodeEObjectPair ->
                                    ruleNodeEObjectPair.get0().getBindingType().equals(BindingType.CREATE))
                            .collect(Collectors.toSet())
            );
        }
        return brokenMatches;
    }

    private List<EChange<EObject>> createEChangesForMatch(VitruviusConsistencyMatch match) {
        List<EChange<EObject>> changes = new LinkedList<>();

        // Each CREATE node object gets a CreateEObject EChange. If the CREATE node has no incoming edges (Context or create), it also gets an InsertRootEObject EChange
        for (Pair<TGGRuleNode, EObject> tggRuleNodeEObjectPair : match.getMatchedCreateNodes()) {
            //we only want to look at eObjects that are still exist
            if (!eObjectHasBeenDeleted(tggRuleNodeEObjectPair.get1())) {
                TGGRuleNode ruleNode = tggRuleNodeEObjectPair.get0();
                EObject eObject = tggRuleNodeEObjectPair.get1();
                CreateEObject<EObject> createEObjectEChange = EobjectFactory.eINSTANCE.createCreateEObject();
                createEObjectEChange.setAffectedElement(eObject);
                createEObjectEChange.setAffectedEObjectType(eObject.eClass());
                changes.add(createEObjectEChange);
                if (ruleNode.getIncomingEdges().stream()
                        .filter(tggRuleEdge -> Set.of(BindingType.CREATE, BindingType.CONTEXT).contains(tggRuleEdge.getBindingType()))
                        .findAny().isEmpty()) {
                    // no incoming CONTEXT or CREATE edges? --> this must be a root node
                    InsertRootEObject<EObject> insertRootEObjectEChange = RootFactory.eINSTANCE.createInsertRootEObject();
                    insertRootEObjectEChange.setResource(eObject.eResource());
                    insertRootEObjectEChange.setNewValue(eObject);
                    changes.add(insertRootEObjectEChange);
                }
            }
        }

        // we must also handle all CREATE edges from the invalidated matches for them to be potientally matchable again.
        for (TGGRuleEdge ruleEdge : match.getTggRule().getEdges()) {
            if (ruleEdge.getBindingType().equals(BindingType.CREATE)
                    && ruleEdge.getDomainType().equals(propagationDirection.equals(PropagationDirection.FORWARD) ? DomainType.SRC : DomainType.TRG)) {
                EObject affectedElement = (EObject) match.get(ruleEdge.getSrcNode().getName());
                EObject newValue = (EObject) match.get(ruleEdge.getTrgNode().getName());
                EReference affectedFeature = ruleEdge.getType();

                InsertEReference<EObject> insertEReference = ReferenceFactory.eINSTANCE.createInsertEReference();
                insertEReference.setAffectedElement(affectedElement);
                insertEReference.setNewValue(newValue);
                insertEReference.setAffectedFeature(affectedFeature);

                if (affectedElement != null) {
                    Object actualValue = affectedElement.eGet(affectedFeature);
                    if (actualValue != null) {
                        if (affectedFeature.isMany() && ((EList<?>) actualValue).contains(newValue)) {
                            changes.add(insertEReference);
                        } else if (actualValue.equals(newValue)) {
                            changes.add(insertEReference);
                        }
                    }
                }
            }
        }
        return changes;
    }

    private boolean eObjectHasBeenDeleted(EObject eObject) {
//        EcoreUtil.delete();
////        if (eObject.eIsProxy()) return true;
        if (eObject == null) return true;
//        InternalEObject internalEObject = (InternalEObject) eObject;
        Resource resource = eObject.eResource();
        return resource == null;
//        if (eObject.eContainer() == null || (resource != null && resource.getContents().c)) return true;
//        return (resource == null) ? true : !resource.getContents().contains(eObject);
    }
}
