package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.EObjectPlaceholder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Wraps EChanges that affect only one element (and its model), e.g. existence changing or root setting
 * <br/>
 * This wrapper is applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.eobject.CreateEObject}
 * <li> ${@link tools.vitruv.change.atomic.eobject.DeleteEObject}
 * <li> ${@link tools.vitruv.change.atomic.root.InsertRootEObject}
 * <li> ${@link tools.vitruv.change.atomic.root.RemoveRootEObject}
 * <li> ${@link tools.vitruv.change.atomic.feature.UnsetFeature}
 */
public class PlainEChangeWrapper extends EChangeWrapper {

    /**
     *
     * @param eChangeType                this template can only be matched against eChanges of this type.
     * @param affectedElementEClass      this template can only be matched against eChanges whose affectedElements are instances of that Eclass
     * @param affectedElementPlaceholder a affectedElementPlaceholder to be able to hold an actual mapping of the change.
     */
    public PlainEChangeWrapper(EClass eChangeType, EClass affectedElementEClass, EObjectPlaceholder affectedElementPlaceholder) {
        super(eChangeType, affectedElementEClass, affectedElementPlaceholder);
    }

    @Override
    protected boolean extendedDataMatches(EChange<EObject> eChange) {
        return true; // no extended data here.
    }

    @Override
    public void initializeExtension(EChange<EObject> eChange) {
        // nothing more to do here.
    }

    @Override
    protected EChangeWrapper shallowCopy() {
        PlainEChangeWrapper copy = new PlainEChangeWrapper(this.getEChangeType(), this.getAffectedElementEClass(), this.getAffectedElementPlaceholder());
        copy.setOriginal(this);
        return copy;
    }

    @Override
    protected Set<EObjectPlaceholder> getAllPlaceholders() {
        Set<EObjectPlaceholder> retSet = new HashSet<>();
        retSet.add(getAffectedElementPlaceholder());
        return retSet;
    }

    @Override
    protected void replaceAllPlaceholders(Map<EObjectPlaceholder, EObjectPlaceholder> oldToNewPlaceholders) {
        if (!oldToNewPlaceholders.containsKey(getAffectedElementPlaceholder())) {
            throw new IllegalStateException("oldToNewPlaceholders does not contain " + getAffectedElementPlaceholder());
        }

        this.setAffectedElementPlaceholder(oldToNewPlaceholders.get(getAffectedElementPlaceholder()));
    }

    @Override
    public String toString() {
        return "[PlainEChangeWrapper of " + getEChangeType().getName() + ". AE-type: " + getAffectedElementEClass().getName() + "] holding: " + getAffectedElementPlaceholder()
                + (this.isInitialized() ? ", initialized with " + Util.eChangeToString(getEChange()) : ", uninitialized");
    }

    @Override
    public String toString(String indent) {
        return "[PlainEChangeWrapper of " + getEChangeType().getName() + ". AE-type: " + getAffectedElementEClass().getName() + "] holding: \n" + indent + "AE: " + getAffectedElementPlaceholder()
                + (this.isInitialized() ? ", initialized with " + Util.eChangeToString(getEChange()) : ", uninitialized");
    }
}
