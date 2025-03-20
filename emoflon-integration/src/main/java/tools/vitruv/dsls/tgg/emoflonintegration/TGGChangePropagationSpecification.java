package tools.vitruv.dsls.tgg.emoflonintegration;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.emoflon.ibex.tgg.operational.strategies.PropagationDirectionHolder.PropagationDirection;
import runtime.CorrespondenceNode;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.atomic.eobject.EObjectExistenceEChange;
import tools.vitruv.change.composite.MetamodelDescriptor;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.change.correspondence.Correspondence;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.change.propagation.impl.AbstractChangePropagationSpecification;
import tools.vitruv.change.utils.ResourceAccess;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.VitruviusTGGChangePropagationRegistrationHelper;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.VitruviusTGGChangePropagationIbexEntrypoint;
import tools.vitruv.dsls.tgg.emoflonintegration.ibex.VitruviusBackwardConversionTGGEngine;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Extend this class for each TGG-defined set of consistency preservation rules between two metamodels.
 * Subclasses handle ${@link VitruviusChange}s.
 */
public abstract class TGGChangePropagationSpecification extends AbstractChangePropagationSpecification {
    static Logger logger = Logger.getLogger(TGGChangePropagationSpecification.class);

    private final String SRCMetamodelPlatformUri;
    private final String TRGMetamodelPlatformUri;

    private final MetamodelDescriptor SRCMetamodelDescriptor;
    private final MetamodelDescriptor TRGMetamodelDescriptor;

    private final File ibexProjectPath;
    private final EClass targetRootEclass;
    private final URI targetRootURI;

    /**
     * Params that concern the source and target metamodel always mean that the source is where the change occurs and the target is where it should be propagated to.<br/>
     * After defining rules in Ibex, one metamodel is labelled SRC and the other TRG. We solve this duality in the following way:<br/>
     * <li/> Whenever we mean source or target in the Vitruvius sense (meaning source --> target indicating the propagation direction), we say <b>source</b> or <b>target</b>.
     * <li/> Whenever we mean source or target in the Ibex sense, we say <b>SRC</b> or <b>TRG</b>
     *
     * @param SRCMetamodelDescriptor just to identify what is SRC and what is TRG.
     * @param TRGMetamodelDescriptor just to identify what is SRC and what is TRG.
     * @param SRCMetamodelPlatformUri the platform uri of the metamodel that is labelled SRC in the ibexProject
     * @param TRGMetamodelPlatformUri the platform uri of the metamodel that is labelled TRG in the ibexProject
     * @param ibexProjectPath file system path to the eMoflon TGG project
     * @param targetRootEclass the root class for the target model to be able to create a corresponding model if none already exists. todo check if this strategy is required.
     * @param targetRootURI URI under which to persist the model created on calling {@code propagateChanges } if no corresponding model already exist.
     */
    public TGGChangePropagationSpecification(MetamodelDescriptor sourceMetamodelDescriptor, MetamodelDescriptor targetMetamodelDescriptor,
                                             MetamodelDescriptor SRCMetamodelDescriptor, MetamodelDescriptor TRGMetamodelDescriptor,
                                             String SRCMetamodelPlatformUri, String TRGMetamodelPlatformUri,
                                             File ibexProjectPath,
                                             EClass targetRootEclass, URI targetRootURI) {
        super(sourceMetamodelDescriptor, targetMetamodelDescriptor);
        this.SRCMetamodelDescriptor = SRCMetamodelDescriptor;
        this.TRGMetamodelDescriptor = TRGMetamodelDescriptor;
        this.SRCMetamodelPlatformUri = SRCMetamodelPlatformUri;
        this.TRGMetamodelPlatformUri = TRGMetamodelPlatformUri;
        this.ibexProjectPath = ibexProjectPath;
        this.targetRootEclass = targetRootEclass;
        this.targetRootURI = targetRootURI;
    }

    /**
     * @return false, This type of CPS only handles change sequences.
     */
    @Override
    public boolean doesHandleChange(EChange<EObject> eChange,
                                    EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView) {
        return false;
    }

