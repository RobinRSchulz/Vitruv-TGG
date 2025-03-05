package tools.vitruv.dsls.tgg.emoflonintegration.patternmatching;

import hipe.engine.match.ProductionMatch;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.emoflon.ibex.common.operational.SimpleMatch;
import org.emoflon.ibex.tgg.compiler.patterns.PatternType;
import org.emoflon.ibex.tgg.compiler.patterns.PatternUtil;
import org.emoflon.ibex.tgg.compiler.patterns.TGGPatternUtil;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import org.emoflon.ibex.tgg.operational.matches.SimpleTGGMatch;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.ChangeSequenceTemplate;

import java.util.stream.Collectors;

public class VitruviusBackwardConversionMatch extends SimpleTGGMatch implements ITGGMatch {

    protected static final Logger logger = Logger.getRootLogger();
    /**
     *
     * @param matchedChangeSequenceTemplate a ${@link ChangeSequenceTemplate} that has been matched against a couple of EChanges
     */
    public VitruviusBackwardConversionMatch(ChangeSequenceTemplate matchedChangeSequenceTemplate) {
        //TODO ähhhhh rausfinden wann FWD wann BWD und irgendwo einrichten... --> DIrectionholder-Struktur nutzen...
        super(matchedChangeSequenceTemplate.getIBeXContextPattern(PatternType.FWD).getName());
//        super(matchedChangeSequenceTemplate.getIBeXContextPattern(PatternType.CONSISTENCY).getName());
        if (!matchedChangeSequenceTemplate.isInitialized()) {
            throw new IllegalStateException("The changeSequenceTemplate must be initialized");
        }
        init(matchedChangeSequenceTemplate);
    }

    private void init(ChangeSequenceTemplate matchedChangeSequenceTemplate) {
        // TODO this should be the equivalent of how HiPEGTMatch does it. check!
        matchedChangeSequenceTemplate.getAllPlaceholders().stream()
                .filter(eObjectPlaceholder -> eObjectPlaceholder.getTggRuleNode() != null) // todo in case of "oldValue", we have a null placeholder occurring...
                .forEach(eObjectPlaceholder ->
                        this.put(eObjectPlaceholder.getTggRuleNode().getName(), eObjectPlaceholder.getAffectedEObject())
                );
    }

    public ITGGMatch copy() {
        SimpleTGGMatch copy = new SimpleTGGMatch(this.getPatternName());
        this.getParameterNames().forEach((n) -> copy.put(n, this.get(n)));
        logger.debug("todo-delete-this-log: VitruvBackConvMatch::copy : \n  -" + copy.getParameterNames().stream()
                .map(paramName -> paramName + ": " + Util.eObjectToString((EObject)copy.get(paramName)))
                .collect(Collectors.joining("\n  -")));
        return copy;
    }

    public PatternType getType() {
        return PatternUtil.resolve(this.getPatternName());
    }

//    private void init(ProductionMatch match) {
//        for(String label : match.getLabels()) {
//            this.put(label, match.getNode(label));
//        }
//    }
}
