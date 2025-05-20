package tools.vitruv.dsls.tgg.emoflonintegration.patternmatching;

import language.BindingType;
import language.TGGRule;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.emoflon.ibex.common.operational.IMatch;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;
import runtime.TGGRuleApplication;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.CreateEObject;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.change.atomic.feature.UnsetFeature;
import tools.vitruv.change.atomic.feature.attribute.RemoveEAttributeValue;
import tools.vitruv.change.atomic.feature.attribute.UpdateAttributeEChange;
import tools.vitruv.change.atomic.feature.reference.RemoveEReference;
import tools.vitruv.change.atomic.feature.single.ReplaceSingleValuedFeatureEChange;
import tools.vitruv.change.atomic.root.RemoveRootEObject;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Detects broken matches based on corrupt protocol nodes and on deleting changes in a given {@link VitruviusChange}.
 */
public class VitruviusChangeBrokenMatchMatcher {
    static Logger logger = Logger.getLogger(VitruviusChangePatternMatcher.class);

    private final VitruviusChange<EObject> vitruviusChange;
    private final Collection<TGGRule> rules;

    private Set<VitruviusConsistencyMatch> brokenMatchesCache;

    /**
     *
     * @param vitruviusChange the change sequence to base the broken match detection on.
     */
    public VitruviusChangeBrokenMatchMatcher(VitruviusChange<EObject> vitruviusChange, Collection<TGGRule> rules) {
        this.vitruviusChange = vitruviusChange;
        this.rules = rules;
    }

    /**
     * Calculates broken matches if not already calculated, and returns them.
     * @param resourceHandler for access to the protocol.
     * @return matches from the protocol that are broken by the given change sequence ({@link VitruviusChange}).
     */
    public Set<VitruviusConsistencyMatch> getBrokenMatches(TGGResourceHandler resourceHandler) {
        if (brokenMatchesCache == null) {
            Map<TGGRuleApplication, TGGRule> tggRuleApplicationTGGRuleMap = Util.getTGGRuleApplicationsWithRules(resourceHandler, rules);
            Set<VitruviusConsistencyMatch> matches = getNodeMissingBrokenMatches(tggRuleApplicationTGGRuleMap);

            // we only want to find NEW matches. That also ensures those matches being complete, i.e. having all their nodes!
            matches.forEach(match -> tggRuleApplicationTGGRuleMap.remove(match.getRuleApplicationNode()));
            matches.addAll(getAdditionalBrokenMatches(tggRuleApplicationTGGRuleMap));
            matches.addAll(getMatchesBrokenByAttributeChanges(tggRuleApplicationTGGRuleMap));
            brokenMatchesCache = matches;
        }
        return brokenMatchesCache;
    }

    /**
     * @return matches that are broken because of a marker that doesn't cover all CONTEXT and CREATE nodes anymore (meaning that some EObject must have been deleted).
     */
    private Set<VitruviusConsistencyMatch> getNodeMissingBrokenMatches(Map<TGGRuleApplication, TGGRule> tggRuleApplicationTGGRuleMap) {
        Set<VitruviusConsistencyMatch> brokenMatches = tggRuleApplicationTGGRuleMap.keySet().stream()
                .filter(tggRuleApplication -> {
                    // is there any node that is null but shouldn't?
                    TGGRule tggRule = tggRuleApplicationTGGRuleMap.get(tggRuleApplication);
                    return tggRule.getNodes().stream()
                            .filter(ruleNode -> // we only look at CONTEXT or CREATE node, those all need to be present for a Match to not be broken.
                                    Set.of(BindingType.CONTEXT, BindingType.CREATE).contains(ruleNode.getBindingType()))
                            .filter(ruleNode ->
                                    tggRuleApplication.eGet(tggRuleApplication.eClass().getEStructuralFeature(Util.getMarkerStyleName(ruleNode))) == null

                            )
                            .anyMatch(ruleNode ->
                                    tggRuleApplication.eGet(tggRuleApplication.eClass().getEStructuralFeature(Util.getMarkerStyleName(ruleNode))) == null);
                })
                .map(tggRuleApplication -> new VitruviusConsistencyMatch(tggRuleApplication, tggRuleApplicationTGGRuleMap.get(tggRuleApplication))).collect(Collectors.toSet());
        logger.debug("  Calculated broken matches: \n    - " + brokenMatches.stream().map(IMatch::toString).collect(Collectors.joining("\n    - ")));
        return brokenMatches;
    }

