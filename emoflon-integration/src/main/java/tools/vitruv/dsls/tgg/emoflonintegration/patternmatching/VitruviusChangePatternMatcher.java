package tools.vitruv.dsls.tgg.emoflonintegration.patternmatching;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.emoflon.ibex.tgg.operational.strategies.PropagationDirectionHolder;
import tools.vitruv.change.atomic.EChange;
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

    private List<EChange<EObject>> unmatchedEChanges;

    public VitruviusChangePatternMatcher(VitruviusChange<EObject> vitruviusChange, ChangeSequenceTemplateSet changeSequenceTemplateSet) {
        this.vitruviusChange = vitruviusChange;
        this.changeSequenceTemplateSet = changeSequenceTemplateSet;
        initialize();
    }

    /**
     * @return patterns that match against this class's VitruviusChange. Context is NOT checked yet, here!
     */
    public Set<VitruviusBackwardConversionMatch> getAdditiveMatches(PropagationDirectionHolder.PropagationDirection propagationDirection) {
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

            patternTemplates.forEach(patternTemplate -> logger.trace("\n- " + patternTemplate));

            for (ChangeSequenceTemplate patternTemplate : patternTemplates) {
                for (EChangeWrapper eChangeWrapper : patternTemplate.getUninitializedEChangeWrappers()) {
                    boolean eChangeWrapperInitialized = false;
                    // find a match for the echangewrapper in the VitruviusChange.
                    for (EChange<EObject> eChangeCandidate : eChangesByEChangeType.getOrDefault(eChangeWrapper.getEChangeType(), Collections.emptySet())) {
                        if (eChangeWrapper.matches(eChangeCandidate)) {
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
        rememberUnmatchedEChanges(allInvokedPatternTemplates);
//        visualizeCoverage(allInvokedPatternTemplates);

        return allInvokedPatternTemplates.stream()
                .map(patternTemplate -> new VitruviusBackwardConversionMatch(patternTemplate, propagationDirection.getPatternType()))
                .collect(Collectors.toSet());
    }

    private void rememberUnmatchedEChanges(Set<ChangeSequenceTemplate> coverage) {
        // a little inefficient...
        unmatchedEChanges = vitruviusChange.getEChanges().stream()
                .filter(eChange ->
                        coverage.stream().flatMap(changeSequenceTemplate -> changeSequenceTemplate.getEChangeWrappers().stream())
                                .noneMatch(filledEChangeWrapper -> filledEChangeWrapper.getEChange().equals(eChange)))
                .toList();
    }

    /**
     *
     * @return changes that have not been matched yet
     */
    public List<EChange<EObject>> getUnmatchedEChanges() {
        return this.unmatchedEChanges;
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
