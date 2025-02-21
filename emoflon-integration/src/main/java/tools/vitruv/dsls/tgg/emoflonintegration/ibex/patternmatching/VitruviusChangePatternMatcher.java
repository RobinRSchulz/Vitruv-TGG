package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternmatching;

import language.TGGRule;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion.EChangeWrapper;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion.IbexPatternTemplate;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion.VitruviusChangeTemplateSet;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Calculates a matching of a change sequence (${@link VitruviusChange}) against a set of converted TGG patterns (${@link VitruviusChangeTemplateSet}).
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
     * @param vitruviusChangeTemplateSet
     * @return
     */
    public Set<IbexPatternTemplate> matchPatterns(VitruviusChangeTemplateSet vitruviusChangeTemplateSet) {
        logger.debug("\n[VitruviusChangePatternMatcher] matching the following eChanges against " + vitruviusChangeTemplateSet.getPatternTemplates().size() + " pattern templates:");
        vitruviusChange.getEChanges().forEach(eChange -> {logger.info("  - " + Util.eChangeToString(eChange));});
        // 1. compute all possible matches
        // TODO optimization: not compute all matches but mark EChanges (at the possible cost of missing sth?)
        Set<IbexPatternTemplate> allInvokedPatternTemplates = new HashSet<>();
        vitruviusChange.getEChanges().forEach(eChange -> {
            Set<IbexPatternTemplate> patternTemplates = vitruviusChangeTemplateSet.getAndInitRelevantIbexPatternTemplatesByEChange(eChange);
            // to avoid duplicates, remember which pattern types are already invoked for the current eChange.
            removeAlreadyInvokedPatternTypes(eChange, patternTemplates);
            rememberInvokedPatternTypes(eChange, patternTemplates);
            logger.debug("[VitruviusChangePatternMatcher] Matching the following eChange against " + patternTemplates.size() + " suitable pattern templates: \n" + Util.eChangeToString(eChange));
            logger.debug("[VitruviusChangePatternMatcher] The suitable pattern templates: ");
            patternTemplates.forEach(patternTemplate -> logger.debug("\n- " + patternTemplate));
            allInvokedPatternTemplates.addAll(patternTemplates);
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
                            rememberInvokedPatternType(eChangeCandidate, patternTemplate);
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
        //TODO log the patterns
        logger.info("\n[VitruviusChangePatternMatcher] +++ Computed the following matches +++\n");
        allInvokedPatternTemplates.forEach(logger::info);
        visualizeÜberdeckung(allInvokedPatternTemplates);

        //TODO need to verhinder duplicates that occur if a patterntemplate has more than one EChangewrapper (1 match for each wrapper...)
        /* Idea: We allow multiple an Echange to be covered by multiple patterns, but only one of each pattern type (i.e. via their rule reference?? That doesnt break anything!
         * --> Create a Map EChange -> TggRule
         */

        // 2. Check if the context of the patterns matches maybe by leveraging existing ibex functionality??

        // 3. choose patterns to form a Überdeckung where each change belongs to exactly one pattern (todo maybe less than exactly one since not everything is consistency-relevant)

        // 4. todo
        return null;
    }

    private void visualizeÜberdeckung(Set<IbexPatternTemplate> überdeckung) {
        logger.info(
                "[VitruviusChangePatternMatcher] Pattern Coverage of the given Vitruvius change:\n" +
                "| # Patterns covering | EChange                                    |\n" +
                "|---------------------|--------------------------------------------|\n" +
                vitruviusChange.getEChanges().stream().map(eChange ->
                                "| " + überdeckung.stream().filter(ibexPatternTemplate ->
                                        ibexPatternTemplate.getEChangeWrappers().stream().anyMatch(eChangeWrapper -> eChangeWrapper.getEChange().equals(eChange))).count()
                                        + "                  | " + Util.eChangeToString(eChange) + "       |")
                        .collect(Collectors.joining("\n"))

        );

    }

    private void initialize() {
        this.eChangesByEChangeType = new HashMap<>();
        this.alreadyInvokedPatternTypes = new HashMap<>();
        this.vitruviusChange.getEChanges()
                .forEach(eChange -> {
                    this.eChangesByEChangeType.computeIfAbsent(eChange.eClass(), k -> new HashSet<>()).add(eChange);
                });
    }

    private void removeAlreadyInvokedPatternTypes(EChange<EObject> eChange, Set<IbexPatternTemplate> ibexPatternTemplates) {
        ibexPatternTemplates.removeAll(
                ibexPatternTemplates.stream()
                        .filter(ibexPatternTemplate -> alreadyInvokedPatternTypes.containsKey(eChange) && alreadyInvokedPatternTypes.get(eChange).contains(ibexPatternTemplate.getTggRule()))
                        .collect(Collectors.toSet())
        );
    }

    private void rememberInvokedPatternTypes(EChange<EObject> eChange, Collection<IbexPatternTemplate> ibexPatternTemplates) {
        ibexPatternTemplates.forEach(ibexPatternTemplate -> {rememberInvokedPatternType(eChange, ibexPatternTemplate);});
    }
    private void rememberInvokedPatternType(EChange<EObject> eChange, IbexPatternTemplate ibexPatternTemplate) {
        this.alreadyInvokedPatternTypes.computeIfAbsent(eChange, k -> new HashSet<>()).add(ibexPatternTemplate.getTggRule());
    }
}
