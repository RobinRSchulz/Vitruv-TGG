package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * Represents an EChange that affects an object which is also affected by other EChanges. That is realized via ${@link EObjectPlaceholder}s which occur in multiple EChangeWrappers to be able to correctly map the pattern structure.
 *
 * Enables referencing the affected EObjects in multiple EChangeWrappers before it is present.
 * This way, the pattern structure can be retained when multiple EChanges should be grouped together and share same entites.
 * E.g. a CreateEObject and a InsertRootEObject EChange should reference the same EObject.
 * Before matching the PatternTemplate to the actual Change sequence, this EObject cannot be known.
 *
 * Subclasses of this Placeholder represent different sets of parameters that EChanges can have.
 * We chose to not make a bijective mapping of all EChange subclasses to EChangeWrapper classes but instead group classes with the same set of parameters/ EObjects.
 */
public abstract class EChangeWrapper {

    private final EClass eChangeType;
    private EChange<EObject> eChange;

    /** Refers to the metamodel's EClass (running example: System, Protocol, ...).
     *  EChangeWrappers can only be applied to EChanges that have objects of that eClass as their affectedEObject  */
    private final EClass affectedElementEClass;
    private EObjectPlaceholder affectedElementPlaceholder;

    /**
     * This is only filled in pattern instantiations. It refers to the "original" EChangeWrapper and is used in pattern matching.
     */
    private EChangeWrapper parent;

    /**
     * We use Eclasses instead of Classes where there is no difference because
     *   * we stay in the "ecore-world"
     *   * no instanceof, which some don't like
     *   * maybe performance in switch-Statements?
     *
     * @param eChangeType this template can only be matched against eChanges of this type.
     * @param affectedElementEClass this template can only be matched against eChanges whose affectedElements are instances of that Eclass
     * @param affectedElementPlaceholder a affectedElementPlaceholder to be able to hold an actual mapping of the change.
     */
    public EChangeWrapper(EClass eChangeType, EClass affectedElementEClass, EObjectPlaceholder affectedElementPlaceholder) {
        this.eChangeType = eChangeType;
        this.affectedElementEClass = affectedElementEClass;
        this.affectedElementPlaceholder = affectedElementPlaceholder;
    }

    public void setEChange(EChange eChange) {
        // ye great old instanceof
        if (!eChangeType.isInstance(eChange)) {
            throw new RuntimeException("eChange is not of type " + eChange.eClass().getName());
        }
        this.eChange = eChange;
    }

    public EChange getEChange() {
        return eChange;
    }

    public EClass getEChangeType() {
        return eChangeType;
    }

    public EClass getAffectedElementEClass() {
        return affectedElementEClass;
    }

    public EObjectPlaceholder getAffectedElementPlaceholder() {
        return affectedElementPlaceholder;
    }
    public void setAffectedElementPlaceholder(EObjectPlaceholder newAffectedElementPlaceholder) {
        this.affectedElementPlaceholder = newAffectedElementPlaceholder;
    }

    protected void setParent(EChangeWrapper parent) {
        this.parent = parent;
    }
    public EChangeWrapper getParent() {
        return this.parent;
    }
    /**
     * [COPY helper]
     * @return a copy of this EChangeWrapper that has the identical Placeholder as this eChangeWrapper.
     */
    protected abstract EChangeWrapper shallowCopy();
    /**
     * [COPY helper]
     * @return all placeholders this EChangeWrapper holds
     */
    protected abstract Set<EObjectPlaceholder> getAllPlaceholders();
    /**
     * [COPY helper]
     * Replace all placeholders with their new objects from the map
     */
    protected abstract void replaceAllPlaceholders(Map<EObjectPlaceholder, EObjectPlaceholder> oldToNewPlaceholders);
}
