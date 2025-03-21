package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion;

import language.*;
import org.apache.log4j.Logger;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContextPattern;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXModel;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import tools.vitruv.change.atomic.eobject.EobjectPackage;
import tools.vitruv.change.atomic.feature.reference.ReferencePackage;
import tools.vitruv.change.atomic.root.RootPackage;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.*;

import java.util.*;
import java.util.stream.Collectors;

public class IbexPatternToChangeSequenceTemplateConverter {
    protected static final Logger logger = Logger.getLogger(IbexPatternToChangeSequenceTemplateConverter.class);

    private final IBeXModel iBeXModel;
    private final TGG tgg;
    /**
     * Remember placeholders because some need to be referenced by multiple EChangeWrappers.
     */
    private final Map<TGGRuleNode, EObjectPlaceholder> nodeToPlaceholderMap;
    /**
     * We need that because patterns are DAGs, not trees, and we want each node to be visited exactly once.
     */
    private Set<TGGRuleNode> graphElementVisitedSet;
    /**
     * Collects EChangeWrappers belonging to ONE rule. That means this needs to be resetted before each rule parsing.
     */
    private Collection<EChangeWrapper> eChangeWrappers;
    
    private DomainType domainType;


    public IbexPatternToChangeSequenceTemplateConverter(IBeXModel iBeXModel, TGG tgg, DomainType domainType) {
        this.iBeXModel = iBeXModel;
        this.tgg = tgg;
        this.nodeToPlaceholderMap = new HashMap<>();
        this.graphElementVisitedSet = new HashSet<>();
        this.eChangeWrappers = new LinkedList<>();
        this.domainType = domainType;
    }

    public ChangeSequenceTemplateSet convert() {
        printRulesDebugInfo();

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
        graphElementVisitedSet = new HashSet<>();
        Util.filterNodes(rule, BindingType.CONTEXT, domainType).forEach(this::parseContextNode);
        Util.filterNodes(rule, BindingType.CREATE, domainType).forEach(this::parseCreateNode);

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
                        && tggRuleEdge.getDomainType().equals(domainType))
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
        for (TGGRuleEdge edge : ruleNode.getOutgoingEdges()) {// created nodes cannot have outgoing edges that "already exist".
            assert edge.getBindingType().equals(BindingType.CREATE);
            parseCreateEdge(edge);
        }

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
        for (TGGRuleEdge edge : ruleNode.getOutgoingEdges()) {
            if (edge.getBindingType().equals(BindingType.CONTEXT)) {
                assert edge.getTrgNode().getBindingType().equals(BindingType.CONTEXT);
                parseContextNode(edge.getTrgNode());
            } else if (edge.getBindingType().equals(BindingType.CREATE)) {
                parseCreateEdge(edge);
            }
        }
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
                /*
                    in the case of ReplaceSingeValuedEReference (which represents replacement (not handled here) but also the insertion of a value into a single-valued EReference),
                    we use null as the TGG rule placeholder for the old value, because there is no such thing as an old value in a TGG pattern, meaning we cannot assign a TGGRuleNode!
                 */
                    new EReferenceTwoValueEChangeWrapper(
                            ReferencePackage.eINSTANCE.getReplaceSingleValuedEReference(),
                            ruleEdge.getSrcNode().getType(), // the affected EObject always is the node where this edge comes from
                            getOrCreatePlaceHolder(ruleEdge.getSrcNode()),
                            ruleEdge.getType(),
                            new EObjectPlaceholder(null), // this isn't mapped by TGG rules/ patterns
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
            EObjectPlaceholder placeholder = new EObjectPlaceholder(tggRuleNode);
            nodeToPlaceholderMap.putIfAbsent(tggRuleNode, placeholder);
            return placeholder;
        }
    }

    /**
     * Map a ${@link TGGRule} to its related ${@link IBeXContextPattern}.
     */
    private Collection<IBeXContextPattern> getRelatedPatterns(TGGRule rule) {
        printPatternsForRuleDebugInfo(rule);

        return this.iBeXModel.getPatternSet().getContextPatterns().stream()
                .filter(IBeXContextPattern.class::isInstance)
                .map(IBeXContextPattern.class::cast)
                .filter(ibexContextPattern -> ibexContextPattern.getName().startsWith(rule.getName()))
                .toList();
    }

    private void printPatternsForRuleDebugInfo(TGGRule rule) {
        logger.trace("+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~");
        logger.trace("PATTERNS FOR RULE: " + rule.getName());
        this.iBeXModel.getPatternSet().getContextPatterns().stream()
                .filter(IBeXContextPattern.class::isInstance)
                .map(IBeXContextPattern.class::cast)
                .filter(ibexContextPattern -> ibexContextPattern.getName().startsWith(rule.getName()))
                .forEach(iBeXContextPattern -> logger.trace("  - [" + iBeXContextPattern.getName() + "] patternType=" + PatternSuffixes.extractType(iBeXContextPattern.getName())));
        logger.trace(" ANY ALTERNATIVES???");
        this.iBeXModel.getPatternSet().getContextPatterns().stream()
                .filter(pattern -> !(pattern instanceof IBeXContextPattern))
                .forEach(pattern -> logger.trace("  - [" + pattern.getName() + "] patternType=" + PatternSuffixes.extractType(pattern.getName())));
        logger.trace("+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~+*~");

    }


    /**
     * debug...
     */
    private void printRulesDebugInfo() {
        logger.debug("*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~");
        logger.debug("Rules: ");
        logger.debug("*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~*+~");
        for (TGGRule rule : this.tgg.getRules()) {
            logger.debug("  - TGGRule(" + rule.getName() + "): ");
            rule.getNodes().forEach(tggRuleNode -> {
                logger.debug("    - " + nodeToString(tggRuleNode));
                logger.trace("      - incoming edges: ");
                tggRuleNode.getIncomingEdges().forEach(edge -> logger.trace("        - " + edgeToString(edge)));
                logger.trace("      - outgoing edges: ");
                tggRuleNode.getOutgoingEdges().forEach(edge -> logger.trace("        - " + edgeToString(edge)));
                logger.trace("      - incomingCorrsSource edges: ");
                tggRuleNode.getIncomingCorrsSource().forEach(node -> logger.trace("        - " + nodeToString(node)));
            });
        }
    }

    /**
     * debug...
     */
    private String edgeToString(TGGRuleEdge edge) {
        return "TGGRuleEdge(" + edge.getName() + ")" +
                "[ " + edge.getSrcNode().getName() + "->" + edge.getTrgNode().getName() + "]: " +
                "domain=" + edge.getDomainType() + ", " +
                "binding=" + edge.getBindingType();
    }

    private String nodeToString(TGGRuleNode node) {
        return node.getClass().getSimpleName() + "(" + node.getName() + "): " +
                "domain=" + node.getDomainType() + ", " +
                "binding=" + node.getBindingType();
    }
}
