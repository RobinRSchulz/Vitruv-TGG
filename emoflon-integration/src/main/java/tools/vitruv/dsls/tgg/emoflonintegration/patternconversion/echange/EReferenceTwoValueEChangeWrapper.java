package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.reference.ReplaceSingleValuedEReference;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.EObjectPlaceholder;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.feature.reference.ReplaceSingleValuedEReference}
 */
public class EReferenceTwoValueEChangeWrapper extends EChangeWrapper {

    private final EReference affectedEReference;
    private EObjectPlaceholder oldValuePlaceholder;
    private EObjectPlaceholder newValuePlaceholder;

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
    protected boolean extendedDataMatches(EChange<EObject> eChange) {
        switch (eChange) {
            case ReplaceSingleValuedEReference<EObject> replaceSingleValuedEReference:
                if (oldValuePlaceholder.isInitialized() && !oldValuePlaceholder.getAffectedEObject().equals(replaceSingleValuedEReference.getOldValue())) {
                    // if this EChangeWrapper is partly initialized, the EObject it holds must be matched, too!
                    return false;
                }
                if (newValuePlaceholder.isInitialized() && !newValuePlaceholder.getAffectedEObject().equals(replaceSingleValuedEReference.getNewValue())) {
                    // if this EChangeWrapper is partly initialized, the EObject it holds must be matched, too!
                    return false;
                }
                return replaceSingleValuedEReference.getAffectedFeature().equals(affectedEReference);
            default: return false;
        }
    }

    @Override
    public void initializeExtension(EChange<EObject> eChange) {
        if (Objects.requireNonNull(eChange) instanceof ReplaceSingleValuedEReference<EObject> replaceSingleValuedEReference) {
            this.newValuePlaceholder.initialize(replaceSingleValuedEReference.getNewValue());
            this.oldValuePlaceholder.initialize(replaceSingleValuedEReference.getNewValue());
        } else throw new IllegalStateException("Unexpected eChange: " + eChange);
    }

    /**
     * [COPY helper]
     * @return a copy of this EChangeWrapper that has the identical Placeholder as this eChangeWrapper.
     */
    @Override
    protected EChangeWrapper shallowCopy() {
        EChangeWrapper copy = new EReferenceTwoValueEChangeWrapper(this.getEChangeType(), this.getAffectedElementEClass(), this.getAffectedElementPlaceholder(),
                this.affectedEReference, this.oldValuePlaceholder, this.newValuePlaceholder);
        copy.setOriginal(this);
        return copy;
    }
    /**
     * [COPY helper]
     * @return all placeholders this EChangeWrapper holds
     */
    @Override
    protected Set<EObjectPlaceholder> getAllPlaceholders() {
        Set<EObjectPlaceholder> retSet = new HashSet<>();
        retSet.add(this.getAffectedElementPlaceholder());
        retSet.add(this.oldValuePlaceholder);
        retSet.add(this.newValuePlaceholder);
        return retSet;
    }
    /**
     * [COPY helper]
     * Replace all placeholders with their new objects from the map
     */
    @Override
    protected void replaceAllPlaceholders(Map<EObjectPlaceholder, EObjectPlaceholder> oldToNewPlaceholders) {
        if (!oldToNewPlaceholders.containsKey(this.getAffectedElementPlaceholder())) {
            throw new IllegalStateException("oldToNewPlaceholders does not contain " + this.getAffectedElementPlaceholder());
        }
        if (!oldToNewPlaceholders.containsKey(this.oldValuePlaceholder)) {
            throw new IllegalStateException("oldToNewPlaceholders does not contain " + oldValuePlaceholder);
        }
        if (!oldToNewPlaceholders.containsKey(this.newValuePlaceholder)) {
            throw new IllegalStateException("oldToNewPlaceholders does not contain " + newValuePlaceholder);
        }

        this.setAffectedElementPlaceholder(oldToNewPlaceholders.get(this.getAffectedElementPlaceholder()));
        this.oldValuePlaceholder = oldToNewPlaceholders.get(this.oldValuePlaceholder);
        this.newValuePlaceholder = oldToNewPlaceholders.get(this.newValuePlaceholder);
    }

    @Override
    public String toString() {
        return "[EReferenceTwoValueEChangeWrapper of " + getEChangeType().getName() + ". AE-type: " + getAffectedElementEClass().getName() + ", reference: " + affectedEReference.getName() + "] " +
                "holding: AE: " + getAffectedElementPlaceholder() + ", oldV: " + getOldValuePlaceholder() + ", newV: " + getNewValuePlaceholder()
                + (this.isInitialized() ? ", initialized with " + Util.eChangeToString(getEChange()) : ", uninitialized");
    }

    @Override
    public String toString(String indent) {
        return "[EReferenceTwoValueEChangeWrapper of " + getEChangeType().getName() + ". AE-type: " + getAffectedElementEClass().getName() + ", reference: " + affectedEReference.getName() + "] " +
                "holding:  \n" + indent + "AE: " + getAffectedElementPlaceholder() + ", oldV: " + getOldValuePlaceholder() + ", newV: " + getNewValuePlaceholder()
                + "\n" + indent
                + (this.isInitialized() ? ", initialized with " + Util.eChangeToString(getEChange()) : ", uninitialized");
    }
}
