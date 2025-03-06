package tools.vitruv.dsls.tgg.emoflonintegration.patternmatching;

import language.TGGRule;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.emoflon.ibex.common.operational.IMatch;
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

    protected static final Logger logger = Logger.getRootLogger();

    private final VitruviusChange<EObject> vitruviusChange;
    private Map<EClass, Set<EChange<EObject>>> eChangesByEChangeType;

    /**
     * To avoid duplicates in invoking patterns, remember which types of patterns have already been invoked for each EChange
     */
    private Map<EChange<EObject>, Set<TGGRule>> alreadyInvokedPatternTypes;

    /**
     * To avoid duplicates in invoking patterns (the same EChange with the same EChangeWrapper original(!) (which is distinctly mapped to a pattern type),
     * remember which EChangeWrapper originals have already been invoked for each EChange.
     */
    private Map<EChange<EObject>, Set<EChangeWrapper>> alreadyInvokedEChangeWrappers;

    public VitruviusChangePatternMatcher(VitruviusChange<EObject> vitruviusChange) {
        this.vitruviusChange = vitruviusChange;
        initialize();
    }

    /**
     * todo unit-tests schreiben!
     * todo evtl an Khelladi orientieren, bzw. überlegen ob mein sofort-matching wirklich besser ist
     * todo CODE REVIEW am 06.03.2025,
     * * Woche vorher Code den Teilnehmern schicken!
     * * im Wiki Zeug lesen.
     *
     * @param changeSequenceTemplateSet
     * @return
     */
    public Set<IMatch> matchPatterns(ChangeSequenceTemplateSet changeSequenceTemplateSet) {
        logger.debug("\n[VitruviusChangePatternMatcher] matching the following eChanges against " + changeSequenceTemplateSet.getPatternTemplateParents().size() + " pattern templates:");
        vitruviusChange.getEChanges().forEach(eChange -> {logger.info("  - " + Util.eChangeToString(eChange));});
        // 1. compute all possible matches
        // TODO optimization: not compute all matches but mark EChanges (at the possible cost of missing sth?)
        Set<ChangeSequenceTemplate> allInvokedPatternTemplates = new HashSet<>();
        vitruviusChange.getEChanges().forEach(eChange -> {
            Set<ChangeSequenceTemplate> patternTemplates = changeSequenceTemplateSet.getAndInitRelevantIbexPatternTemplatesByEChange(eChange);
            /*
                To avoid duplicates, remember which EChangeWrapper originals are already invoked for the current eChange and discard the templates which invoked the
             */
            removeDuplicateTemplateInvocations(eChange, patternTemplates);
            rememberWrappersInvokedWithEChange(eChange, patternTemplates);
            allInvokedPatternTemplates.addAll(patternTemplates);

            logger.debug("[VitruviusChangePatternMatcher] Matching the following eChange against " + patternTemplates.size() + " suitable pattern templates: \n" + Util.eChangeToString(eChange));
            logger.debug("[VitruviusChangePatternMatcher] The suitable pattern templates: ");
            patternTemplates.forEach(patternTemplate -> logger.debug("\n- " + patternTemplate));
            logger.debug("[VitruviusChangePatternMatcher] Trying to match the uninitialized wrappers, too...");

            patternTemplates.forEach(patternTemplate -> {
                for (EChangeWrapper eChangeWrapper : patternTemplate.getUninitializedEChangeWrappers()) {
                    logger.debug("[VitruviusChangePatternMatcher] Trying to match " + eChangeWrapper);
                    boolean eChangeWrapperInitialized = false;
                    // find a match for the echangewrapper in the VitruviusChange.
                    for (EChange<EObject> eChangeCandidate : eChangesByEChangeType.get(eChangeWrapper.getEChangeType())) {
                        logger.debug("[VitruviusChangePatternMatcher] Trying to match it against " + Util.eChangeToString(eChangeCandidate));
                        if (eChangeWrapper.matches(eChangeCandidate)) {
                            logger.debug("[VitruviusChangePatternMatcher] SUCCESS!");
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
            });
        });
        logger.debug("\n[VitruviusChangePatternMatcher] +++ Computed the following matches +++\n");
        allInvokedPatternTemplates.forEach(logger::debug);
        visualizeCoverage(allInvokedPatternTemplates);

        //TODO need to prevent duplicates that occur if a patterntemplate has more than one EChangewrapper (1 match for each wrapper...)
        /* Idea: We allow multiple an Echange to be covered by multiple patterns, but only one of each pattern type (i.e. via their rule reference?? That doesnt break anything!
         * --> Create a Map EChange -> TggRule
         */

        // 2. Check if the context of the patterns matches maybe by leveraging existing ibex functionality??

        // 3. choose patterns to form a Coverage where each change belongs to exactly one pattern (todo maybe less than exactly one since not everything is consistency-relevant)

        // 4. todo
        return allInvokedPatternTemplates.stream().map(VitruviusBackwardConversionMatch::new).collect(Collectors.toSet());
    }

    private void visualizeCoverage(Set<ChangeSequenceTemplate> coverage) {
        logger.info(
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
        this.vitruviusChange.getEChanges()
                .forEach(eChange -> {
                    this.eChangesByEChangeType.computeIfAbsent(eChange.eClass(), k -> new HashSet<>()).add(eChange);
                });
    }

    /**
     * Remove all ${@link ChangeSequenceTemplate}s from the given set that have an EChangeWrapper invoked with the eChange that has already been invoked before.
     * That means that the templates would be duplicates of already existing ones.
     * @param eChange
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
        changeSequenceTemplates.forEach(ibexPatternTemplate -> {
            rememberWrapperInvokedWithEChange(eChange, ibexPatternTemplate);});
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
