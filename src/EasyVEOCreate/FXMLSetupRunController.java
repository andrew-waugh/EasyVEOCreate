/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EasyVEOCreate;

import FXMLGUICommon.FXMLBaseController;
import FXMLGUICommon.FXMLProgressController;
import VERSCommon.AppError;
import VERSCommon.AppFatal;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import javafx.application.HostServices;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.json.simple.JSONObject;

/**
 * FXML Controller class
 *
 * @author Andrew
 */
public class FXMLSetupRunController extends FXMLBaseController implements Initializable {

    private HostServices hostServices;

    public static final String TITLE_ABOUT = "About";
    public static final String TITLE_HELP = "Help";

    @FXML
    private AnchorPane mainAP;
    @FXML
    private TextField outputDirTF; // text field to enter the output directory
    @FXML
    private ListView<String> excFoldersLV; // list view to enter the subdirectories *not* to 
    @FXML
    private TextArea excFilesTA;
    @FXML
    private ComboBox<String> hashAlgorithmCB; // select the hash algorithm
    @FXML
    private CheckBox verboseCB;
    @FXML
    private CheckBox debugCB;
    @FXML
    private TextField pfxFileTF;
    @FXML
    private TextField archivalFileTF;
    @FXML
    private PasswordField pfxFilePasswordTF;
    @FXML
    private Button createVEOsB;
    @FXML
    private Button outputDirBrowseB;
    @FXML
    private Button pfxBrowseB;
    @FXML
    private TextField sourceDirectoryTF;
    @FXML
    private Button sourceDirectoryBrowseB;
    @FXML
    private TextField versTemplateTF;
    @FXML
    private Button versTemplateBrowseB;
    @FXML
    private Button archivalBrowseB;
    @FXML
    private Button excFolderBrowseB;
    @FXML
    private Button excFolderDelB;
    @FXML
    private CheckBox fileExtensionCB;
    @FXML
    private CheckBox includeDummyCB;
    
    Job job;    // encapsulation of the VEO creation tool
    Stage progressStage;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        progressStage = null;
        baseDirectory = new File(System.getProperty("user.dir", "/"));

