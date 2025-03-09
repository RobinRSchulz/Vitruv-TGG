package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange;


import language.*;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContextPattern;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.compiler.patterns.PatternType;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.EObjectPlaceholder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents a pattern in the form of Vitruvius EChanges.
 *
 */
public class ChangeSequenceTemplate {

    private final Collection<EChangeWrapper> eChangeWrappers;
    /**
     * maps an Echange Type to all EChange-Wrappers this pattern contains
     */
    private Map<EClass, Set<EChangeWrapper>> eChangeWrappersByEChangeType;

    private final TGGRule tggRule;
    private final Map<PatternType, IBeXContextPattern> iBeXContextPatternMap;

    /**
     * This is a convenvience field for instantiated Pattern Template, mapping wrappers of the original to this instance's wrappers.
     */
    Map<EChangeWrapper, EChangeWrapper> parentToChildEChangeWrapperMap;

    /**
     * private constructor for copying the Template
     */
    private ChangeSequenceTemplate(TGGRule tggRule, Map<PatternType, IBeXContextPattern> iBeXContextPatternMap, Collection<EChangeWrapper> eChangeWrappers, Map<EChangeWrapper, EChangeWrapper> parentToChildEChangeWrapperMap) {
        this.tggRule = tggRule;
        this.iBeXContextPatternMap = iBeXContextPatternMap;

        this.eChangeWrappers = eChangeWrappers;
        this.parentToChildEChangeWrapperMap = parentToChildEChangeWrapperMap;
        initialize();
    }

    public ChangeSequenceTemplate(TGGRule tggRule, Collection<IBeXContextPattern> iBeXContextPatterns, Collection<EChangeWrapper> eChangeWrappers) {
        this.tggRule = tggRule;
        this.iBeXContextPatternMap = new HashMap<>();
        for (IBeXContextPattern iBeXContextPattern : iBeXContextPatterns) {
            this.iBeXContextPatternMap.put(getPatternType(iBeXContextPattern), iBeXContextPattern);
        }

        this.eChangeWrappers = eChangeWrappers;
        initialize();
    }

    private void initialize() {
        // make the mapping EChangeType -> relevant eChangeWrappers easily accessible
        eChangeWrappersByEChangeType = new HashMap<>();
        for (EChangeWrapper eChangeWrapper : eChangeWrappers) {
            eChangeWrappersByEChangeType.computeIfAbsent(eChangeWrapper.getEChangeType(), k -> new HashSet<>()).add(eChangeWrapper);
        }
    }

    private PatternType getPatternType(IBeXContextPattern pattern) {
        return PatternSuffixes.extractType(pattern.getName());
    }

    public String toString() {
        return "[IbexPatternTemplate of " + tggRule.getName() + "] \n  - " + eChangeWrappers.stream().map(eChangeWrapper -> eChangeWrapper.toString("      ")).collect(Collectors.joining(",\n  - "));
    }

    /**
     *
     * @return the IBexContextPattern this ChangeSequenceTemplate represents, typed with the given PatternType.
     */
    public IBeXContextPattern getIBeXContextPattern(PatternType patternType) {
        return this.iBeXContextPatternMap.get(patternType);
    }

    /**
     *
     * @return whether all eChanges this ChangeSequenceTemplate holds have been full initialized with EChanges.
     */
    public boolean isInitialized() {
        return this.eChangeWrappers.stream().allMatch(EChangeWrapper::isInitialized);
    }

    public Collection<EChangeWrapper> getEChangeWrappers() {
        return eChangeWrappers;
    }

