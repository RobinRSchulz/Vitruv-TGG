package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EClass;
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
 *
 * This wrapper is applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.eobject.CreateEObject}
 * <li> ${@link tools.vitruv.change.atomic.eobject.DeleteEObject}
 * <li> ${@link tools.vitruv.change.atomic.root.InsertRootEObject}
 * <li> ${@link tools.vitruv.change.atomic.root.RemoveRootEObject}
 * <li> ${@link tools.vitruv.change.atomic.feature.UnsetFeature}
 */
public class EChangeWrapper {

    private EClass eChangeType;
    private EChange eChange;

    /** Refers to the metamodel's EClass (running example: System, Protocol, ...).
     *  EChangeWrappers can only be applied to EChanges that have objects of that eClass as their affectedEObject  */
    private EClass affectedElementEClass;
    private EObjectPlaceholder affectedElementPlaceholder;


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

    /**
     * [COPY helper]
     * @return a copy of this EChangeWrapper that has the identical Placeholder as this eChangeWrapper.
     * TODO need to override this in every subclass!
     */
    protected EChangeWrapper shallowCopy() {
        // don't know if very beautiful but better than nothing...
        if (!this.getClass().getName().equals(EChangeWrapper.class.getName())) {
            throw new IllegalStateException("This Subclass of " + EChangeWrapper.class.getName() + " does not override shallowCopy! Implement this method in your subclass!");
        }
        return new EChangeWrapper(eChangeType, affectedElementEClass, affectedElementPlaceholder);
    }
    /**
     * [COPY helper]
     * @return all placeholders this EChangeWrapper holds
     * TODO need to override this in every subclass!
     */
    protected Set<EObjectPlaceholder> getAllPlaceholders() {
        // don't know if very beautiful but better than nothing...
        if (!this.getClass().getName().equals(EChangeWrapper.class.getName())) {
            throw new IllegalStateException("This Subclass of " + EChangeWrapper.class.getName() + " does not override getAllPlaceholders! Implement this method in your subclass!");
        }

        Set<EObjectPlaceholder> retSet = new HashSet<>();
        retSet.add(affectedElementPlaceholder);
        return retSet;
    }
    /**
     * [COPY helper]
     * Replace all placeholders with their new objects from the map
     * TODO need to override this in every subclass!
     */
    protected void replaceAllPlaceholders(Map<EObjectPlaceholder, EObjectPlaceholder> oldToNewPlaceholders) {
        // don't know if very beautiful but better than nothing...
        if (!this.getClass().getName().equals(EChangeWrapper.class.getName())) {
            throw new IllegalStateException("This Subclass of " + EChangeWrapper.class.getName() + " does not override replaceAllPlaceholders! Implement this method in your subclass!");
        }

        if (!oldToNewPlaceholders.containsKey(affectedElementPlaceholder)) {
            throw new IllegalStateException("oldToNewPlaceholders does not contain " + affectedElementPlaceholder);
        }

        this.affectedElementPlaceholder = oldToNewPlaceholders.get(affectedElementPlaceholder);
    }

    @Override
    public String toString() {
        return "[EChangeWrapper of " + getEChangeType().getName() + ". affectedElement Type: " + getAffectedElementEClass().getName() + "] holding: " + getAffectedElementPlaceholder();
    }

    //TODO need a method matches(EChange echange) that matches against eChangeType and referenceType

//    /**
//     *
//     * @param eChange
//     * @return whether
//     * * the eChange's class matches this Template's eChange type
//     * * the eChange's affected EObject matches
//     */
//    public boolean matches(EChange eChange) {
//        /*
//            TODO hier muss man ganz viel instance checking machen, allein nur um das affectedEObject zu bekommen (da EChange das nicht in seiner Signatur hat, obwohl ALLE konkreten Subklassen das nutzen...)
//            TODO vermutlich will man das nicht hier haben, sonst muss man die Methode in den Subklassen nachimplementieren...
//         */
//        return this.getAffectedElementEClass().equals(eChangeType.cast(eChange));
//    }
}
