package tools.vitruv.dsls.tgg.emoflonintegration.patternmatching;

import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.TypeInferringAtomicEChangeFactory;
import tools.vitruv.change.atomic.feature.attribute.InsertEAttributeValue;
import tools.vitruv.change.atomic.feature.attribute.ReplaceSingleValuedEAttribute;
import tools.vitruv.change.atomic.feature.reference.InsertEReference;
import tools.vitruv.change.atomic.feature.reference.RemoveEReference;
import tools.vitruv.change.atomic.feature.reference.ReplaceSingleValuedEReference;
import tools.vitruv.change.atomic.feature.single.ReplaceSingleValuedFeatureEChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.composite.description.VitruviusChangeFactory;

import java.util.*;

/**
 * Transforms a {@link VitruviusChange} to
 */
public class VitruviusChangeTransformer {

    private final VitruviusChange<EObject> vitruviusChange;

    public VitruviusChangeTransformer(VitruviusChange<EObject> vitruviusChange) {
        this.vitruviusChange = vitruviusChange;
    }


    public VitruviusChange<EObject> mocktransformDELETEME() {
        return vitruviusChange;
    }

    public VitruviusChange<EObject> transform() {
//        if ((1+1 == 2) == true) {
//            return mocktransformDELETEME();
//        }
        assert new HashSet<>(this.vitruviusChange.getEChanges()).size() == this.vitruviusChange.getEChanges().size();
//        VitruviusChange<EObject> newChange = this.vitruviusChange.copy();

        List<EChange<EObject>> newEChanges = new LinkedList<>();

        // create new eChange list, replacing ReplaceSingleValuedFeatureEChange with its respective Additive and Subtracitve elements
        for (EChange<EObject> eChange : vitruviusChange.getEChanges()) {
            if (eChange instanceof ReplaceSingleValuedFeatureEChange<EObject, ?, ?> replaceSingleValuedFeatureEChange) {
                for (EChange<EObject> transformedEChange: transformReplaceSingeValuedFeatureEChange(replaceSingleValuedFeatureEChange)) {
                    newEChanges.add(transformedEChange);
                }
            } else newEChanges.add(eChange);
        }
        return VitruviusChangeFactory.getInstance().createTransactionalChange(newEChanges);
    }

    /**
     * Transform {@link ReplaceSingleValuedFeatureEChange} into a subtractive (e.g. {@link RemoveEReference}) and an additive (e.g. {@link InsertEReference})change.
     * Problem: Changes the semantics of these changes, because they are described as representing many valued features...
     */
    public Collection<EChange> transformReplaceSingeValuedFeatureEChange(ReplaceSingleValuedFeatureEChange<?,?,?> replaceSingleValuedFeatureEChange) {
        if (replaceSingleValuedFeatureEChange instanceof ReplaceSingleValuedEAttribute<?,?> replaceSingleValuedEAttribute) {
            InsertEAttributeValue additiveChangePart = TypeInferringAtomicEChangeFactory.getInstance().createInsertAttributeChange(
                    replaceSingleValuedEAttribute.getAffectedElement(),
                    replaceSingleValuedEAttribute.getAffectedFeature(),
                    -1,
                    replaceSingleValuedEAttribute.getNewValue());
            if (replaceSingleValuedEAttribute.getNewValue().equals(replaceSingleValuedEAttribute.getOldValue())
                    || replaceSingleValuedEAttribute.getOldValue() == null) {
                // The change represents setting the value without replacing the previous (because there wasn't any.)
                return List.of(additiveChangePart);
            }
            return List.of(
                    TypeInferringAtomicEChangeFactory.getInstance().createRemoveAttributeChange(
                            replaceSingleValuedEAttribute.getAffectedElement(),
                            replaceSingleValuedEAttribute.getAffectedFeature(),
                            -1,
                            replaceSingleValuedEAttribute.getOldValue()
                    ),
                    additiveChangePart);

        } else if (replaceSingleValuedFeatureEChange instanceof ReplaceSingleValuedEReference<?> replaceSingleValuedEReference) {
            InsertEReference additiveChangePart = TypeInferringAtomicEChangeFactory.getInstance().createInsertReferenceChange(
                    replaceSingleValuedEReference.getAffectedElement(),
                    replaceSingleValuedEReference.getAffectedFeature(),
                    replaceSingleValuedEReference.getNewValue(),
                    -1
            );
            if (replaceSingleValuedEReference.getNewValue().equals(replaceSingleValuedEReference.getOldValue())
                    || replaceSingleValuedEReference.getOldValue() == null) {
                // The change represents setting the value without replacing the previous (because there wasn't any.)
                return List.of(additiveChangePart);
            }
            return List.of(
                    TypeInferringAtomicEChangeFactory.getInstance().createRemoveReferenceChange(
                            replaceSingleValuedEReference.getAffectedElement(),
                            replaceSingleValuedEReference.getAffectedFeature(),
                            replaceSingleValuedEReference.getOldValue(),
                            -1),
                    additiveChangePart);
        }
        else throw new IllegalStateException("replaceSingleValuedFeatureEChange is of an unknown third concrete class: " + replaceSingleValuedFeatureEChange.getClass());
    }
}
