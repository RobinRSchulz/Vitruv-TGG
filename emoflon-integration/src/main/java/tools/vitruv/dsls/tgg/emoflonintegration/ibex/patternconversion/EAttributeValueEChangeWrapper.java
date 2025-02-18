package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.attribute.InsertEAttributeValue;
import tools.vitruv.change.atomic.feature.attribute.RemoveEAttributeValue;
import tools.vitruv.change.atomic.feature.attribute.ReplaceSingleValuedEAttribute;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternmatching.Util;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Wraps EChanges, that represent a modification of an EAttribute of an EObject with a Value.
 *
 * Applicable to the following types of EChange:
 * <li> ${@link tools.vitruv.change.atomic.feature.attribute.InsertEAttributeValue}
 * <li> ${@link tools.vitruv.change.atomic.feature.attribute.RemoveEAttributeValue}
 */
public class EAttributeValueEChangeWrapper extends EChangeWrapper {

    private final EAttribute affectedEAttribute;
    private EObjectPlaceholder valuePlaceholder;

    /**
     * We use Eclasses instead of Classes where there is no difference because
     * * we stay in the "ecore-world"
     * * no instanceof, which some don't like
     * * maybe performance in switch-Statements?
     *
     * @param eChangeType this template can only be matched against eChanges of this type.
     * @param affectedElementEClass this template can only be matched against eChanges whose affectedElements are instances of that Eclass
     * @param affectedElementPlaceholder a placeholder to be able to hold an actual mapping of the change.
     * @param affectedEAttribute this template can only be matched against eChanges whose affected eAttribute matches that EAttribute.
     * @param valuePlaceholder a placeholder for the value that is modified by an actual eChange.
     */
    public EAttributeValueEChangeWrapper(EClass eChangeType, EClass affectedElementEClass, EObjectPlaceholder affectedElementPlaceholder, EAttribute affectedEAttribute, EObjectPlaceholder valuePlaceholder) {
        super(eChangeType, affectedElementEClass, affectedElementPlaceholder);
        this.affectedEAttribute = affectedEAttribute;
        this.valuePlaceholder = valuePlaceholder;
    }

    public EAttribute getAffectedEAttribute() {
        return affectedEAttribute;
    }

    public EObjectPlaceholder getValuePlaceholder() {
        return valuePlaceholder;
    }
    //todo add isInitialized method in superclass which this overrides

    @Override
    protected boolean extendedDataMatches(EChange<EObject> eChange, VitruviusChange<EObject> vitruviusChange) {
        switch (eChange) {
            case InsertEAttributeValue<EObject, EObject> insertEAttributeValue: return insertEAttributeValue.getAffectedFeature().equals(affectedEAttribute);
            case RemoveEAttributeValue removeEAttributeValue: return removeEAttributeValue.getAffectedFeature().equals(affectedEAttribute);
            default: return false;
        }
    }

    @Override
    public void initializeImpl(EChange<EObject> eChange, VitruviusChange<EObject> vitruviusChange) {
        this.setEChange(eChange);
        this.getAffectedElementPlaceholder().initialize(Util.getAffectedEObjectFromEChange(eChange, vitruviusChange));
        switch (eChange) {
            case InsertEAttributeValue<EObject, EObject> insertEAttributeValue:
                this.valuePlaceholder.initialize(insertEAttributeValue.getNewValue());
                break;
            case RemoveEAttributeValue<EObject, EObject> removeEAttributeValue:
                this.valuePlaceholder.initialize(removeEAttributeValue.getOldValue());
                break;
            default: throw new IllegalStateException("Unexpected eChange: " + eChange);
        }
    }

    /**
     * [COPY helper]
     * @return a copy of this EChangeWrapper that has the identical Placeholder as this eChangeWrapper.
     */
    @Override
    protected EChangeWrapper shallowCopy() {
        EChangeWrapper copy = new EAttributeValueEChangeWrapper(this.getEChangeType(), this.getAffectedElementEClass(), this.getAffectedElementPlaceholder(),
                this.affectedEAttribute, this.valuePlaceholder);
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
        retSet.add(this.getAffectedElementPlaceholder());
        retSet.add(this.valuePlaceholder);
        return retSet;
    }
    /**
     * [COPY helper]
     * Replace all placeholders with their new objects from the map
     */
    @Override
    protected void replaceAllPlaceholders(Map<EObjectPlaceholder, EObjectPlaceholder> oldToNewPlaceholders) {
        if (!oldToNewPlaceholders.containsKey(this.getAffectedElementPlaceholder())) {
            throw new IllegalStateException("oldToNewPlaceholders does not contain " + this.getAffectedElementPlaceholder());
        }
        if (!oldToNewPlaceholders.containsKey(this.valuePlaceholder)) {
            throw new IllegalStateException("oldToNewPlaceholders does not contain " + valuePlaceholder);
        }

        this.setAffectedElementPlaceholder(oldToNewPlaceholders.get(this.getAffectedElementPlaceholder()));
        this.valuePlaceholder = oldToNewPlaceholders.get(this.valuePlaceholder);
    }
    @Override
    public String toString() {
        return "[EAttributeValueEChangeWrapper of " + getEChangeType().getName() + ". affectedElement Type: " + getAffectedElementEClass().getName() + ", attribute: " + affectedEAttribute.getName() + "] " +
                "holding: AE: " + getAffectedElementPlaceholder() + ", V: " + getValuePlaceholder();
    }
}
