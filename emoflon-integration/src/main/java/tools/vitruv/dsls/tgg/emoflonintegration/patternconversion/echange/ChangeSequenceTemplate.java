package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange;


import edu.kit.ipd.sdq.commons.util.java.Pair;
import language.*;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContextPattern;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.compiler.patterns.PatternType;
import org.emoflon.ibex.tgg.operational.strategies.modules.TGGResourceHandler;
import runtime.CorrespondenceNode;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.feature.reference.InsertEReference;
import tools.vitruv.dsls.tgg.emoflonintegration.Util;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.EObjectPlaceholder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents a pattern in the form of Vitruvius EChanges.
 *
 */
public class ChangeSequenceTemplate {
    private static final Logger logger = Logger.getLogger(ChangeSequenceTemplate.class);
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
                .filter(eChangeWrapper -> eChange.equals(eChangeWrapper.getEChange()))
                .collect(Collectors.toSet());

        if (holdingWrappers.size() > 1) throw new IllegalStateException("More than one EChangeWrapper holds " + Util.eChangeToString(eChange) + "!");
        return holdingWrappers.stream().findAny();
    }

    /**
     * 1. Get EChangeWrapper that holds an EChange (of type todto), where<br/>
     *    * srcTGGRUleNode is EChange.AE,   <br/>
     *    * eReference     is EChange.F     <br/>
     *    * objectValue    is EChange.V     <br/>
     *    if there is exactly one. Otherwise, return nothing
     */
    public Optional<InsertEReference<EObject>> getMatchedEChangeConcerning(EObject affectedEObject, EReference eReference, EList<?> values) {
        Set<InsertEReference<EObject>> echangeCandidates = this.getEChangeWrappers().stream()
                .filter(EReferenceValueIndexEChangeWrapper.class::isInstance)
                .map(EReferenceValueIndexEChangeWrapper.class::cast)
                .map(EChangeWrapper::getEChange)
                .filter(InsertEReference.class::isInstance)
                .map(insertEReference -> (InsertEReference<EObject>) insertEReference)
                .filter(insertEReference -> insertEReference.getAffectedElement().equals(affectedEObject)
                        && insertEReference.getAffectedFeature().equals(eReference)
                        && values.contains(insertEReference.getNewValue()))
                .collect(Collectors.toSet());
        if (echangeCandidates.size() == 1) {
            InsertEReference<EObject> insertEReferenceEChange = echangeCandidates.iterator().next();
//            if (values.stream().filter(value -> value.equals(insertEReferenceEChange.getNewValue())).count() != 1) {
//                logger.info("Found value twice in EChange! Not matching this branch any further...");
//                return Optional.empty();
//            }
            return Optional.of(echangeCandidates.iterator().next());
        } else if (echangeCandidates.size() > 1) {
            logger.info("Found more than one matching EChange! Not matching this branch any further...");
        }
        return Optional.empty();
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
    public Optional<Map<TGGRuleNode, EObject>> contextMatches(TGGResourceHandler tggResourceHandler) {
        //TODO potential optimization keep the context matcher! We might need to call this method multiple times and that saves us recursion effort.
        ContextMatcher contextMatcher = new ContextMatcher(tggResourceHandler);
        return (contextMatcher.contextMatches())
                ? Optional.of(contextMatcher.getTggRuleNode2EObjectMap())
                : Optional.empty();
    }

    /**
     * Precondition: This ChangeSequenceTemplate has been mapped to a subset of the <i>closed neighborhood</i> (abgeschlossene Nachbarschaft)
     * of the {@link BindingType#CREATE} nodes of the {@link TGGRule} this represents. This subset at least contains all {@link BindingType#CREATE} nodes.
     * <br/><br/>
     * In general that matched subset does not cover the whole {@link TGGRule}!
     * <br/>For instance, the SRC side of a rule might look like that:<br/>
     * (CONTEXT node1) ----> (CONTEXT node2) --(+)--> (CREATE node3)<br/>
     * Then our subset consists of node2 and node3, but not of node1!
     * <br/><br/>
     *
     * To check if our match really is a match, we need to also match nodes like node1.
     * This is what the {@link ContextMatcher} does.
     * <br/><br/>
     * Usage: As new nodes are created in the process of pattern matching, the matchability status of this initialized {@link ChangeSequenceTemplate}
     * might change after another pattern has been applied. That needs to be kept in mind.
     *
     */
    private class ContextMatcher {

        /**
         * initially only contains CREATE nodes.
         */
        private final MapStack<TGGRuleNode, EObject> tggRuleNode2EObjectMapStack;
        private final Set<TGGRuleNode> nodesVisited;
        private boolean matchingFailed;

        private final TGGResourceHandler tggResourceHandler;

        public ContextMatcher(TGGResourceHandler tggResourceHandler) {
            this.tggRuleNode2EObjectMapStack = new MapStack<>();
            this.nodesVisited = new HashSet<>();
            this.matchingFailed = true; // initial assumption.
            this.tggResourceHandler = tggResourceHandler;

            initTggRuleNode2EObjectMap();
        }

        /*
         * Constructor for forking the DFS if we have multiple EObject candidates for mapping a context node against.
         * We check each candidate by copying(!) the ContextMatcher-State +the candidate added to the map
         * and let it continue with that choice.
         * --> would need to remember the call stack...
         * --> aufwändig...
         *
         * Optimization/ alternative idea:
         * Let the situation where multiple candidates (EList situation) are present be called a CHOICE:
         * 1. Keep the mappings in a Map AND a Stack -> new DataStructure "MapStack"
         * 1. before a CHOICE a stack for the mappings is created,
         * 2.
         */

        /**
         * Only call this after successfully having called ::contextMatches. Otherwise, we throw.
         *
         * @return the map containing the collected context nodes mapped to their eObjects. (also the pre-known create node mappings).
         */
        public Map<TGGRuleNode, EObject> getTggRuleNode2EObjectMap() {
            if (matchingFailed) throw new IllegalStateException("Matching either failed or not called yet! No Context nodes mapped! You should not be calling this.");
            return tggRuleNode2EObjectMapStack.getMap();
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
                if (!visitNode(createNode)) {
                    matchingFailed = true;
                    // early return
                    return false;
                }
            }
            matchingFailed = false;
            return true;
        }

        /**
         * visit a node and DFS through its domain and, via corr, also move to the other domain.
         * <br/><br/>
         * Only check all CONTEXT nodes/edges, if this is adjacent to a CREATE node, that will be handled by the caller!<br/>
         * Since we also switch domains in this recursion (because we also match the target contexts),<br/>
         * this hinders us breaking our precondition by mistakenly assuming that we have already matched "TRG" CREATE nodes!
         * @param tggRuleNode
         * @return whether this recursion branch has successfully been matched
         */
        private boolean visitNode(TGGRuleNode tggRuleNode) {
            logger.trace("Visiting node " + tggRuleNode.getName());
            assert tggRuleNode2EObjectMapStack.containsKey(tggRuleNode);

            if (tggRuleNode.getBindingType() == BindingType.CREATE) {
                /*
                    An invariant we use is that CREATE nodes are only visited by the caller!.
                    If this node has been visited before it is called by the caller, there must be a bug.
                 */
                assert !nodesVisited.contains(tggRuleNode);
            }
            if (nodesVisited.contains(tggRuleNode)) { return true; }
            nodesVisited.add(tggRuleNode);

            // recurse through incoming and outgoing edges, return early if matching has failed.
            for (TGGRuleEdge incomingEdge : tggRuleNode.getIncomingEdges()) {
                if (!visitIncomingEdge(incomingEdge)) return false;
            }
            for (TGGRuleEdge outgoingEdge : tggRuleNode.getOutgoingEdges()) {
                if (!visitOutgoingEdge(outgoingEdge)) return false;
            }
            return true;
        }

        private boolean visitIncomingEdge(TGGRuleEdge incomingEdge) {
            logger.trace("Visiting incoming edge" + Util.tGGRuleEdgeToString(incomingEdge));
            TGGRuleNode srcTGGRuleNode = incomingEdge.getSrcNode();
            TGGRuleNode trgTGGRuleNode = incomingEdge.getTrgNode();
            if (nodesVisited.contains(srcTGGRuleNode)) { return true; }
            EObject trgNodeEObject = tggRuleNode2EObjectMapStack.get(trgTGGRuleNode);
            /*
             * Let trgTGGRuleNode.getDomainType be THIS and the other be OTHER
             * Incoming edges can be:
             *   * BindingType: CREATE or CONTEXT   (source nodes: CREATE or CONTEXT)
             *   * DomainType:  THIS or CORR        (source nodes: THIS or CORR)
             *
             * We ignore all edges where the source node is CREATE, since we handle that in the caller (iter over each CREATE).
             * In THIS domain, we look at a CONTEXT node and try to match it (and the edge) to actual model elements.
             * In CORR domain, we look at CONTEXT nodes and move to the respective node in the OTHER domain
             *    and try to find an existing correlation to an existing model element in the model of the OTHER domain.
             */
            if (srcTGGRuleNode.getBindingType().equals(BindingType.CREATE)) {return true;}

            if (incomingEdge.getDomainType().equals(trgTGGRuleNode.getDomainType())) {
                logger.trace("  THIS domain, Context trg node: " + Util.tGGRuleNodeToString(trgTGGRuleNode));
                /*
                 * If our relation is a containment relation, w
                 */
                if (incomingEdge.getType().isContainment()) {
                    logger.trace("    HAVE WE FOUND IT? (container!) " + Util.eObjectToString(trgNodeEObject.eContainer()));
                    //TODO handle that stuff.
                    EObject potentialSrcNodeEObject = trgNodeEObject.eContainer();
                    if (srcTGGRuleNode.getType().equals(potentialSrcNodeEObject.eClass())) {
                        // dont know if this check is necessary
                        tggRuleNode2EObjectMapStack.putPush(srcTGGRuleNode, potentialSrcNodeEObject);
                        return visitNode(srcTGGRuleNode);
                    } else return false;
                } else {
                    // THIS domain, CONTEXT trg node, CONTEXT or CREATE edge
                    Set<EObject> parentCandidates = EcoreUtil.UsageCrossReferencer.find(trgNodeEObject, trgNodeEObject.eResource()).stream()
                            .filter(setting -> setting.getEStructuralFeature().equals(incomingEdge.getType()))
                            .map(EStructuralFeature.Setting::getEObject)
                            .collect(Collectors.toSet());
                    return recurseAndTryAllMatchingCandidatesFor(parentCandidates, srcTGGRuleNode);
//                    if (handleMatchingCandidatesFor(parentCandidates, srcTGGRuleNode)) {
//                        return visitNode(srcTGGRuleNode); // recurse
//                    } else return false;
                }
            } else if (incomingEdge.getDomainType().equals(DomainType.CORR)) {
                logger.trace("  CORR domain, Context trg node: " + Util.tGGRuleNodeToString(trgTGGRuleNode));
                if (incomingEdge.getBindingType().equals(BindingType.CREATE)) {
                    // cannot and should not be matched, because no corr exists in the model, yet!
                    return true;
                }
                TGGRuleCorr sourceTGGRuleCorrNode = (TGGRuleCorr) srcTGGRuleNode; // this MUST be a TGGRuleCorr node
                TGGRuleNode correlatedNodeInOTHERDomain = trgTGGRuleNode.getDomainType().equals(DomainType.SRC) ? sourceTGGRuleCorrNode.getTarget() : sourceTGGRuleCorrNode.getSource();
                logger.trace("  correlated node in other domain:" + Util.tGGRuleNodeToString(correlatedNodeInOTHERDomain));

                if (correlatedNodeInOTHERDomain.getBindingType().equals(BindingType.CONTEXT)) {
                    // we are only interested in matching CONTEXT nodes in the OTHER domain.
                    Optional<Pair<CorrespondenceNode,EObject>> instantiatedCorrNodeAndCorrelatedEObject = getInstantiatedCorrNodeAndOthersidedEObjectCorrelatedToAMatchingEObject(trgNodeEObject, sourceTGGRuleCorrNode, correlatedNodeInOTHERDomain);
                    if (instantiatedCorrNodeAndCorrelatedEObject.isPresent()) {
                        //TODO we might want to put the sourceTGGRuleCorrNode in the map, too? (but it has no EObject...) what do? --> check if SYNC meckers...
                        nodesVisited.add(correlatedNodeInOTHERDomain);
                        tggRuleNode2EObjectMapStack.putPush(sourceTGGRuleCorrNode, instantiatedCorrNodeAndCorrelatedEObject.get().getFirst());
                        tggRuleNode2EObjectMapStack.putPush(correlatedNodeInOTHERDomain, instantiatedCorrNodeAndCorrelatedEObject.get().getSecond());
                        return visitNode(correlatedNodeInOTHERDomain); // recurse in the OTHER domain
                    } else return false;
                }
                // else we got a CREATE node in the OTHER domain which doesn't interest us, as it is not present in the model, yet.
                return true;
            } else {
                throw new IllegalStateException("This must be an algorithm error!");
            }
        }

        /**
         * Try to match the target node of an outgoing edge.
         * This can be used for CREATE or CONTEXT edges, the source node can be CREATE or CONTEXT.
         */
        private boolean visitOutgoingEdge(TGGRuleEdge outgoingEdge) {
            logger.trace("Visiting outgoing edge" + Util.tGGRuleEdgeToString(outgoingEdge));
            TGGRuleNode srcTGGRuleNode = outgoingEdge.getSrcNode();
            TGGRuleNode trgTGGRuleNode = outgoingEdge.getTrgNode();
            if (nodesVisited.contains(trgTGGRuleNode)) { return true; }
            EObject srcNodeEObject = tggRuleNode2EObjectMapStack.get(srcTGGRuleNode);
            /*
             * Let createNode.getDomainType be THIS and the other be OTHER
             * Outgoing edges can be:
             *   * BindingType: CREATE or CONTEXT   (target nodes: CREATE or CONTEXT)
             *   * DomainType:  THIS                (source nodes: THIS)
             *
             * We ignore all edges where the target node is CREATE, since we handle that in the caller (iter over each CREATE).
             * We stay in THIS domain.
             * We look at a CONTEXT target node and try to match it (and the edge) to actual model elements. We don't treat them differently.
             */
            assert outgoingEdge.getDomainType().equals(srcTGGRuleNode.getDomainType());

            if (trgTGGRuleNode.getBindingType().equals(BindingType.CONTEXT)) {
                // here, we don't need the cross-referencing stuff!
                Set<EObject> matchingEObjects = srcNodeEObject.eClass().getEAllReferences().stream()
                        .filter(eReference -> outgoingEdge.getType().equals(eReference)) //
                        .map(eReference -> {
                            Object objectValue = srcNodeEObject.eGet(eReference);
                            Set<EObject> set = new HashSet<>();
                            if (objectValue instanceof EObject) {
                                set.add((EObject) objectValue);
                            } else if (objectValue instanceof EList<?> objectValueList) {

                                /*
                                    1. Get EChangeWrapper that holds an EChange (of type todto), where
                                       * srcTGGRUleNode is EChange.AE,
                                       * eReference     is EChange.F
                                       * objectValue    is EChange.V
                                    2. Use objectValue.get(EChange.Index)
                                    3. Not have the problem anymore (except when two nodes are added..)
                                 */

                                // might be this case never occurs --> todo nochmal überlegen!
                                Optional<InsertEReference<EObject>> eChangeOptional = getMatchedEChangeConcerning(srcNodeEObject, eReference, objectValueList);
                                if (eChangeOptional.isPresent()) {
                                    set.add((EObject) objectValueList.get(eChangeOptional.get().getIndex()));
                                } else {
                                    // okeee, problem hier ist, dass system -> protocol zwei protocol zur Auswahl hat.
                                    // Falls man protocol schon kennt (man schaue in der Map nach),
                                    // kann man das lösen...
                                    set.addAll((EList<EObject>) objectValue);
                                }
                            }
                            return set;
                        })
                        .flatMap(eObjectSet -> eObjectSet.stream())
                        .filter(trgEObjectCandidate -> trgEObjectCandidate.eClass().equals(trgTGGRuleNode.getType()))
                        .collect(Collectors.toSet());
                return recurseAndTryAllMatchingCandidatesFor(matchingEObjects, trgTGGRuleNode);
//                if (handleMatchingCandidatesFor(matchingEObjects, trgTGGRuleNode)) {
//                    return visitNode(trgTGGRuleNode);
//                } else return false;
            } else return true; // not look at CREATE nodes...
        }



        /**
         * We "fork" the DFS if we have multiple EObject candidates for mapping a context node against:<br/>
         *
         * We check each candidate by "attaching" a stack to the TGGRule-->Object map, using new DataStructure {@link MapStack} for convenience.<br/>
         * If a candidate recursion branch is unsuccessful, we <br/>
         * <ol>
         *     <li/> pop the stack until we get the tggRuleNode from before branching,
         *     <li/> remove the respective entries from the map and
         *     <li/> un-visit the nodes visited by the failed branch.
         *     <li/> try the next candidate
         * </ol>
         * <br/><br/>
         */
        private boolean recurseAndTryAllMatchingCandidatesFor(Set<EObject> eObjectCandidates, TGGRuleNode tggRuleNode) {
            logger.trace("      recurseAllMatchingCandidatesFor rule node " + Util.tGGRuleNodeToString(tggRuleNode) + "with candidates: "
                    + eObjectCandidates.stream().map(Util::eObjectToString).collect(Collectors.joining(", ")));

            if (tggRuleNode2EObjectMapStack.containsKey(tggRuleNode)
                    && eObjectCandidates.contains(tggRuleNode2EObjectMapStack.get(tggRuleNode))) {
                //it might be we have preinitialized our map with the contextnode.
                // In that case we dont need to go through the whole set, but we still need to recurse further!
                return visitNode(tggRuleNode);
            }

            for (EObject eObjectCandidate : eObjectCandidates) {
                tggRuleNode2EObjectMapStack.putPush(tggRuleNode, eObjectCandidate);
                //recurse
                if (visitNode(tggRuleNode)) {
                    //early return, no need to recurse each candidate.
                    return true;
                }
                //reset the state after failed recursion: Stack, Map and nodes visited.
                nodesVisited.removeAll(tggRuleNode2EObjectMapStack.removePopUntil(tggRuleNode));
            }
            // none match --> failure.
            return false;
        }

        /**
         * Init the map with what is already known.
         */
        private void initTggRuleNode2EObjectMap() {
            for (EObjectPlaceholder eObjectPlaceholder : getAllPlaceholders()) {
                /*
                    in the case of ReplaceSingeValuedEReference (which represents replacement but also the insertion of an value into a single-valued EReference),
                    we use null as the TGG rule placeholder for the old value, because there is no such thing as an old value in a TGG pattern, meaning we cannot assign a TGGRuleNode!
                    So here, we have to check for null nodes...
                 */
                if (eObjectPlaceholder.getTggRuleNode() != null) {
                    tggRuleNode2EObjectMapStack.putPush(eObjectPlaceholder.getTggRuleNode(), eObjectPlaceholder.getAffectedEObject());
                }
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
        private Optional<Pair<CorrespondenceNode,EObject>> getInstantiatedCorrNodeAndOthersidedEObjectCorrelatedToAMatchingEObject(
                EObject eObject, TGGRuleCorr tggRuleCorrFromRule, TGGRuleNode ruleNodeInOtherDomain) {
            logger.trace("    trying to find Eobject corrleated to " + Util.eObjectToString(eObject)
                    + "by corr=" + Util.tGGRuleNodeToString(tggRuleCorrFromRule) + ". COrrelated EObject must be " + Util.tGGRuleNodeToString(ruleNodeInOtherDomain));
            logger.trace("      containskey? " + tggResourceHandler.getCorrCaching().containsKey(eObject));
            // find all Corrs for eObject that match the given tggRuleCorrFromRule.
            if (!tggResourceHandler.getCorrCaching().containsKey(eObject)) return Optional.empty();
            logger.trace("      cachedCorr=" + Util.eSomethingToString(tggResourceHandler.getCorrCaching().get(eObject)));
            EObject deleteMe = tggResourceHandler.getCorrCaching().get(eObject).stream().findAny().get();
            logger.trace("      source=" + Util.eSomethingToString(deleteMe.eGet(deleteMe.eClass().getEStructuralFeature("source"))));
            logger.trace("      target=" + Util.eSomethingToString(deleteMe.eGet(deleteMe.eClass().getEStructuralFeature("target"))));
            //todo eGet(source) ausprobierne
            Set<CorrespondenceNode> matchingCorrespondenceNodes = tggResourceHandler.getCorrCaching().get(eObject).stream()
                    .filter(corrEObject -> corrEObject instanceof CorrespondenceNode) // in case there are others...
                    .map(corrEObject -> (CorrespondenceNode) corrEObject)
                    .filter(correspondenceNode -> tggRuleCorrFromRule.getType().equals(correspondenceNode.eClass())) // we want correspondence nodes that match the one from the rule...
                    .collect(Collectors.toSet());
            if (matchingCorrespondenceNodes.size() > 1) {
                throw new IllegalStateException("More than one correlation names " + tggRuleCorrFromRule.getName()
                        + " for eObject " + Util.eObjectToString(eObject) + " found.");
            } else if (matchingCorrespondenceNodes.size() == 1) {
                CorrespondenceNode matchingInstantiatedTGGRuleCorr = matchingCorrespondenceNodes.stream().findAny().get();
                // ensure to always return the eobject from the OTHER Domain
                EObject matchingInstantiatedNodeInOtherDomain = (EObject) matchingInstantiatedTGGRuleCorr.eGet(matchingInstantiatedTGGRuleCorr.eClass().getEStructuralFeature(
                        ruleNodeInOtherDomain.getDomainType().equals(DomainType.SRC) ? "source" : "target"
                ));
                // only return the EObject if it matches the rule node (which should be guaranteed via the corr node but who knows...)
                return ruleNodeInOtherDomain.getType().equals(matchingInstantiatedNodeInOtherDomain.eClass())
                        ? Optional.of(new Pair<>(matchingInstantiatedTGGRuleCorr, matchingInstantiatedNodeInOtherDomain))
                        : Optional.empty();
            } else return Optional.empty();
        }
    }
}
