package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.emoflon.ibex.tgg.operational.csp.IRuntimeTGGAttrConstrContainer;
import org.emoflon.ibex.tgg.operational.debug.LoggerConfig;
import org.emoflon.ibex.tgg.operational.matches.ITGGMatch;
import org.emoflon.ibex.tgg.operational.repair.strategies.AttributeRepairStrategy;
import org.emoflon.ibex.tgg.operational.repair.strategies.RepairApplicationPoint;
import org.emoflon.ibex.tgg.operational.strategies.PropagatingOperationalStrategy;
import org.emoflon.ibex.tgg.operational.strategies.PropagationDirectionHolder;
import tools.vitruv.dsls.tgg.emoflonintegration.patternmatching.VitruviusConsistencyMatch;

import java.util.Collection;
import java.util.Collections;

/**
 * Ensures that not fully matched matches are not tried to repair.
 */
public class FixedAttributeRepairStrategy extends AttributeRepairStrategy {

    public FixedAttributeRepairStrategy(PropagatingOperationalStrategy opStrat) {
        super(opStrat);
    }

    @Override
    protected ITGGMatch repair(ITGGMatch repairCandidate, IRuntimeTGGAttrConstrContainer csp) {
        if (repairCandidate instanceof VitruviusConsistencyMatch vitruviusConsistencyMatch && !vitruviusConsistencyMatch.isFullyMatched()) {
            return repairCandidate;
        }
        if (!csp.solve()) {
            return null;
        } else {
            csp.applyCSPValues(repairCandidate);
            LoggerConfig.log(LoggerConfig.log_repair(), () -> {
                String var10000 = repairCandidate.getPatternName();
                return "Repaired Attributes: " + var10000 + " (" + repairCandidate.hashCode() + ")";
            });
            return repairCandidate;
        }
    }
}
