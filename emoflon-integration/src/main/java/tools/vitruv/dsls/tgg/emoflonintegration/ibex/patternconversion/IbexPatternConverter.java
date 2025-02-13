package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import language.*;
import org.apache.log4j.Logger;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContext;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContextPattern;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXModel;
import tools.vitruv.change.atomic.feature.reference.InsertEReference;
import tools.vitruv.change.atomic.feature.reference.ReplaceSingleValuedEReference;

import java.util.Collection;
import java.util.LinkedList;

public class IbexPatternConverter {

    protected static final Logger logger = Logger.getRootLogger();

    private IBeXModel iBeXModel;
    private final TGG tgg;

    public IbexPatternConverter(IBeXModel iBeXModel, TGG tgg) {
        this.iBeXModel = iBeXModel;
        this.tgg = tgg;
    }

    public VitruviusChangeTemplateSet convert() {
        // TODO implement
        // TODO don't return a list but a smaaaart datastructure
        // todo ibexPatterns mit nem Debugger die Struktur rausfinden!
//        this.iBeXModel.getRuleSet().getRules().forEach(rule -> {
////            rule
//        });
        logger.info("Rules: ");
        this.tgg.getRules().forEach(rule -> {
            logger.info("  - " + rule.getName());
//            rule.getNodes().forEach(tggRuleNode -> logger.info("    - [" +tggRuleNode.getType() + "]: " + tggRuleNode.getName()));

//            filterEdges(rule, BindingType.CONTEXT, DomainType.SRC).forEach( edge -> logger.info("    - Context_src_edge: " + edgeToString(edge)));
            filterEdges(rule, BindingType.CREATE, DomainType.SRC).forEach( edge -> logger.info("    - Create_src_edge: " + edgeToString(edge)));
//            filterEdges(rule, BindingType.CONTEXT, DomainType.TRG).forEach( edge -> logger.info("    - Context_trg_edge: " + edge));
//            filterEdges(rule, BindingType.CREATE, DomainType.TRG).forEach( edge -> logger.info("    - Create_trg_edge: " + edge));
//            filterEdges(rule, BindingType.CONTEXT, DomainType.CORR).forEach( edge -> logger.info("    - Context_corr_edge: " + edgeToString(edge)));
//            filterEdges(rule, BindingType.CREATE, DomainType.CORR).forEach( edge -> logger.info("    - Create_corr_edge: " + edgeToString(edge)));

//            filterNodes(rule, BindingType.CONTEXT, DomainType.SRC).forEach( node -> logger.info("    - Context_src_node: " + node));
//            filterNodes(rule, BindingType.CREATE, DomainType.SRC).forEach( node -> logger.info("    - Create_src_node: " + node));
//            filterNodes(rule, BindingType.CREATE, DomainType.TRG).forEach( node -> logger.info("    - Create_trg_node: " + node));
//            filterNodes(rule, BindingType.CONTEXT, DomainType.TRG).forEach( node -> logger.info("    - Context_trg_node: " + node));
//            filterNodes(rule, BindingType.CONTEXT, DomainType.CORR).forEach( node -> logger.info("    - Context_corr_node: " + node));
//            filterNodes(rule, BindingType.CREATE, DomainType.CORR).forEach( node -> logger.info("    - Create_corr_node: " + node));
        });

//        this.iBeXModel.getPatternSet().getContextPatterns().forEach(contextPattern -> {
//            logger.info("ContextPattern: " + contextPattern.getName());
//            parseIBeXContext(contextPattern);
//        });
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Make here weiter!");
//        return null;
    }

    private IbexPatternTemplate parseRule(final TGGRule rule) {
        /*
        * Pseudocode:
        * for edge : rule.
        * */
        Collection<EChangeWrapper> eChangeWrappers = new LinkedList<>();
//        eChangeWrappers.addAll()
//        filterNodes(rule, BindingType.CONTEXT, DomainType.SRC).
        filterNodes(rule, BindingType.CONTEXT, DomainType.SRC).forEach(node -> this.parseContextNode(node, eChangeWrappers));

        return new IbexPatternTemplate(eChangeWrappers);
    }

    private void parseContextNode(TGGRuleNode ruleNode, Collection<EChangeWrapper> eChangeWrappers) {
        // recurse through context nodes and edges until a CREATE node or edge is found.
        assert ruleNode.getBindingType().equals(BindingType.CONTEXT);
        ruleNode.getOutgoingEdges().forEach(edge -> {
            if (edge.getBindingType().equals(BindingType.CONTEXT)) {
                assert edge.getTrgNode().getBindingType().equals(BindingType.CONTEXT);
                parseContextNode(edge.getTrgNode(), eChangeWrappers);
            } else if (edge.getBindingType().equals(BindingType.CREATE)) {
                parseCreateEdge(edge, eChangeWrappers);
            }
        });
    }

