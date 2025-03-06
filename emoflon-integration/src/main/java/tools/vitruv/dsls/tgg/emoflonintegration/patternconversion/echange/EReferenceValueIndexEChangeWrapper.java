package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.reference.InsertEReference;
import tools.vitruv.change.atomic.feature.reference.RemoveEReference;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.EObjectPlaceholder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Wraps EChanges that represent a modification of an EReference of an EObject with a Value at a specific index.<br/><br/>
 *
 * Applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.feature.reference.InsertEReference}
 * <li> ${@link tools.vitruv.change.atomic.feature.reference.RemoveEReference}
 */
public class EReferenceValueIndexEChangeWrapper extends EChangeWrapper {

    private final EReference affectedEReference;
    private EObjectPlaceholder valuePlaceholder;
    private Integer index;

    /**
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
    protected boolean extendedDataMatches(EChange<EObject> eChange) {
        return switch (eChange) {
            case InsertEReference<EObject> insertEReference -> {
                if (valuePlaceholder.isInitialized() && !valuePlaceholder.getAffectedEObject().equals(insertEReference.getNewValue())) {
                    // if this EChangeWrapper is partly initialized, the EObject it holds must be matched, too!
                    yield false;
                }
                yield insertEReference.getAffectedFeature().equals(affectedEReference);
            }
            case RemoveEReference<EObject> removeEReference -> {
                if (valuePlaceholder.isInitialized() && !valuePlaceholder.getAffectedEObject().equals(removeEReference.getOldValue())) {
                    // if this EChangeWrapper is partly initialized, the EObject it holds must be matched, too!
                    yield false;
                }
                yield removeEReference.getAffectedFeature().equals(affectedEReference);
            }
            default -> false;
        };
    }

    @Override
    public void initializeExtension(EChange<EObject> eChange) {
        switch (eChange) {
            case InsertEReference<EObject> insertEReference:
                this.valuePlaceholder.initialize(insertEReference.getNewValue());
                this.index = insertEReference.getIndex();
                break;
            case RemoveEReference<EObject> removeEReference:
                this.valuePlaceholder.initialize(removeEReference.getOldValue());
                this.index = removeEReference.getIndex();
                break;
            default: throw new IllegalStateException("Unexpected eChange: " + eChange);
        }
    }

    @Override
    protected EChangeWrapper shallowCopy() {
        EChangeWrapper copy = new EReferenceValueIndexEChangeWrapper(this.getEChangeType(), this.getAffectedElementEClass(), this.getAffectedElementPlaceholder(),
                this.affectedEReference, this.valuePlaceholder);
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
        return "[EReferenceValueIndexEChangeWrapper of " + getEChangeType().getName() + ". AE-type: " + getAffectedElementEClass().getName() + ", reference: " + affectedEReference.getName() + "] " +
                "holding: AE: " + getAffectedElementPlaceholder() + ", V: " + getValuePlaceholder() + ", I:" + getIndex()
                + (this.isInitialized() ? ", initialized with " + Util.eChangeToString(getEChange()) : ", uninitialized");
    }

    @Override
    public String toString(String indent) {
        return "[EReferenceValueIndexEChangeWrapper of " + getEChangeType().getName() + ". AE-type: " + getAffectedElementEClass().getName() + ", reference: " + affectedEReference.getName() + "] " +
                "holding:  \n" + indent + "AE: " + getAffectedElementPlaceholder() + ", V: " + getValuePlaceholder() + ", I:" + getIndex()
                + "\n" + indent
                + (this.isInitialized() ? ", initialized with " + Util.eChangeToString(getEChange()) : ", uninitialized");
    }
}
