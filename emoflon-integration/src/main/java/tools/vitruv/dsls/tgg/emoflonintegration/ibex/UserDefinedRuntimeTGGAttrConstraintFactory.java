package tools.vitruv.dsls.tgg.emoflonintegration.ibex;

import org.emoflon.ibex.tgg.operational.csp.constraints.factories.RuntimeTGGAttrConstraintFactory;

import java.util.HashMap;
import java.util.HashSet;

// This is done similarly to the class in org.emoflon.ibex.tgg.operational.csp.constraints.factories.ibextgggantt2cpm;
public class UserDefinedRuntimeTGGAttrConstraintFactory extends RuntimeTGGAttrConstraintFactory {

    public UserDefinedRuntimeTGGAttrConstraintFactory() {
        super();
    }
    @Override
    protected void initialize() {
        creators = new HashMap<>();
        //TODO put something in there, if constraints are needed (?)
//        creators.put("notADependencyViaNamingConvention", () -> new UserDefined_notADependencyViaNamingConvention());

        constraints = new HashSet<>();
        constraints.addAll(creators.keySet());
    }
}
