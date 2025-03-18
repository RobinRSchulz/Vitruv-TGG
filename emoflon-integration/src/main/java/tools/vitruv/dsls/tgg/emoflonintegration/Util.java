package tools.vitruv.dsls.tgg.emoflonintegration;

import language.*;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.emoflon.ibex.common.operational.IMatch;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;
import runtime.CorrespondenceNode;
import runtime.Protocol;
import runtime.TGGRuleApplication;
import tools.vitruv.change.atomic.AdditiveEChange;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.SubtractiveEChange;
import tools.vitruv.change.atomic.eobject.CreateEObject;
import tools.vitruv.change.atomic.eobject.DeleteEObject;
import tools.vitruv.change.atomic.feature.UnsetFeature;
import tools.vitruv.change.atomic.feature.attribute.InsertEAttributeValue;
import tools.vitruv.change.atomic.feature.attribute.RemoveEAttributeValue;
import tools.vitruv.change.atomic.feature.attribute.ReplaceSingleValuedEAttribute;
import tools.vitruv.change.atomic.feature.reference.InsertEReference;
import tools.vitruv.change.atomic.feature.reference.RemoveEReference;
import tools.vitruv.change.atomic.feature.reference.ReplaceSingleValuedEReference;
import tools.vitruv.change.atomic.feature.single.ReplaceSingleValuedFeatureEChange;
import tools.vitruv.change.atomic.root.InsertRootEObject;
import tools.vitruv.change.atomic.root.RemoveRootEObject;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Util {

    private static final Logger logger = Logger.getLogger(Util.class);

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

    public static boolean isCreatingOrAdditiveEChange(EChange<EObject> eChange) {
        return switch (eChange) {
            case CreateEObject<EObject> createEObject -> true;
            case DeleteEObject<EObject> deleteEObject -> false;
            case ReplaceSingleValuedFeatureEChange<EObject, ?, ?> replaceSingleValuedFeatureEChange -> false;
            case AdditiveEChange<EObject, ?> additiveEChange-> true;
            case SubtractiveEChange<EObject, ?> subtractiveEChange -> false;
            case UnsetFeature<EObject, ?> unsetFeature -> false;
            case EChange<EObject> eChange1 -> throw new IllegalStateException("Inconcrete eChange: " + eChange1);
        };
    }

    public static String getMarkerStyleName(TGGRuleNode tggRuleNode) {
        return tggRuleNode.getBindingType().getName() + "__" + tggRuleNode.getDomainType() + "__" + tggRuleNode.getName();
    }

    public static String modelResourceToString(Resource resource) {
        String str = "";
        for (TreeIterator<EObject> it = resource.getAllContents(); it.hasNext(); ) {
            EObject eObject = it.next();

            str += "  - [" + eObjectToString(eObject) + "] contents: " + eObject.eClass().getEAllReferences().stream().map(eObject::eGet)
                    .map(Util::eSomethingToString)
                    .collect(Collectors.joining(", ")) + ",\n";
        }
        return str;
    }

    public static String resourceSetToString(ResourceSet resourceSet) {
        return "ResourceSet:" + Integer.toHexString(resourceSet.hashCode());
    }

    public static String correspondenceNodeToString(CorrespondenceNode correspondenceNode) {
        return "[CorrespondenceNode " + correspondenceNode.eClass().getName() +  "] SRC=" + Util.eSomethingToString(correspondenceNode.eGet(correspondenceNode.eClass().getEStructuralFeature("source")))
                + ", TRG=" + Util.eSomethingToString(correspondenceNode.eGet(correspondenceNode.eClass().getEStructuralFeature("target")));
    }

    public static String eSomethingToString(Object object) {
        return switch (object) {
            case EObject eObject -> eObjectToString(eObject);
            case EObjectContainmentEList eObjectContainmentEList -> eObjectContainmentEList.getEStructuralFeature().getName() + ":["
                    + eObjectContainmentEList.stream().map(Util::eSomethingToString).collect(Collectors.joining(", "))+ "]";
            case null -> "null";
            default -> object.toString();
        };
    }

    public static String iMatchToVerboseString(IMatch iMatch) {
            return iMatch.toString() + ", params: \n  -" + iMatch.getParameterNames().stream()
                    .map(paramName -> paramName + ": " + Util.eSomethingToString(iMatch.get(paramName)))
                    .collect(Collectors.joining("\n  -"));
    }

    public static String eObjectToString(Object object) {
        EObject eObject = (EObject) object;
        return eObject.eClass().getName() + ":" + Integer.toHexString(eObject.hashCode());
    }

    public static String tGGRuleNodeToString(TGGRuleNode tggRuleNode) {
        return "TGGRuleNode[" + tggRuleNode.getClass().getSimpleName() + "]: " + tggRuleNode.getName() + ", type="
                + tggRuleNode.getType().getName() + ", bind=" + tggRuleNode.getBindingType() + ", dom=" + tggRuleNode.getDomainType();
    }

    public static String tGGRuleEdgeToString(TGGRuleEdge tggRuleEdge) {
        return "TGGRuleEdge[" + tggRuleEdge.getSrcNode().getName() + "-->" + tggRuleEdge.getTrgNode().getName() + "]: "
                + tggRuleEdge.getName() + ", type=" + tggRuleEdge.getType().getName() + ", bind=" + tggRuleEdge.getBindingType() + ", dom=" + tggRuleEdge.getDomainType();

    }

    public static Optional<Set<TGGRuleApplication>> getProtocolSteps(TGGResourceHandler resourceHandler) {
        return Optional.of(resourceHandler.getProtocolResource().getContents().stream()
                .map(protocolEObject -> (TGGRuleApplication) protocolEObject)
                .collect(Collectors.toSet()));
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
