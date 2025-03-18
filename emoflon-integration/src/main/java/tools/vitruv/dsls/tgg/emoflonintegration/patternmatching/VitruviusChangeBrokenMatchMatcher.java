package tools.vitruv.dsls.tgg.emoflonintegration.patternmatching;

import language.BindingType;
import language.TGGRule;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.emoflon.ibex.common.operational.IMatch;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;
import runtime.TGGRuleApplication;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.change.atomic.feature.UnsetFeature;
import tools.vitruv.change.atomic.feature.attribute.RemoveEAttributeValue;
import tools.vitruv.change.atomic.feature.reference.RemoveEReference;
import tools.vitruv.change.atomic.feature.single.ReplaceSingleValuedFeatureEChange;
import tools.vitruv.change.atomic.root.RemoveRootEObject;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class VitruviusChangeBrokenMatchMatcher {
    static Logger logger = Logger.getLogger(VitruviusChangePatternMatcher.class);

    private final VitruviusChange<EObject> vitruviusChange;
    private final Collection<TGGRule> rules;

    public VitruviusChangeBrokenMatchMatcher(VitruviusChange<EObject> vitruviusChange, Collection<TGGRule> rules) {
        this.vitruviusChange = vitruviusChange;
        this.rules = rules;
    }

    public Set<ITGGMatch> getBrokenMatches(TGGResourceHandler resourceHandler) {
        //TODO might need to recursively invalidate matches: nodes created by the found broken matches that occur in OTHER, intact, matches invalidate these matches and nodes created by those.
        // might be that this is handled by the RedInterpreter or whatever --> need to check!
        Map<TGGRuleApplication, TGGRule> tggRuleApplicationTGGRuleMap = Util.getTGGRuleApplicationsWithRules(resourceHandler, rules);
        Set<ITGGMatch> matches = getNodeMissingBrokenMatches(resourceHandler, tggRuleApplicationTGGRuleMap);

        // we only want to find NEW matches. That also ensures those matches being complete, i.e. having all their nodes!
        matches.forEach(match -> tggRuleApplicationTGGRuleMap.remove(match.getRuleApplicationNode()));
        matches.addAll(getAdditionalBrokenMatches(resourceHandler, tggRuleApplicationTGGRuleMap));
        return matches;
    }

    /**
     * @return matches that are broken because of a marker that doesn't cover all CONTEXT and CREATE nodes anymore (meaning that some EObject must have been deleted).
     */
    private Set<ITGGMatch> getNodeMissingBrokenMatches(TGGResourceHandler resourceHandler, Map<TGGRuleApplication, TGGRule> tggRuleApplicationTGGRuleMap) {
        Set<ITGGMatch> brokenMatches = tggRuleApplicationTGGRuleMap.keySet().stream()
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
        logger.warn("  Calculated broken matches: \n    - " + brokenMatches.stream().map(IMatch::toString).collect(Collectors.joining("\n    - ")));
        return brokenMatches;
    }

    /**
     * We currently do not cover all cases.
     * If we report broken matches that "free" (in terms of eMoflon "unmark") EObjects from being covered by a pattern application,
     * and that is not handled by the application of short-cut rules in the SYNC algorithm, we cannot cover this (yet).
     * It would be solveable like this:
     * <ol>
     *    <li>detect the subgraph of EObjects + interrelations that are unmarked</li>
     *    <li>generate EChanges for that subgraph</li>
     *    <li>generate new forward matches with {@link VitruviusChangePatternMatcher#getForwardMatches()}</li>
     *    <li>reiterate...</li>
     * </ol>
     *
     * TODO we currently ignore
     *
     * @param resourceHandler provides access to the protocol resource.
     */
    private Set<ITGGMatch> getAdditionalBrokenMatches(TGGResourceHandler resourceHandler, Map<TGGRuleApplication, TGGRule> intactTGGRuleApplicationTGGRuleMap) {
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
     * @param breakingChange
     * @param intactTGGRuleApplicationTGGRuleMap contains markers (and their respecitve rules) that are INTACT (not missing any nodes) and should be checked
     * @return broken matches that don't miss a node.
     */
    private Set<ITGGMatch> handleNonTrivialBreakingEChange(EChange<EObject> breakingChange, Map<TGGRuleApplication, TGGRule> intactTGGRuleApplicationTGGRuleMap) {
        logger.warn("  handle breaking change: " + Util.eChangeToString(breakingChange));
        Set<TGGRuleApplication> calculatedBrokenTGGRuleApplications = Set.of();
        switch (breakingChange) {
            case DeleteEObject<EObject> deleteEObject -> {
                logger.warn("  DeleteEObject changes are not handled, here!: " + Util.eChangeToString(deleteEObject));
            }
            case ReplaceSingleValuedFeatureEChange<EObject, ?, ?> replaceSingleValuedFeatureEChange -> {
                // captures ReplaceSingleValuedEReference and ReplaceSingleValuedEAttribute
                throw new RuntimeException("todo implement");
            }
            case RemoveRootEObject<EObject> removeRootEObject -> {
                calculatedBrokenTGGRuleApplications = getMarkersWhereEObjectOccursAs(removeRootEObject.getOldValue(),
                        Set.of(BindingType.CREATE), intactTGGRuleApplicationTGGRuleMap);
            }
            case RemoveEAttributeValue<EObject, ?> removeEAttributeValue -> {
                logger.debug("RemoveEAttributeValue EChanges that only change attributes are ignored: " + Util.eChangeToString(removeEAttributeValue));
            }
            case RemoveEReference<EObject> removeEReference -> {
                calculatedBrokenTGGRuleApplications = getMarkersWhereAEReferencesValuePossiblyIndexed(removeEReference.getAffectedElement(), removeEReference.getAffectedFeature(),
                        removeEReference.getOldValue(), removeEReference.getIndex(), intactTGGRuleApplicationTGGRuleMap);
            }
            case UnsetFeature<EObject, ?> unsetFeature -> {
                calculatedBrokenTGGRuleApplications = getMarkersWhereFeatureOccurs(unsetFeature.getAffectedElement(), unsetFeature.getAffectedFeature(), intactTGGRuleApplicationTGGRuleMap);
            }
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
                throw new IllegalStateException("We do not support lists where elements occur twice. TODO make this a warning?");
            }
        }

        Set<BindingType> bindingTypes = Set.of(BindingType.CONTEXT, BindingType.CREATE);
        return getMatchingMarkers(marker2TGGRule -> {
            TGGRuleApplication marker = marker2TGGRule.getKey();
            TGGRule rule = marker2TGGRule.getValue();
            return rule.getNodes().stream()
                    .filter(ruleNode -> bindingTypes.contains(ruleNode.getBindingType()))
                    .filter(ruleNode -> affectedEObject.eClass().equals(ruleNode.getType()))
                    // filter rule nodes that match the marker and the AE
                    .filter(tggRuleNode -> affectedEObject.equals(marker.eGet(marker.eClass().getEStructuralFeature(Util.getMarkerStyleName(tggRuleNode)))))
                    // check references (type and marker)
                    .anyMatch(ruleNode -> ruleNode.getOutgoingEdges().stream()
                            .filter(tggRuleEdge -> eReference.equals(tggRuleEdge.getType())) //TODO is name must-have? probably not...
                            // check the VALUE on the marker. if the reference is manyvalued, we have to check List and index, otherwise only equality.
                            .anyMatch(tggRuleEdge -> {
                                Object eGetReturn = marker.eGet(marker.eClass().getEStructuralFeature(Util.getMarkerStyleName(tggRuleEdge.getTrgNode())));
                                // the following cannot be handled!
//                                if (eReference.isMany()) {
//                                }
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
                    .filter(ruleNode -> affectedEObject.eClass().equals(ruleNode.getType()))
                    // filter rule nodes that match the marker and the AE
                    .filter(tggRuleNode -> affectedEObject.equals(marker.eGet(marker.eClass().getEStructuralFeature(Util.getMarkerStyleName(tggRuleNode)))))
                    // check references (type and marker)
                    .anyMatch(ruleNode -> ruleNode.getOutgoingEdges().stream()
                            .anyMatch(tggRuleEdge -> feature.eClass().equals(tggRuleEdge.getType()) && tggRuleEdge.getName().equals(feature.getName())) //TODO is name must-have? probably not...
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
                    .filter(tggRuleNode -> eObject.eClass().equals(tggRuleNode.getType())) // not necessary
                    .anyMatch(tggRuleNode -> {
                        return eObject.equals( // can the eObject be found anywhere in the marker?
                                marker.eGet(marker.eClass().getEStructuralFeature(Util.getMarkerStyleName(tggRuleNode))));
                    });
        }, tggRuleApplications2TGGRulesMap);
    }

    private Set<TGGRuleApplication> getMatchingMarkers(Predicate<Map.Entry<TGGRuleApplication, TGGRule>> ruleFilter, Map<TGGRuleApplication, TGGRule> intactTGGRuleApplicationTGGRuleMap) {
        return intactTGGRuleApplicationTGGRuleMap.entrySet().stream()
                .filter(ruleFilter)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

}
