/*
 * MainController.java - Copyright(c) 2013 Joe Pasqua
 * Provided under the MIT License. See the LICENSE file for details.
 * Created: Jul 22, 2013
 */

package org.noroomattheinn.visibletesla;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Dialogs;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.noroomattheinn.tesla.Options;
import org.noroomattheinn.tesla.Tesla;
import org.noroomattheinn.tesla.Vehicle;
import org.noroomattheinn.utils.Utils;

// TO DO:
// * If the user has more than one vehicle on their account, the app should pop up a list and
//   them to select a specific car. Perhaps represent them by vin and small colored car icon
//

public class MainController {
    private static final String ProductName = "VisibleTesla";
    private static final String ProductVersion = "0.18";
    
    //  The Tesla, Vehicle, and App Objects
    private Tesla tesla;
    private List<Vehicle> vehicles;
    private Vehicle selectedVehicle;
    private Application app;
    private Stage stage;
    
    // Boilerplate FXML instance variables
    @FXML private ResourceBundle resources;
    @FXML private URL location;
    
    // The top level AnchorPane and the TabPane that sits inside it
    @FXML private AnchorPane root;
    @FXML private TabPane tabPane;

    // The individual tabs that comprise the overall UI
    @FXML private Tab graphTab;
    @FXML private Tab chargeTab;
    @FXML private Tab hvacTab;
    @FXML private Tab locationTab;
    @FXML private Tab loginTab;
    @FXML private Tab overviewTab;
    
    /**
     * Implementation of the File->Close menu option
     * @param event     The event generated by File->Close
     */
    @FXML void closeHandler(ActionEvent event) {
        Platform.exit();
    }
    
