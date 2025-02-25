package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion;

import org.eclipse.emf.ecore.EObject;

/**
 * A placeholder for an ${@link EObject} for initializing after creating the ${@link tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.EChangeWrapper}.
 *
 * This enables referencing the affected ${@link EObject}s in multiple EChangeWrappers before it is present.
 * This way, the pattern structure can be retained when multiple EChanges should be grouped together and share same entites.
 */
public class EObjectPlaceholder {

    private EObject affectedEObject;

    public EObjectPlaceholder() {  }

    /**
     * Initialize this placeholder with the given ${@link EObject}.
     * This is an idempotent operation, but ${@link IllegalStateException} is thrown if this placeholder is already initialized but initialize is called with an EObject different to the one it already holds.
     * @param affectedEObject
     */
    public void initialize(EObject affectedEObject) {
        if (this.isInitialized() && this.affectedEObject != affectedEObject) throw new IllegalStateException("EObjectPlaceholder already initialized. It cannot be initialized with a different EObject!");
        this.affectedEObject = affectedEObject;
    }

    /**
     *
     * @return whether this placeholder already holds an affected ${@link EObject}
     */
    public boolean isInitialized() {
        return affectedEObject != null;
    }

    /**
     *
     * @return the ${@link EObject} this placeholder holds.
     */
    public EObject getAffectedEObject() {
        return affectedEObject;
    }

    @Override
    public String toString() {
        return "[Placeholder:" + this.hashCode() + "]" + (this.isInitialized() ? (": " + eObjectToString(affectedEObject)) : "");
    }

    private String eObjectToString(EObject eObject) {
        return eObject.eClass().getName() + ":" + eObject.hashCode();
    }
}