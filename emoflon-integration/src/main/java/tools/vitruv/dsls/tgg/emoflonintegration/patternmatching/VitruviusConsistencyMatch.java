package tools.vitruv.dsls.tgg.emoflonintegration.patternmatching;

import language.BindingType;
import language.TGGRule;
import org.apache.log4j.Logger;
import org.emoflon.ibex.tgg.compiler.patterns.PatternType;
import org.emoflon.ibex.tgg.compiler.patterns.PatternUtil;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import org.emoflon.ibex.tgg.operational.matches.SimpleTGGMatch;
import runtime.TGGRuleApplication;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Used as broken or intact CONSISTENCY match, i.e. a representation of a protocol marker.
 */
public class VitruviusConsistencyMatch extends SimpleTGGMatch implements ITGGMatch {
    protected static final Logger logger = Logger.getLogger(VitruviusConsistencyMatch.class);

    private TGGRuleApplication tggRuleApplication;

    public VitruviusConsistencyMatch(TGGRuleApplication ruleApplication, TGGRule tggRule) {
        super(tggRule.getName() + "__" + PatternType.CONSISTENCY.name());
        this.tggRuleApplication = ruleApplication;
        init(ruleApplication, tggRule);
    }


    private void init(TGGRuleApplication ruleApplication, TGGRule tggRule) {
        tggRule.getNodes().stream()
                .filter(ruleNode ->  // we only want CONTEXT or CREATE nodes in a Match.
                        Set.of(BindingType.CONTEXT, BindingType.CREATE).contains(ruleNode.getBindingType()))
                .forEach(ruleNode -> {
                    Object objectCanidate = ruleApplication.eGet(ruleApplication.eClass().getEStructuralFeature(Util.getMarkerStyleName(ruleNode)));
                    if (objectCanidate != null) {
                        this.put(ruleNode.getName(), objectCanidate);
                    } else logger.info("Object " + ruleNode.getName() + " not found. This must really be a broken match!");
                }
        );
    }

    public ITGGMatch copy() {
        SimpleTGGMatch copy = new SimpleTGGMatch(this.getPatternName());
        for (String n : this.getParameterNames()) {
            copy.put(n, this.get(n));
        }
        logger.trace("VitruviusConsistencyMatch::copy : \n  -" + copy.getParameterNames().stream()
                .map(paramName -> paramName + ": " + Util.eObjectToString(copy.get(paramName)))
                .collect(Collectors.joining("\n  -")));
        return copy;
    }

    public PatternType getType() {
        return PatternUtil.resolve(this.getPatternName());
    }

    @Override
    public TGGRuleApplication getRuleApplicationNode() {
        return tggRuleApplication;
    }

    @Override
    public String toString() {
        return "[VitruviusConsistencyMatch] patternName=" + this.getPatternName() + ", type=" + getType() + ", ruleName=" + getRuleName();
    }

    public String toVerboseString() {
        return Util.iMatchToVerboseString(this);
    }
}