    /**
     *  This class does not propagate single changes. Calls to this result in a warning.
     */
    @Override
    public void propagateChange(EChange<EObject> eChange,
                                EditableCorrespondenceModelView<Correspondence> editableCorrespondenceModelView,
                                ResourceAccess resourceAccess) {
        logger.warn("propagateChange not implemented, use propagateChanges (with an s).");
    }

    @Override
    public boolean doesHandleNonAtomicChanges() {
        return true;
    }

    /**
     * Propagate vitruviusChange sequences to the target model by using the tgg emoflon integration, with BackwardConversionPatternMatching.
     *
     * @param vitruviusChange the vitruviusChange sequence which is to be propagated to this CPS's target model
     */
    @Override
    public void propagateNonAtomicChange(VitruviusChange<EObject> vitruviusChange,
                                         EditableCorrespondenceModelView<Correspondence> correspondenceModel,
                                         ResourceAccess resourceAccess) {
        logger.debug("In propagateNonAtomicChange: Gotten the following EChanges: ");
        vitruviusChange.getEChanges().forEach(eChange -> logger.debug("  - " + Util.eChangeToString(eChange)));
        logger.debug(String.format("Propagate changes from %s to %s", this.getSourceMetamodelDescriptor(), this.getTargetMetamodelDescriptor()));

        // get metamodel resources
        if (this.getSourceMetamodelDescriptor().getNsUris().size() != 1) {
            throw new IllegalStateException("none or more than one source metamodel. Can only handle exactly one! " + this.getSourceMetamodelDescriptor());
        }
        if (this.getTargetMetamodelDescriptor().getNsUris().size() != 1) {
            throw new IllegalStateException("none or more than one target metamodel. Can only handle exactly one! " + this.getTargetMetamodelDescriptor());
        }
        EPackage sourceMetamodel = EPackage.Registry.INSTANCE.getEPackage(this.getSourceMetamodelDescriptor().getNsUris().stream()
                .findAny().orElseThrow(() -> new IllegalStateException("No source metamodel registered! " + this.getSourceMetamodelDescriptor()))
        );
        EPackage targetMetamodel = EPackage.Registry.INSTANCE.getEPackage(this.getTargetMetamodelDescriptor().getNsUris().stream()
                .findAny().orElseThrow(() -> new IllegalStateException("No target metamodel registered! " + this.getTargetMetamodelDescriptor()))
        );

        // get source and target models
        Resource sourceModel = findModel(vitruviusChange).orElseThrow(() -> new IllegalArgumentException("Change not related to a source model: " + vitruviusChange));
        logger.debug("In propagateNonAtomicChange: Found source model " + sourceModel);

        logger.info("------- Calling ibex -------");
        VitruviusTGGChangePropagationRegistrationHelper registrationHelper = new VitruviusTGGChangePropagationRegistrationHelper().withSRCMetamodelPackage(sourceMetamodel)
                .withTRGMetamodelPackage(targetMetamodel)
                .withSRCMetamodelPlatformUri(SRCMetamodelPlatformUri)
                .withTRGMetamodelPlatformUri(TRGMetamodelPlatformUri)
                .withIbexProjectPath(ibexProjectPath)
                .withPatternMatcher(new VitruviusBackwardConversionTGGEngine(vitruviusChange, this.getPropagationDirection()))
                .withPropagationDirection(getPropagationDirection());

        registrationHelper = getPropagationDirection().equals(PropagationDirection.FORWARD)
                ? registrationHelper.withSRCModel(sourceModel)
                : registrationHelper.withTRGModel(sourceModel);
        propagateChangesHandlingTargetModelRetrieval(sourceModel, targetMetamodel, correspondenceModel, resourceAccess,
                registrationHelper,
                vitruviusEntrypoint -> {
                    try {
                        return vitruviusEntrypoint.propagateChanges();
                    } catch (IOException e) {
                        throw new RuntimeException("Could not propagate changes via eMoflon! " + e);
                    }
                }
        );
    }

    /**
     *
     * @return what direction ibex is to be used. Remember: SRC is fixed by design decision, source and target may vary. See class Doc of {@link TGGChangePropagationSpecification}!
     */
    private PropagationDirection getPropagationDirection() {
        return this.SRCMetamodelDescriptor.equals(this.getSourceMetamodelDescriptor()) ? PropagationDirection.FORWARD : PropagationDirection.BACKWARD;
    }