        // set up behavoir of GUI elements
        sourceDirectoryTF.focusedProperty().addListener(new FocusLostListener("sourceDirectoryTF"));
        sourceDirectoryTF.setOnKeyTyped((KeyEvent keyEvent) -> {
            if (keyEvent.getCharacter().equals("\030")) {
                handleCutAction();
            } else if (keyEvent.getCharacter().equals("\003")) {
                handleCopyAction();
            } else if (keyEvent.getCharacter().equals("\026")) {
                handlePasteAction();
            }
        });
        outputDirTF.focusedProperty().addListener(new FocusLostListener("outputDirTF"));
        outputDirTF.setOnKeyTyped((KeyEvent keyEvent) -> {
            if (keyEvent.getCharacter().equals("\030")) {
                handleCutAction();
            } else if (keyEvent.getCharacter().equals("\003")) {
                handleCopyAction();
            } else if (keyEvent.getCharacter().equals("\026")) {
                handlePasteAction();
            }
        });
        versTemplateTF.focusedProperty().addListener(new FocusLostListener("versTemplateTF"));
        versTemplateTF.setOnKeyTyped((KeyEvent keyEvent) -> {
            if (keyEvent.getCharacter().equals("\030")) {
                handleCutAction();
            } else if (keyEvent.getCharacter().equals("\003")) {
                handleCopyAction();
            } else if (keyEvent.getCharacter().equals("\026")) {
                handlePasteAction();
            }
        });
        excFoldersLV.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        excFoldersLV.setOnKeyTyped(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                switch (keyEvent.getCharacter()) {
                    case "\030":
                        handleCutAction();
                        break;
                    case "\003":
                        handleCopyAction();
                        break;
                    case "\026":
                        handlePasteAction();
                        break;
                    default:
                        break;
                }
            }
        });
        excFilesTA.focusedProperty().addListener(new FocusLostListener("excFilesTA"));
        pfxFileTF.focusedProperty().addListener(new FocusLostListener("pfxFileTF"));
        pfxFileTF.setOnKeyTyped((KeyEvent keyEvent) -> {
            if (keyEvent.getCharacter().equals("\030")) {
                handleCutAction();
            } else if (keyEvent.getCharacter().equals("\003")) {
                handleCopyAction();
            } else if (keyEvent.getCharacter().equals("\026")) {
                handlePasteAction();
            }
        });
        pfxFilePasswordTF.focusedProperty().addListener(new FocusLostListener("pfxFilePasswordTF"));
        pfxFilePasswordTF.setOnKeyTyped((KeyEvent keyEvent) -> {
            if (keyEvent.getCharacter().equals("\030")) {
                handleCutAction();
            } else if (keyEvent.getCharacter().equals("\003")) {
                handleCopyAction();
            } else if (keyEvent.getCharacter().equals("\026")) {
                handlePasteAction();
            }
        });
        pfxFilePasswordTF.setOnKeyPressed((KeyEvent keyEvent) -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                String s = pfxFilePasswordTF.getText();
                if (s == null || s.trim().equals("")) {
                    job.pfxFilePassword = null;
                } else {
                    job.pfxFilePassword = s;
                }
                updateCreateButtonState();
            }
        });
        archivalFileTF.focusedProperty().addListener(new FocusLostListener("archivalFileTF"));
        archivalFileTF.setOnKeyTyped((KeyEvent keyEvent) -> {
            if (keyEvent.getCharacter().equals("\030")) {
                handleCutAction();
            } else if (keyEvent.getCharacter().equals("\003")) {
                handleCopyAction();
            } else if (keyEvent.getCharacter().equals("\026")) {
                handlePasteAction();
            }
        });
        hashAlgorithmCB.getItems().addAll("SHA-1", "SHA-256", "SHA-384", "SHA-512");
        hashAlgorithmCB.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            job.hashAlg = newValue;
        });
        verboseCB.setIndeterminate(false);
        verboseCB.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            job.verbose = newValue;
        });
        debugCB.setIndeterminate(false);
        debugCB.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            job.debug = newValue;
        });
        fileExtensionCB.setIndeterminate(false);
        fileExtensionCB.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            job.ignoreFilesWithoutExtension = newValue;
        });
        includeDummyCB.setIndeterminate(false);
        includeDummyCB.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            job.includeDummyContent = newValue;
        });
        //handleOutputFolderChangeAction(null);

        // synchronise GUI with values in job
        try {
        job = new Job("assets/defaults.json");
        } catch (AppError ae) {
            System.err.println("Panic: Exception in initialising job: "+ae.getMessage());
        }
        sync(job);

        // create the tool tips
        try {
            initTooltips();
        } catch (AppFatal af) {
            System.err.println(af.getMessage());
        }

        // can we create VEOs?
        updateCreateButtonState();
    }

    public void shutdown() {
        if (progressStage != null) {
            progressStage.close();
        }
    }

    /**
     * Put a tool tip on each control
     */
    protected void initTooltips() throws AppFatal {
        JSONObject json;

        json = openTooltips();
        createTooltip(sourceDirectoryTF, (String) json.get("sourceDirectory"));
        createTooltip(versTemplateTF, (String) json.get("versTemplateDirectory"));
        createTooltip(outputDirTF, (String) json.get("outputFolder"));
        createTooltip(excFoldersLV, (String) json.get("excludeFolders"));
        createTooltip(excFilesTA, (String) json.get("excludeFiles"));
        createTooltip(hashAlgorithmCB, (String) json.get("hashAlgorithm"));
        createTooltip(verboseCB, (String) json.get("verboseOutput"));
        createTooltip(debugCB, (String) json.get("debugOutput"));
        createTooltip(pfxFileTF, (String) json.get("pfxFile"));
        createTooltip(pfxFilePasswordTF, (String) json.get("pfxFilePassword"));
        createTooltip(archivalFileTF, (String) json.get("archivalFile"));
        createTooltip(createVEOsB, (String) json.get("createVEOs"));
        createTooltip(sourceDirectoryBrowseB, (String) json.get("browse"));
        createTooltip(outputDirBrowseB, (String) json.get("browse"));
        createTooltip(excFolderBrowseB, (String) json.get("browse"));
        createTooltip(versTemplateBrowseB, (String) json.get("browse"));
        createTooltip(excFolderDelB, (String) json.get("delete"));
        createTooltip(pfxBrowseB, (String) json.get("browse"));
        createTooltip(archivalBrowseB, (String) json.get("browse"));
        createTooltip(fileExtensionCB, (String) json.get("fileExtension"));
        createTooltip(includeDummyCB, (String) json.get("includeDummy"));
    }

    /**
     * User has selected the File/close menu item
     *
     * @param event
     */
    @FXML
    public void handleMenuFileCloseAction(ActionEvent event) {
        final Stage stage = (Stage) mainAP.getScene().getWindow();
        stage.close();
    }

    /**
     * User has selected the File/Save Job menu item. This saves the current job
     * in a JSON file for subsequent reloading
     *
     * @param event
     */
    @FXML
    private void handleMenuSaveJobAction(ActionEvent event) {
        File f;

        f = browseForSaveFile("Select Job File to save", "json", null);
        if (f == null) {
            return;
        }
        try {
            job.saveJob(f.toPath());
        } catch (AppError ae) {
            System.out.println(ae.toString());
        }
    }

    /**
     * User has selected the File/Load Job menu item. This loads a job
     * specification from the selected JSON file and syncs the state of the GUI
     * fields with the specification
     *
     * @param event
     */
    @FXML
    private void handleMenuLoadJobAction(ActionEvent event) {
        File f;

        f = browseForOpenFile("Select Job File to load", "json", null);
        if (f == null) {
            return;
        }
        try {
            job.loadJob(f.toPath());
        } catch (AppError ae) {
            System.out.println(ae.toString());
            return;
        }
        sync(job);
        updateCreateButtonState();
    }

    /**
     * This method syncs the GUI state with the current state of the job
     *
     * @param job
     */
    private void sync(Job job) {
        String s;
        int i;

        verboseCB.setSelected(job.verbose);
        debugCB.setSelected(job.debug);
        if (job.sourceDir != null) {
            s = job.sourceDir.toString();
            sourceDirectoryTF.setText(s);
            sourceDirectoryTF.positionCaret(s.length());
        }
        if (job.versTemplates != null) {
            s = job.versTemplates.toString();
            versTemplateTF.setText(s);
            versTemplateTF.positionCaret(s.length());
        }
        if (job.outputDir != null) {
            s = job.outputDir.toString();
            outputDirTF.setText(s);
            outputDirTF.positionCaret(s.length());
        }
        if (job.pfxFile != null) {
            s = job.pfxFile.toString();
            pfxFileTF.setText(s);
            pfxFileTF.positionCaret(s.length());
        }
        if (job.pfxFilePassword != null) {
            s = job.pfxFilePassword;
            pfxFilePasswordTF.setText(s);
            pfxFilePasswordTF.positionCaret(s.length());
        }
        if (job.archiveDescFile != null) {
            s = job.archiveDescFile.toString();
            archivalFileTF.setText(s);
            archivalFileTF.positionCaret(s.length());
        }
        if (job.hashAlg != null) {
            hashAlgorithmCB.getSelectionModel().select(job.hashAlg);
        }
        excFoldersLV.setItems(job.foldersToExclude);
        excFilesTA.clear();
        for (i = 0; i < job.filesToExclude.size(); i++) {
            excFilesTA.appendText(job.filesToExclude.get(i));
            excFilesTA.appendText("\n");
        }
        fileExtensionCB.setSelected(job.verbose);
        includeDummyCB.setSelected(job.verbose);
        // job.ignoreFile = null;
    }

    /**
     * User has pressed the 'Browse' button to select a folder to harvest
     */
    @FXML
    private void browseForSourceDirB(ActionEvent event) {
        File f;
        int i;
        String s;

        f = browseForDirectory("Select directory containing the VEO content", null);
        if (f == null) {
            return;
        }
        job.sourceDir = f.toPath();
        s = job.sourceDir.toString();
        sourceDirectoryTF.setText(s);
        sourceDirectoryTF.positionCaret(s.length());
        updateCreateButtonState();
    }

    /*
     * User has changed the file name in the VERS template text field
     */
    @FXML
    private void versTemplateDirChange(ActionEvent event) {
        job.versTemplates = Paths.get(versTemplateTF.getText());
        updateCreateButtonState();
    }

    /*
     * User has browsed for a new template directory
     */
    @FXML
    private void browseForVERSTemplateDirB(ActionEvent event) {
        File f;
        String s;

        f = browseForDirectory("Select Template Folder", null);
        if (f == null) {
            return;
        }
        job.versTemplates = f.toPath();
        s = job.versTemplates.toString();
        versTemplateTF.setText(s);
        versTemplateTF.positionCaret(s.length());
        updateCreateButtonState();
    }

    /*
     * User has changed the output directory in the text field
     */
    @FXML
    private void outputDirChange(ActionEvent event) {
        job.outputDir = Paths.get(outputDirTF.getText());
        updateCreateButtonState();
    }

    /*
     * User has browsed for a new Output Directory
     */
    @FXML
    private void outputDirBrowse(ActionEvent event) {
        File f;
        String s;

        f = browseForDirectory("Select Output Folder", null);
        if (f == null) {
            return;
        }
        job.outputDir = f.toPath();
        s = job.outputDir.toString();
        outputDirTF.setText(s);
        outputDirTF.positionCaret(s.length());
        updateCreateButtonState();
    }

    /*
     * User has changed the PFX file in the text field
     */
    private void pfxFileChange(ActionEvent event) {
        job.pfxFile = Paths.get(pfxFileTF.getText());
        updateCreateButtonState();
    }

    /*
     * User has browsed for a new new PFX file
     */
    @FXML
    private void pfxFileBrowse(ActionEvent event) {
        File f;
        String s;

        f = browseForOpenFile("Select PFX file", "pfx", null);
        if (f == null) {
            return;
        }
        job.pfxFile = f.toPath();
        s = job.pfxFile.toString();
        pfxFileTF.setText(s);
        pfxFileTF.positionCaret(s.length());
        updateCreateButtonState();
    }

    /*
     * User has changed the PFX file password
     */
    private void pfxFilePasswordChange(ActionEvent event) {
        job.pfxFilePassword = pfxFilePasswordTF.getText();
        updateCreateButtonState();
    }

    /*
     * User has changed the archival file in the text field
     */
    private void archivalFileChange(ActionEvent event) {
        job.archiveDescFile = Paths.get(archivalFileTF.getText());
        updateCreateButtonState();
    }

    /*
     * User has browsed for a new new archival file
     */
    @FXML
    private void archivalFileBrowse(ActionEvent event) {
        File f;
        String s;

        f = browseForOpenFile("Select archival description file", "txt", null);
        if (f == null) {
            return;
        }
        job.archiveDescFile = f.toPath();
        s = job.archiveDescFile.toString();
        archivalFileTF.setText(s);
        archivalFileTF.positionCaret(s.length());
        updateCreateButtonState();
    }

    /**
     * User has pressed the 'Browse' button to select a folder to exclude from
     * the harvest
     */
    @FXML
    private void browseForExcFolderB(ActionEvent event) {
        File f;
        int i;
        String s;

        f = browseForDirectory("Select Folder to exclude from harvest", null);
        if (f == null) {
            return;
        }
        s = f.toString();
        for (i = 0; i < job.foldersToExclude.size(); i++) {
            if (s.equals(job.foldersToExclude.get(i))) { // only put a directory in once
                return;
            }
            if (s.compareTo(job.foldersToExclude.get(i)) < 0) {
                job.foldersToExclude.add(i, s);
                break;
            }
        }
        if (i == job.foldersToExclude.size()) {
            job.foldersToExclude.add(s);
        }
        updateCreateButtonState();
    }

    /*
    * User has selected some lines in the exclude folder text field and pushed the 'delete' button
     */
    @FXML
    private void deleteExcFolderB(ActionEvent event) {
        ObservableList<Integer> toDelete;
        int i;

        toDelete = excFoldersLV.getSelectionModel().getSelectedIndices();
        for (i = 0; i < toDelete.size(); i++) {
            job.foldersToExclude.remove(toDelete.get(i).intValue());
        }
        updateCreateButtonState();
    }

    @FXML
    private void templateDirChange(ActionEvent event) {
    }


    /**
     * Respond to focus lost events. A new instance of this class is created for
     * each object, with its name passed as a parameter. When focus is lost in
     * the object, the associated listener instance is called. If newValue is
     * false, this is a lost focus event.
     */
    private class FocusLostListener implements ChangeListener<Boolean> {

        String type;

        FocusLostListener(String type) {
            this.type = type;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            String s;
            int i;

            if (newValue == false) {
                switch (type) {
                    case "outputDirTF":
                        s = outputDirTF.getText();
                        if (s == null || s.trim().equals("")) {
                            job.outputDir = null;
                        } else {
                            job.outputDir = Paths.get(s);
                        }
                        break;
                    case "versTemplateTF":
                        s = versTemplateTF.getText();
                        if (s == null || s.trim().equals("")) {
                            job.versTemplates = null;
                        } else {
                            job.versTemplates = Paths.get(s);
                        }
                        break;
                    case "pfxFileTF":
                        s = pfxFileTF.getText();
                        if (s == null || s.trim().equals("")) {
                            job.pfxFile = null;
                        } else {
                            job.pfxFile = Paths.get(s);
                        }
                        break;
                    case "pfxFilePasswordTF":
                        s = pfxFilePasswordTF.getText();
                        if (s == null || s.trim().equals("")) {
                            job.pfxFilePassword = null;
                        } else {
                            job.pfxFilePassword = s;
                        }
                        break;
                    case "archivalFileTF":
                        s = archivalFileTF.getText();
                        if (s == null || s.trim().equals("")) {
                            job.archiveDescFile = null;
                        } else {
                            job.archiveDescFile = Paths.get(s);
                        }
                        break;
                    case "excFilesTA":
                        String[] lines;

                        lines = excFilesTA.getText().split("\n");
                        job.filesToExclude.clear();
                        for (i = 0; i < lines.length; i++) {
                            if (lines[i].equals("") || lines[i].trim().equals("")) {
                                continue;
                            }
                            job.filesToExclude.add(lines[i]);
                        }
                    default:
                        break;
                }
            }
            updateCreateButtonState();
        }
    }

    /**
     * Check to see if all the information necessary to create VEOs has been
     * entered. If so, make the createVEOs button active
     */
    private void updateCreateButtonState() {
        boolean isValid = job.validate();
        createVEOsB.setDisable(!isValid);
    }

    /**
     * User has pressed the 'Create VEOs' button at the bottom of the window.
     * This button is only active if sufficient information has been entered.
     *
     * @param event
     */
    @FXML
    private void createVEOs(ActionEvent event) {
        Parent root;
        FXMLProgressController controller;
        Alert a;

        // create instance of class that will actually do the work
        try {
            job.task = new EasyVEOCreate(job);
        } catch (AppFatal ap) {
            a = alertWarning("Failed", "Failed creating EasyVEOCreate()", ap.getMessage());
            a.showAndWait();
            return;
        }

        // count the number of VEOs to create
        job.totalObjects = job.task.countUnitsOfWork();

        // fire up window
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXMLGUICommon/FXMLProgress.fxml"));
        try {
            root = (Parent) fxmlLoader.load();
        } catch (IOException ioe) {
            System.out.println("Failed getting the root for progress controller: " + ioe.toString());
            return;
        }
        controller = fxmlLoader.getController();
        controller.setHostServices(hostServices);
        controller.generate(job, baseDirectory);

        progressStage = new Stage();
        progressStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                // System.exit(0);
            }
        });
        progressStage.setTitle("Creating VEOs Progress");
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/FXMLGUICommon/ControllerStyles.css").toExternalForm());
        progressStage.setScene(scene);
        progressStage.setOnHidden(e -> controller.shutdown());
        progressStage.showAndWait();
        progressStage = null;
    }

    /**
     * User has selected the Edit/Cut menu item. This cuts the selected text
     * from the focussed TextField into a Clipboard
     *
     * @param event
     */
    @FXML
    private void handleCutAction(ActionEvent event) {
        handleCutAction();
    }

    private void handleCutAction() {
        Node n;
        String s;
        int i;
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();

        n = mainAP.getScene().getFocusOwner();
        if (n instanceof TextField) {
            TextField tf = (TextField) n;
            tf.cut();
        } else if (n instanceof ListView) {
            ListView lv = (ListView) n;
            if ((i = lv.getSelectionModel().getSelectedIndex()) == -1) {
                return;
            } else if (lv == excFoldersLV) {
                s = excFoldersLV.getItems().get(i);
                content.putString(s);
                clipboard.setContent(content);
                excFoldersLV.getItems().remove(i);
            }
        }
    }

    /**
     * User has selected the Edit/Copy menu item. This cuts the selected text
     * from the focussed TextField into a Clipboard
     *
     * @param event
     */
    @FXML
    private void handleCopyAction(ActionEvent event) {
        handleCopyAction();
    }

    private void handleCopyAction() {
        Node n;
        String s;
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        int i;

        n = mainAP.getScene().getFocusOwner();
        if (n instanceof TextField) {
            TextField tf = (TextField) n;
            tf.copy();
        } else if (n instanceof ListView) {
            ListView lv = (ListView) n;
            if ((i = lv.getSelectionModel().getSelectedIndex()) == -1) {
                return;
            } else if (lv == excFoldersLV) {
                s = excFoldersLV.getItems().get(i);
                content.putString(s);
                clipboard.setContent(content);
            }
        }
    }

    /**
     * User has selected the Edit/Paste action.
     *
     * @param event
     */
    @FXML
    private void handlePasteAction(ActionEvent event) {
        handlePasteAction();
    }

    private void handlePasteAction() {
        Node n;
        String s;
        int i;
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();

        n = mainAP.getScene().getFocusOwner();
        if (n instanceof TextField) {
            TextField tf = (TextField) n;
            tf.paste();
        } else if (n instanceof ListView) {
            ListView lv = (ListView) n;
            if (lv == excFoldersLV) {
                s = clipboard.getString();
                if (s != null) {
                    for (i = 0; i < job.foldersToExclude.size(); i++) {
                        if (s.equals(job.foldersToExclude.get(i))) { // only put a directory in once
                            return;
                        }
                        if (s.compareTo(job.foldersToExclude.get(i)) < 0) {
                            job.foldersToExclude.add(i, s);
                            break;
                        }
                    }
                    if (i == job.foldersToExclude.size()) {
                        job.foldersToExclude.add(s);
                    }
                }
            }
        }
    }
    
    /**
     * Pop up a window that explains what this program is
     *
     * @param event
     * @throws Exception
     */
    @FXML
    private void handleAboutAction(ActionEvent event) throws Exception {
        displayHTMLFile("Easy VEO Creator - About", "/about.html", "aj");
    }

    /**
     * Callback to open online help
     *
     * @param event
     * @throws Exception
     */
    @FXML
    private void handleOnlineHelpAction(ActionEvent event) {
        displayHTMLFile("Easy VEO Creator - Help", "/help.html", "aj");
    }
}
