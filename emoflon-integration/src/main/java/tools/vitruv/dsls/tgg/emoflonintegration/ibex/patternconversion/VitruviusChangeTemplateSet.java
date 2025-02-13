package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import java.util.Collection;

/**
 * A representation of Ibex TGG patterns to ease mapping them to VitruviusChanges (sequences of atomic changes)
 */
public class VitruviusChangeTemplateSet {

    private Collection<IbexPatternTemplate> patternTemplates;

    public VitruviusChangeTemplateSet(Collection<IbexPatternTemplate> patternTemplates) {
        this.patternTemplates = patternTemplates;
    }
}
