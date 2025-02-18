package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternmatching.Util;

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

    private boolean isInitialized;

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
        this.isInitialized = false;
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
     * This is a basic check which every matches-implementation should use.
     * @param eChange
     * @param vitruviusChange
     * @return whether the eChange type and affectedEObject type of this and the eChange match.
     */
    private boolean eChangeTypeAndAffectedEObjectMatches(EChange<EObject> eChange, VitruviusChange<EObject> vitruviusChange) {
        return this.getAffectedElementEClass().equals(eChange.eClass()) && this.getAffectedElementEClass().equals(Util.getAffectedEObjectFromEChange(eChange, vitruviusChange).eClass());
    }


    /**
     *
     * @param eChange
     * @return whether this eChange matches this eChangeWrapper.
     */
    protected abstract boolean extendedDataMatches(EChange<EObject> eChange, VitruviusChange<EObject> vitruviusChange);

    /**
     *
     * @param eChange
     * @return whether this eChange matches this eChangeWrapper.
     */
    public boolean matches(EChange<EObject> eChange, VitruviusChange<EObject> vitruviusChange) {
        return  eChangeTypeAndAffectedEObjectMatches(eChange, vitruviusChange) && extendedDataMatches(eChange, vitruviusChange);
    }

    protected abstract void initializeImpl(EChange<EObject> eChange, VitruviusChange<EObject> vitruviusChange);

    /**
     * Initialize this wrapper with an actual eChange, by filling all this wrapper's placeholders.
     * @param eChange
     * @param vitruviusChange only for certain cases to extract affected object...
     */
    public void initialize(EChange<EObject> eChange, VitruviusChange<EObject> vitruviusChange) {
        initializeImpl(eChange, vitruviusChange);
        this.isInitialized = true;
    }

    /**
     *
     * @return whether ${@code initialize()} has been called, i.e. whether this wrapper holds an actual eChange.
     */
    public boolean isInitialized() {
        return this.isInitialized;
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
