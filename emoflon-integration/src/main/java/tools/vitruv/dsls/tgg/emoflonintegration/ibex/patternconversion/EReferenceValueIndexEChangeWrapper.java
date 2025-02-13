package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;

/**
 * Applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.feature.reference.InsertEReference}
 * <li> ${@link tools.vitruv.change.atomic.feature.reference.RemoveEReference}
 */
public class EReferenceValueIndexEChangeWrapper extends EChangeWrapper {

    private final EReference affectedEReference;
    private final EObjectPlaceholder valuePlaceholder;
    private Integer index;

    /**
     * We use Eclasses instead of Classes where there is no difference because
     * * we stay in the "ecore-world"
     * * no instanceof, which some don't like
     * * maybe performance in switch-Statements?
     *
     * @param eChangeType                this template can only be matched against eChanges of this type.
     * @param affectedElementEClass      this template can only be matched against eChanges whose affectedElements are instances of that Eclass
     * @param affectedElementPlaceholder a affectedElementPlaceholder to be able to hold an actual mapping of the change.
     * @param affectedEReference this template can only be matched against eChanges whose affected eReference matches that EReference.
     */
    public EReferenceValueIndexEChangeWrapper(EClass eChangeType, EClass affectedElementEClass, EObjectPlaceholder affectedElementPlaceholder, EReference affectedEReference,
                                              EObjectPlaceholder valuePlaceholder) {
        super(eChangeType, affectedElementEClass, affectedElementPlaceholder);
        this.affectedEReference = affectedEReference;
        this.valuePlaceholder = valuePlaceholder;
        this.index = null;
    }

    public EReference getAffectedEReference() {
        return affectedEReference;
    }

    public EObjectPlaceholder getValuePlaceholder() {
        return valuePlaceholder;
    }

    public Integer getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "[EAttributeValueEChangeWrapper of " + getEChangeType().getName() + ". affectedElement Type: " + getAffectedElementEClass().getName() + ", attribute: " + affectedEReference.getName() + "] " +
                "holding: AE: " + getAffectedElementPlaceholder() + ", V: " + getValuePlaceholder() + ", I:" + getIndex();
        //TODO add what this is holding.
    }
}
