package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternmatching.Util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * This wrapper is applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.eobject.CreateEObject}
 * <li> ${@link tools.vitruv.change.atomic.eobject.DeleteEObject}
 * <li> ${@link tools.vitruv.change.atomic.root.InsertRootEObject}
 * <li> ${@link tools.vitruv.change.atomic.root.RemoveRootEObject}
 * <li> ${@link tools.vitruv.change.atomic.feature.UnsetFeature}
 */
public class PlainEChangeWrapper extends EChangeWrapper {
    /**
     * We use Eclasses instead of Classes where there is no difference because
     * * we stay in the "ecore-world"
     * * no instanceof, which some don't like
     * * maybe performance in switch-Statements?
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

    /**
     * [COPY helper]
     * @return a copy of this EChangeWrapper that has the identical Placeholder as this eChangeWrapper.
     */
    @Override
    protected EChangeWrapper shallowCopy() {
        PlainEChangeWrapper copy = new PlainEChangeWrapper(this.getEChangeType(), this.getAffectedElementEClass(), this.getAffectedElementPlaceholder());
        copy.setParent(this);
        return copy;
    }
    /**
     * [COPY helper]
     * @return all placeholders this EChangeWrapper holds
     */
    @Override
    protected Set<EObjectPlaceholder> getAllPlaceholders() {
        Set<EObjectPlaceholder> retSet = new HashSet<>();
        retSet.add(getAffectedElementPlaceholder());
        return retSet;
    }

    /**
     * [COPY helper]
     * Replace all placeholders with their new objects from the map
     */
    @Override
    protected void replaceAllPlaceholders(Map<EObjectPlaceholder, EObjectPlaceholder> oldToNewPlaceholders) {
        if (!oldToNewPlaceholders.containsKey(getAffectedElementPlaceholder())) {
            throw new IllegalStateException("oldToNewPlaceholders does not contain " + getAffectedElementPlaceholder());
        }

        this.setAffectedElementPlaceholder(oldToNewPlaceholders.get(getAffectedElementPlaceholder()));
    }

    @Override
    public String toString() {
        return "[PlainEChangeWrapper of " + getEChangeType().getName() + ". affectedElement Type: " + getAffectedElementEClass().getName() + "] holding: " + getAffectedElementPlaceholder();
    }
}
