package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.EObjectPlaceholder;

import java.util.Map;
import java.util.Set;

/**
 * Represents an EChange that affects an object (or more, in subclasses) which is also affected by other EChanges.
 * That is realized via ${@link EObjectPlaceholder}s which occur in multiple EChangeWrappers to be able to correctly map the pattern structure.<br/><br/>
 *
 * Enables referencing the affected EObjects in multiple EChangeWrappers before it is present.<br/>
 * This way, the pattern structure can be retained when multiple EChanges should be grouped together and share same entities.<br/>
 * E.g. a CreateEObject and a InsertRootEObject EChange should reference the same EObject.<br/>
 * Before matching the PatternTemplate to the actual Change sequence, this EObject cannot be known.<br/><br/>
 *
 * Subclasses of this Placeholder represent different sets of parameters that EChanges can have.<br/>
 * We chose to not make a bijective mapping of all EChange subclasses to EChangeWrapper classes but instead group classes with the same set of parameters/ EObjects.<br/><br/>
 *
 * EChangeWrappers are placed in their own package, because  the protected methods should be unreachable by the classes using the EChangeWrappers.
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
     * This is only filled in pattern instantiations. It refers to the "original" EChangeWrapper from the ${@link tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.ChangeSequenceTemplateSet} and is used in pattern matching.
     */
    private EChangeWrapper original;

    private boolean isInitialized;

    /**
     * We use EClasses instead of Classes where there is no difference because
     *   * we stay in the "ecore-world"
     *   * no instanceof, which some don't like
     *   * maybe performance in switch-Statements?
     *
     * @param eChangeType this template can only be matched against eChanges of this type.
     * @param affectedElementEClass this template can only be matched against eChanges whose affectedElements are instances of that EClass
     * @param affectedElementPlaceholder a affectedElementPlaceholder to be able to hold an actual mapping of the change.
     */
    public EChangeWrapper(EClass eChangeType, EClass affectedElementEClass, EObjectPlaceholder affectedElementPlaceholder) {
        this.eChangeType = eChangeType;
        this.affectedElementEClass = affectedElementEClass;
        this.affectedElementPlaceholder = affectedElementPlaceholder;
        this.isInitialized = false;
    }

    /**
     * Initialize this EChangeWrapper with the given EChange if it has the correct type. Else, throw.
     */
    public void setEChange(EChange<EObject> eChange) {
        if (!eChangeType.isInstance(eChange)) {
            throw new IllegalStateException("eChange is not of type " + eChange.eClass().getName());
        }
        this.eChange = eChange;
    }

    /**
     * @return the ${@link EChange} this wrapper holds if it holds one.
     */
    public EChange<EObject> getEChange() {
        return eChange;
    }

    /**
     * @return the type of ${@link EChange} this wrapper can hold.
     */
    public EClass getEChangeType() {
        return eChangeType;
    }

    /**
     * @return the type the "main" element (affectedElement) that an ${@link EChange} which this wrapper can hold will affect.
     */
    public EClass getAffectedElementEClass() {
        return affectedElementEClass;
    }

    /**
     * @return the placeholder for the affectedElement of the ${@link EChange} that this wrapper represents or will represent.
     * This placeholder might be initialized although this Wrapper is not yet mapped to an ${@link EChange}. That is intentional.
     */
    public EObjectPlaceholder getAffectedElementPlaceholder() {
        return affectedElementPlaceholder;
    }
    public void setAffectedElementPlaceholder(EObjectPlaceholder newAffectedElementPlaceholder) {
        this.affectedElementPlaceholder = newAffectedElementPlaceholder;
    }

    /**
     * Remember the original EChangeWrapper that this is an invocation (copy) of.
     */
    protected void setOriginal(EChangeWrapper original) {
        this.original = original;
    }

    /**
     *
     * @return the original EChangeWrapper that this is an invocation (copy) of, IF this is a copy. If this is an original, return null.
     */
    public EChangeWrapper getOriginal() {
        return this.original;
    }

    /**
     *
     * @return whether this is an original or an invocation (copy) of one.
     */
    public boolean isOriginal() { return this.original == null; }

    /**
     * This is a helper for this::matches()
     * @return whether the eChange type and affectedEObject type of this and the eChange match. If the affectedElementPlaceholder is initialized, this is matched, too.
     */
    private boolean eChangeTypeAndAffectedEObjectMatches(EChange<EObject> eChange) {
        final EObject affectedEObjectFromEChange = Util.getAffectedEObjectFromEChange(eChange);

        if (affectedElementPlaceholder.isInitialized() && !affectedElementPlaceholder.getAffectedEObject().equals(affectedEObjectFromEChange)) {
            // if this EChangeWrapper is partly initialized, the EObject it holds must be matched, too!
            return false;
        }

        return this.getEChangeType().isSuperTypeOf(eChange.eClass()) && this.getAffectedElementEClass().equals(affectedEObjectFromEChange.eClass());
    }


    /**
     *
     * @return whether this eChange matches this eChangeWrapper.
     *         Beware! This EChangeWrapper might be partly initialized, as some placeholders could have already been filled
     *         by initializing another EChangeWrapper belonging to the same ${@link ChangeSequenceTemplate} as this one.
     *         This introduces the requirement for matching not only the "statical properties" (meaning EClasses and EStructuralFeatures)
     *         but also checking whether the <b>already initialized</b> placeholders match the eChange!
     */
    protected abstract boolean extendedDataMatches(EChange<EObject> eChange);

    /**
     *
     * @return whether this eChange matches this eChangeWrapper.
     *         Beware! This EChangeWrapper might be partly initialized, as some placeholders could have already been filled
     *         by initializing another EChangeWrapper belonging to the same ${@link ChangeSequenceTemplate} as this one.
     *         This introduces the requirement for matching not only the "statical properties" (meaning EClasses and EStructuralFeatures)
     *         but also checking whether the <b>already initialized</b> placeholders match the eChange!
     */
    public boolean matches(EChange<EObject> eChange) {
        return  eChangeTypeAndAffectedEObjectMatches(eChange) && extendedDataMatches(eChange);
    }

    /**
     * Initialize everything that doesn't concern setting the eChange and filling the affectedElementPlaceholder.
     * Beware! This EChangeWrapper might be partly initialized, as some placeholders could have already been filled
     * by initializing another EChangeWrapper belonging to the same ${@link ChangeSequenceTemplate} as this one.
     * Implementations need to account for that by throwing ${@link IllegalStateException}.
     */
    protected abstract void initializeExtension(EChange<EObject> eChange);

    /**
     * Initialize this wrapper with an actual eChange, by filling all this wrapper's placeholders.
     * Beware! This EChangeWrapper might be partly initialized, as some placeholders could have already been filled
     * by initializing another EChangeWrapper belonging to the same ${@link ChangeSequenceTemplate} as this one.
     * This is intended and this method accounts for that.
     * It is intended that the caller calls ${@code matches()} first.
     * This method will throw ${@link IllegalStateException}, if a placeholder that already contains something would be overwritten by applying the given eChange.
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
     * @return a copy of this EChangeWrapper that has the identical Placeholder as this eChangeWrapper and has this eChangeWrapper set as its original.
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

    /**
     * [DEBUG helper]
     * @return a string representation that looks better on console print
     */
    public abstract String toString(String indent);
}
