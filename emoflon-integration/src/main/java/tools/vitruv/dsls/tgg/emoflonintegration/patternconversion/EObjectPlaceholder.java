package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion;

import org.eclipse.emf.ecore.EObject;

/**
 * A placeholder for an EObject for initializing after creating the EChangeWrappers.
 *
 * This enables referencing the affected EObjects in multiple EChangeWrappers before it is present.
 * This way, the pattern structure can be retained when multiple EChanges should be grouped together and share same entites.
 */
public class EObjectPlaceholder {

    private EObject affectedEObject;

    public EObjectPlaceholder() {  }

    /**
     * Initialize this placeholder with the given eObject.
     * This is an idempotent operation, but ${@link IllegalStateException} is thrown if this placeholder is already initialized but initialize is called with an EObject different to the one it already holds.
     * @param affectedEObject
     */
    public void initialize(EObject affectedEObject) {
        if (this.isInitialized() && this.affectedEObject != affectedEObject) throw new IllegalStateException("EObjectPlaceholder already initialized. It cannot be initialized with a different EObject!");
        this.affectedEObject = affectedEObject;
    }

    public boolean isInitialized() {
        return affectedEObject != null;
    }

    public EObject getAffectedEObject() {
        return affectedEObject;
    }

    public String toString() {
        return "[Placeholder:" + this.hashCode() + "]" + (this.isInitialized() ? (": " + eObjectToString(affectedEObject)) : "");
    }

    private String eObjectToString(EObject eObject) {
        return eObject.eClass().getName() + ":" + eObject.hashCode();
    }
}