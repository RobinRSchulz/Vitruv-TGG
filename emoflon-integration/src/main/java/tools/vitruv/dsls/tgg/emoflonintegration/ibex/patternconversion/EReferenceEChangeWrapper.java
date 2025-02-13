package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import tools.vitruv.change.atomic.EChange;

/**
 *
 */
public class EReferenceEChangeWrapper extends EChangeWrapper {

    private EReference affectedEReference;

    /**
     *
     * @param eChangeType this template can only be matched against eChanges of this type.
     * @param affectedObjectEclass this template can only be matched against eChanges whose affectedElements are instances of that Eclass
     * @param placeholder a placeholder to be able to hold an actual mapping of the change.
     * @param affectedEReference this template can only be matched against eChanges whose affected eReference matches that EReference.
     */
    public EReferenceEChangeWrapper(Class eChangeType, EClass affectedObjectEclass, Placeholder placeholder, EReference affectedEReference) {
        super(eChangeType, affectedObjectEclass, placeholder);
        this.affectedEReference = affectedEReference;
    }

    public EReference getAffectedEReference() {
        return affectedEReference;
    }
}
