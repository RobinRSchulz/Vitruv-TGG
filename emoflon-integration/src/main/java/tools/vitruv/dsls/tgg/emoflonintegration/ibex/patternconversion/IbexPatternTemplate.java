package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;


import language.TGGRule;
import org.eclipse.emf.ecore.EClass;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContextPattern;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.compiler.patterns.PatternType;

import java.util.Collection;
import java.util.HashMap;
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
    private Map<PatternType, IBeXContextPattern> iBeXContextPatternMap;

    /**
     * maps an Echange Type to all EChange-Wrappers this pattern contains
     */
    private Map<EClass, EChangeWrapper> echangeTypeToEchangeWrapperMap;

    public IbexPatternTemplate(TGGRule tggRule, Collection<IBeXContextPattern> iBeXContextPatterns, Collection<EChangeWrapper> eChangeWrappers) {
        this.tggRule = tggRule;
        this.eChangeWrappers = eChangeWrappers;

        this.iBeXContextPatternMap = new HashMap<>();
        iBeXContextPatterns.forEach(iBeXContextPattern -> this.iBeXContextPatternMap.put(getPatternType(iBeXContextPattern), iBeXContextPattern));
    }

    //TODO restructure this class after the pattern matching algorithm design is complete

    private PatternType getPatternType(IBeXContextPattern pattern) {
        return PatternSuffixes.extractType(pattern.getName());
    }

    public String toString() {
        return "[IbexPatternTemplate of " + tggRule.getName() + "] \n  - " + eChangeWrappers.stream().map(EChangeWrapper::toString).collect(Collectors.joining(",\n  - "));
    }
}
