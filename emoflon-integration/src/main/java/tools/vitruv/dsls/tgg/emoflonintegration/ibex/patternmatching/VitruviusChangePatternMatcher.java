package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternmatching;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion.VitruviusChangeTemplateSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VitruviusChangePatternMatcher {

    private final VitruviusChange<EObject> vitruviusChange;
    private Map<EClass, Set<EChange<EObject>>> eChangesByEChangeType;

    public VitruviusChangePatternMatcher(VitruviusChange<EObject> vitruviusChange) {
        this.vitruviusChange = vitruviusChange;
        initialize();
    }

    public void matchPatterns(VitruviusChangeTemplateSet vitruviusChangeTemplateSet) {

    }

    private void initialize() {
        this.eChangesByEChangeType = new HashMap<>();
        this.vitruviusChange.getEChanges().stream()
                .forEach(eChange -> {
                    this.eChangesByEChangeType.computeIfAbsent(eChange.eClass(), k -> new HashSet<>()).add(eChange);
                });
    }
}
