package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;


import language.TGGRule;
import org.eclipse.emf.ecore.EClass;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Todo: This class should
 * * represent a pattern in the form of Vitruvius EChanges
 * * contain
 *
 *
 * * Problem: We need multiple identical instantiations of the IbexPatternTemplate for the matching process...
 * * Idea: Maybe a Factory that has one set of EChangeWrappers and always copies those each time a fresh IbexPatternTemplate is needed?
 * * apply-Methode, die dann "angewandtes Template" zurückgibt.
 * * todo -> Mal Lars fragen
 *
 */
public class IbexPatternTemplate {

    private Collection<EChangeWrapper> eChangeWrappers;

    private TGGRule tggRule;

    /**
     * maps an Echange Type to all EChange-Wrappers this pattern contains
     */
    private Map<EClass, EChangeWrapper> echangeTypeToEchangeWrapperMap;

    public IbexPatternTemplate(TGGRule tggRule, Collection<EChangeWrapper> eChangeWrappers) {
        this.tggRule = tggRule;
        this.eChangeWrappers = eChangeWrappers;
    }

    //TODO restructure this class after the pattern matching algorithm design is complete

    public String toString() {
        return "[IbexPatternTemplate of " + tggRule.getName() + "] \n  - " + eChangeWrappers.stream().map(EChangeWrapper::toString).collect(Collectors.joining(",\n  - "));
    }
}
