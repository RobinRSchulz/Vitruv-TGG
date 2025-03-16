package tools.vitruv.dsls.tgg.emoflonintegration.patternmatching;

import com.ibm.icu.impl.Pair;
import language.BindingType;
import language.TGGRule;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.emoflon.ibex.common.operational.IMatch;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;
import runtime.Protocol;
import runtime.TGGRuleApplication;
import tools.vitruv.change.atomic.AdditiveEChange;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.SubtractiveEChange;
import tools.vitruv.change.atomic.eobject.CreateEObject;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.change.atomic.feature.UnsetFeature;
import tools.vitruv.change.atomic.feature.attribute.RemoveEAttributeValue;
import tools.vitruv.change.atomic.feature.attribute.ReplaceSingleValuedEAttribute;
import tools.vitruv.change.atomic.feature.reference.RemoveEReference;
import tools.vitruv.change.atomic.feature.reference.ReplaceSingleValuedEReference;
import tools.vitruv.change.atomic.feature.single.ReplaceSingleValuedFeatureEChange;
import tools.vitruv.change.atomic.root.RemoveRootEObject;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;

import java.util.*;
import java.util.stream.Collectors;

public class VitruviusChangeBrokenMatchMatcher {
    static Logger logger = Logger.getLogger(VitruviusChangePatternMatcher.class);

    private final VitruviusChange<EObject> vitruviusChange;
    private final Collection<TGGRule> rules;

    public VitruviusChangeBrokenMatchMatcher(VitruviusChange<EObject> vitruviusChange, Collection<TGGRule> rules) {
        this.vitruviusChange = vitruviusChange;
        this.rules = rules;
    }