    private Set<VitruviusConsistencyMatch> getMatchesBrokenByAttributeChanges(Map<TGGRuleApplication, TGGRule> tggRuleApplicationTGGRuleMap) {
        Set<UpdateAttributeEChange<EObject>> updateAttributeEChanges = vitruviusChange.getEChanges().stream()
                .filter(eChange -> eChange instanceof UpdateAttributeEChange<EObject>)
                .map(eChange -> (UpdateAttributeEChange<EObject>) eChange)
                .collect(Collectors.toSet());
        // only look at EChanges whose affected EObject has not been created in the current change sequence.
        Set<UpdateAttributeEChange<EObject>> relevantUpdateAttributeEChanges = updateAttributeEChanges.stream().filter(updateAttributeEChange -> {
            EObject affectedEObject = updateAttributeEChange.getAffectedElement();
            return vitruviusChange.getEChanges().stream()
                    .filter(eChange -> eChange instanceof CreateEObject<EObject>)
                    .map(eChange -> ((CreateEObject<EObject>) eChange).getAffectedElement())
                    .noneMatch(createdEObject -> createdEObject.equals(affectedEObject));
        }).collect(Collectors.toSet());
         Set<VitruviusConsistencyMatch> matches = new HashSet<>();
         relevantUpdateAttributeEChanges.forEach(updateAttributeEChange -> {
             // get the Marker/ pattern application where it was CREATEd
             for (Map.Entry<TGGRuleApplication, TGGRule> entry : tggRuleApplicationTGGRuleMap.entrySet()) {
                 TGGRuleApplication tggRuleApplication = entry.getKey();
                 TGGRule tggRule = entry.getValue();
                 //check if there are any pattern applications where the AE of updateAttributeEChange is covered with a CREATE node.
                 if (tggRule.getNodes().stream().filter(tggRuleNode -> tggRuleNode.getBindingType().equals(BindingType.CREATE))
                         .anyMatch(ruleNode -> {
                             EObject candidate = (EObject) tggRuleApplication.eGet(tggRuleApplication.eClass().getEStructuralFeature(Util.getMarkerStyleName(ruleNode)));
                             return updateAttributeEChange.getAffectedElement().equals(candidate);
                         })) {
                     matches.add(new VitruviusConsistencyMatch(tggRuleApplication, tggRule));
                     break;
                 }

             }
         });
         return matches;
    }

