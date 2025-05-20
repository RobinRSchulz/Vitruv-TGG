package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion;

import language.TGGRuleNode;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;

/**
 * A placeholder for an ${@link EObject} for initializing after creating the ${@link tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.EChangeWrapper}.
 * <br/>
 * This enables referencing the affected ${@link EObject}s in multiple EChangeWrappers before it is present.
 * This way, the pattern structure can be retained when multiple EChanges should be grouped together and share same entities.
 * <br/>
 * Also, placeholders represent nodes in the pattern whence they came from.
 */
public class EObjectPlaceholder {

    private EObject affectedEObject;
    private final TGGRuleNode tggRuleNode;

    /**
     *
     * @param tggRuleNode remember the node for pattern matching later on.
     */
    public EObjectPlaceholder(TGGRuleNode tggRuleNode) {
        this.tggRuleNode = tggRuleNode;
    }

    /**
     * Initialize this placeholder with the given ${@link EObject}.
     * This is an idempotent operation, but ${@link IllegalStateException} is thrown if this placeholder is already initialized but initialize is called with an EObject different to the one it already holds.
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

    /**
     *
     * @return the node in the TGG rule that this placeholder represents.
     */
    public TGGRuleNode getTggRuleNode() {
        return tggRuleNode;
    }

    @Override
    public String toString() {
        return "[Placeholder:" + Integer.toHexString(this.hashCode()) + "]" + (this.isInitialized() ? (": " + Util.eObjectToString(affectedEObject)) : "");
    }
}