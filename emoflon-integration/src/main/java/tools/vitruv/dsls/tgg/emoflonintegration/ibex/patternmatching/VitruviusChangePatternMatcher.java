package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternmatching;

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

public class VitruviusChangePatternMatcher {

    private final VitruviusChange<EObject> vitruviusChange;
    private Map<EClass, Set<EChange<EObject>>> eChangesByEChangeType;

    public VitruviusChangePatternMatcher(VitruviusChange<EObject> vitruviusChange) {
        this.vitruviusChange = vitruviusChange;
        initialize();
    }

    public void matchPatterns(VitruviusChangeTemplateSet vitruviusChangeTemplateSet) {
        // 1. compute all possible matches
        // TODO optimization: not compute all matches but mark EChanges (at the possible cost of missing sth?)
        Set<IbexPatternTemplate> allInvokedPatternTemplates = new HashSet<>();
        vitruviusChange.getEChanges().forEach(eChange -> {
            Set<IbexPatternTemplate> patternTemplates = vitruviusChangeTemplateSet.getAndInitRelevantIbexPatternTemplatesByEChange(eChange, vitruviusChange);
            allInvokedPatternTemplates.addAll(patternTemplates);
            patternTemplates.forEach(patternTemplate -> {
                for (EChangeWrapper eChangeWrapper : patternTemplate.getUninitializedEChangeWrappers()) {
                    boolean eChangeWrapperInitialized = false;
                    // find a match for the echangewrapper in the VitruviusChange.
                    for (EChange<EObject> eChangeCandidate : eChangesByEChangeType.get(eChangeWrapper.getEChangeType())) {
                        if (eChangeWrapper.matches(eChangeCandidate, vitruviusChange)) {
                            eChangeWrapper.initialize(eChangeCandidate, vitruviusChange);
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

        // 2.
    }

    private void initialize() {
        this.eChangesByEChangeType = new HashMap<>();
        this.vitruviusChange.getEChanges()
                .forEach(eChange -> {
                    this.eChangesByEChangeType.computeIfAbsent(eChange.eClass(), k -> new HashSet<>()).add(eChange);
                });
    }
}
