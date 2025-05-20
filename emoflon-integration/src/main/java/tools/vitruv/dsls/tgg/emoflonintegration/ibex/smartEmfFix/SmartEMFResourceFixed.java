package tools.vitruv.dsls.tgg.emoflonintegration.ibex.smartEmfFix;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.emoflon.smartemf.persistence.AdapterList;
import org.emoflon.smartemf.persistence.SmartEMFResource;

/**
 * A bugfix class that replaces the buggy class {@link AdapterList} with {@link AdapterListFixed}.
 */
public class SmartEMFResourceFixed extends SmartEMFResource {

    private AdapterListFixed adapters = new AdapterListFixed(this);
    public SmartEMFResourceFixed(URI uri, String workspacePath) {
        super(uri, workspacePath);
    }

    @Override
    public EList<Adapter> eAdapters() {
        return this.adapters;
    }

}
