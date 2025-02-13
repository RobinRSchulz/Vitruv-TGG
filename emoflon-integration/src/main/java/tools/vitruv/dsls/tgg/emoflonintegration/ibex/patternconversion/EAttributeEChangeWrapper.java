package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;

public class EAttributeEChangeWrapper extends EChangeWrapper {


    private EAttribute affectedEAttribute;

    /**
     *
     * @param eChangeType this template can only be matched against eChanges of this type.
     * @param affectedObjectEclass this template can only be matched against eChanges whose affectedElements are instances of that Eclass
     * @param placeholder a placeholder to be able to hold an actual mapping of the change.
     * @param affectedEAttribute this template can only be matched against eChanges whose affected eAttribute matches that EAttribute.
     */
    public EAttributeEChangeWrapper(Class eChangeType, EClass affectedObjectEclass, Placeholder placeholder, EAttribute affectedEAttribute) {
        super(eChangeType, affectedObjectEclass, placeholder);
        this.affectedEAttribute = affectedEAttribute;
    }

    public EAttribute getAffectedEAttribute() {
        return affectedEAttribute;
    }
}
