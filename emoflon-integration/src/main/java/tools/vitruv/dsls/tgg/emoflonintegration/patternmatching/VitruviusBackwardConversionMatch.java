package tools.vitruv.dsls.tgg.emoflonintegration.patternmatching;

import org.apache.log4j.Logger;
import org.emoflon.ibex.tgg.compiler.patterns.PatternType;
import org.emoflon.ibex.tgg.compiler.patterns.PatternUtil;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import org.emoflon.ibex.tgg.operational.matches.SimpleTGGMatch;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.ChangeSequenceTemplate;

import java.util.stream.Collectors;

public class VitruviusBackwardConversionMatch extends SimpleTGGMatch implements ITGGMatch {

    protected static final Logger logger = Logger.getRootLogger();
    private final ChangeSequenceTemplate matchedChangeSequenceTemplate;
    /**
     *
     * @param matchedChangeSequenceTemplate a ${@link ChangeSequenceTemplate} that has been matched against a couple of EChanges
     */
    public VitruviusBackwardConversionMatch(ChangeSequenceTemplate matchedChangeSequenceTemplate) {
        // TODO find out when FWD, when BWD and use Directionholder from ibex!
        super(matchedChangeSequenceTemplate.getIBeXContextPattern(PatternType.FWD).getName());
        if (!matchedChangeSequenceTemplate.isInitialized()) {
            throw new IllegalStateException("The changeSequenceTemplate must be initialized");
        }
        init(matchedChangeSequenceTemplate);
        this.matchedChangeSequenceTemplate = matchedChangeSequenceTemplate;
    }

    public ChangeSequenceTemplate getMatchedChangeSequenceTemplate() {
        return matchedChangeSequenceTemplate;
    }

    private void init(ChangeSequenceTemplate matchedChangeSequenceTemplate) {
        // TODO this should be the equivalent of how HiPEGTMatch does it. check!
        matchedChangeSequenceTemplate.getAllPlaceholders().stream()
                .filter(eObjectPlaceholder -> eObjectPlaceholder.getTggRuleNode() != null) // todo in case of "oldValue", we have a null placeholder occurring...
                .forEach(eObjectPlaceholder ->
                        this.put(eObjectPlaceholder.getTggRuleNode().getName(), eObjectPlaceholder.getAffectedEObject())
                );
        //TODO Add all context nodes! also add corr? probably not...
//        matchedChangeSequenceTemplate.getTggRule().getNodes()
    }

    public ITGGMatch copy() {
        SimpleTGGMatch copy = new SimpleTGGMatch(this.getPatternName());
        for (String n : this.getParameterNames()) {
            copy.put(n, this.get(n));
        }
        logger.debug("VitruvBackConvMatch::copy : \n  -" + copy.getParameterNames().stream()
                .map(paramName -> paramName + ": " + Util.eObjectToString(copy.get(paramName)))
                .collect(Collectors.joining("\n  -")));
        return copy;
    }

    public PatternType getType() {
        return PatternUtil.resolve(this.getPatternName());
    }

    @Override
    public String toString() {
        return "[VitruviusBackwardConversionMatch] patternName=" + this.getPatternName() + ", type=" + getType() + ", ruleName=" + getRuleName();
    }
}
