package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EClass;
import tools.vitruv.change.atomic.EChange;

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