    /**
     * Handles controller-specific initialization such as adding a listener on
     * tab selection changes and setting up our userData.
     */
    @FXML void initialize() {
        tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            public void changed(ObservableValue<? extends Tab> ov, Tab t, Tab t1) {
                BaseController c = controllerFromTab(t1);
                if (c != null) c.activate(selectedVehicle);
            }
        });
        
        root.setUserData(this);
    }

    /**
     * Set all of the tabs (except the Login tab) as either enabled or disabled.
     * The login tab is always enabled.
     * @param enabled     Indicates whether the tabs should be enabled or disabled.
     */
    public void setTabsEnabled(boolean enabled) {
        graphTab.setDisable(!enabled);
        chargeTab.setDisable(!enabled);
        hvacTab.setDisable(!enabled);
        locationTab.setDisable(!enabled);
        loginTab.setDisable(false);     // The Login Tab is always enabled
        overviewTab.setDisable(!enabled);
    }
    
    /**
     * Called by the main application to allow us to store away the app context
     * and perform any other app startup tasks. In particular, we (1) distribute
     * app context to all of the controllers, and (2) we set a listener for login
     * completion and try and automatic login.
     * @param a 
     */
    public void start(Application a, Stage s) {
        this.app = a;   // Set the application context for all the tabs...
        this.stage = s; // And remember the top-level stage
        stage.setTitle(ProductName + " " + ProductVersion);
        stage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream(
                "org/noroomattheinn/TeslaResources/Icon-72@2x.png")));

        tesla = new Tesla();
        LoginController lc = ((LoginController)loginTab.getContent().getUserData());
        BooleanProperty loginState = lc.getLoginCompleteProperty();
        loginState.addListener(new HandleLoginEvent());
        lc.attemptAutoLogin(tesla);
        
        controllerFromTab(chargeTab).setAppContext(a, stage);
        controllerFromTab(hvacTab).setAppContext(a, stage);
        controllerFromTab(locationTab).setAppContext(a, stage);
        controllerFromTab(overviewTab).setAppContext(a, stage);
    }
    
    /**
     * Whenever we try to login, a boolean property is set with the result of
     * the attempt. We monitor changes on that boolean property and if we see
     * a successful login, we gather the appropriate state and make the app
     * ready to go.
     */
    class HandleLoginEvent implements ChangeListener<Boolean> {
        @Override public void changed(
                ObservableValue<? extends Boolean> observable,
                Boolean oldValue, Boolean newValue) {
            if (newValue) {
                vehicles = tesla.getVehicles();
                selectedVehicle = vehicles.get(0);  // TO DO: Allow user to select vehicle from list
                setTabsEnabled(true);
                jumpToTab(overviewTab);
            } else {
                vehicles = null;
                selectedVehicle = null;
                setTabsEnabled(false);
            }
        }
    }

    private void jumpToTab(final Tab tab) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                tabPane.getSelectionModel().select(tab);    // Jump to Overview Tab     
            }
        });
    }

    /**
     * Utility method that returns the BaseController object associated with
     * a given tab. It does this by extracting the userData object which each
     * BaseController sets to itself.
     * @param   t   The tab for which we want the BaseController
     * @return      The BaseController
     */
    private BaseController controllerFromTab(Tab t) {
        Object userData = t.getContent().getUserData();
        return (userData instanceof BaseController) ? (BaseController)userData : null;
    }
    
    @FXML private void aboutHandler(ActionEvent event) {
        Dialogs.showInformationDialog(
                stage,
                "Copyright (c) 2013, Joe Pasqua\n" +
                "Free for personal and non-commercial use.\n" +
                "Based on the great API detective work of many members\n" +
                "of teslamotorsclub.com.  All Tesla imagery derives\n" +
                "from the official Tesla iPhone app.",
                ProductName + " " + ProductVersion, "About " + ProductName);
    }

    //
    // Everything below has to do with simulated vehicle properties. For example,
    // the user can ask to simulate a different car color, roof type, or wheel
    // type. The simulation options are selected here and implemented in the
    // OverviewController. They could be implemented in the lower level state
    // objects, but I decided to keep those unsullied by this stuff.
    //
    // TO DO: This way of handling the simulation feels clunky. Perhaps do it
    // with observable properties instead.
    
    @FXML MenuItem simColorRed, simColorBlue, simColorGreen, simColorBrown, simColorBlack;
    @FXML MenuItem simColorSigRed, simColorSilver, simColorGray, simColorPearl, simColorWhite;
    @FXML MenuItem darkRims, lightRims, silver19Rims;
    @FXML MenuItem simSolidRoof, simPanoRoof, simBlackRoof;
    @FXML private MenuItem simImperialUnits, simMetricUnits;

    @FXML void simUnitsHandler(ActionEvent event) {
        ChargeController cc = (ChargeController)controllerFromTab(chargeTab);
        HVACController hc = (HVACController)controllerFromTab(hvacTab);
        MenuItem source = (MenuItem)event.getSource();
        Utils.UnitType ut = (source == simImperialUnits) ? Utils.UnitType.Imperial : Utils.UnitType.Metric;
        cc.setSimulatedUnits(ut);
        hc.setSimulatedUnits(ut);
    }
    
    @FXML void simRimsHandler(ActionEvent event) {
        OverviewController oc = (OverviewController)controllerFromTab(overviewTab);
        HVACController hc = (HVACController)controllerFromTab(hvacTab);
        MenuItem source = (MenuItem)event.getSource();
        Options.WheelType simWheels = null;
        if (source == darkRims) simWheels = Options.WheelType.WTSP;
        else if (source == lightRims) simWheels = Options.WheelType.WT21;
        else if (source == silver19Rims) simWheels = Options.WheelType.WT19;
        if (simWheels != null) {
            oc.setSimulatedWheels(simWheels);
            hc.setSimulatedWheels(simWheels);
        }
    }
    
    @FXML void simColorHandler(ActionEvent event) {
        OverviewController oc = (OverviewController)controllerFromTab(overviewTab);
        MenuItem source = (MenuItem)event.getSource();
        if (source == simColorRed) oc.setSimulatedColor(Options.PaintColor.PPMR);
        else if (source == simColorGreen) oc.setSimulatedColor(Options.PaintColor.PMSG);
        else if (source == simColorBlue) oc.setSimulatedColor(Options.PaintColor.PMMB);
        else if (source == simColorBlack) oc.setSimulatedColor(Options.PaintColor.PBSB);
        else if (source == simColorSilver) oc.setSimulatedColor(Options.PaintColor.PMSS);
        else if (source == simColorGray) oc.setSimulatedColor(Options.PaintColor.PMTG);
        else if (source == simColorSigRed) oc.setSimulatedColor(Options.PaintColor.PPSR);
        else if (source == simColorPearl) oc.setSimulatedColor(Options.PaintColor.PPSW);
        else if (source == simColorBrown) oc.setSimulatedColor(Options.PaintColor.PMAB);
        else if (source == simColorWhite) oc.setSimulatedColor(Options.PaintColor.PPSW);
    }

    @FXML void simRoofHandler(ActionEvent event) {
        OverviewController oc = (OverviewController)controllerFromTab(overviewTab);
        MenuItem source = (MenuItem)event.getSource();
        if (source == simSolidRoof) oc.setSimulatedRoof(Options.RoofType.RFBC);
        else if (source == simBlackRoof) oc.setSimulatedRoof(Options.RoofType.RFBK);
        else if (source == simPanoRoof) oc.setSimulatedRoof(Options.RoofType.RFPO);
    }
 
}
