package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;


/**
 * Represents a modification of a StructuralFeature of an EObject with a Value.
 * Applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.feature.reference.InsertEReference}
 * <li> ${@link tools.vitruv.change.atomic.feature.reference.RemoveEReference}
 *
 */
public class EObjectEStructuralFeatureValueIndexPlaceholder extends Placeholder {

    private EObject affectedEObject;
    private EStructuralFeature affectedEStructuralFeature;
    private EObject value;
    private Integer index;

    public EObjectEStructuralFeatureValueIndexPlaceholder() {}

    public void initialize(EObject affectedEObject, EStructuralFeature affectedEStructuralFeature, EObject value, int index) {
        this.affectedEObject = affectedEObject;
        this.affectedEStructuralFeature = affectedEStructuralFeature;
        this.value = value;
        this.index = index;
    }

    public boolean isInitialized() {
        return affectedEObject != null && affectedEStructuralFeature != null && value != null && index != null;
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
    public Integer getIndex() {
        return index;
    }
}

