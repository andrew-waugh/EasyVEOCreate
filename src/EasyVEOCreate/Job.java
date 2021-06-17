/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EasyVEOCreate;

import FXMLGUICommon.JobBase;
import VERSCommon.AppError;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * This class encapsulates the parameters of the VEO generation
 *
 * @author Andrew
 */
public final class Job extends JobBase {
    Path sourceDir;                 // directory in which to find the VEO contents
    Path outputDir;                 // directory where the VEOs are to be created
    Path versTemplates;             // VERS template directory
    Path pfxFile;                   // PFX file to sign VEOs
    String pfxFilePassword;         // password of PFX file
    String hashAlg;                 // hash algorithm to use
    ObservableList<String> foldersToExclude; // list of subdirectories to exclude from harvest 
    ArrayList<String> filesToExclude; // list of patterns of file names to exclude from harvest
    boolean ignoreFilesWithoutExtension; // if true, don't include any content files without an extension
    Path archiveDescFile;           // contains an archival description of the creation
    boolean includeDummyContent;    // if true, include a dummy content file if the real file to be added is not a LTSF
    Path dummyContentFile;          // file to be added in case of content that is not a LTSF

    /**
     * Constructor. Note that this also sets the basic default parameters in
     * code, and then loads a default job file over the top of this (if
     * specified)
     *
     * @param defaultFile file where default values can be found
     * @throws VERSCommon.AppError if the default file had errors
     */
    public Job(String defaultFile) throws AppError {
        super();
        title = "Easy VEO Create";  // cannot be overwritten by a job file
        version = "1.0 (2021)";     // cannot be overwritten by a job file
        clear();
        outputDir = Paths.get(".");
        if (defaultFile != null) {
            loadJob(Paths.get(defaultFile));
        }
    }

    /**
     * Free the resources associated with this object;
     */
    @Override
    public void free() {
        super.free();
        clear();
        foldersToExclude = null;
        filesToExclude = null;
    }
    
    /**
     * Clear the user variables
     */
    private void clear() {
        sourceDir = null;
        outputDir = null;
        versTemplates = null;
        pfxFile = null;
        pfxFilePassword = null;
        hashAlg = "SHA-512";
        if (foldersToExclude == null) {
            foldersToExclude = FXCollections.observableArrayList();
        } else {
            foldersToExclude.clear();
        }
        if (filesToExclude == null) {
            filesToExclude = new ArrayList<>();
        } else {
            filesToExclude.clear();
        }
        ignoreFilesWithoutExtension = true;
        archiveDescFile = null;
        includeDummyContent = true;
        dummyContentFile = null;
    }

    /**
     * Check to see if sufficient information has been entered to generate VEOs
     *
     * @return
     */
    @Override
    public boolean validate() {
        if (sourceDir == null) {
            return false;
        }
        if (outputDir == null) {
            return false;
        }
        if (versTemplates == null) {
            return false;
        }
        if (includeDummyContent && dummyContentFile == null) {
            return false;
        }
        if (pfxFile != null && (pfxFilePassword == null || pfxFilePassword.equals("") || pfxFilePassword.trim().equals(" "))) {
            return false;
        }
        return true;
    }

    /**
     * Create a JSON file capturing the Job. Note that this must save the fields
     * in the JobBase if they are necessary.
     *
     * @param file
     * @throws AppError
     */
    @Override
    public void saveJob(Path file) throws AppError {
        JSONObject j1, j2;
        JSONArray ja1;
        int i;

        j1 = new JSONObject();

        if (title != null) {
            j1.put("application", title);
        }
        if (version != null) {
            j1.put("version", version);
        }
        if (sourceDir != null) {
            j1.put("sourceDirectory", sourceDir.toString());
        }
        if (outputDir != null) {
            j1.put("outputDirectory", outputDir.toString());
        }
        if (versTemplates != null) {
            j1.put("versTemplates", versTemplates.toString());
        }
        if (pfxFile != null) {
            j1.put("pfxFile", pfxFile.toString());
        }
        if (pfxFilePassword != null) {
            j1.put("pfxPassword", pfxFilePassword);
        }
        if (hashAlg != null) {
            j1.put("hashAlgorithm", hashAlg);
        }
        if (logFile != null) {
            j1.put("logFile", logFile.toString());
        }
        if (foldersToExclude != null && foldersToExclude.size() > 0) {
            ja1 = new JSONArray();
            for (i = 0; i < foldersToExclude.size(); i++) {
                j2 = new JSONObject();
                j2.put("folder", foldersToExclude.get(i));
                ja1.add(j2);
            }
            j1.put("foldersToExclude", ja1);
        }
        if (filesToExclude != null && filesToExclude.size() > 0) {
            ja1 = new JSONArray();
            for (i = 0; i < filesToExclude.size(); i++) {
                j2 = new JSONObject();
                j2.put("filePattern", filesToExclude.get(i));
                ja1.add(j2);
            }
            j1.put("filesToExclude", ja1);
        }
        j1.put("ignoreFilesWithoutExtension", ignoreFilesWithoutExtension);
        if (archiveDescFile != null) {
            j1.put("archivalDescriptionFile", archiveDescFile.toString());
        }
        j1.put("includeDummyContent", includeDummyContent);
        if (dummyContentFile != null) {
            j1.put("dummyContentFile", dummyContentFile.toString());
        }
        saveJSONObject(file, j1);
    }

