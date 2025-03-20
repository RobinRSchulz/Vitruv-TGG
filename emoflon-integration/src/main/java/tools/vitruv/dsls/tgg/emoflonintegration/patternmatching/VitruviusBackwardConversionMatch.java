package tools.vitruv.dsls.tgg.emoflonintegration.patternmatching;

import language.BindingType;
import language.TGGRuleNode;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.emoflon.ibex.tgg.compiler.patterns.PatternType;
import org.emoflon.ibex.tgg.compiler.patterns.PatternUtil;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import org.emoflon.ibex.tgg.operational.matches.SimpleTGGMatch;
import org.emoflon.ibex.tgg.operational.strategies.PropagationDirectionHolder.PropagationDirection;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.ChangeSequenceTemplate;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class VitruviusBackwardConversionMatch extends SimpleTGGMatch implements ITGGMatch {
    protected static final Logger logger = Logger.getLogger(VitruviusBackwardConversionMatch.class);
    private final ChangeSequenceTemplate matchedChangeSequenceTemplate;
    private boolean contextHasBeenMatchedSuccessfully;

    private Map<TGGRuleNode, EObject> tggRuleNodeEObjectMap;
    /**
     *
     * @param matchedChangeSequenceTemplate a ${@link ChangeSequenceTemplate} that has been matched against a couple of EChanges
     */
    public VitruviusBackwardConversionMatch(ChangeSequenceTemplate matchedChangeSequenceTemplate, PatternType patternType) {
        // TODO find out when FWD, when BWD and use Directionholder from ibex!
        super(matchedChangeSequenceTemplate.getIBeXContextPattern(patternType).getName());
        if (!matchedChangeSequenceTemplate.isInitialized()) {
            throw new IllegalStateException("The changeSequenceTemplate must be initialized");
        }
        init(matchedChangeSequenceTemplate);
        this.matchedChangeSequenceTemplate = matchedChangeSequenceTemplate;
        this.contextHasBeenMatchedSuccessfully = false;
    }

    public boolean contextMatches(TGGResourceHandler tggResourceHandler, PropagationDirection propagationDirection) {
        logger.debug("VitruviusBackwardConversionMatch::contextMatches called for: " + this.getMatchedChangeSequenceTemplate().getTggRule().getName());
        if (contextHasBeenMatchedSuccessfully) {
            return true;
        } else {
            Optional<Map<TGGRuleNode, EObject>> tggRuleNodeEObjectMapOptional = matchedChangeSequenceTemplate.contextMatches(tggResourceHandler, propagationDirection);
            if (tggRuleNodeEObjectMapOptional.isPresent()) {
                logger.debug("  SUCCESSS!!!!");
                contextHasBeenMatchedSuccessfully = true;
                this.tggRuleNodeEObjectMap = tggRuleNodeEObjectMapOptional.get();
                // add the matched EObjects to this (TODO check if the name is right...)
                this.tggRuleNodeEObjectMap.forEach((tggRuleNode, eObject) -> this.put(tggRuleNode.getName(), eObject));
                return true;
            } else return false;
        }
    }

    public Set<EObject> getEObjectsCreatedByThisMatch() {
        if (this.contextHasBeenMatchedSuccessfully) {
            return this.tggRuleNodeEObjectMap.entrySet().stream()
                    .filter(entry -> entry.getKey().getBindingType().equals(BindingType.CREATE))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toSet());
        } else throw new IllegalStateException("This match's context has not been matched successfully!");
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
        logger.trace("VitruvBackConvMatch::copy : \n  -" + copy.getParameterNames().stream()
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
