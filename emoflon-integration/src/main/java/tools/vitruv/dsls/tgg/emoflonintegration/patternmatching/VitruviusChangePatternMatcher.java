package tools.vitruv.dsls.tgg.emoflonintegration.patternmatching;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.impl.EDataTypeImpl;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;
import runtime.Protocol;
import runtime.TGGRuleApplication;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.change.atomic.feature.attribute.RemoveEAttributeValue;
import tools.vitruv.change.atomic.feature.reference.RemoveEReference;
import tools.vitruv.change.atomic.feature.single.ReplaceSingleValuedFeatureEChange;
import tools.vitruv.change.atomic.root.RemoveRootEObject;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.EChangeWrapper;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.ChangeSequenceTemplate;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.ChangeSequenceTemplateSet;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Calculates a matching of a change sequence (${@link VitruviusChange}) against a set of converted TGG patterns (${@link ChangeSequenceTemplateSet}).
 *
 */
public class VitruviusChangePatternMatcher {
    static Logger logger = Logger.getLogger(VitruviusChangePatternMatcher.class);

    private final VitruviusChange<EObject> vitruviusChange;
    private ChangeSequenceTemplateSet changeSequenceTemplateSet;
    private Map<EClass, Set<EChange<EObject>>> eChangesByEChangeType;

    /**
     * To avoid duplicates in invoking patterns (the same EChange with the same EChangeWrapper original(!) (which is distinctly mapped to a pattern type)),
     * remember which EChangeWrapper originals have already been invoked for each EChange.
     */
    private Map<EChange<EObject>, Set<EChangeWrapper>> alreadyInvokedEChangeWrappers;

    public VitruviusChangePatternMatcher(VitruviusChange<EObject> vitruviusChange, ChangeSequenceTemplateSet changeSequenceTemplateSet) {
        this.vitruviusChange = vitruviusChange;
        this.changeSequenceTemplateSet = changeSequenceTemplateSet;
        initialize();
    }