    /**
     *  todo keep for a while until more tests...
     * @param sourceModel
     * @param targetModel
     */
    private void handleDanglingEObjects(Resource sourceModel, Resource targetModel) {
        logger.trace("+++handleDanglingEObjects+++");
        logger.trace("  - sourceModel: " + sourceModel.getURI());
        sourceModel.getAllContents().forEachRemaining(eObject -> {
            logger.trace("    - " + Util.eObjectToString(eObject) + ",eResource=" + eObject.eResource().getURI());
        });
        logger.trace("  - targetModel: " + targetModel.getURI());
        targetModel.getAllContents().forEachRemaining(eObject -> {
            logger.trace("    - " + Util.eObjectToString(eObject) + ",eResource=" + eObject.eResource().getURI());
        });
    }
    /**
     *
     * Tries to find a model ${@link Resource} related to a ${@link VitruviusChange}.
     * Problem: a change either inserts a root object to a model, changes an existing model element or creates/ deletes an EObject.
     * The latter type of EChange (subclasses of ${@link  EObjectExistenceEChange}) don't directly relate to a model ${@link Resource}.
     * Thus, if a ${@link VitruviusChange} only consists of ${@link  EObjectExistenceEChange}s, it cannot be related to a model ${@link Resource}.
     *
     * @return the model ${@link Resource} a ${@link VitruviusChange} relates to, if it relates to one.
     */
    private Optional<Resource> findModel(VitruviusChange<EObject> vitruviusChange) {
        /*
            1. Find whether there is any non-existence-changing change, because those are not related to a model (resource)!
               We only need to look at one.
            2. Get the affected EObject of that change (every concrete EChange has that property) and retrieve the related Resource.
         */
        AtomicReference<Optional<Resource>> resourceOptional = new AtomicReference<>();
        vitruviusChange.getEChanges().stream()
                .filter( change -> !(change instanceof EObjectExistenceEChange))
                .findAny().ifPresentOrElse(
                        (eChange) -> resourceOptional.set(Optional.ofNullable(Util.getAffectedEObjectFromEChange(eChange).eResource())),
                        () -> resourceOptional.set(Optional.empty())
                );
        return resourceOptional.get();
    }

