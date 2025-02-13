package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * Represents a modification of a StructuralFeature of an EObject with a Value.
 * Applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.feature.attribute.InsertEAttributeValue}
 * <li> ${@link tools.vitruv.change.atomic.feature.attribute.RemoveEAttributeValue}
 *
 */
public class EObjectEStructuralFeatureValuePlaceholder extends Placeholder {

    private EObject affectedEObject;
    private EStructuralFeature affectedEStructuralFeature;
    private EObject value;

    public EObjectEStructuralFeatureValuePlaceholder() {}

    public void initialize(EObject affectedEObject, EStructuralFeature affectedEStructuralFeature, EObject value) {
        this.affectedEObject = affectedEObject;
        this.affectedEStructuralFeature = affectedEStructuralFeature;
        this.value = value;
    }

    public boolean isInitialized() {
        return affectedEObject != null && affectedEStructuralFeature != null && value != null;
    }

    public EObject getAffectedEObject() {
        return affectedEObject;
    }
    public EStructuralFeature getAffectedEOStructuralFeature() {
        return affectedEStructuralFeature;
    }
    public EObject getValue() {
        return value;
    }
}
