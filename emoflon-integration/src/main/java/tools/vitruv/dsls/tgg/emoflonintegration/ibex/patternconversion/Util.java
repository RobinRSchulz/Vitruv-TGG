package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EReference;

public class Util {
    private Util() {}

    public static boolean isManyValued(EReference eReference) {
        return eReference.getUpperBound() == -1;
    }
}