    /**
     * TODO return further broken matches, since only looking at deleted nodes in markers doesn't cut it..
     * To that end, modify {@link VitruviusChangeBrokenMatchMatcher#getBrokenMatches(TGGResourceHandler)} to not try to match everything
     * and further implement it.
     * @param resourceHandler
     * @return
     */
    public Set<IMatch> getBrokenMatchesSimple(TGGResourceHandler resourceHandler) {
        logger.warn("*~*~*~*~*~*~*~*~*~*~*~* GET BROKEN MaTCHES simple! *~*~*~*~*~*~*~*~*~*~");
        Optional<Set<TGGRuleApplication>> protocolStepsOptional = Util.getProtocolSteps(resourceHandler);
        if (protocolStepsOptional.isEmpty()) { return Set.of(); }
        Set<TGGRuleApplication> protocolSteps = protocolStepsOptional.get();
        protocolSteps.forEach(tggRuleApplicationStep -> logger.info("protocolEObject " + Util.eObjectToString(tggRuleApplicationStep)));
        Map<TGGRuleApplication, TGGRule> tggRuleApplicationTGGRuleMap = mapTGGRuleApplicationsToTGGRules(protocolSteps);

        Set<IMatch> brokenMatches = protocolSteps.stream()
                .filter(tggRuleApplication -> {
                    // is there any node that is null but shouldn't?
                    TGGRule tggRule = tggRuleApplicationTGGRuleMap.get(tggRuleApplication);
                    return tggRule.getNodes().stream()
                            .filter(ruleNode -> // we only look at CONTEXT or CREATE node, those all need to be present for a Match to not be broken.
                                    Set.of(BindingType.CONTEXT, BindingType.CREATE).contains(ruleNode.getBindingType()))
                            .filter(ruleNode ->
                                    tggRuleApplication.eGet(tggRuleApplication.eClass().getEStructuralFeature(Util.getMarkerStyleName(ruleNode))) == null

                            ).peek(ruleNode ->
                                    logger.warn("  --this rulenode is not mapped in the tggRuleApplication: \n    ruleNode="
                                            + Util.tGGRuleNodeToString(ruleNode) + "\n    tggRuleApplication=" + Util.eObjectToString(tggRuleApplication)))
                            .anyMatch(ruleNode ->
                                    tggRuleApplication.eGet(tggRuleApplication.eClass().getEStructuralFeature(Util.getMarkerStyleName(ruleNode))) == null);
                })
                .map(tggRuleApplication -> this.ruleApplicationToBrokenMatch(
                tggRuleApplication,
                tggRuleApplicationTGGRuleMap.get(tggRuleApplication)
        )).collect(Collectors.toSet());
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
    public void getBrokenMatches(TGGResourceHandler resourceHandler) {
        // we need
        /*
            1. Protocol     todo look at ibex repo how that is handled
            2. to look at all EChanges that translate to broken matches
            3. to correlate them with protocol
            3. to filter out the matches that ::getForwardMatches gets!
         */
        logger.warn("*~*~*~*~*~*~*~*~*~*~*~* GET BROKEN MaTCHES! *~*~*~*~*~*~*~*~*~*~");
        Optional<Set<TGGRuleApplication>> protocolStepsOptional = Util.getProtocolSteps(resourceHandler);
        if (protocolStepsOptional.isEmpty()) { return; }
        Set<TGGRuleApplication> protocolSteps = protocolStepsOptional.get();
        protocolSteps.forEach(tggRuleApplicationStep -> logger.info("protocolEObject " + Util.eObjectToString(tggRuleApplicationStep)));
        Map<TGGRuleApplication, TGGRule> tggRuleApplicationTGGRuleMap = mapTGGRuleApplicationsToTGGRules(protocolSteps);

        // 2. iterate breaking changes
        vitruviusChange.getEChanges().stream()
                .filter(eChange -> !Util.isCreatingOrAdditiveEChange(eChange))
                .filter(breakingEChange -> {
                    if (breakingEChange instanceof ReplaceSingleValuedFeatureEChange<EObject, ?, ?>) {
                        // covers: ReplaceSingleValuedEAttribute, ReplaceSingleValuedEReference
                        //TODO ist sowohl additiv als auch subtraktiv! wie behandeln??
                        // müsste man ggf in delete + create aufsplitten --> TODO issue schreiben
                        logger.warn("ReplaceSingleValuedEAttribute not covered! Ignoring the following eChange: " + Util.eChangeToString(breakingEChange));
                        return false;
                    } else return true;
                }).forEach(breakingEChange -> handleBreakingEChange(breakingEChange, tggRuleApplicationTGGRuleMap));
        //todo let handleBreakingCHange return Set<IMatch> or whatever and flatmap(..).collectToSet instead of forEach...

                    /* todo
                        1. find out which EObjects are introduced by which marker (from the protocol)   --> Map<EObject, Marker<?>
                        2. find out which eObject(s) this breakingEChange deletes.                      --> Set<EObject> eObjectsBrokenByThisEChange
                        2.5 flatmap                                                                     --> Set<EObject> eObjectsBroken
                        3. look up all EObjects in the former map, getting a                            --> Set<Marker> brokenMarkers
                        3. wurschtel that into a match somehow (maybe that can be gotten easily from the marker!)
                     */
    }

    private Set<IMatch> handleBreakingEChange(EChange<EObject> breakingChange, Map<TGGRuleApplication, TGGRule> tggRuleApplications2TGGRulesMap) {
        logger.warn("  handle breaking change: " + Util.eChangeToString(breakingChange));
        switch (breakingChange) {
            case DeleteEObject<EObject> deleteEObject -> {
                Set<TGGRuleApplication> matchingRuleApplications = getMarkersWhereEObjectOccursAs(deleteEObject.getAffectedElement(),
                        Set.of(BindingType.CONTEXT, BindingType.CREATE), tggRuleApplications2TGGRulesMap);
                logger.warn("  matchingRuleApplications: ");
                matchingRuleApplications.forEach(ruleApplication -> {
                    logger.warn("  - " + Util.eObjectToString(ruleApplication));
                });
                return matchingRuleApplications.stream().map(tggRuleApplication -> this.ruleApplicationToBrokenMatch(
                        tggRuleApplication,
                        tggRuleApplications2TGGRulesMap.get(tggRuleApplication)
                )).collect(Collectors.toSet());
            }
            case ReplaceSingleValuedFeatureEChange<EObject, ?, ?> replaceSingleValuedFeatureEChange -> {
                // captures ReplaceSingleValuedEReference and ReplaceSingleValuedEAttribute
                // TODO look how to handle...
                throw new RuntimeException("todo implement");
            }
            case RemoveRootEObject<EObject> removeRootEObject -> {
                Set<TGGRuleApplication> matchingRuleApplications = getMarkersWhereEObjectOccursAs(removeRootEObject.getOldValue(),
                        Set.of(BindingType.CREATE), tggRuleApplications2TGGRulesMap);
                logger.warn("  matchingRuleApplications: ");
                matchingRuleApplications.forEach(ruleApplication -> {
                    logger.warn("  - " + Util.eObjectToString(ruleApplication));
                });
                return matchingRuleApplications.stream().map(tggRuleApplication -> this.ruleApplicationToBrokenMatch(
                        tggRuleApplication,
                        tggRuleApplications2TGGRulesMap.get(tggRuleApplication)
                )).collect(Collectors.toSet());
            }
            case RemoveEAttributeValue<EObject, ?> removeEAttributeValue -> {
                logger.debug("RemoveEAttributeValue EChanges that only change attributes are ignored: " + Util.eChangeToString(removeEAttributeValue));
                return Set.of();
            }
            case RemoveEReference<EObject> removeEReference -> {
                logger.error("THIS IS TO BE REPLACED WTH A THROWS, and then an impl...");
                //TODO impl!
                return Set.of();
            }
            case UnsetFeature<EObject, ?> unsetFeature -> {
                throw new RuntimeException("todo implement");
            }
            case EChange<EObject> eChange1 -> throw new IllegalStateException("Inconcrete eChange: " + eChange1);
        }
    }
    private IMatch ruleApplicationToBrokenMatch(TGGRuleApplication ruleApplication, TGGRule tggRule) {
        return new VitruviusBrokenMatch(ruleApplication, tggRule);
    }

    /**
     * @return all markers where the eObject is contained as an invocation of one of the rules' nodes.
     *          The ruleNode's bindingType must be contained in the given set.
     */
    private Set<TGGRuleApplication> getMarkersWhereEObjectOccursAs(EObject eObject, Set<BindingType> bindingTypes,
                                                                   Map<TGGRuleApplication, TGGRule> tggRuleApplications2TGGRulesMap) {
        return tggRuleApplications2TGGRulesMap.entrySet().stream()
                .filter(marker2TGGRule -> {
                    TGGRuleApplication marker = marker2TGGRule.getKey();
                    logger.warn("    Trying to match marker=" + Util.eObjectToString(marker) + ", rule=" + marker2TGGRule.getValue().getName());
                    logger.warn("      All StrucFeats from the marker: " + marker.eClass().getEAllStructuralFeatures().stream()
                            .map(ENamedElement::getName).collect(Collectors.joining(", ")));
                    logger.warn("      TggRuleNodes:");
                    return marker2TGGRule.getValue().getNodes().stream()
                            .filter(tggRuleNode -> bindingTypes.contains(tggRuleNode.getBindingType()))
                            .filter(tggRuleNode -> eObject.eClass().equals(tggRuleNode.getType())) // not necessary
                            .anyMatch(tggRuleNode -> {
                                logger.warn("        - current tggRuleNode: " + Util.getMarkerStyleName(tggRuleNode) + ", valInMarker="
                                        + marker.eGet(marker.eClass().getEStructuralFeature(Util.getMarkerStyleName(tggRuleNode)))

                                );
                                return eObject.equals( // can the eObject be found anywhere in the marker?
                                        marker.eGet(marker.eClass().getEStructuralFeature(Util.getMarkerStyleName(tggRuleNode))));
                            });
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        }


    private Map<TGGRuleApplication, TGGRule> mapTGGRuleApplicationsToTGGRules(Set<TGGRuleApplication> markers) {
        Map<TGGRuleApplication, TGGRule> map = new HashMap<>();
        markers.stream().forEach(marker -> {
                    Set<TGGRule> ruleCandidates = rules.stream()
                            .filter(rule -> marker.eClass().getName().startsWith(rule.getName()))
                            .collect(Collectors.toSet());
                    logger.debug("Marker: " + Util.eObjectToString(marker) + ", ruleCandidates: " + ruleCandidates);
                    if (ruleCandidates.size() != 1) throw new IllegalStateException("Markers could not be mapped to a rule!");
                    map.put(marker, ruleCandidates.iterator().next());
                });
        return map;
    }
}