    /**
     * Read a JSON file describing the Job to run
     *
     * @param file file containing the job file
     * @throws AppError
     */
    public void loadJob(Path file) throws AppError {
        JSONObject j1, j2;
        JSONArray ja1;
        int i;
        String s;
        Object o;

        j1 = loadJSONObject(file);
        if ((s = (String) j1.get("sourceDirectory")) != null) {
            sourceDir = Paths.get(s);
        }
        if ((s = (String) j1.get("outputDirectory")) != null) {
            outputDir = Paths.get(s);
        }
        if ((s = (String) j1.get("pfxFile")) != null) {
            pfxFile = Paths.get(s);
        }
        if ((s = (String) j1.get("pfxPassword")) != null) {
            pfxFilePassword = s;
        }
        if ((s = (String) j1.get("hashAlgorithm")) != null) {
            hashAlg = s;
        }
        ja1 = (JSONArray) j1.get("foldersToExclude");
        if (ja1 != null) {
            foldersToExclude.clear();
            for (i = 0; i < ja1.size(); i++) {
                j2 = (JSONObject) ja1.get(i);
                foldersToExclude.add((String) j2.get("folder"));
            }
        }
        ja1 = (JSONArray) j1.get("filesToExclude");
        if (ja1 != null) {
            filesToExclude.clear();
            for (i = 0; i < ja1.size(); i++) {
                j2 = (JSONObject) ja1.get(i);
                filesToExclude.add((String) j2.get("filePattern"));
            }
        }
        if ((o = j1.get("ignoreFilesWithoutExtension")) != null) {
            ignoreFilesWithoutExtension = ((Boolean) o);
        }
        if ((s = (String) j1.get("archivalDescriptionFile")) != null) {
            archiveDescFile = Paths.get(s);
        }
        if ((o = j1.get("includeDummyContent")) != null) {
            includeDummyContent = ((Boolean) o);
        }
        if ((s = (String) j1.get("dummyContentFile")) != null) {
            dummyContentFile = Paths.get(s);
        }

        if ((s = (String) j1.get("versTemplates")) != null) {
            versTemplates = Paths.get(s);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int i;

        sb.append("Application:" + title);
        sb.append(" (version:" + version + ")\n");
        sb.append("Source directory: " + ((sourceDir == null) ? "not specified" : sourceDir.toString()) + "\n");
        sb.append("Output directory: " + ((outputDir == null) ? "not specified" : outputDir.toString()) + "\n");
        sb.append("VERS template directory: " + ((versTemplates == null) ? "not specified" : versTemplates.toString()) + "\n");
        sb.append("PFX file: " + ((pfxFile == null) ? "not specified" : pfxFile.toString()) + "\n");
        if (verbose) { // slight security feature
            sb.append("PFX file password: '" + pfxFilePassword + "'\n");
        }
        sb.append("Hash algorithm: " + hashAlg + "\n");
        sb.append("Folder to exclude: ");
        if (foldersToExclude != null && foldersToExclude.size() > 0) {
            sb.append("\n");
            for (i = 0; i < foldersToExclude.size(); i++) {
                sb.append("\t" + foldersToExclude.get(i) + "\n");
            }
        } else {
            sb.append("none\n");
        }
        sb.append("File patterns to exclude: ");
        if (filesToExclude != null && filesToExclude.size() > 0) {
            sb.append("\n");
            for (i = 0; i < filesToExclude.size(); i++) {
                sb.append("\t'" + filesToExclude.get(i) + "'\n");
            }
        } else {
            sb.append("none\n");
        }
        sb.append("Ignore files without file extensions: " + ignoreFilesWithoutExtension + "\n");
        sb.append("ArchiveDescFile: " + ((archiveDescFile == null) ? "not specified" : archiveDescFile.toString()) + "\n");
        sb.append("Include dummy file for non LTSF content files: " + includeDummyContent + "\n");
        if (includeDummyContent) {
            sb.append("Dummy File: " + ((dummyContentFile == null) ? "not specified" : dummyContentFile.toString()) + "\n");
        }
        sb.append("Verbose logging: " + verbose + "\n");
        sb.append("Debug logging: " + debug + "\n");
        sb.append("Log file:" + ((logFile == null) ? "null" : logFile.toString()) + "\n");
        return sb.toString();
    }
}
