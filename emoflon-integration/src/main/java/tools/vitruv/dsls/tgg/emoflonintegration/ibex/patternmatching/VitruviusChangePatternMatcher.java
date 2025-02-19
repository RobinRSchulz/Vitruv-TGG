package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternmatching;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion.EChangeWrapper;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion.IbexPatternTemplate;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion.VitruviusChangeTemplateSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    public VitruviusChangePatternMatcher(VitruviusChange<EObject> vitruviusChange) {
        this.vitruviusChange = vitruviusChange;
        initialize();
    }

    public Set<IbexPatternTemplate> matchPatterns(VitruviusChangeTemplateSet vitruviusChangeTemplateSet) {
        logger.debug("\n[VitruviusChangePatternMatcher] matching the following eChanges against " + vitruviusChangeTemplateSet.getPatternTemplates().size() + " pattern templates:");
        vitruviusChange.getEChanges().forEach(eChange -> {logger.info("  - " + Util.eChangeToString(eChange));});
        // 1. compute all possible matches
        // TODO optimization: not compute all matches but mark EChanges (at the possible cost of missing sth?)
        Set<IbexPatternTemplate> allInvokedPatternTemplates = new HashSet<>();
        vitruviusChange.getEChanges().forEach(eChange -> {
            Set<IbexPatternTemplate> patternTemplates = vitruviusChangeTemplateSet.getAndInitRelevantIbexPatternTemplatesByEChange(eChange);
            logger.debug("[VitruviusChangePatternMatcher] Matching the following eChange against " + patternTemplates.size() + " suitable pattern templates: \n" + Util.eChangeToString(eChange));
            logger.debug("[VitruviusChangePatternMatcher] The suitable pattern templates: ");
            patternTemplates.forEach(patternTemplate -> logger.debug("  - " + patternTemplate));
            allInvokedPatternTemplates.addAll(patternTemplates);
            patternTemplates.forEach(patternTemplate -> {
                for (EChangeWrapper eChangeWrapper : patternTemplate.getUninitializedEChangeWrappers()) {
                    boolean eChangeWrapperInitialized = false;
                    // find a match for the echangewrapper in the VitruviusChange.
                    for (EChange<EObject> eChangeCandidate : eChangesByEChangeType.get(eChangeWrapper.getEChangeType())) {
                        if (eChangeWrapper.matches(eChangeCandidate)) {
                            eChangeWrapper.initialize(eChangeCandidate);
                            eChangeWrapperInitialized = true;
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
        logger.info("\n[VitruviusChangePatternMatcher] +++ Computed the following matches +++\n" + allInvokedPatternTemplates);

        // 2. Check if the context of the patterns matches maybe by leveraging existing ibex functionality??

        // 3. choose patterns to form a Überdeckung where each change belongs to exactly one pattern (todo maybe less than exactly one since not everything is consistency-relevant)

        // 4. todo
        return null;
    }

    private void initialize() {
        this.eChangesByEChangeType = new HashMap<>();
        this.vitruviusChange.getEChanges()
                .forEach(eChange -> {
                    this.eChangesByEChangeType.computeIfAbsent(eChange.eClass(), k -> new HashSet<>()).add(eChange);
                });
    }
}
