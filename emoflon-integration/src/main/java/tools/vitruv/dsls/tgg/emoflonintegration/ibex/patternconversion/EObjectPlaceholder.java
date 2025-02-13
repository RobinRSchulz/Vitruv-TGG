package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EObject;

/**
 * Represents changes that refer to only the affected elements.
 * Applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.eobject.CreateEObject}
 * <li> ${@link tools.vitruv.change.atomic.eobject.DeleteEObject}
 * <li> ${@link tools.vitruv.change.atomic.root.InsertRootEObject}
 * <li> ${@link tools.vitruv.change.atomic.root.RemoveRootEObject}
 * <li> ${@link tools.vitruv.change.atomic.feature.UnsetFeature}
 *
 */
public class EObjectPlaceholder extends Placeholder{

    private EObject affectedEObject;

    public EObjectPlaceholder() {  }

    public void initialize(EObject affectedEObject) {
        this.affectedEObject = affectedEObject;
    }

    public boolean isInitialized() {
        return affectedEObject != null;
    }

    public EObject getAffectedEObject() {
        return affectedEObject;
    }
}