    private void propagateChangesHandlingTargetModelRetrieval(Resource sourceModel,
                                                              EPackage targetMetamodel,
                                                              EditableCorrespondenceModelView<Correspondence> correspondenceModel,
                                                              ResourceAccess resourceAccess,
                                                              VitruviusTGGChangePropagationRegistrationHelper ibexRegistrationHelper,
                                                              Function<VitruviusTGGChangePropagationIbexEntrypoint, Set<CorrespondenceNode>> changePropgationFunction) {
        Set<CorrespondenceNode> newlyCreatedIbexCorrs;
        //  If no target model exists yet, we need to create one. There are different possible approaches. Currently, the third is in place.
        try {
            if (!modelHasCorrespondencesToResourceOfTargetMetamodel(sourceModel, targetMetamodel, correspondenceModel)) {
                logger.info("Source Model has no respective target model yet. Creating one.");
                Resource targetModel = sourceModel.getResourceSet().createResource(this.targetRootURI); // in this case, this has to be filled later!

                // do the SYNC calling
                ibexRegistrationHelper = getPropagationDirection().equals(PropagationDirection.FORWARD) // SRC == source ???
                        ? ibexRegistrationHelper.withTRGModel(targetModel)
                        : ibexRegistrationHelper.withSRCModel(targetModel);
                newlyCreatedIbexCorrs = changePropgationFunction.apply(new VitruviusTGGChangePropagationIbexEntrypoint(ibexRegistrationHelper));

                persistNewTargetRoot(targetModel, correspondenceModel, resourceAccess);
            } else {
                logger.debug("Found target model via correspondence model");
                Resource targetModel = correspondenceModel.getCorrespondingEObjects(sourceModel.getContents().getFirst()).stream()
                        .filter(correspondingEObject -> correspondingEObject.eClass().getEPackage().equals(targetMetamodel)) // we only want to look at corrs to the target metamodel
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Target model found (via correspondence model) but source model has no contents..."))
                        .eResource();
                // do the SYNC calling
                newlyCreatedIbexCorrs = changePropgationFunction.apply(new VitruviusTGGChangePropagationIbexEntrypoint(ibexRegistrationHelper.withTRGModel(targetModel)));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not set up eMoflon! " + e);
        }

        // apply the changes
        addNewlyCreatedCorrespondencesToCorrespondenceModel(newlyCreatedIbexCorrs, correspondenceModel);
        //TODO Add generating a change sequence out of the factually applied matches. This currently relies on change derivation!
//        handleDanglingEObjects(sourceModel, targetModel);
    }

    private void addNewlyCreatedCorrespondencesToCorrespondenceModel(Set<CorrespondenceNode> newlyCreatedIbexCorrs,
                                                                     EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
        logger.info(newlyCreatedIbexCorrs.isEmpty() ? "--- No correspondences added." : "-- Added the following corrs: ");
        newlyCreatedIbexCorrs.forEach(correspondenceNode -> {
                logger.info("  - " + Util.correspondenceNodeToString(correspondenceNode));
                correspondenceModel.addCorrespondenceBetween(
                        (EObject) correspondenceNode.eGet(correspondenceNode.eClass().getEStructuralFeature("source")),
                        (EObject) correspondenceNode.eGet(correspondenceNode.eClass().getEStructuralFeature("target")),
                        correspondenceNode.eClass().getName()); // todo just using the name. is that enough?
    });
    }

    /**
     *
     * @return the target model resource, based on
     * <ul>
     * <li>existing correspondences between the (known) source model and the target model</li>
     * <li>information provided by the methodologist implementing this class. The following approaches are to be tried. Currently, the third is in place:<br/>
     * First approach:<br/>
     * 1. try simply creating an empty resource<br/>
     *       2. handing that to emolfon<br/>
     *       3. gettin' the filled resource back and registering it in Vitruv<br/>
     *       ==> doesnt work, no access to resourceSet via resourceAccess<br/><br/>
     *
     *       Second approach:<br/>
     *       1. Create an artificial root (methodologist gives the EClass needed for that).<br/>
     *       2. Persist that as root in resourceAccess.<br/>
     *       3. Add a correspondence between a source object and that artificial root<br/>
     *       ==> A little hacky, but kept in commented-out code, in case third doesnt work as expected...<br/><br/>
     *
     *       Third approach:<br/>
     *       1. Create a resource for the target model via the resourceSet of the source model.<br/>
     *       2. perform change propagation.<br/>
     *       3. persist the now-existing root node as root in the resourceAccess. todo implement that step!<br/>
     *       ==> Better (not having to create artificial root), but (presumably?) Vitruvius doesn't automatically monitor the changes made to the target model.<br/>
     *       Since we want to create the Changes manually, this comes in handy, if true.
     * </li>
     * </ul>
     */

    /**
     * this persists the new mod
     */
    private void persistNewTargetRoot(Resource targetModel,
                                      EditableCorrespondenceModelView<Correspondence> correspondenceModel,
                                      ResourceAccess resourceAccess) {

        Set<EObject> potentialRoots = targetModel.getContents().stream().filter(eChange -> eChange.eClass().equals(this.targetRootEclass)).collect(Collectors.toSet());
        if (potentialRoots.isEmpty()) {
            logger.debug("No changes to the target model.");
        } else if (potentialRoots.size() == 1) {
            EObject targetRoot = potentialRoots.iterator().next();
            logger.debug("Found newly created root node " + Util.eObjectToString(targetRoot) + ". Persisting it...");
            resourceAccess.persistAsRoot(targetRoot, this.targetRootURI);
        } else throw new IllegalStateException("Multiple target roots! Don't know which to persist as root!");
    }

    private boolean modelHasCorrespondencesToResourceOfTargetMetamodel(Resource sourceModelResource, EPackage targetMetamodelPackage, EditableCorrespondenceModelView<Correspondence> correspondenceModel) {
        Set<EObject> corrs = correspondenceModel.getCorrespondingEObjects(sourceModelResource.getContents()).stream().flatMap(Collection::stream).collect(Collectors.toSet());
        return corrs.stream().anyMatch(correspondingEObject -> correspondingEObject.eClass().getEPackage().equals(targetMetamodelPackage));

    }
}
