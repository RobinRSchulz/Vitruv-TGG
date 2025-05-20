package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.attribute.ReplaceSingleValuedEAttribute;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.EObjectPlaceholder;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Wraps EChanges that represent a replacement of an old value of an EAttribute of an EObject with a new Value.<br/><br/>
 *
 * Applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.feature.attribute.ReplaceSingleValuedEAttribute}
 */
public class EAttributeTwoValueEChangeWrapper extends EChangeWrapper {

    private final EAttribute affectedEAttribute;
    private EObjectPlaceholder oldValuePlaceholder;
    private EObjectPlaceholder newValuePlaceholder;

    /**
     *
     * @param eChangeType this template can only be matched against eChanges of this type.
     * @param affectedElementEClass this template can only be matched against eChanges whose affectedElements are instances of that Eclass
     * @param affectedElementPlaceholder a placeholder to be able to hold an actual mapping of the change.
     * @param affectedEAttribute this template can only be matched against eChanges whose affected eAttribute matches that EAttribute.
     * @param oldValuePlaceholder a placeholder for the (EDataType) value that is replaced by the newValue in an actual eChange.
     * @param newValuePlaceholder a placeholder for the new value that replaces the old value.
     */
    public EAttributeTwoValueEChangeWrapper(EClass eChangeType, EClass affectedElementEClass, EObjectPlaceholder affectedElementPlaceholder, EAttribute affectedEAttribute,
                                            EObjectPlaceholder oldValuePlaceholder, EObjectPlaceholder newValuePlaceholder) {
        super(eChangeType, affectedElementEClass, affectedElementPlaceholder);
        this.affectedEAttribute = affectedEAttribute;
        this.oldValuePlaceholder = oldValuePlaceholder;
        this.newValuePlaceholder = newValuePlaceholder;
    }

    public EAttribute getAffectedEAttribute() {
        return affectedEAttribute;
    }

    public EObjectPlaceholder getOldValuePlaceholder() {
        return oldValuePlaceholder;
    }

    public EObjectPlaceholder getNewValuePlaceholder() {
        return newValuePlaceholder;
    }

    @Override
    protected boolean extendedDataMatches(EChange<EObject> eChange) {
        switch (eChange) {
            case ReplaceSingleValuedEAttribute<EObject, ?> replaceSingleValuedEAttribute:
                if (oldValuePlaceholder.isInitialized() && !oldValuePlaceholder.getAffectedEObject().equals(replaceSingleValuedEAttribute.getOldValue())) {
                    // if this EChangeWrapper is partly initialized, the EObject it holds must be matched, too!
                    return false;
                }
                if (newValuePlaceholder.isInitialized() && !newValuePlaceholder.getAffectedEObject().equals(replaceSingleValuedEAttribute.getNewValue())) {
                    // if this EChangeWrapper is partly initialized, the EObject it holds must be matched, too!
                    return false;
                }
                return replaceSingleValuedEAttribute.getAffectedFeature().equals(affectedEAttribute);
            default: return false;
        }
    }

    @Override
    public void initializeExtension(EChange<EObject> eChange) {
        if (Objects.requireNonNull(eChange) instanceof ReplaceSingleValuedEAttribute<EObject, ?> replaceSingleValuedEAttribute) {
            this.newValuePlaceholder.initialize((EObject) replaceSingleValuedEAttribute.getNewValue());
            this.oldValuePlaceholder.initialize((EObject) replaceSingleValuedEAttribute.getNewValue());
        } else throw new IllegalStateException("Unexpected eChange: " + eChange);
    }

    @Override
    protected EChangeWrapper shallowCopy() {
        EChangeWrapper copy = new EAttributeTwoValueEChangeWrapper(this.getEChangeType(), this.getAffectedElementEClass(), this.getAffectedElementPlaceholder(),
                this.affectedEAttribute, this.oldValuePlaceholder, this.newValuePlaceholder);
        copy.setOriginal(this);
        return copy;
    }

    @Override
    protected Set<EObjectPlaceholder> getAllPlaceholders() {
        Set<EObjectPlaceholder> retSet = new HashSet<>();
        retSet.add(this.getAffectedElementPlaceholder());
        retSet.add(this.oldValuePlaceholder);
        retSet.add(this.newValuePlaceholder);
        return retSet;
    }

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
        return "[EAttributeTwoValueEChangeWrapper of " + getEChangeType().getName() + ". AE-type: " + getAffectedElementEClass().getName() + ", attribute: " + affectedEAttribute.getName() + "] " +
                "holding: AE: " + getAffectedElementPlaceholder() + ", oldV: " + getOldValuePlaceholder() + ", newV: " + getNewValuePlaceholder()
                + (this.isInitialized() ? ", initialized with " + Util.eChangeToString(getEChange()) : ", uninitialized");
    }

    @Override
    public String toString(String indent) {
        return "[EAttributeTwoValueEChangeWrapper of " + getEChangeType().getName() + ". AE-type: " + getAffectedElementEClass().getName() + ", attribute: " + affectedEAttribute.getName() + "] " +
                "holding:  \n" + indent + "AE: " + getAffectedElementPlaceholder() + ", oldV: " + getOldValuePlaceholder() + ", newV: " + getNewValuePlaceholder()
                + "\n" + indent
                + (this.isInitialized() ? ", initialized with " + Util.eChangeToString(getEChange()) : ", uninitialized");
    }
}
