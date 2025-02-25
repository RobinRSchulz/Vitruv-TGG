package tools.vitruv.dsls.tgg.emoflonintegration;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.CreateEObject;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.change.atomic.feature.UnsetFeature;
import tools.vitruv.change.atomic.feature.attribute.InsertEAttributeValue;
import tools.vitruv.change.atomic.feature.attribute.RemoveEAttributeValue;
import tools.vitruv.change.atomic.feature.attribute.ReplaceSingleValuedEAttribute;
import tools.vitruv.change.atomic.feature.reference.InsertEReference;
import tools.vitruv.change.atomic.feature.reference.RemoveEReference;
import tools.vitruv.change.atomic.feature.reference.ReplaceSingleValuedEReference;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.atomic.root.RemoveRootEObject;
import tools.vitruv.change.composite.description.VitruviusChange;

public class Util {

    private Util() { }

    /**
     *
     * @return the affectedEObject, that an EChange almost always has, but is somehow missing from the root class...
     */
    public static EObject getAffectedEObjectFromEChange(EChange<EObject> eChange) {
        switch (eChange) {
            case CreateEObject<EObject> createEObject: return (EObject) createEObject.getAffectedElement();
            case DeleteEObject<EObject> deleteEObject: return (EObject) deleteEObject.getAffectedElement();
              // staging area crap...  we need to give the whole vitruviuschange here and trace back to the last CreateEObject dominating this InsertRootEObject EChange !
            case InsertRootEObject<EObject> insertRootEObject: return insertRootEObject.getNewValue();
            case RemoveRootEObject<EObject> removeRootEObject: return (EObject) removeRootEObject.getOldValue() ;
            case UnsetFeature unsetFeature: return (EObject) unsetFeature.getAffectedElement();
            case InsertEAttributeValue insertEAttributeValue: return (EObject) insertEAttributeValue.getAffectedElement();
            case RemoveEAttributeValue removeEAttributeValue: return (EObject) removeEAttributeValue.getAffectedElement();
            case ReplaceSingleValuedEAttribute replaceSingleValuedEAttribute: return (EObject) replaceSingleValuedEAttribute.getAffectedElement();
            case InsertEReference insertEReference: return (EObject) insertEReference.getAffectedElement();
            case RemoveEReference removeEReference: return (EObject) removeEReference.getAffectedElement();
            case ReplaceSingleValuedEReference replaceSingleValuedEReference: return (EObject) replaceSingleValuedEReference.getAffectedElement();
            case EChange<EObject> eChange1: throw new IllegalStateException("Inconcrete eChange: " + eChange1);
        }
    }

    /**
     * Workaround for the staging area stuff. An InsertRootEObject change does not hold its affected Element but takes it from the dominating CreateEObject change.
     * todo this is not needed, we can use ::getNewValue. Check nonetheless for edge cases...
     * @param eChange
     * @param vitruviusChange
     * @return the affectedElement of the CreateEObject which dominates the eChange in the vitruviusChange
     */
    public static EObject getAffectedEObjectForInsertRootEobject(InsertRootEObject<EObject> eChange, VitruviusChange<EObject> vitruviusChange) {
        int indexOfEChange = vitruviusChange.getEChanges().indexOf(eChange);
        int i = indexOfEChange-1;
        EObject affectedEObject = null;
        while (i >= 0 && affectedEObject == null) {
            EChange createEObjectCandidate = vitruviusChange.getEChanges().get(i);
            if (createEObjectCandidate instanceof CreateEObject) {
                // found
                affectedEObject = ((CreateEObject<EObject>) createEObjectCandidate).getAffectedElement();
            }
            i--;
        }
        if (affectedEObject == null) {
            throw new IllegalStateException("InsertRootEObject without preceding CreateEObject! eChange: ");
        }
        return affectedEObject;
    }

    public static String eChangeToString(EChange<EObject> eChange) {
        switch (eChange) {
            case CreateEObject createEObject: return "[CreateEObject] AE=" + eObjectToString(createEObject.getAffectedElement());
            case DeleteEObject deleteEObject: return "[DeleteEObject] AE=" + eObjectToString(deleteEObject.getAffectedElement());
            case InsertRootEObject insertRootEObject: return "[InsertRootEObject] AE=" + eObjectToString(insertRootEObject.getNewValue());
            case RemoveRootEObject removeRootEObject: return "[RemoveRootEObject] AE=" + eObjectToString(removeRootEObject.getOldValue());
            case UnsetFeature unsetFeature: return "[UnsetFeature] AE=" + eObjectToString(unsetFeature.getAffectedElement());
            case InsertEAttributeValue insertEAttributeValue: return "[InsertEAttributeValue] AE=" + eObjectToString(insertEAttributeValue.getAffectedElement());
            case RemoveEAttributeValue removeEAttributeValue: return "[RemoveEAttributeValue] AE=" + eObjectToString(removeEAttributeValue.getAffectedElement());
            case ReplaceSingleValuedEAttribute replaceSingleValuedEAttribute: return "[ReplaceSingleValuedEAttribute] AE=" + eObjectToString(replaceSingleValuedEAttribute.getAffectedElement());
            case InsertEReference insertEReference: return "[InsertEReference] AE=" + eObjectToString(insertEReference.getAffectedElement());
            case RemoveEReference removeEReference: return "[RemoveEReference] AE=" + eObjectToString(removeEReference.getAffectedElement());
            case ReplaceSingleValuedEReference replaceSingleValuedEReference: return "[ReplaceSingleValuedEReference] AE=" + eObjectToString(replaceSingleValuedEReference.getAffectedElement());
            case EChange<EObject> eChange1: return "[EChange] " + eChange1;
        }
    }


    public static String eObjectToString(Object object) {
        EObject eObject = (EObject) object;
        return eObject.eClass().getName() + ":" + eObject.hashCode();
    }

    public static String resourceToString(Resource resource) {
        return resource.getURI().toString();
    }

    public static boolean isManyValued(EReference eReference) {
        return eReference.getUpperBound() == -1;
    }
}
