package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.attribute.InsertEAttributeValue;
import tools.vitruv.change.atomic.feature.attribute.RemoveEAttributeValue;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.EObjectPlaceholder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Wraps EChanges that represent a modification of an EAttribute of an EObject with a Value.<br/><br/>
 *
 * Applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.feature.attribute.InsertEAttributeValue}
 * <li> ${@link tools.vitruv.change.atomic.feature.attribute.RemoveEAttributeValue}
 */
public class EAttributeValueEChangeWrapper extends EChangeWrapper {

    private final EAttribute affectedEAttribute;
    private EObjectPlaceholder valuePlaceholder;

    /**
     *
     * @param eChangeType this template can only be matched against eChanges of this type.
     * @param affectedElementEClass this template can only be matched against eChanges whose affectedElements are instances of that Eclass
     * @param affectedElementPlaceholder a placeholder to be able to hold an actual mapping of the change.
     * @param affectedEAttribute this template can only be matched against eChanges whose affected eAttribute matches that EAttribute.
     * @param valuePlaceholder a placeholder for the value that is modified by an actual eChange.
     */
    public EAttributeValueEChangeWrapper(EClass eChangeType, EClass affectedElementEClass, EObjectPlaceholder affectedElementPlaceholder, EAttribute affectedEAttribute, EObjectPlaceholder valuePlaceholder) {
        super(eChangeType, affectedElementEClass, affectedElementPlaceholder);
        this.affectedEAttribute = affectedEAttribute;
        this.valuePlaceholder = valuePlaceholder;
    }

    public EAttribute getAffectedEAttribute() {
        return affectedEAttribute;
    }

    public EObjectPlaceholder getValuePlaceholder() {
        return valuePlaceholder;
    }

    @Override
    protected boolean extendedDataMatches(EChange<EObject> eChange) {
        return switch (eChange) {
            case InsertEAttributeValue<EObject, ?> insertEAttributeValue -> {
                if (valuePlaceholder.isInitialized() && !valuePlaceholder.getAffectedEObject().equals(insertEAttributeValue.getNewValue())) {
                    // if this EChangeWrapper is partly initialized, the EObject it holds must be matched, too!
                    yield false;
                }
                yield insertEAttributeValue.getAffectedFeature().equals(affectedEAttribute);
            }
            case RemoveEAttributeValue<EObject, ?> removeEAttributeValue -> {
                if (valuePlaceholder.isInitialized() && !valuePlaceholder.getAffectedEObject().equals(removeEAttributeValue.getOldValue())) {
                    // if this EChangeWrapper is partly initialized, the EObject it holds must be matched, too!
                    yield false;
                }
                yield removeEAttributeValue.getAffectedFeature().equals(affectedEAttribute);
            }
            default -> false;
        };
    }

    @Override
    public void initializeExtension(EChange<EObject> eChange) {
        switch (eChange) {
            case InsertEAttributeValue<EObject, ?> insertEAttributeValue:
                this.valuePlaceholder.initialize((EObject) insertEAttributeValue.getNewValue());
                break;
            case RemoveEAttributeValue<EObject, ?> removeEAttributeValue:
                this.valuePlaceholder.initialize((EObject) removeEAttributeValue.getOldValue());
                break;
            default: throw new IllegalStateException("Unexpected eChange: " + eChange);
        }
    }

    @Override
    protected EChangeWrapper shallowCopy() {
        EChangeWrapper copy = new EAttributeValueEChangeWrapper(this.getEChangeType(), this.getAffectedElementEClass(), this.getAffectedElementPlaceholder(),
                this.affectedEAttribute, this.valuePlaceholder);
        copy.setOriginal(this);
        return copy;
    }

    @Override
    protected Set<EObjectPlaceholder> getAllPlaceholders() {
        Set<EObjectPlaceholder> retSet = new HashSet<>();
        retSet.add(this.getAffectedElementPlaceholder());
        retSet.add(this.valuePlaceholder);
        return retSet;
    }

    @Override
    protected void replaceAllPlaceholders(Map<EObjectPlaceholder, EObjectPlaceholder> oldToNewPlaceholders) {
        if (!oldToNewPlaceholders.containsKey(this.getAffectedElementPlaceholder())) {
            throw new IllegalStateException("oldToNewPlaceholders does not contain " + this.getAffectedElementPlaceholder());
        }
        if (!oldToNewPlaceholders.containsKey(this.valuePlaceholder)) {
            throw new IllegalStateException("oldToNewPlaceholders does not contain " + valuePlaceholder);
        }

        this.setAffectedElementPlaceholder(oldToNewPlaceholders.get(this.getAffectedElementPlaceholder()));
        this.valuePlaceholder = oldToNewPlaceholders.get(this.valuePlaceholder);
    }

    @Override
    public String toString() {
        return "[EAttributeValueEChangeWrapper of " + getEChangeType().getName() + ". AE-type: " + getAffectedElementEClass().getName() + ", attribute: " + affectedEAttribute.getName() + "] " +
                "holding: AE: " + getAffectedElementPlaceholder() + ", V: " + getValuePlaceholder()
                + (this.isInitialized() ? ", initialized with " + Util.eChangeToString(getEChange()) : ", uninitialized");
    }

    @Override
    public String toString(String indent) {
        return "[EAttributeValueEChangeWrapper of " + getEChangeType().getName() + ". AE-type: " + getAffectedElementEClass().getName() + ", attribute: " + affectedEAttribute.getName() + "] " +
                "holding:  \n" + indent + "AE: "  + getAffectedElementPlaceholder() + ", V: " + getValuePlaceholder()
                + "\n" + indent
                + (this.isInitialized() ? ", initialized with " + Util.eChangeToString(getEChange()) : ", uninitialized");
    }
}
