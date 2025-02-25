package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion;

import language.*;
import org.apache.log4j.Logger;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContext;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContextPattern;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXModel;
import tools.vitruv.change.atomic.eobject.EobjectPackage;
import tools.vitruv.change.atomic.feature.reference.ReferencePackage;
import tools.vitruv.change.atomic.root.RootPackage;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.*;

import java.util.*;
import java.util.stream.Collectors;

public class IbexPatternToChangeSequenceTemplateConverter {

    protected static final Logger logger = Logger.getRootLogger();

    private final IBeXModel iBeXModel;
    private final TGG tgg;
    /**
     * Remember placeholders because some need to be referenced by multiple EChangeWrappers.
     */
    private final Map<TGGRuleNode, EObjectPlaceholder> nodeToPlaceholderMap;
    /**
     * We need that because patterns are DAGs, not trees, and we want each node to be visited exactly once.
     */
    private final Set<TGGRuleNode> graphElementVisitedSet;
    /**
     * Collects EChangeWrappers belonging to ONE rule. That means this needs to be resetted before each rule parsing.
     */
    private Collection<EChangeWrapper> eChangeWrappers;


    public IbexPatternToChangeSequenceTemplateConverter(IBeXModel iBeXModel, TGG tgg) {
        this.iBeXModel = iBeXModel;
        this.tgg = tgg;
        this.nodeToPlaceholderMap = new HashMap<>();
        this.graphElementVisitedSet = new HashSet<>();
        this.eChangeWrappers = new LinkedList<>();
    }

    public ChangeSequenceTemplateSet convert() {
        printDebugInfo();

        Collection<ChangeSequenceTemplate> patternTemplates = this.tgg.getRules().stream().map(this::parseRule).collect(Collectors.toList());
        patternTemplates.forEach(logger::debug);
        return new ChangeSequenceTemplateSet(patternTemplates);
    }

    /**
     * DFS through a pattern, divided into CONTEXT nodes and CREATE nodes for less complex submethods.
     * Depending on different CREATE nodes and CREATE edges, EChangeWrappers are generated and collected globally.
     *
     * @return a ${@link ChangeSequenceTemplate} containing the collected EChangeWrappers and references to their ibeX pendants.
     */
    private ChangeSequenceTemplate parseRule(final TGGRule rule) {
        eChangeWrappers = new HashSet<>();
        filterNodes(rule, BindingType.CONTEXT, DomainType.SRC).forEach(this::parseContextNode);
        filterNodes(rule, BindingType.CREATE, DomainType.SRC).forEach(this::parseCreateNode);

        return new ChangeSequenceTemplate(rule, getRelatedPatterns(rule) ,eChangeWrappers);
    }

    /**
     * Handle a node that adds an Object to a model by creating the related ${@link EChangeWrapper}s and recurse further.
     */
    private void parseCreateNode(TGGRuleNode ruleNode) {
        if (graphElementVisitedSet.contains(ruleNode)) { return; }
        assert ruleNode.getBindingType().equals(BindingType.CREATE);

        eChangeWrappers.add(
                new PlainEChangeWrapper(
                        EobjectPackage.eINSTANCE.getCreateEObject(),
                        ruleNode.getType(),
                        getOrCreatePlaceHolder(ruleNode))
        );

        if (ruleNode.getIncomingEdges().stream().filter(tggRuleEdge ->
                (tggRuleEdge.getBindingType().equals(BindingType.CONTEXT) || tggRuleEdge.getBindingType().equals(BindingType.CREATE))
                        && tggRuleEdge.getDomainType().equals(DomainType.SRC))
                .findAny().isEmpty()) {
            // no incoming edges --> root EObject
            eChangeWrappers.add(
                    new PlainEChangeWrapper(
                            RootPackage.eINSTANCE.getInsertRootEObject(),
                            ruleNode.getType(),
                            getOrCreatePlaceHolder(ruleNode))
            );
        }

        // continue further on
        graphElementVisitedSet.add(ruleNode);
        ruleNode.getOutgoingEdges().forEach(edge -> {
            // created nodes cannot have outgoing edges that "already exist".
            assert edge.getBindingType().equals(BindingType.CREATE);
            parseCreateEdge(edge);
        });

    }

    /**
     * Ignore this node but decide, based on its outgoing edges, what is to be done.
     * A CREATEing edge adds EChangeWrappers, while a CONTEXT edge is skipped.
     */
    private void parseContextNode(TGGRuleNode ruleNode) {
        if (graphElementVisitedSet.contains(ruleNode)) { return; }
        // recurse through context nodes and edges until a CREATE node or edge is found.
        assert ruleNode.getBindingType().equals(BindingType.CONTEXT);
        graphElementVisitedSet.add(ruleNode);
        ruleNode.getOutgoingEdges().forEach(edge -> {
            if (edge.getBindingType().equals(BindingType.CONTEXT)) {
                assert edge.getTrgNode().getBindingType().equals(BindingType.CONTEXT);
                parseContextNode(edge.getTrgNode());
            } else if (edge.getBindingType().equals(BindingType.CREATE)) {
                parseCreateEdge(edge);
            }
        });
    }