    private void parseCreateEdge(TGGRuleEdge ruleEdge, Collection<EChangeWrapper> eChangeWrappers) {
        /*
            Ibex treats every edge as an EReference, not an EAttribute.
            That is in accordance to ecore, where Attributes refer to EDataTypes and references refer to EClasses.
         */
        //TODO implement marking visited nodes (?)

//        ruleEdge.getType().eContainer().eClass().getEAllReferences();
//        ruleEdge.getType().eContainer().eClass().getEAllAttributes();
        //TODO we need to extract the following information:
        /*
            1. EChange Type
            2. AffectedEObject Type
            3. Reference name/type (?) (should it be system.protocols, System::links or System::components)?
               This is NOT the type of the element(s) in the reference but the type of the reference itself.
            4. Choice of a placeholder.
         */
        if (Util.isManyValued(ruleEdge.getType())) {
            eChangeWrappers.add(
                    new EReferenceEChangeWrapper(
                            InsertEReference.class,
                            ruleEdge.getSrcNode().getType(), //TODO müsste das sein, da affectedElement der InsertEReference der source-Knoten der betrachteten Kante ist.
                            new EObjectEStructuralFeatureValueIndexPlaceholder(),
                            ruleEdge.getType())
            );
        } else {
            // TODO check if this is correct (according to the change model, this is the only option...)
            eChangeWrappers.add(
                    new EReferenceEChangeWrapper(
                            ReplaceSingleValuedEReference.class,
                            ruleEdge.getSrcNode().getType(), //TODO müsste das sein, da affectedElement der InsertEReference der source-Knoten der betrachteten Kante ist.
                            new EObjectEStructuralFeatureTwoValuePlaceholder(),
                            ruleEdge.getType())
            );
        }
        // more cases? Think about delete later.
        /**
         * Depending on the ruleEdge Type (which is an EReference), we know, what kind of property we're dealing with.
         * Need to look at upper and lower bound for that? todo check ruleEdge.getType().getEType()
         * Isn't there some Util for that?
         */
        //TODO call TRG node but with existing placeholder!
    }

    private int parseIBeXContext(IBeXContext iBeXContext) {
        switch (iBeXContext) {
            case IBeXContextPattern iBeXContextPattern -> {
                logger.info("  - localNodes: " + iBeXContextPattern.getLocalNodes());
                logger.info("  - signatureNodes: ");
                iBeXContextPattern.getSignatureNodes().forEach(signatureNode -> logger.info( "    - " + signatureNode.getName() + ", type(Eclass)=" + signatureNode.getType().getName()));
                logger.info("  - localEdges: [Type:EReferenceType.Name, Type.name][SourceNodeType -> TargetNodeType]: edgeName: ");
                iBeXContextPattern.getLocalEdges().forEach(localEdge -> logger.info("    - " +
                        "[" + localEdge.getType().getEReferenceType().getName() + ", " + localEdge.getType().getName()  +"]: "
                        + "["+ localEdge.getSourceNode().getType().getName() + " -> " + localEdge.getTargetNode().getType().getName() + "]: " + localEdge.getName() ));
            }
            case IBeXContext iBeXContext1 -> {
                logger.info("ibexContext: "  + iBeXContext1 +", name=" + iBeXContext1.getName());
            }
        }
        return -1;
    }

    private Collection<TGGRuleEdge> filterEdges(TGGRule tggRule, BindingType bindingType, DomainType domainType) {
        //TODO nehm ich jetzt Patterns oder rules???? mit rules geht das filtern leichter... Patterns haben Protocol-Knoten & Kanten! SYNC-Matcher-Pingpong basiert auf Patterns (soweit ich weiß)
        return tggRule.getEdges().stream().filter(tggRuleEdge -> tggRuleEdge.getBindingType().equals(bindingType) && tggRuleEdge.getDomainType().equals(domainType)).toList();
    }
    private Collection<TGGRuleNode> filterNodes(TGGRule tggRule, BindingType bindingType, DomainType domainType) {
        return tggRule.getNodes().stream().filter(tggRuleNode -> tggRuleNode.getBindingType().equals(bindingType) && tggRuleNode.getDomainType().equals(domainType)).toList();
    }

    private String edgeToString(TGGRuleEdge edge) {
//        ((ReplaceSingleValuedEAttribute)edge).get

        return "TGGRuleEdge(" + edge.getName() + "): " +
                "type.EReferenceType()= " + edge.getType().getEReferenceType().getName() + ", " +
                "domainType=" + edge.getDomainType() + ", " +
                "bindingType=" + edge.getBindingType() + "\n                               " +
                "type.getName=" + edge.getType().getName() + ", " +
                "type.EReferenceType()= " + edge.getType().getEReferenceType().getName() + ", " +
                "type.eContainingFeature=" + edge.getType().eContainingFeature().getName() + ", " +
                "type.eContainingFeature.eContainingFeature=" + edge.getType().eContainingFeature().eContainingFeature().getName() + ", " +
                "type.upperbound=" + edge.getType().getUpperBound();
//                "type.eContainer=" + edge.getType().eContainer() + ",\n                               " +
//                "type.eContainer=" + edge.getType().eContainer().eClass().getName() + ",\n                               " +
//                "type.eClass=" + edge.getType().eClass().getName() + ", \n                               " +
//                "EReferences of the Eclass: " + edge.getType().eContainer().eClass().getEAllReferences().stream().map(ENamedElement::getName).reduce("", (a, b) -> a + ", " + b) + "\n                               " +
//                "EAttributes of the Eclass: " + edge.getType().eContainer().eClass().getEAllAttributes().stream().map(ENamedElement::getName).reduce("", (a, b) -> a + ", " + b) + "\n                               " +
//                "type.eContainer=" + edge.getType().eContainer() + ", ";
    }
}
