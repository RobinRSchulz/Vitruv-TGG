package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EObject;

/**
 * Enables referencing the affected EObjects in multiple EChangeWrappers before it is present.
 * This way, the pattern structure can be retained when multiple EChanges should be grouped together and share same entites.
 * E.g. a CreateEObject and a InsertRootEObject EChange should reference the same EObject.
 * Before matching the PatternTemplate to the actual Change sequence, this EObject cannot be known.
 *
 * Subclasses of this Placeholder represent different sets of parameters that EChanges can have.
 * We chose to not make a bijective mapping of all EChange subclasses to Placeholder classes but instead group classes with the same set of parameters/ EObjects.
 *
 * @param <E> EObject Type of the affected EObject each placeholder can hold. Subclasses introduce further types.
 */
public abstract class Placeholder {

    public abstract boolean isInitialized();

    public abstract EObject getAffectedEObject();
}