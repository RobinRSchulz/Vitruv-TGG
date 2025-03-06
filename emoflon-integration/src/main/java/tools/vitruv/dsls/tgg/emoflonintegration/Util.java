package tools.vitruv.dsls.tgg.emoflonintegration;

import language.*;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
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

import java.util.Collection;

public class Util {

    private Util() { }

    /**
     *
     * @return the affectedEObject, that an EChange almost always has, but is somehow missing from the root class...
     */
    public static EObject getAffectedEObjectFromEChange(EChange<EObject> eChange) {
        return switch (eChange) {
            case CreateEObject<EObject> createEObject -> createEObject.getAffectedElement();
            case DeleteEObject<EObject> deleteEObject -> deleteEObject.getAffectedElement();
            case InsertRootEObject<EObject> insertRootEObject -> insertRootEObject.getNewValue();
            case RemoveRootEObject<EObject> removeRootEObject -> removeRootEObject.getOldValue();
            case UnsetFeature<EObject, ?> unsetFeature -> unsetFeature.getAffectedElement();
            case InsertEAttributeValue<EObject, ?> insertEAttributeValue -> insertEAttributeValue.getAffectedElement();
            case RemoveEAttributeValue<EObject, ?> removeEAttributeValue -> removeEAttributeValue.getAffectedElement();
            case ReplaceSingleValuedEAttribute<EObject, ?> replaceSingleValuedEAttribute -> replaceSingleValuedEAttribute.getAffectedElement();
            case InsertEReference<EObject> insertEReference -> insertEReference.getAffectedElement();
            case RemoveEReference<EObject> removeEReference -> removeEReference.getAffectedElement();
            case ReplaceSingleValuedEReference<EObject> replaceSingleValuedEReference -> replaceSingleValuedEReference.getAffectedElement();
            case EChange<EObject> eChange1 -> throw new IllegalStateException("Inconcrete eChange: " + eChange1);
        };
    }

    public static String eChangeToString(EChange<EObject> eChange) {
        return switch (eChange) {
            case CreateEObject<EObject> createEObject -> "[CreateEObject] AE=" + eObjectToString(createEObject.getAffectedElement());
            case DeleteEObject<EObject> deleteEObject -> "[DeleteEObject] AE=" + eObjectToString(deleteEObject.getAffectedElement());
            case InsertRootEObject<EObject> insertRootEObject -> "[InsertRootEObject] AE=" + eObjectToString(insertRootEObject.getNewValue());
            case RemoveRootEObject<EObject> removeRootEObject -> "[RemoveRootEObject] AE=" + eObjectToString(removeRootEObject.getOldValue());
            case UnsetFeature<EObject, ?> unsetFeature -> "[UnsetFeature] AE=" + eObjectToString(unsetFeature.getAffectedElement());
            case InsertEAttributeValue<EObject, ?> insertEAttributeValue -> "[InsertEAttributeValue] AE=" + eObjectToString(insertEAttributeValue.getAffectedElement());
            case RemoveEAttributeValue<EObject, ?> removeEAttributeValue -> "[RemoveEAttributeValue] AE=" + eObjectToString(removeEAttributeValue.getAffectedElement());
            case ReplaceSingleValuedEAttribute<EObject, ?> replaceSingleValuedEAttribute -> "[ReplaceSingleValuedEAttribute] AE=" + eObjectToString(replaceSingleValuedEAttribute.getAffectedElement());
            case InsertEReference<EObject> insertEReference -> "[InsertEReference] AE=" + eObjectToString(insertEReference.getAffectedElement());
            case RemoveEReference<EObject> removeEReference -> "[RemoveEReference] AE=" + eObjectToString(removeEReference.getAffectedElement());
            case ReplaceSingleValuedEReference<EObject> replaceSingleValuedEReference -> "[ReplaceSingleValuedEReference] AE=" + eObjectToString(replaceSingleValuedEReference.getAffectedElement());
            case EChange<EObject> eChange1 -> "[EChange] " + eChange1;
        };
    }

    public static String eObjectToString(Object object) {
        EObject eObject = (EObject) object;
        return eObject.eClass().getName() + ":" + eObject.hashCode();
    }

    public static boolean isManyValued(EReference eReference) {
        return eReference.getUpperBound() == -1;
    }
    public static  Collection<TGGRuleEdge> filterEdges(TGGRule tggRule, BindingType bindingType, DomainType domainType) {
        return tggRule.getEdges().stream().filter(tggRuleEdge -> tggRuleEdge.getBindingType().equals(bindingType) && tggRuleEdge.getDomainType().equals(domainType)).toList();
    }
    public static  Collection<TGGRuleNode> filterNodes(TGGRule tggRule, BindingType bindingType, DomainType domainType) {
        return tggRule.getNodes().stream().filter(tggRuleNode -> tggRuleNode.getBindingType().equals(bindingType) && tggRuleNode.getDomainType().equals(domainType)).toList();
    }
}
