package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * Represents a modification of a StructuralFeature of an EObject with an old and a new Value.
 * Applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.feature.attribute.ReplaceSingleValuedEAttribute}
 * <li> ${@link tools.vitruv.change.atomic.feature.reference.ReplaceSingleValuedEReference}
 *
 * @param <E>
 * @param <EStruc>
 */
public class EObjectEStructuralFeatureTwoValuePlaceholder extends Placeholder {

    private EObject affectedEObject;
    private EStructuralFeature affectedEStructuralFeature;
    private EObject oldValue;
    private EObject newValue;

    public EObjectEStructuralFeatureTwoValuePlaceholder() {}

    public void initialize(EObject affectedEObject, EStructuralFeature affectedEStructuralFeature,
                           EObject oldValue, EObject newValue) {
        this.affectedEObject = affectedEObject;
        this.affectedEStructuralFeature = affectedEStructuralFeature;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public boolean isInitialized() {
        return affectedEObject != null && affectedEStructuralFeature != null && oldValue != null && newValue != null;
    }

    public EObject getAffectedEObject() {
        return affectedEObject;
    }
    public EStructuralFeature getAffectedEStructuralFeature() {
        return affectedEStructuralFeature;
    }
    public EObject getOldValue() {
        return oldValue;
    }
    public EObject getNewValue() {
        return newValue;
    }
}