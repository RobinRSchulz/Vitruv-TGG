package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EObject;

/**
 * A placeholder for an EObject for initializing after creating the EChangeWrappers.
 *
 * This enables referencing the affected EObjects in multiple EChangeWrappers before it is present.
 * This way, the pattern structure can be retained when multiple EChanges should be grouped together and share same entites.
 */
public  class EObjectPlaceholder {

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