    /**
     * todo evtl an Khelladi orientieren, bzw. überlegen ob mein sofort-matching wirklich besser ist
     *
     * @return patterns that match against this class's VitruviusChange. Context is NOT checked yet, here!
     */
    public Set<VitruviusBackwardConversionMatch> getForwardMatches() {
        logger.debug("\n[VitruviusChangePatternMatcher] matching the following eChanges against " + changeSequenceTemplateSet.getPatternTemplateParents().size() + " pattern templates:");
        vitruviusChange.getEChanges().forEach(eChange -> logger.debug("  - " + Util.eChangeToString(eChange)));
        // 1. compute all possible matches
        // TODO optimization: not compute all matches but mark EChanges (at the possible cost of missing sth?)
        Set<ChangeSequenceTemplate> allInvokedPatternTemplates = new HashSet<>();
        for (EChange<EObject> eChange : vitruviusChange.getEChanges()) {
            Set<ChangeSequenceTemplate> patternTemplates = changeSequenceTemplateSet.getAndInitRelevantIbexPatternTemplatesByEChange(eChange);
            /*
                To avoid duplicates, remember which EChangeWrapper originals are already invoked for the current eChange and discard the templates which invoked the
             */
            removeDuplicateTemplateInvocations(eChange, patternTemplates);
            rememberWrappersInvokedWithEChange(eChange, patternTemplates);
            allInvokedPatternTemplates.addAll(patternTemplates);

            logger.trace("[VitruviusChangePatternMatcher] Matching the following eChange against " + patternTemplates.size() + " suitable pattern templates: \n" + Util.eChangeToString(eChange));
            logger.trace("[VitruviusChangePatternMatcher] The suitable pattern templates: ");
            patternTemplates.forEach(patternTemplate -> logger.trace("\n- " + patternTemplate));
            logger.trace("[VitruviusChangePatternMatcher] Trying to match the uninitialized wrappers, too...");

            for (ChangeSequenceTemplate patternTemplate : patternTemplates) {
                for (EChangeWrapper eChangeWrapper : patternTemplate.getUninitializedEChangeWrappers()) {
                    logger.trace("[VitruviusChangePatternMatcher] Trying to match " + eChangeWrapper);
                    boolean eChangeWrapperInitialized = false;
                    // find a match for the echangewrapper in the VitruviusChange.
                    for (EChange<EObject> eChangeCandidate : eChangesByEChangeType.getOrDefault(eChangeWrapper.getEChangeType(), Collections.emptySet())) {
                        logger.trace("[VitruviusChangePatternMatcher] Trying to match it against " + Util.eChangeToString(eChangeCandidate));
                        if (eChangeWrapper.matches(eChangeCandidate)) {
                            logger.trace("[VitruviusChangePatternMatcher] SUCCESS!");
                            eChangeWrapper.initialize(eChangeCandidate);
                            eChangeWrapperInitialized = true;
                            // to avoid duplicates, remember which pattern types are already invoked for the current eChange.
                            rememberWrapperInvokedWithEChange(eChangeCandidate, patternTemplate);
                            // we can break, since we're finished with this eChangeWrapper. TODO do we miss anything by not continuing the search and splitting the pattern invocation again?
                            break;
                        }
                    }
                    // if one eChangeWrapper could not find a match, the whole pattern invocation is useless and other patterns do not need to be looked at.
                    if (!eChangeWrapperInitialized) {
                        allInvokedPatternTemplates.remove(patternTemplate);
                        break;
                    }
                }
            }
        }
        logger.debug("[VitruviusChangePatternMatcher] Computed the following matches \n");
        allInvokedPatternTemplates.forEach(logger::debug);
        visualizeCoverage(allInvokedPatternTemplates);

        //TODO need to prevent duplicates that occur if a patterntemplate has more than one EChangewrapper (1 match for each wrapper...)
        /* Idea: We allow multiple an Echange to be covered by multiple patterns, but only one of each pattern type (i.e. via their rule reference?? That doesnt break anything!
         * --> Create a Map EChange -> TggRule
         */

        // 2. Check if the context of the patterns matches maybe by leveraging existing ibex functionality??
        // 3. choose patterns to form a Coverage where each change belongs to exactly one pattern (todo maybe less than exactly one since not everything is consistency-relevant)
        return allInvokedPatternTemplates.stream().map(VitruviusBackwardConversionMatch::new).collect(Collectors.toSet());
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
        Optional<Protocol> protocolOptional = Util.getProtocol(resourceHandler);
        if (protocolOptional.isEmpty()) { return; }
        Protocol protocol = protocolOptional.get();
        protocol.getSteps().forEach(tggRuleApplicationStep -> logger.info("protocolEObject " + Util.eObjectToString(tggRuleApplicationStep)));


        // 2. iterate breaking changes
        vitruviusChange.getEChanges().stream()
                .filter(eChange -> !Util.isCreatingOrAdditiveEChange(eChange))
                .filter(breakingEChange -> {
                    if (breakingEChange instanceof ReplaceSingleValuedFeatureEChange<EObject, ?, ?> ) {
                        // covers: ReplaceSingleValuedEAttribute, ReplaceSingleValuedEReference
                        //TODO ist sowohl additiv als auch subtraktiv! wie behandeln??
                        // müsste man ggf in delete + create aufsplitten --> TODO issue schreiben
                        logger.warn("ReplaceSingleValuedEAttribute not covered! Ignoring the following eChange: " + Util.eChangeToString(breakingEChange));
                        return false;
                    } else return true;
                }).forEach(breakingChange -> {
                    // todo map change to eObjects that are being deleted by it
                    if (breakingChange instanceof RemoveEReference<EObject> removeEReference) {
                        // AE is
                        removeEReference.getAffectedFeature().getEKeys();
                    }
                });
                    /* todo
                        1. find out which EObjects are introduced by which marker (from the protocol)   --> Map<EObject, Marker<?>
                        2. find out which eObject(s) this breakingEChange deletes.                      --> Set<EObject> eObjectsBrokenByThisEChange
                        2.5 flatmap                                                                     --> Set<EObject> eObjectsBroken
                        3. look up all EObjects in the former map, getting a                            --> Set<Marker> brokenMarkers
                        3. wurschtel that into a match somehow (maybe that can be gotten easily from the marker!)
                     */

//        throw new RuntimeException("TODO implement broken match stuff");
    }