    /**
     * Handle an edge that modifies a reference of an existing or newly created EObject by creating the related ${@link EChangeWrapper}s and recurse further.
     */
    private void parseCreateEdge(TGGRuleEdge ruleEdge) {
        assert ruleEdge.getBindingType().equals(BindingType.CREATE);
        /*
            Ibex treats every edge as an EReference, not an EAttribute.
            That is in accordance to ecore, where Attributes refer to EDataTypes and references refer to EClasses.

            We need to extract the following information:
            1. EChange Type --> This is done ased on many valued or not
            2. AffectedEObject Type --> this is known by looking at the source node of this edge.
            3. The EReference which is modified --> this edge's type.
               This is NOT the type of the element(s) in the reference but the type of the reference itself.
            4. Choice of a placeholder.

            Since we do a DFS, we expect an already created placeholder representing the src node.
            This is encapsulated by getOrCreatePlaceHolder
         */
        if (Util.isManyValued(ruleEdge.getType())) {
            eChangeWrappers.add(
                    new EReferenceValueIndexEChangeWrapper(
                            ReferencePackage.eINSTANCE.getInsertEReference(),
                            ruleEdge.getSrcNode().getType(), // the affected EObject always is the node where this edge comes from
                            getOrCreatePlaceHolder(ruleEdge.getSrcNode()),
                            ruleEdge.getType(),
                            getOrCreatePlaceHolder(ruleEdge.getTrgNode()))
            );
        } else {
            eChangeWrappers.add(
                    new EReferenceTwoValueEChangeWrapper(
                            ReferencePackage.eINSTANCE.getReplaceSingleValuedEReference(),
                            ruleEdge.getSrcNode().getType(), // the affected EObject always is the node where this edge comes from
                            getOrCreatePlaceHolder(ruleEdge.getSrcNode()),
                            ruleEdge.getType(),
                            new EObjectPlaceholder(), // this isn't mapped by TGGs
                            getOrCreatePlaceHolder(ruleEdge.getTrgNode()))
            );
        }

        // continue DFS
        if (ruleEdge.getTrgNode().getBindingType().equals(BindingType.CONTEXT)) {
            parseContextNode(ruleEdge.getTrgNode());
        } else if (ruleEdge.getTrgNode().getBindingType().equals(BindingType.CREATE)) {
            parseCreateNode(ruleEdge.getTrgNode());
        }
    }

    /**
     * Ensure that each ${@link TGGRuleNode} is represented by exactly ONE ${@link EObjectPlaceholder}.
     * This is needed because we want the pattern structure to be implicitly represented in the resulting
     * ${@link ChangeSequenceTemplate} by having placeholders occurring in different ${@link EChangeWrapper}s.
     */
    private EObjectPlaceholder getOrCreatePlaceHolder(TGGRuleNode tggRuleNode) {
        if (nodeToPlaceholderMap.containsKey(tggRuleNode)) {
            return nodeToPlaceholderMap.get(tggRuleNode);
        } else {
            EObjectPlaceholder placeholder = new EObjectPlaceholder();
            nodeToPlaceholderMap.putIfAbsent(tggRuleNode, placeholder);
            return placeholder;
        }
    }

    /**
     * Map a ${@link TGGRule} to its related ${@link IBeXContextPattern}.
     */
    private Collection<IBeXContextPattern> getRelatedPatterns(TGGRule rule) {
        return this.iBeXModel.getPatternSet().getContextPatterns().stream()
                .filter(IBeXContextPattern.class::isInstance)
                .map(IBeXContextPattern.class::cast)
                .filter(ibexContextPattern -> ibexContextPattern.getName().startsWith(rule.getName()))
                .toList();
    }

    private Collection<TGGRuleEdge> filterEdges(TGGRule tggRule, BindingType bindingType, DomainType domainType) {
        return tggRule.getEdges().stream().filter(tggRuleEdge -> tggRuleEdge.getBindingType().equals(bindingType) && tggRuleEdge.getDomainType().equals(domainType)).toList();
    }
    private Collection<TGGRuleNode> filterNodes(TGGRule tggRule, BindingType bindingType, DomainType domainType) {
        return tggRule.getNodes().stream().filter(tggRuleNode -> tggRuleNode.getBindingType().equals(bindingType) && tggRuleNode.getDomainType().equals(domainType)).toList();
    }


    /**
     * debug...
     */
    private void printDebugInfo() {
        logger.debug("Rules: ");
        this.tgg.getRules().forEach(rule -> {
            logger.debug("  - " + rule.getName());
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

    }

    /**
     * debug...
     */
    private String edgeToString(TGGRuleEdge edge) {
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
