package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.attribute.InsertEAttributeValue;
import tools.vitruv.change.atomic.feature.attribute.RemoveEAttributeValue;
import tools.vitruv.change.atomic.feature.reference.InsertEReference;
import tools.vitruv.change.atomic.feature.reference.RemoveEReference;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternmatching.Util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.feature.reference.InsertEReference}
 * <li> ${@link tools.vitruv.change.atomic.feature.reference.RemoveEReference}
 */
public class EReferenceValueIndexEChangeWrapper extends EChangeWrapper {

    private final EReference affectedEReference;
    private EObjectPlaceholder valuePlaceholder;
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
    protected boolean extendedDataMatches(EChange<EObject> eChange, VitruviusChange<EObject> vitruviusChange) {
        switch (eChange) {
            case InsertEReference<EObject> insertEReference:
                if (valuePlaceholder.isInitialized() && !valuePlaceholder.getAffectedEObject().equals(insertEReference.getNewValue())) {
                    // if this EChangeWrapper is partly initialized, the EObject it holds must be matched, too!
                    return false;
                }
                return insertEReference.getAffectedFeature().equals(affectedEReference);
            case RemoveEReference removeEReference:
                if (valuePlaceholder.isInitialized() && !valuePlaceholder.getAffectedEObject().equals(removeEReference.getOldValue())) {
                    // if this EChangeWrapper is partly initialized, the EObject it holds must be matched, too!
                    return false;
                }
                return removeEReference.getAffectedFeature().equals(affectedEReference);
            default: return false;
        }
    }

    @Override
    public void initializeExtension(EChange<EObject> eChange, VitruviusChange<EObject> vitruviusChange) {
        switch (eChange) {
            case InsertEReference<EObject> insertEReference:
                this.valuePlaceholder.initialize(insertEReference.getNewValue());
                break;
            case RemoveEReference<EObject> removeEReference:
                this.valuePlaceholder.initialize(removeEReference.getOldValue());
                break;
            default: throw new IllegalStateException("Unexpected eChange: " + eChange);
        }
    }

    /**
     * [COPY helper]
     * @return a copy of this EChangeWrapper that has the identical Placeholder as this eChangeWrapper.
     */
    @Override
    protected EChangeWrapper shallowCopy() {
        EChangeWrapper copy = new EReferenceValueIndexEChangeWrapper(this.getEChangeType(), this.getAffectedElementEClass(), this.getAffectedElementPlaceholder(),
                this.affectedEReference, this.valuePlaceholder);
        copy.setParent(this);
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
        retSet.add(this.valuePlaceholder);
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
        if (!oldToNewPlaceholders.containsKey(this.valuePlaceholder)) {
            throw new IllegalStateException("oldToNewPlaceholders does not contain " + valuePlaceholder);
        }

        this.setAffectedElementPlaceholder(oldToNewPlaceholders.get(this.getAffectedElementPlaceholder()));
        this.valuePlaceholder = oldToNewPlaceholders.get(this.valuePlaceholder);
    }
    @Override
    public String toString() {
        return "[EReferenceValueIndexEChangeWrapper of " + getEChangeType().getName() + ". affectedElement Type: " + getAffectedElementEClass().getName() + ", reference: " + affectedEReference.getName() + "] " +
                "holding: AE: " + getAffectedElementPlaceholder() + ", V: " + getValuePlaceholder() + ", I:" + getIndex();
        //TODO add what this is holding.
    }
}
