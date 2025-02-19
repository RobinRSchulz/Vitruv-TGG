package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
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

    protected static final Logger logger = Logger.getRootLogger();

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
     * @return whether the eChange type and affectedEObject type of this and the eChange match.
     */
    private boolean eChangeTypeAndAffectedEObjectMatches(EChange<EObject> eChange) {
        final EObject affectedEObjectFromEChange = Util.getAffectedEObjectFromEChange(eChange);

        if (affectedElementPlaceholder.isInitialized() && !affectedElementPlaceholder.getAffectedEObject().equals(affectedEObjectFromEChange)) {
            // if this EChangeWrapper is partly initialized, the EObject it holds must be matched, too!
            return false;
        }
        return this.getEChangeType().equals(eChange.eClass()) && this.getAffectedElementEClass().equals(affectedEObjectFromEChange.eClass());
    }


    /**
     *
     * @param eChange
     * @return whether this eChange matches this eChangeWrapper.
     *         Obacht! This EChangeWrapper might be partly initialized, as some placeholders could have already been filled
     *         by initializing another EChangeWrapper belonging to the same ${@link IbexPatternTemplate} as this one.
     *         This introduces the requirement for matching not only the "statical properties" (meaning EClasses and EStructuralFeatures)
     *         but also checking whether the <b>already initialized</b> placeholders match the echange!
     *         TODO implement this!
     */
    protected abstract boolean extendedDataMatches(EChange<EObject> eChange);

    /**
     *
     * @param eChange
     * @return whether this eChange matches this eChangeWrapper.
     *         Obacht! This EChangeWrapper might be partly initialized, as some placeholders could have already been filled
     *         by initializing another EChangeWrapper belonging to the same ${@link IbexPatternTemplate} as this one.
     *         This introduces the requirement for matching not only the "statical properties" (meaning EClasses and EStructuralFeatures)
     *         but also checking whether the <b>already initialized</b> placeholders match the echange!
     */
    public boolean matches(EChange<EObject> eChange) {
        return  eChangeTypeAndAffectedEObjectMatches(eChange) && extendedDataMatches(eChange);
    }

    /**
     * Initialize everything that doesn't concern setting the eChange and filling the affectedElementPlaceholder.
     * Obacht! This EChangeWrapper might be partly initialized, as some placeholders could have already been filled
     * by initializing another EChangeWrapper belonging to the same ${@link IbexPatternTemplate} as this one.
     * Implementations need to account for that by throwing ${@link IllegalStateException}. TODO implement that!
     * @param eChange
     */
    protected abstract void initializeExtension(EChange<EObject> eChange);

    /**
     * Initialize this wrapper with an actual eChange, by filling all this wrapper's placeholders.
     * Obacht! This EChangeWrapper might be partly initialized, as some placeholders could have already been filled
     * by initializing another EChangeWrapper belonging to the same ${@link IbexPatternTemplate} as this one.
     * This is intended and this method accounts for that.
     * It is intended that the caller calls ${@code matches()} first.
     * This method will throw ${@link IllegalStateException}, if a placeholder that already contains something would be overwritten by applying the given eChange.
     * @param eChange
     */
    public void initialize(EChange<EObject> eChange) {
        this.setEChange(eChange);
        this.getAffectedElementPlaceholder().initialize(Util.getAffectedEObjectFromEChange(eChange));
        // delegate further initialization to subclasses
        initializeExtension(eChange);
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
