package tools.vitruv.dsls.tgg.emoflonintegration.ibex.smartEmfFix;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.emoflon.smartemf.persistence.SmartEMFResource;
import org.emoflon.smartemf.persistence.SmartEMFResourceFactoryImpl;

public class SmartEMFResourceFactoryImplFixed extends SmartEMFResourceFactoryImpl {
    private String workspacePath;
    public SmartEMFResourceFactoryImplFixed(String workspacePath) {
        super(workspacePath);
        this.workspacePath = workspacePath;
    }

    @Override
    public Resource createResource(URI uri) {
        return new SmartEMFResourceFixed(uri, this.workspacePath);
    }
}