    /**
     *
     * @return all placeholders that are being held by the EChangeWrappers this ChangeSequenceTemplate consists of.
     */
    public Set<EObjectPlaceholder> getAllPlaceholders() {
        return this.getEChangeWrappers().stream()
                .map(EChangeWrapper::getAllPlaceholders).flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public Collection<EChangeWrapper> getUninitializedEChangeWrappers() {
        return eChangeWrappers.stream().filter(eChangeWrapper -> !eChangeWrapper.isInitialized()).collect(Collectors.toSet());
    }


    /**
     * This is a convenvience method for instantiated Pattern templates, mapping wrappers of the original to this instance's wrappers.
     * @param parent, belonging to the IbexPatternTemplate of which this instance is a copy
     * @return the child, belonging to this instance
     */
    public EChangeWrapper getThisInstancesEChangeWrapperFromParent(EChangeWrapper parent) {
        return this.parentToChildEChangeWrapperMap.get(parent);
    }

    /**
     *
     * @return all EChangeWrappers that match the given eChangeType and thus are a possible candidate
     */
    public Set<EChangeWrapper> getRelevantEChangeWrappersByEChangeType(EClass eChangeType) {
        return eChangeWrappersByEChangeType.get(eChangeType);
    }

    /**
     *
     * @return the EChangeWrapper that holds the given eChange, if there is any.
     */
    public Optional<EChangeWrapper> getEChangeWrapperHolding(EChange<EObject> eChange) {
        Set<EChangeWrapper> holdingWrappers = this.getRelevantEChangeWrappersByEChangeType(eChange.eClass()).stream()
                .filter(eChangeWrapper -> eChangeWrapper.getEChange().equals(eChange))
                .collect(Collectors.toSet());

        if (holdingWrappers.size() > 1) throw new IllegalStateException("More than one EChangeWrapper holds " + Util.eChangeToString(eChange) + "!");
        return holdingWrappers.stream().findAny();
    }

    /**
     *
     * @return the tgg rule this pattern template is based on.
     */
    public TGGRule getTggRule() {
        return tggRule;
    }

    /**
     *
     * @return a deep copy with new wrappers and placeholders, while retaining the placeholder structure.
     * Also, this ChangeSequenceTemplate's EChangeWrappers are remembered as originals of the copy's wrappers, which is useful for duplicate avoidance/ detection.
     */
    public ChangeSequenceTemplate deepCopy() {
        /*
         * copy the echange wrapper and their placeholder. Afterwards, we got NEW eChangeWrappers with OLD Placeholders.
         * The new eChangeWrappers have their originals set as original.
         */
        Collection<EChangeWrapper> newEChangeWrappers = new LinkedList<>();
        Map<EChangeWrapper, EChangeWrapper> oldToNewEChangeWrapperMap = new HashMap<>();
        for (EChangeWrapper changeWrapper : this.eChangeWrappers) {
            EChangeWrapper newEChangeWrapper = changeWrapper.shallowCopy();
            oldToNewEChangeWrapperMap.put(changeWrapper, newEChangeWrapper);
            newEChangeWrappers.add(newEChangeWrapper);
        }
        ChangeSequenceTemplate copiedTemplate = new ChangeSequenceTemplate(this.tggRule, this.iBeXContextPatternMap, newEChangeWrappers, oldToNewEChangeWrapperMap);

        // now, we can systematically replace the OLD placeholders in the NEW eChangeWrappers with NEW placeholders to achieve a deep copy
        // 1. Create a Map OLDPlaceholder -> NEWPlaceholder
        Set<EObjectPlaceholder> allPlaceholders = new HashSet<>();
        for (EChangeWrapper changeWrapper : copiedTemplate.getEChangeWrappers()) {
            allPlaceholders.addAll(changeWrapper.getAllPlaceholders());
        }
        Map<EObjectPlaceholder, EObjectPlaceholder> oldToNewPlaceholders = new HashMap<>();
        for (EObjectPlaceholder oldPlaceholder : allPlaceholders) {
            oldToNewPlaceholders.put(oldPlaceholder, new EObjectPlaceholder(oldPlaceholder.getTggRuleNode()));
        }

        // 2. Replace the OLD placeholders in the NEW eChangeWrappers with the NEW placeholders.
        for (EChangeWrapper eChangeWrapper : copiedTemplate.getEChangeWrappers()) {
            eChangeWrapper.replaceAllPlaceholders(oldToNewPlaceholders);
        }

        return copiedTemplate;
    }

    /**
     * @param tggResourceHandler we need access to the CORRs
     * This must be initialized to be able to return sth meaningful.
     * Further, this only returns something meaningful, if changes that create sth in the "target" model that this context match relies on are already applied by the SYNC!
     * @return whether this (matched!) template also has the context that the ${@link TGGRule} this represents requires!
     */
    public boolean contextMatches(TGGResourceHandler tggResourceHandler) {
        ContextMatcher contextMatcher = new ContextMatcher(tggResourceHandler);
        if (!contextMatcher.contextMatches()) {
            return false;
        } else {
            // keep the results for later. Todo here? must be present in the VitruviusBackwardConversionMatch
            throw new RuntimeException("not implemented yet");
        }
    }
    private class ContextMatcher {

        /**
         * initially only contains CREATE nodes.
         */
        private final Map<TGGRuleNode, EObject> tggRuleNode2EObjectMap;
        private final Set<TGGRuleNode> nodesVisited;
        private boolean matchingFailed;

        private final TGGResourceHandler tggResourceHandler;

        public ContextMatcher(TGGResourceHandler tggResourceHandler) {
            this.tggRuleNode2EObjectMap = new HashMap<>();
            this.nodesVisited = new HashSet<>();
            this.matchingFailed = false;
            this.tggResourceHandler = tggResourceHandler;

            initTggRuleNode2EObjectMap();
        }

        /**
         * Only call this after successfully having called ::contextMatches. Otherwise, we throw.
         *
         * @return the map containing the collected context nodes mapped to their eObjects. (also the pre-known create node mappings).
         */
        public Map<TGGRuleNode, EObject> getTggRuleNode2EObjectMap() {
            if (matchingFailed) throw new IllegalStateException("Matching failed! No Context nodes mapped! You should not be calling this.");
            return tggRuleNode2EObjectMap;
        }

        /**
         *
         * @return whether this initialized/ matchted(!) changeSequenceTemplate also matches the context, which the rule requires.<br/>
         *  In addition to {@link BindingType#CREATE} nodes, {@link TGGRule}s can also (more often than not) require those {@link BindingType#CREATE} nodes
         *  to be embedded in a {@link BindingType#CONTEXT} graph.<br/>
         *  Here, we check whether that context graph (given by the {@link TGGRule}'s nodes and edges that are typed with {@link BindingType#CONTEXT})
         *  can be found in the model "around" the {@link EObject}s that have been matched to {@link BindingType#CREATE} nodes.  <br/>
         *  Although {@link BindingType#CREATE} nodes have been matched only in one domain ({@link DomainType#SRC} or {@link DomainType#TRG}), we need to match
         *  <b>both</b> {@link DomainType}s here.
         */
        public boolean contextMatches() {
            // Precondition: Each CREATE node and edge has been matched with the respective EObjects
            if (!isInitialized()) return false;
        /*
            Idea: (Backwards) DFS through the pattern. CREATE nodes and edges are related to EObjects because of the precondition.
            * recurse through all(!) CONTEXT nodes, always following along the eObject-"hierarchy" provided by the CREATE nodes.
            * mark visited nodes

         */
            //TODO that stupid bijectivity...
            for (TGGRuleNode createNode : Util.filterNodes(tggRule, BindingType.CREATE, DomainType.SRC)) {
                if (!visitCREATENode(createNode)) {
                    matchingFailed = true;
                    // early return
                    return false;
                }
            }
            return true;
        }

        /**
         * visit a CREATE node and DFS through its domain and, via corr, also move to the other domain.
         * <br/><br/>
         * Only check all context nodes/edges, if this is adjacent to a CREATE node, that will be handled by the caller!<br/>
         * Since we also switch domains in this recursion (because we also match the target contexts),<br/>
         * this hinders us breaking our precondition by mistakenly assuming that we have already matched "TRG" CREATE nodes!
         * @param createNode
         * @return
         */
        private boolean visitCREATENode(TGGRuleNode createNode) {
            /*
             */
            nodesVisited.add(createNode);
            EObject createNodeEObject = tggRuleNode2EObjectMap.get(createNode);
            for (TGGRuleEdge incomingEdge : createNode.getIncomingEdges()) {
                /*
                 * Let createNode.getDomainType be THIS and the other be OTHER
                 * Incoming edges can be:
                 *   * BindingType: CREATE or CONTEXT   (source nodes: CREATE or CONTEXT)
                 *   * DomainType:  THIS or CORR        (source nodes: THIS or CORR)
                 *
                 * We ignore all edges where the source node is CREATE, since we handle that in the caller (iter over each CREATE).
                 * In THIS domain, we look at a CONTEXT node and try to match it (and the edge) to actual model elements.
                 * In CORR domain, we look at CONTEXT nodes and move to the respective node in the OTHER domain
                 *    and try to find an existing correlation to an existing model element in the model of the OTHER domain.
                 */
                if (incomingEdge.getSrcNode().getBindingType().equals(BindingType.CREATE)) {continue;}
                assert incomingEdge.getBindingType().equals(BindingType.CONTEXT);

                if (incomingEdge.getDomainType().equals(createNode.getDomainType())) {
                    // THIS domain, CONTEXT node
                    Set<EObject> parentCandidates = EcoreUtil.UsageCrossReferencer.find(createNodeEObject, createNodeEObject.eResource()).stream()
                            .filter(setting -> setting.getEStructuralFeature().equals(incomingEdge))
                            .map(EStructuralFeature.Setting::getEObject)
                            .collect(Collectors.toSet());
                    if (handleParentCandidatesFor(parentCandidates, incomingEdge.getSrcNode())) {
                        return visitCONTEXTNode(incomingEdge.getSrcNode());
                    } else return false;
                } else if (incomingEdge.getDomainType().equals(DomainType.CORR)) {
                    if (incomingEdge.getBindingType().equals(BindingType.CREATE)) {
                        // cannot and should not be matched, because no corr exists in the model, yet!
                        continue;
                    }
                    // we leave the domain and have a CONTEXT node
//                    this.createCorrs(comatch, greenPattern.getCorrNodes(), this.resourceHandler.getCorrResource());
//                        --calls-->
//                    private EObject createCorr(ITGGMatch comatch, TGGRuleNode node, Object src, Object trg) { // src & trg sind die Namen der Regel-Knoten
//                        EObject corr = this.createNode(comatch, node);
//                        corr.eSet(corr.eClass().getEStructuralFeature("source"), src);
//                        corr.eSet(corr.eClass().getEStructuralFeature("target"), trg);
//                        ++this.numOfCreatedCorrNodes;
//                        return corr;
//                    }
                    TGGRuleCorr sourceTGGRuleNode = (TGGRuleCorr) incomingEdge.getSrcNode(); // this MUST be a TGGRuleCorr node
                    TGGRuleNode correlatedNodeInOTHERDomain = createNode.getDomainType().equals(DomainType.SRC) ? sourceTGGRuleNode.getTarget() : sourceTGGRuleNode.getSource();

                    if (correlatedNodeInOTHERDomain.getBindingType().equals(BindingType.CONTEXT)) {
                        // we are only interested in matching CONTEXT nodes in the other domain.
                        Optional<EObject> correlatedEObject = getEObjectCorrellatedToAMatchingEObject(createNodeEObject, sourceTGGRuleNode, correlatedNodeInOTHERDomain);
                        if (correlatedEObject.isPresent()) {
                            //we have found a matching EObject -> put it in the global map
                            tggRuleNode2EObjectMap.put(sourceTGGRuleNode, correlatedEObject.get());
                            // TODO from here, we can recurse further. Remember that we are in the OTHER domain.
                            visitCONTEXTNode(sourceTGGRuleNode);
                            // return recurseFurther(...);
                            throw new RuntimeException("todo implement recursing further");
                        }
                    }
                    // else we got a CREATE node in the OTHER domain which doesn't interest us...
                    //TODO don't know whats best here... we need the target model resource or the EcoreUtil search...
                    // TODO we need the ibex CORR map, all else makes no sense..
                    /* TODO
                        PROBLEM: if we check the "Target side" before a change that possibly creates the relevant correlation and target context is applied, this match is wrongfully labelled as invalid
                        SOLUTION options: not checking the context immediately after matching, but EACH time that SYNC wants new matches!
                        GOOD SIDE: This way, this is always called by the Vitruvius...TGGEngine (IBlackInterpreter), which means that we have access to the corr map!!!
                    */
                    throw new RuntimeException("TODO impl");
                } else {
                    throw new IllegalStateException("This must be an algorithm error!");
                }
            }

            for (TGGRuleEdge outgoingEdge : createNode.getOutgoingEdges()) {
                /*
                 * Let createNode.getDomainType be THIS and the other be OTHER
                 * Outgoing edges can be:
                 *   * BindingType: CREATE  (target nodes: CREATE or CONTEXT)
                 *   * DomainType:  THIS    (source nodes: THIS)
                 *
                 * We ignore all edges where the target node is CREATE, since we handle that in the caller (iter over each CREATE).
                 * We stay in THIS domain.
                 * We look at a CONTEXT target node and try to match it (and the edge) to actual model elements.
                 */

                // created nodes cannot have outgoing edges that "already exist".
                assert outgoingEdge.getBindingType().equals(BindingType.CREATE);
                assert outgoingEdge.getDomainType().equals(createNode.getDomainType());

                if (outgoingEdge.getTrgNode().getBindingType().equals(BindingType.CONTEXT)) {
                    // TODO do similar stuff as above
                }
                // not look at CREATE nodes...
            }

            return true;
        }

        private boolean visitCONTEXTNode(TGGRuleNode contextNode) {

            throw new RuntimeException("TODO impl");
        }

        private boolean handleParentCandidatesFor(Set<EObject> parentCandidates, TGGRuleNode parentNode) {
            // this can possibly be more than one. But we need the one that satisfies our match, if there is one. For now, we assume that there is only one...
            EObject parentEObject = parentCandidates.iterator().next();
            if (parentCandidates.size() > 1) {
                throw new IllegalStateException("Checking for more than one parent currently not supported. Maybe this should just fail the check instead of throwing...");
            } else if (parentCandidates.size() == 1) {
                // found!
                tggRuleNode2EObjectMap.put(parentNode, parentEObject);
                return true;
            } else {
                return false;
            }
        }

        /**
         * Init the map with what is already known.
         */
        private void initTggRuleNode2EObjectMap() {
            for (EObjectPlaceholder eObjectPlaceholder : getAllPlaceholders()) {
                tggRuleNode2EObjectMap.put(eObjectPlaceholder.getTggRuleNode(), eObjectPlaceholder.getAffectedEObject());
            }
        }

        /**
         * Check if the given eObject is correllated via a CORR node that resembles the tggRuleCorrFromRule to a node in the OTHER domain
         * that matches the given ruleNodeInOtherDomain.
         *
         * @param eObject the eObject which is to be checked
         * @param tggRuleCorrFromRule
         * @param ruleNodeInOtherDomain
         * @return
         */
        private Optional<EObject> getEObjectCorrellatedToAMatchingEObject(EObject eObject, TGGRuleCorr tggRuleCorrFromRule, TGGRuleNode ruleNodeInOtherDomain) {

            //Todo remove this code from IbexGreenInterpreter
//                    this.createCorrs(comatch, greenPattern.getCorrNodes(), this.resourceHandler.getCorrResource());
//                        --calls-->
//                    private EObject createCorr(ITGGMatch comatch, TGGRuleNode node, Object src, Object trg) { // src & trg sind die Namen der Regel-Knoten
//                        EObject corr = this.createNode(comatch, node);
//                        corr.eSet(corr.eClass().getEStructuralFeature("source"), src);
//                        corr.eSet(corr.eClass().getEStructuralFeature("target"), trg);
//                        ++this.numOfCreatedCorrNodes;
//                        return corr;
//                    }

            // find all Corrs for eObject that match the given tggRuleCorrFromRule.
            Set<TGGRuleCorr> matchingInstantiatedTGGRuleCorrs = tggResourceHandler.getCorrCaching().get(eObject).stream()
                    .filter(corrEObject -> corrEObject instanceof TGGRuleCorr) // in case there are others...
                    .map(corrEObject -> (TGGRuleCorr) corrEObject)
                    .filter(corrEObject -> tggRuleCorrFromRule.getName().equals(corrEObject.getName())) // we want correlations that match the one from the rule...
                    .collect(Collectors.toSet());
            if (matchingInstantiatedTGGRuleCorrs.size() > 1) {
                throw new IllegalStateException("More than one correlation names " + tggRuleCorrFromRule.getName()
                        + " for eObject " + Util.eObjectToString(eObject) + " found.");
            } else if (matchingInstantiatedTGGRuleCorrs.size() == 1) {
                TGGRuleCorr matchingInstantiatedTGGRuleCorr = matchingInstantiatedTGGRuleCorrs.stream().findAny().get();
                // ensure to always return the eobject from the OTHER Domain
                EObject matchingInstantiatedNodeInOtherDomain = (EObject) matchingInstantiatedTGGRuleCorr.eGet(matchingInstantiatedTGGRuleCorr.eClass().getEStructuralFeature(
                        ruleNodeInOtherDomain.getDomainType().equals(DomainType.SRC) ? "source" : "target"
                ));
                // only return the EObject if it matches the rule node (which should be guaranteed via the corr node but who knows...)
                return ruleNodeInOtherDomain.getType().equals(matchingInstantiatedNodeInOtherDomain.eClass())
                        ? Optional.of(matchingInstantiatedNodeInOtherDomain)
                        : Optional.empty();
            } else return Optional.empty();
        }
    }
}
