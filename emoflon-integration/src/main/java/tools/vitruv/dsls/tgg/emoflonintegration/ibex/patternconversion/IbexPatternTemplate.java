package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;


import language.TGGRule;
import org.eclipse.emf.ecore.EClass;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContextPattern;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.compiler.patterns.PatternType;

import java.util.*;
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
    /**
     * maps an Echange Type to all EChange-Wrappers this pattern contains
     */
    private Map<EClass, Set<EChangeWrapper>> eChangeWrappersByEChangeType;

    private TGGRule tggRule;
    private Map<PatternType, IBeXContextPattern> iBeXContextPatternMap;

    /**
     * private constructor for copying the Template
     */
    private IbexPatternTemplate(TGGRule tggRule, Map<PatternType, IBeXContextPattern> iBeXContextPatternMap, Collection<EChangeWrapper> eChangeWrappers) {
        this.tggRule = tggRule;
        this.iBeXContextPatternMap = iBeXContextPatternMap;
        this.eChangeWrappers = new LinkedList<>();

        this.eChangeWrappers = eChangeWrappers;
        initialize();
    }

    public IbexPatternTemplate(TGGRule tggRule, Collection<IBeXContextPattern> iBeXContextPatterns, Collection<EChangeWrapper> eChangeWrappers) {
        this.tggRule = tggRule;
        this.iBeXContextPatternMap = new HashMap<>();
        iBeXContextPatterns.forEach(iBeXContextPattern -> this.iBeXContextPatternMap.put(getPatternType(iBeXContextPattern), iBeXContextPattern));

        this.eChangeWrappers = eChangeWrappers;
        initialize();
    }

    private void initialize() {
        // make the mapping EChangeType -> relevant eChangeWrappers easily accessible
        eChangeWrappersByEChangeType = new HashMap<>();
        eChangeWrappers.forEach(eChangeWrapper -> {
            eChangeWrappersByEChangeType.computeIfAbsent(eChangeWrapper.getEChangeType(), k -> new HashSet<>()).add(eChangeWrapper);
        });
    }

    //TODO restructure this class after the pattern matching algorithm design is complete

    private PatternType getPatternType(IBeXContextPattern pattern) {
        return PatternSuffixes.extractType(pattern.getName());
    }

    public String toString() {
        return "[IbexPatternTemplate of " + tggRule.getName() + "] \n  - " + eChangeWrappers.stream().map(EChangeWrapper::toString).collect(Collectors.joining(",\n  - "));
    }

    public Collection<EChangeWrapper> getEChangeWrappers() {
        return eChangeWrappers;
    }

    /**
     *
     * @param eChangeType
     * @return all EChangeWrappers that match the given eChangeType and thus are a possible candidate
     */
    public Set<EChangeWrapper> getRelevantEChangeWrappersByEChangeType(EClass eChangeType) {
        return eChangeWrappersByEChangeType.get(eChangeType);
    }

    /**
     *
     * @return a deep copy with new wrappers and placeholders, while retaining the placeholder structure.
     */
    public IbexPatternTemplate deepCopy() {
        // copy the echange wrapper and their placeholder. Afterwards, we got NEW eChangeWrappers with OLD Placeholders.
        IbexPatternTemplate copiedTemplate = new IbexPatternTemplate(this.tggRule, this.iBeXContextPatternMap, this.eChangeWrappers.stream().map(EChangeWrapper::shallowCopy).toList());

        // now, we can systematically replace the OLD placeholders in the NEW eChangeWrappers with NEW placeholders to achieve a deep copy
        // 1. Create a Map OLDplaceholder -> NEWPlaceholder
        Set<EObjectPlaceholder> allPlaceholders = new HashSet<>();
        copiedTemplate.getEChangeWrappers().forEach(eChangeWrapper -> {
            allPlaceholders.addAll(eChangeWrapper.getAllPlaceholders());
        });
        Map<EObjectPlaceholder, EObjectPlaceholder> oldToNewPlaceholders = new HashMap<>();
        allPlaceholders.forEach(oldPlaceholder -> oldToNewPlaceholders.put(oldPlaceholder, new EObjectPlaceholder()));

        // 2. Replace the OLD placeholders in the NEW eChangeWrappers with the NEW placeholders.
        copiedTemplate.getEChangeWrappers().forEach(eChangeWrapper -> eChangeWrapper.replaceAllPlaceholders(oldToNewPlaceholders));

        return copiedTemplate;
    }
}
