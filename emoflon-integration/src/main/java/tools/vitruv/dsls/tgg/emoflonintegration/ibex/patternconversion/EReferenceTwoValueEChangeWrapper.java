package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;

/**
 * Applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.feature.reference.ReplaceSingleValuedEReference}
 */
public class EReferenceTwoValueEChangeWrapper extends EChangeWrapper {

    private final EReference affectedEReference;
    private final EObjectPlaceholder oldValuePlaceholder;
    private final EObjectPlaceholder newValuePlaceholder;

    /**
     * We use Eclasses instead of Classes where there is no difference because
     * * we stay in the "ecore-world"
     * * no instanceof, which some don't like
     * * maybe performance in switch-Statements?
     *
     * @param eChangeType                this template can only be matched against eChanges of this type.
     * @param affectedElementEClass      this template can only be matched against eChanges whose affectedElements are instances of that Eclass
     * @param affectedElementPlaceholder a affectedElementPlaceholder to be able to hold an actual mapping of the change.
     * @param oldValuePlaceholder a placeholder for the (EDataType) value that is replaced by the newValue in an actual eChange.
     * @param newValuePlaceholder a placeholder for the new value that replaces the old value.
     */
    public EReferenceTwoValueEChangeWrapper(EClass eChangeType, EClass affectedElementEClass, EObjectPlaceholder affectedElementPlaceholder, EReference affectedEReference,
                                            EObjectPlaceholder oldValuePlaceholder, EObjectPlaceholder newValuePlaceholder) {
        super(eChangeType, affectedElementEClass, affectedElementPlaceholder);
        this.affectedEReference = affectedEReference;
        this.oldValuePlaceholder = oldValuePlaceholder;
        this.newValuePlaceholder = newValuePlaceholder;
    }

    public EReference getAffectedEReference() {
        return affectedEReference;
    }

    public EObjectPlaceholder getOldValuePlaceholder() {
        return oldValuePlaceholder;
    }

    public EObjectPlaceholder getNewValuePlaceholder() {
        return newValuePlaceholder;
    }
    //todo add isInitialized method in superclass which this overrides


    @Override
    public String toString() {
        return "[EAttributeValueEChangeWrapper of " + getEChangeType().getName() + ". affectedElement Type: " + getAffectedElementEClass().getName() + ", attribute: " + affectedEReference.getName() + "] " +
                "holding: AE: " + getAffectedElementPlaceholder() + ", oldV: " + getOldValuePlaceholder() + ", newV: " + getNewValuePlaceholder();
    }
}