    private void visualizeCoverage(Set<ChangeSequenceTemplate> coverage) {
        logger.debug(
                "[VitruviusChangePatternMatcher] Pattern Coverage of the given Vitruvius change:\n" +
                "| # Patterns covering | EChange                                    |\n" +
                "|---------------------|--------------------------------------------|\n" +
                vitruviusChange.getEChanges().stream().map(eChange ->
                                "| " + coverage.stream().filter(ibexPatternTemplate ->
                                        ibexPatternTemplate.getEChangeWrappers().stream().anyMatch(eChangeWrapper -> eChangeWrapper.getEChange().equals(eChange))).count()
                                        + "                  | " + Util.eChangeToString(eChange) + "       |")
                        .collect(Collectors.joining("\n"))
        );
    }

    private void initialize() {
        this.eChangesByEChangeType = new HashMap<>();
        alreadyInvokedEChangeWrappers = new HashMap<>();
        for (EChange<EObject> eChange : vitruviusChange.getEChanges()) {
            this.eChangesByEChangeType.computeIfAbsent(eChange.eClass(), k -> new HashSet<>()).add(eChange);
        }
    }

    /**
     * Remove all ${@link ChangeSequenceTemplate}s from the given set that have an EChangeWrapper invoked with the eChange that has already been invoked before.
     * That means that the templates would be duplicates of already existing ones.
     * @param changeSequenceTemplates templates that have been partly invoked with the given eChange.
     */
    private void removeDuplicateTemplateInvocations(EChange<EObject> eChange, Set<ChangeSequenceTemplate> changeSequenceTemplates) {
        changeSequenceTemplates.removeAll(
                changeSequenceTemplates.stream()
                        .filter(ibexPatternTemplate -> {
                            // remove all pattern templates that would be copies of an already existing template.
                            Optional<EChangeWrapper> eChangeWrapperHolding = ibexPatternTemplate.getEChangeWrapperHolding(eChange);
                            if (eChangeWrapperHolding.isPresent()) {
                                return alreadyInvokedEChangeWrappers.containsKey(eChange) &&
                                        alreadyInvokedEChangeWrappers.get(eChange).contains(eChangeWrapperHolding.get().getOriginal());
                            } else {
                                throw new IllegalStateException("This method expects changeSequenceTemplates that have been invoked with the given EChange!");
                            }
                        })
                        .collect(Collectors.toSet())
        );
    }

    private void rememberWrappersInvokedWithEChange(EChange<EObject> eChange, Collection<ChangeSequenceTemplate> changeSequenceTemplates) {
        for (ChangeSequenceTemplate changeSequenceTemplate : changeSequenceTemplates) {
            rememberWrapperInvokedWithEChange(eChange, changeSequenceTemplate);
        }
    }
    private void rememberWrapperInvokedWithEChange(EChange<EObject> eChange, ChangeSequenceTemplate changeSequenceTemplate) {
        changeSequenceTemplate.getEChangeWrapperHolding(eChange).ifPresentOrElse(
                eChangeWrapper -> {
                    EChangeWrapper eChangeWrapperOriginal = eChangeWrapper.getOriginal();
                    if (!eChangeWrapperOriginal.isOriginal()) throw new IllegalStateException("The given EChangeWrapper is not an original!");
                    alreadyInvokedEChangeWrappers
                            .computeIfAbsent(eChange, k -> new HashSet<>())
                            .add(eChangeWrapperOriginal);
                }, () -> {
                    throw new IllegalStateException("ChangeSequenceTemplate doesn't hold echange, which it should: " + Util.eChangeToString(eChange));
                }
        );
    }
}
