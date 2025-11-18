package edu.sjsu.android.servicesfinder.controller;

import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.sjsu.android.servicesfinder.database.CatalogueDatabase;
import edu.sjsu.android.servicesfinder.database.ServiceDatabase;
import edu.sjsu.android.servicesfinder.model.Catalogue;
import edu.sjsu.android.servicesfinder.model.Service;

/* =========================================================
 * CONTROLLER FOR CATALOGUE BUSINESS LOGIC
 * COORDINATES BETWEEN VIEW AND DATABASE LAYERS
 ***********************************************************/
public class CatalogueController {

    private final CatalogueDatabase catalogueDatabase;
    private CatalogueControllerListener listener;

    public CatalogueController() {
        this.catalogueDatabase = new CatalogueDatabase();
    }

    public void setListener(CatalogueControllerListener listener) {
        this.listener = listener;
    }

    /* ****************************************************************************
     * LOAD CATALOGUE MAP FOR DROPDOWN (CATALOGUE TITLE -> LIST OF SERVICE NAMES)
     * THIS IS FORMATTED SPECIFICALLY FOR MULTISELECTDROPDOWN
     * USES EMBEDDED SERVICES ARRAY FROM CATALOGUE DOCUMENTS
     ***********************************************************************/

    public void loadCatalogueMapForDropdown() {
        catalogueDatabase.getCatalogueMapWithEmbeddedServices(new CatalogueDatabase.OnCatalogueMapLoadedListener() {
            @Override
            public void onSuccess(Map<String, List<String>> catalogueMap) {
                if (listener != null) {
                    listener.onCatalogueMapLoaded(catalogueMap);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (listener != null) {
                    listener.onError(errorMessage);
                }
            }
        });
    }

    /* *********************************************************************
     * LISTENER INTERFACE FOR CALLBACKS TO VIEW
     **************************************************************************/
    public interface CatalogueControllerListener {
        void onCataloguesLoaded(List<Catalogue> catalogues);
        void onCatalogueWithServicesLoaded(Catalogue catalogue, List<Service> services);
        void onCatalogueMapLoaded(Map<String, List<String>> catalogueMap);
        void onError(String errorMessage);
    }
}