    private Set<VitruviusConsistencyMatch> getAdditionalBrokenMatches(Map<TGGRuleApplication, TGGRule> intactTGGRuleApplicationTGGRuleMap) {
        return vitruviusChange.getEChanges().stream()
                .filter(eChange -> !Util.isCreatingOrAdditiveEChange(eChange))
                .filter(eChange -> !(eChange instanceof DeleteEObject<EObject>)) // those are handled by getNodeMissingBrokenMatches already!
                .filter(breakingEChange -> {
                    if (breakingEChange instanceof ReplaceSingleValuedFeatureEChange<EObject, ?, ?> replaceSingleValuedFeatureEChange) {
                        // covers: ReplaceSingleValuedEAttribute, ReplaceSingleValuedEReference
                        if (!replaceSingleValuedFeatureEChange.getNewValue().equals(replaceSingleValuedFeatureEChange.getNewValue())) {
                            logger.warn("non-additive ReplaceSingleValuedFeatureEChanges not covered currently! Ignoring the following eChange: " + Util.eChangeToString(replaceSingleValuedFeatureEChange));
                        }
                        return false;
                    } else return true;
                })
                .map(breakingEChange -> handleNonTrivialBreakingEChange(breakingEChange, intactTGGRuleApplicationTGGRuleMap))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    /**
     * This detects match-break cases where the concerned EObjects have not been deleted. E.g. if a reference was created by the green match but the node still remains.
     *
     * @param intactTGGRuleApplicationTGGRuleMap contains markers (and their respective rules) that are INTACT (not missing any nodes) and should be checked
     * @return broken matches that don't miss a node.
     */
    private Set<VitruviusConsistencyMatch> handleNonTrivialBreakingEChange(EChange<EObject> breakingChange, Map<TGGRuleApplication, TGGRule> intactTGGRuleApplicationTGGRuleMap) {
        logger.trace("  handle breaking change: " + Util.eChangeToString(breakingChange));
        Set<TGGRuleApplication> calculatedBrokenTGGRuleApplications = Set.of();
        switch (breakingChange) {
            case DeleteEObject<EObject> deleteEObject -> logger.warn("  DeleteEObject changes are not handled, here!: " + Util.eChangeToString(deleteEObject));
            case ReplaceSingleValuedFeatureEChange<EObject, ?, ?> replaceSingleValuedFeatureEChange -> {
                // captures ReplaceSingleValuedEReference and ReplaceSingleValuedEAttribute
                throw new RuntimeException("This should not occur! ReplaceSingleValuedFeatureEChange must be converted!");
            }
            case RemoveRootEObject<EObject> removeRootEObject ->
                    calculatedBrokenTGGRuleApplications = getMarkersWhereEObjectOccursAs(removeRootEObject.getOldValue(),
                            Set.of(BindingType.CREATE), intactTGGRuleApplicationTGGRuleMap);
            case RemoveEAttributeValue<EObject, ?> removeEAttributeValue -> logger.debug("RemoveEAttributeValue EChanges that only change attributes are ignored: " + Util.eChangeToString(removeEAttributeValue));
            case RemoveEReference<EObject> removeEReference ->
                    calculatedBrokenTGGRuleApplications =
                            getMarkersWhereAEReferencesValuePossiblyIndexed(removeEReference.getAffectedElement(), removeEReference.getAffectedFeature(),
                                    removeEReference.getOldValue(), removeEReference.getIndex(), intactTGGRuleApplicationTGGRuleMap);
            case UnsetFeature<EObject, ?> unsetFeature ->
                    calculatedBrokenTGGRuleApplications = getMarkersWhereFeatureOccurs(unsetFeature.getAffectedElement(), unsetFeature.getAffectedFeature(), intactTGGRuleApplicationTGGRuleMap);
            case EChange<EObject> eChange1 -> throw new IllegalStateException("Inconcrete eChange: " + eChange1);
        }
        return calculatedBrokenTGGRuleApplications.stream()
                .map(tggRuleApplication -> new VitruviusConsistencyMatch(tggRuleApplication, intactTGGRuleApplicationTGGRuleMap.get(tggRuleApplication)))
                .collect(Collectors.toSet());

    }

    /**
        1. AE --(F)--> V     (manyValued-case: index I)
        2. AE must be present in the change!
        3. --> Get all Markers containing AE
     */
    private Set<TGGRuleApplication> getMarkersWhereAEReferencesValuePossiblyIndexed(EObject affectedEObject, EReference eReference, EObject value, int index, Map<TGGRuleApplication, TGGRule> intactTGGRuleApplicationTGGRuleMap) {
        //We cannot check the index. We CAN check whether the value occurs twice in the list and ignore if that is the case
        if (eReference.isMany()) {
            EList<EObject> eList = (EList<EObject>) affectedEObject.eGet(eReference);
            if (eList != null && eList.contains(value)) {
                // contains(value) means that the list still contains the eObject after it has been deleted.
                throw new IllegalStateException("We do not support lists where elements occur twice. TODO make this a warning?");
            }
        }

        Set<BindingType> bindingTypes = Set.of(BindingType.CONTEXT, BindingType.CREATE);
        return getMatchingMarkers(marker2TGGRule -> {
            TGGRuleApplication marker = marker2TGGRule.getKey();
            TGGRule rule = marker2TGGRule.getValue();
            return rule.getNodes().stream()
                    .filter(ruleNode -> bindingTypes.contains(ruleNode.getBindingType()))
                    .filter(ruleNode -> ruleNode.getType().isSuperTypeOf(affectedEObject.eClass()))
                    // filter rule nodes that match the marker and the AE
                    .filter(tggRuleNode -> affectedEObject.equals(marker.eGet(marker.eClass().getEStructuralFeature(Util.getMarkerStyleName(tggRuleNode)))))
                    // check references (type and marker)
                    .anyMatch(ruleNode -> ruleNode.getOutgoingEdges().stream()
                            .filter(tggRuleEdge -> eReference.equals(tggRuleEdge.getType()))
                            // check the VALUE on the marker. if the reference is many-valued, we have to check List and index, otherwise only equality.
                            .anyMatch(tggRuleEdge -> {
                                Object eGetReturn = marker.eGet(marker.eClass().getEStructuralFeature(Util.getMarkerStyleName(tggRuleEdge.getTrgNode())));
                                return value.equals(eGetReturn);
                            }));
        }, intactTGGRuleApplicationTGGRuleMap);
    }

    private Set<TGGRuleApplication> getMarkersWhereFeatureOccurs(EObject affectedEObject, EStructuralFeature feature, Map<TGGRuleApplication, TGGRule> intactTGGRuleApplicationTGGRuleMap) {
        Set<BindingType> bindingTypes = Set.of(BindingType.CONTEXT, BindingType.CREATE);
        return getMatchingMarkers(marker2TGGRule -> {
            TGGRuleApplication marker = marker2TGGRule.getKey();
            TGGRule rule = marker2TGGRule.getValue();
            return rule.getNodes().stream()
                    .filter(ruleNode -> bindingTypes.contains(ruleNode.getBindingType()))
                    .filter(ruleNode -> ruleNode.getType().isSuperTypeOf(affectedEObject.eClass()))
                    // filter rule nodes that match the marker and the AE
                    .filter(tggRuleNode -> affectedEObject.equals(marker.eGet(marker.eClass().getEStructuralFeature(Util.getMarkerStyleName(tggRuleNode)))))
                    // check references (type and marker)
                    .anyMatch(ruleNode -> ruleNode.getOutgoingEdges().stream()
                            .anyMatch(tggRuleEdge -> feature.equals(tggRuleEdge.getType()))
                            );
        }, intactTGGRuleApplicationTGGRuleMap);
    }

    /**
     * @return all markers where the eObject is contained as an invocation of one of the rules' nodes.
     *          The ruleNode's bindingType must be contained in the given set.
     */
    private Set<TGGRuleApplication> getMarkersWhereEObjectOccursAs(EObject eObject, Set<BindingType> bindingTypes, Map<TGGRuleApplication, TGGRule> tggRuleApplications2TGGRulesMap) {
        return getMatchingMarkers(marker2TGGRule -> {
            TGGRuleApplication marker = marker2TGGRule.getKey();
            return marker2TGGRule.getValue().getNodes().stream()
                    .filter(tggRuleNode -> bindingTypes.contains(tggRuleNode.getBindingType()))
                    .filter(tggRuleNode -> tggRuleNode.getType().isSuperTypeOf(eObject.eClass())) // not necessary
                    .anyMatch(tggRuleNode -> eObject.equals( // can the eObject be found anywhere in the marker?
                            marker.eGet(marker.eClass().getEStructuralFeature(Util.getMarkerStyleName(tggRuleNode)))));
        }, tggRuleApplications2TGGRulesMap);
    }

    private Set<TGGRuleApplication> getMatchingMarkers(Predicate<Map.Entry<TGGRuleApplication, TGGRule>> ruleFilter, Map<TGGRuleApplication, TGGRule> intactTGGRuleApplicationTGGRuleMap) {
        return intactTGGRuleApplicationTGGRuleMap.entrySet().stream()
                .filter(ruleFilter)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

}
