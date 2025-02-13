package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.vitruv.change.atomic.EChange;

/**
 * Represents an EChange that affects an object which is also affected by other EChanges.
 * This object is represented by the EObjectPlaceholder.
 *
 */
public class EChangeWrapper {

    private EChange eChange;
    // todo nicht Eclass sonder Java-Klasse? oder ID
    private Class eChangeType;
    /** Refers to the metamodel's Eclass (running example: System, Protocol, ...).
     *  EChangeWrappers can only be applied to EChanges that have objects of that eClass as their affectedEObject  */
    private EClass affectedObjectEclass;
    private Placeholder placeholder;


    /**
     *
     * @param eChangeType this template can only be matched against eChanges of this type.
     * @param affectedObjectEclass this template can only be matched against eChanges whose affectedElements are instances of that Eclass
     * @param placeholder a placeholder to be able to hold an actual mapping of the change.
     */
    public EChangeWrapper(Class eChangeType, EClass affectedObjectEclass, Placeholder placeholder) {
        this.eChangeType = eChangeType;
        this.affectedObjectEclass = affectedObjectEclass;
        this.placeholder = placeholder;
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

    public EClass getAffectedObjectEclass() {
        return affectedObjectEclass;
    }

    public Placeholder getPlaceholder() {
        return placeholder;
    }

    //TODO need a method matches(EChange echange) that matches against eChangeType and referenceType

    /**
     *
     * @param eChange
     * @return whether
     * * the eChange's class matches this Template's eChange type
     * * the eChange's affected EObject matches
     */
    public boolean matches(EChange eChange) {
        /*
            TODO hier muss man ganz viel instance checking machen, allein nur um das affectedEObject zu bekommen (da EChange das nicht in seiner Signatur hat, obwohl ALLE konkreten Subklassen das nutzen...)
            TODO vermutlich will man das nicht hier haben, sonst muss man die Methode in den Subklassen nachimplementieren...
         */
        return this.getAffectedObjectEclass().equals(eChangeType.cast(eChange));
    }
}
