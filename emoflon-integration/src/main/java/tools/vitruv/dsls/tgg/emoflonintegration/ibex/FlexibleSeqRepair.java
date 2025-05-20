package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.emoflon.ibex.tgg.operational.repair.SeqRepair;
import org.emoflon.ibex.tgg.operational.repair.strategies.AttributeRepairStrategy;
import org.emoflon.ibex.tgg.operational.strategies.PropagatingOperationalStrategy;
import org.emoflon.ibex.tgg.operational.strategies.PropagationDirectionHolder;
import tools.vitruv.dsls.tgg.emoflonintegration.patternmatching.VitruviusConsistencyMatch;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class whose sole purpose is injecting a {@link FixedAttributeRepairStrategy} to avoid {@link NullPointerException}s being thrown
 * in cases where {@link VitruviusConsistencyMatch}es are not fully matched and are tried to be repaired, regarding attribute conditions.
 */
public class FlexibleSeqRepair extends SeqRepair {

    public FlexibleSeqRepair(PropagatingOperationalStrategy opStrat, PropagationDirectionHolder propDirHolder) {
        super(opStrat, propDirHolder);
        this.initializeStrategies();
        replaceAttributeRepairStrategy();
    }

    private void replaceAttributeRepairStrategy() {
        Set<AttributeRepairStrategy> attributeRepairStrategySet = this.repairStrategies.stream()
                .filter(repairStrategy -> repairStrategy instanceof AttributeRepairStrategy)
                .map(repairStrategy -> (AttributeRepairStrategy) repairStrategy)
                .collect(Collectors.toSet());
        if (!attributeRepairStrategySet.isEmpty()) {
            this.repairStrategies.removeAll(attributeRepairStrategySet);
            this.repairStrategies.add(new FixedAttributeRepairStrategy(this.opStrat));
        }
    }
}
