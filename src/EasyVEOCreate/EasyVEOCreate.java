/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EasyVEOCreate;

import FXMLGUICommon.FXMLProgressController;
import FXMLGUICommon.FXMLTask;
import VEOCreate.CreateVEO;
import VERSCommon.AppError;
import VERSCommon.AppFatal;
import VERSCommon.LTSF;
import VERSCommon.PFXUser;
import VERSCommon.VEOError;
import VERSCommon.VEOFatal;
import VERSCommon.XMLParser;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author Andrew
 */
public class EasyVEOCreate extends FXMLTask {

    private static String classname = "EasyVEOCreate"; // for reporting
    private static ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private Runtime r;

    // global variables storing information about this export (as a whole)
    private Job job;                // details of the job to be undertaken
    private String userId;          // user performing the converstion

    private boolean ignoreFileWithNoExtension; // if true, don't harvest any files with no file extension
    private TreeMap<String, String> directoriesIgnored; // list of directories to ignore (keep)
    private ArrayList<Pattern> ignorePatterns; // list of file name patterns to ignore (keep)

    private LTSF ltsf;              // valid long term sustainable formats
    private PFXUser user;           // User that will sign the VEOs
    private String archivalDesc;    // precanned description of this harvesting
    private boolean addedDummyLTPF; // true if dummy LTPF has been added to VEO
    private int iocnt;              // count of the information objects added (used to make a unique id

    // private final static Logger rootLog = Logger.getLogger("EasyVEOCreate");
    private final static Logger LOG = Logger.getLogger("FileHarvest.FileHarvestAnalysis");

    /**
     * Constructor called when running from the command line with the Job
     * specified on the command line.
     *
     * @param args arguments passed to program
     * @throws AppFatal if a fatal error occurred
     */
    public EasyVEOCreate(String args[]) throws AppFatal {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");

        // process command line arguments
        configure(args);

        // set logging
        LOG.setLevel(Level.WARNING);
        if (job.verbose) {
            LOG.setLevel(Level.INFO);
        }
        if (job.debug) {
            LOG.setLevel(Level.FINE);
        }

        // setup
        constructorCommon();

        // log generic things
        LOG.log(Level.INFO, job.toString());
    }

    /**
     * Constructor called when configuring using a Job specification (typically
     * from the GUI)
     *
     * @param j the Job specification
     * @throws AppFatal something went wrong
     */
    public EasyVEOCreate(Job j) throws AppFatal {
        job = j;
        constructorCommon();
    }

    /**
     * Common code for constructor irrespective of how the class is instatiated
     *
     * @throws AppFatal
     */
    private void constructorCommon() throws AppFatal {
        int i;

        // sanity checks
        if (job == null) {
            throw new AppFatal("Job was null");
        }
        if (job.pfxFile == null) {
            throw new AppFatal("A PFX file must be specified");
        }

        // set up and configure
        setup();
        checkFile("VERS support directory", job.versTemplates, true);
        checkFile("PFX file", job.pfxFile, false);
        try {
            user = new PFXUser(job.pfxFile.toString(), job.pfxFilePassword);
        } catch (VEOError ve) {
            throw new AppFatal(ve.toString());
        }
        checkFile("output directory", job.outputDir, true);

        //process the excluded files
        for (i = 0; i < job.filesToExclude.size(); i++) {
            try {
                ignorePatterns.add(Pattern.compile(job.filesToExclude.get(i)));
            } catch (PatternSyntaxException pse) {
                LOG.log(Level.WARNING, "Invalid pattern: ''{0}'' when ignoring files: {1}", new Object[]{job.filesToExclude.get(i), pse.getMessage()});
            }
        }

        //process the excluded folders
        for (i = 0; i < job.foldersToExclude.size(); i++) {
            String s = job.foldersToExclude.get(i);
            if (!directoriesIgnored.containsKey(s)) {
                directoriesIgnored.put(s, s);
            }
        }

        // read valid long term preservation formats
        try {
            ltsf = new LTSF(job.versTemplates.resolve("validLTSF.txt"));
        } catch (VEOFatal vf) {
            throw new AppFatal(vf.getMessage());
        }

        // read description file
        if (job.archiveDescFile != null) {
            archivalDesc = readDescFile();
        } else {
            archivalDesc = "No archival description";
        }
    }

    /**
     * This call registers a Handler and a callback for doing the work. The
     * Handler captures output, which is reported back to the callback
     *
     * @param hndlr Handler to receive log messages
     * @param reporting
     * @throws AppFatal
     */
    @Override
    public void register(Handler hndlr, FXMLProgressController.DoTask reporting) throws AppFatal {
        Handler h[];
        int i;

        // remove any handlers associated with the LOG & log messages aren't to
        // go to the parent
        h = LOG.getHandlers();
        for (i = 0; i < h.length; i++) {
            LOG.removeHandler(h[i]);
        }
        LOG.setUseParentHandlers(false);

        // add log handler from calling program
        LOG.addHandler(hndlr);
        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s");

        // remember the callback
        job.callback = reporting;
    }

    /**
     * Initialise all the global variables
     */
    final void setup() {

        // set up default global variables
        LOG.setLevel(Level.WARNING);
        ignoreFileWithNoExtension = false;
        directoriesIgnored = new TreeMap<>();
        ignorePatterns = new ArrayList<>();
        userId = System.getProperty("user.name");
        if (userId == null) {
            userId = "Unknown user";
        }
        user = null;
        addedDummyLTPF = false;
        iocnt = 1;

        // variables for the whole processing
        r = Runtime.getRuntime();
    }

    /**
     * Configure
     *
     * This method gets the options for this run of the file harvester from the
     * command line. See the comment at the start of this file for the command
     * line arguments.
     *
     * @param args[] the command line arguments
     * @param Exception if a fatal error occurred
     */
    private void configure(String args[]) throws AppFatal {
        Path p;
        int i;
        String usage = "easyVEOCreate -j jobFile directory";

        // process command line arguments
        i = 0;
        try {
            while (i < args.length) {
                switch (args[i]) {

                    // load the job file
                    case "-j":
                        i++;
                        p = checkFile("job file", args[i], false);
                        try {
                            job.loadJob(p);
                        } catch (AppError ae) {
                            throw new AppFatal(ae.getMessage());
                        }
                        break;

                    default:
                        // if unrecognised arguement, print help string and exit
                        if (args[i].charAt(0) == '-') {
                            throw new AppFatal("Unrecognised argument '" + args[i] + "' Usage: " + usage);
                        }

                        // if doesn't start with '-' assume a file or directory name
                        job.sourceDir = checkFile("source directory", args[i], true);
                        i++;
                        break;
                }
            }
        } catch (ArrayIndexOutOfBoundsException ae) {
            throw new AppFatal("Missing argument. Usage: " + usage);
        }
    }

    /**
     * Check a file to see that it exists and is of the correct type (regular
     * file or directory). The program terminates if an error is encountered.
     *
     * @param type a String describing the file to be opened
     * @param name the file name to be opened
     * @param isDirectory true if the file is supposed to be a directory
     * @throws VEOFatal if the file does not exist, or is of the correct type
     * @return the File opened
     */
    private Path checkFile(String type, String name, boolean isDirectory) throws AppFatal {
        return checkFile(type, Paths.get(name), isDirectory);
    }

    private Path checkFile(String type, Path p, boolean isDirectory) throws AppFatal {
        if (p == null) {
            throw new AppFatal(classname, 6, type + " path is null");
        }
        if (!Files.exists(p)) {
            throw new AppFatal(classname, 6, type + " '" + p.toString() + "' does not exist");
        }
        if (isDirectory && !Files.isDirectory(p)) {
            throw new AppFatal(classname, 7, type + " '" + p.toString() + "' is a file not a directory");
        }
        if (!isDirectory && Files.isDirectory(p)) {
            throw new AppFatal(classname, 8, type + " '" + p.toString() + "' is a directory not a file");
        }
        return p;
    }

    /**
     * Free the resources used by this instance.
     */
    @Override
    public void free() {

        if (job != null) {
            job.free();
        }
        if (baos != null) {
            try {
                baos.close();
            } catch (IOException ioe) {
                // ignore
            }
            baos = null;
        }
        r = null;
        userId = null;
        if (directoriesIgnored != null) {
            directoriesIgnored.clear();
            directoriesIgnored = null;
        }
        if (ignorePatterns != null) {
            ignorePatterns.clear();
            ignorePatterns = null;
        }
        if (ltsf != null) {
            ltsf.free();
            ltsf = null;
        }
        if (user != null) {
            user.free();
            user = null;
        }
        archivalDesc = null;
    }

    /**
     * Read the archival description file. This method reads a text file that
     * contains an archival description of the harvest.
     */
    private String readDescFile() {
        FileReader fr = null;
        BufferedReader br = null;
        String line;
        StringBuilder sb;

        sb = new StringBuilder();
        try {
            fr = new FileReader(job.archiveDescFile.toString());
            br = new BufferedReader(fr);
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (FileNotFoundException fnfe) {
            LOG.log(Level.WARNING, "Ignore File ''{0}'' does not exist", new Object[]{job.archiveDescFile.toString()});
        } catch (IOException ioe) {
            LOG.log(Level.WARNING, "Error when reading Ignore File ''{0}'': ''{1}''", new Object[]{job.archiveDescFile.toString(), ioe.getMessage()});
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                /* ignore */
            }
            try {
                if (fr != null) {
                    fr.close();
                }
            } catch (IOException e) {
                /* ignore */
            }
        }
        return sb.toString();
    }

    /**
     * Describe ignore file. This method turns the ignore file into text
     */
    private String describeIgnoreFile() {
        StringBuilder sb;
        Iterator<String> it;
        Iterator<Pattern> itp;
        String s;

        sb = new StringBuilder();

        it = directoriesIgnored.keySet().iterator();
        if (!it.hasNext()) {
            sb.append("All subdirectories in '");
            sb.append(XMLParser.xmlEncode(job.sourceDir.toString()));
            sb.append("' have been harvested\n");
            sb.append("When harvesting '");
            sb.append(XMLParser.xmlEncode(job.sourceDir.toString()));
            sb.append("' the following subdirectories were specified to be ignored:\n");
            while (it.hasNext()) {
                s = directoriesIgnored.get(it.next());
                sb.append("\t'");
                sb.append(XMLParser.xmlEncode(s));
                sb.append("'\n");
            }
        }

        if (ignoreFileWithNoExtension) {
            sb.append("\nWhen harvesting '");
            sb.append(XMLParser.xmlEncode(job.sourceDir.toString()));
            sb.append("', without a file extension were specified to be ignored.\n");
        }

        itp = ignorePatterns.iterator();
        if (!itp.hasNext()) {
            sb.append("No file types in '");
            sb.append(XMLParser.xmlEncode(job.sourceDir.toString()));
            sb.append("' have been ignored due to a specified pattern\n");
        } else {
            sb.append("When harvesting '");
            sb.append(XMLParser.xmlEncode(job.sourceDir.toString()));
            sb.append("' the file names matching the following patterns were specified to be ignored:");
            while (itp.hasNext()) {
                s = itp.next().pattern();
                sb.append(XMLParser.xmlEncode(s));
                sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Count the number of VEOs to create
     *
     * @return the number of VEOs to create
     */
    @Override
    public int countUnitsOfWork() {
        DirectoryStream<Path> ds;
        int count;

        // go through the source directory, creating VEOs...
        count = 0;
        try {
            ds = Files.newDirectoryStream(job.sourceDir);
            for (Path entry : ds) {
                if (Files.isDirectory(entry)) {
                    count++;
                }
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed counting tasks in ''{0}'' because: {1}", new Object[]{job.sourceDir.toString(), e.toString()});
        }
        return count;
    }

    /**
     * Actually create the VEOs from the source directory. After each VEO is
     * created, the call-back is called to check-in with the GUI.
     *
     * @throws AppFatal
     * @throws AppError
     */
    @Override
    public void doTask() throws AppFatal, AppError {
        DirectoryStream<Path> ds;
        int count;

        // go through the source directory, creating VEOs...
        try {
            count = 0;
            ds = Files.newDirectoryStream(job.sourceDir);
            for (Path entry : ds) {
                if (job.callback != null) {
                    if (job.callback.updateStatus(entry.getFileName().toString(), null, count)) {
                        break;
                    }
                }
                if (Files.isDirectory(entry)) {
                    try {
                        LOG.log(Level.INFO, "Processing ''{0}''", new Object[]{entry.toString()});
                        createVEO(entry);
                    } catch (AppError e) {
                        LOG.log(Level.WARNING, "VEO ''{0}.veo.zip'' incomplete because:\n{1}", new Object[]{entry.toString(), e.getMessage()});
                    }
                }
                count++;
            }
        } catch (IOException | AppFatal e) {
            LOG.log(Level.WARNING, "Failed processing ''{0}'' because: {1}", new Object[]{job.sourceDir.toString(), e.toString()});
        }
    }

    /**
     * Create VEO
     *
     * This method creates a new VEO
     *
     * @param baseDirectory the directory to create the VEO from
     * @throws AppFatal if an error occurred that is not worthwhile continuing
     * @throws AppError if this VEO failed, but can create other VEOs
     */
    private void createVEO(Path baseDirectory) throws AppFatal, AppError {
        CreateVEO cv;
        Path p;
        String recordName;      // name of this record element (from the file, without the final '.xml')
        String[] description = new String[3];
        String errors[] = {""};

        // check parameters
        if (baseDirectory == null) {
            throw new AppFatal("Passed null base directory to be processed");
        }

        // reset, free memory, and print status
        baos.reset();
        r.gc();
        // LOG.log(Level.WARNING, "{0} Processing: ''{1}''", new Object[]{versDateTime(false, 0), baseDirectory.toString()});

        // get the record name from the name of the base directory
        // recordName = "FSC-" + baseDirectory.getFileName().toString() + "-" + versDateTime(true, System.currentTimeMillis());
        recordName = baseDirectory.getFileName().toString();

        // create a record directory in the output directory
        p = job.outputDir.resolve(recordName + ".veo");
        if (!deleteDirectory(p)) {
            throw new AppError("Arrgh: directory '" + p.toString() + "' already exists & couldn't be deleted");
        }

        // we haven't added the dummy LTPF file yet
        addedDummyLTPF = false;

        // create VEO...
        LOG.log(Level.WARNING, "Processing {0} into {1}.veo.zip", new Object[]{baseDirectory.getFileName().toString(), recordName});
        try {
            cv = new CreateVEO(job.outputDir, recordName, job.hashAlg, job.debug);
        } catch (VEOError ve) {
            throw new AppError(ve.getMessage());
        }
        try {
            cv.addVEOReadme(job.versTemplates);
            description[0] = "Created with EasyVEOCreate";
            description[1] = archivalDesc;
            description[2] = harvestReport();
            cv.addEvent(versDateTime(false, System.currentTimeMillis()), "Converted to a V3 VEO", userId, description, errors);
            cv.addContent(baseDirectory);
            try {
                processDirectory(cv, recordName, baseDirectory, baseDirectory, 1);
            } catch (AppError ae) {
                LOG.log(Level.WARNING, "VEO ''{0}.veo.zip'' incomplete because:\n{1}", new Object[]{recordName, ae.getMessage()});
            }
            cv.finishFiles();
            cv.sign(user, job.hashAlg);
            cv.finalise(false);
        } catch (VEOError ve) {
            cv.abandon(true);
            throw new AppError(ve.getMessage());
        } finally {
            System.gc();
        }
    }

    /**
     * Process a directory into an Information Object. Files in the directory
     * that begin with the string 'VERS-MD-' are converted into metadata
     * packages. Subdirectories in the directory that begin with the string
     * 'VERS-IP-' get converted into Information Pieces with multiple
     * representations.
     */
    private void processDirectory(CreateVEO cv, String recordName, Path dir, Path baseDirectory, int depth) throws AppError, AppFatal {
        DirectoryStream<Path> ds;
        String[] objMetadata = new String[7];
        Path reportedFile;
        String filename;

        // sanity check
        if (!Files.exists(dir)) {
            throw new AppFatal("VEO incomplete because file/directory '" + dir.toString() + "' was supposed to exist, but does not");
        }
        if (!Files.isDirectory(dir)) {
            throw new AppFatal("VEO incomplete because file '" + dir.toString() + "' was supposed to be a directory, but is not");
        }

        // if this is the baseDirectory, report this, otherwise report relative to the baseDirectory
        if (dir == baseDirectory) {
            reportedFile = baseDirectory;
        } else {
            reportedFile = baseDirectory.relativize(dir);
        }

        // should this file/directory be ignored?
        filename = dir.getFileName().toString();
        Iterator<Pattern> itp;
        Pattern p1;
        itp = ignorePatterns.iterator();
        while (itp.hasNext()) {
            p1 = itp.next();
            if (p1.matcher(filename).matches()) {
                LOG.log(Level.WARNING, "File or directory ''{0}'' (and its contents) were not included as the file name matched pattern {1}", new Object[]{reportedFile.toString(), p1.pattern()});
                return;
            }
        }
        if (directoriesIgnored.containsKey(dir.toString())) {
            LOG.log(Level.WARNING, "Directory ''{0}'' (and its contents) were not included due to capture configuration", new Object[]{reportedFile.toString()});
            return;
        }

        try {
            cv.addInformationObject(dir.getFileName().toString(), depth);

            // if at the root, add metadata about this record as a whole
            if (depth == 1) {
                // cv.addMetadataPackage(recordAGLS, recMetadata);
                // addHarvestDescription(cv, baseDirectory);
                objMetadata[0] = "http://www.prov.vic.gov.au/records/" + recordName;
            } else {
                objMetadata[0] = "http://www.prov.vic.gov.au/records/" + recordName + "/" + iocnt;
                iocnt++;
            }

            LOG.log(Level.FINE, "Directory ''{0}'' was added", new Object[]{dir.toString()});

            // add the metadata objects
            ds = Files.newDirectoryStream(dir);
            for (Path p : ds) {
                if (Files.isRegularFile(p) && p.getFileName().toString().trim().startsWith("VERS-MD-")) {
                    processMetadata(cv, p);
                }
            }
            ds.close();

            // add the simple files
            ds = Files.newDirectoryStream(dir);
            for (Path p : ds) {
                if (Files.isRegularFile(p) && !p.getFileName().toString().trim().startsWith("VERS-MD-")) {
                    processFile(cv, p, baseDirectory);
                }
            }
            ds.close();

            // add the subordinate directories as further information objects
            ds = Files.newDirectoryStream(dir);
            for (Path p : ds) {
                if (Files.isDirectory(p)) {
                    processDirectory(cv, recordName, p, baseDirectory, depth + 1);
                }
            }
            ds.close();
        } catch (IOException e) {
            throw new AppFatal("Failed to process directory '" + dir.toAbsolutePath() + toString() + "': " + e.getMessage());
        } catch (VEOError e) {
            throw new AppError("Failed to process directory '" + dir.toAbsolutePath() + toString() + "': " + e.getMessage());
        }
    }

    private enum MetadataType {
        AGLS_SIMPLE, // text type metadata to be turned into AGLS
        AGLS_RDF, // raw AGLS in RDF
        ASNZ_RDF, // raw ASNZ in RDF            
        XML,            // raw XML
    }

    /**
     * Process a metadata file (begining with 'VERS-MD-'). There are two avenues
     * of processing. The simplest (from a programming perspective) simply
     * copies the contents of the metadata file into the VEO as is. The contents
     * are assumed to be valid AGLS or ASNZ-5476 expressed as RDF, or valid XML.
     * Which of these options is flagged by the file extension of the metadata
     * file ('.agls' indicates AGLS in RDF, '.asnz' indicates ASNZ in RDF, and
     * '.xml' indicates plain XML).
     *
     * The simplest from a user perspective is a plain text file that contains a
     * sequence of AGLS property/value pairs, one per line. These are read and
     * wrapped to form an AGLS in RDF metadata package. Each line has the form
     * propertyName"="propertyValue (note without any white space). The property
     * names are AGLS property names (e.g. 'dcterms:title'), except that common
     * properties can omit the leading domain (e.g. 'title'). The property value
     * is delimitted by the '=' and extends to the end of the line. The value is
     * encoded to be XML safe (e.g. replacing '&' with '&amp;'). The first line
     * of the metadata file *must* be an identifier property
     * ('dcterms:identifier' or 'identifier').
     */
    private void processMetadata(CreateVEO cv, Path file) throws AppError, AppFatal {
        StringBuilder sb;
        String s, mdProperty, mdValue;
        FileReader fr;
        BufferedReader br;
        int i;
        MetadataType mt;
        boolean firstLine;

        // sanity check
        if (!Files.exists(file)) {
            throw new AppFatal("VEO incomplete because metadata file '" + file.toString() + "' was supposed to exist, but does not");
        }

        // decide on encoding of metadata
        s = file.getFileName().toString().toLowerCase();
        if ((i = s.lastIndexOf('.')) != -1) {
            s = s.substring(i);
            switch (s) {
                case ".txt":
                    mt = MetadataType.AGLS_SIMPLE;
                    break;
                case ".agls":
                    mt = MetadataType.AGLS_RDF;
                    break;
                case ".asnz":
                    mt = MetadataType.ASNZ_RDF;
                    break;
                case ".xml":
                    mt = MetadataType.XML;
                    break;
                default:
                    throw new AppError("VEO incomplete because metadata file '" + file.toString() + "' was of unknown encooding (file extension: '.txt', '.agls', '.asnz', or '.xml'");
            }
        } else {
            throw new AppError("VEO incomplete because metadata file '" + file.toString() + "' was of unknown encooding (had no file extension)");
        }

        // read metadata
        sb = new StringBuilder();
        try {
            fr = new FileReader(file.toFile());
            br = new BufferedReader(fr);
            if (mt == MetadataType.AGLS_SIMPLE) {
                sb.append("<rdf:RDF xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:aglsterms=\"http://www.agls.gov.au/agls/terms/\">\n");
                firstLine = true;
                while ((s = br.readLine()) != null) {
                    if ((i = s.indexOf('=')) != -1) {
                        mdProperty = s.substring(0, i);
                        mdValue = s.substring(i + 1);
                        if (firstLine) {
                            if (!(mdProperty.equals("dcterms:identifier") || mdProperty.equals("identifier"))) {
                                throw new AppError("VEO incomplete because metadata file '" + file.toString() + "' did not start with an identifier property (" + mdProperty + ")");
                            }
                            sb.append("<rdf:Description rdf:about=\"http://prov.vic.gov.au/rdfIdSpace/" + XMLParser.xmlEncode(mdValue) + "\">\n");
                            firstLine = false;
                        }
                        switch (mdProperty) {
                            case "identifier":
                                mdProperty = "dcterms:identifier";
                                break;
                            case "title":
                                mdProperty = "dcterms:title";
                                break;
                            case "creator":
                                mdProperty = "dcterms:creator";
                                break;
                            case "date":
                                mdProperty = "dcterms:date";
                                break;
                            case "created":
                                mdProperty = "dcterms:created";
                                break;
                            case "protectiveMarking":
                                mdProperty = "aglsterms:protectiveMarking";
                                break;
                            case "disposalDate":
                                mdProperty = "versterms:disposal-ReviewDate";
                                break;
                            case "disposalAction":
                                mdProperty = "versterms:disposal-Action";
                                break;
                            case "disposalReference":
                                mdProperty = "versterms:disposal-Reference";
                                break;
                            case "description":
                                mdProperty = "dcterms:description";
                                break;
                            case "format":
                                mdProperty = "dcterms:format";
                                break;
                            default:
                                break;
                        }
                        if (mdProperty.equals("dcterms:created") && mdValue.equals("$$currentDateTime$$")) {
                            mdValue = versDateTime(true, System.currentTimeMillis());
                        }
                        s = " <" + mdProperty + ">" + XMLParser.xmlEncode(mdValue) + "</" + mdProperty + ">";
                    } else {
                        throw new AppError("VEO incomplete because metadata file '" + file.toString() + "' contained line '" + s + "' that did not contain an '=' separating the metadata property from the value");
                    }
                    sb.append(s);
                    sb.append("\n");
                }
                sb.append("</rdf:Description>\n");
                sb.append("</rdf:RDF>\n");
            } else {
                while ((s = br.readLine()) != null) {
                    sb.append(s);
                    sb.append("\n");
                }
            }
            br.close();
            fr.close();
        } catch (IOException e) {
            throw new AppError("VEO incomplete because metadata file '" + file.toString() + "' failed to be read: " + e.toString());
        }

        // output the metadata with appropriate flags for semantics and syntax
        try {
            switch (mt) {
                case AGLS_RDF:
                case AGLS_SIMPLE:
                    cv.addMetadataPackage("http://www.vic.gov.au/blog/wp-content/uploads/2013/11/AGLS-Victoria-2011-V4-Final-2011.pdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns", sb);
                    break;
                case ASNZ_RDF:
                    cv.addMetadataPackage("‘http://www.prov.vic.gov.au/VERS-as5478", "http://www.w3.org/1999/02/22-rdf-syntax-ns", sb);
                    break;
                case XML:
                    cv.addMetadataPackage("http://prov.vic.gov.au/vers/schema/unknown", "https://www.w3.org/TR/2008/REC-xml-20081126/", sb);
                    break;
                default:
                    throw new AppError("VEO incomplete because metadata file '" + file.toString() + "' was of unknown encooding");
            }
        } catch (VEOError ve) {
            throw new AppError("VEO incomplete because metadata file '" + file.toString() + "' failed to be added as a package: " + ve.toString());
        }
    }

    /**
     * Process the specified file into an Information Piece
     */
    private void processFile(CreateVEO cv, Path dir, Path baseDirectory) throws VEOError {
        int i;
        String filename;
        Path reportedFile;

        // sanity check
        if (!Files.exists(dir)) {
            throw new VEOFatal("VEO incomplete because file '" + dir.toString() + "' was supposed to exist, but does not");
        }

        // if this is the baseDirectory, report this, otherwise report relative to the baseDirectory
        if (dir == baseDirectory) {
            reportedFile = baseDirectory;
        } else {
            reportedFile = baseDirectory.relativize(dir);
        }

        // should this file be ignored?
        filename = dir.getFileName().toString();
        Iterator<Pattern> itp;
        Pattern p1;
        itp = ignorePatterns.iterator();
        while (itp.hasNext()) {
            p1 = itp.next();
            if (p1.matcher(filename).matches()) {
                LOG.log(Level.WARNING, "File or directory ''{0}'' (and its contents) were not included as the file name matched pattern {1}", new Object[]{reportedFile.toString(), p1.pattern()});
                return;
            }
        }

        i = filename.lastIndexOf(".");
        if (i == -1 && ignoreFileWithNoExtension) {
            LOG.log(Level.WARNING, "File ''{0}'' was not included as files with no file extensions were ignored", new Object[]{reportedFile.toString()});
            return;
        }

        cv.addInformationPiece("file");
        cv.addContentFile(baseDirectory.getParent().relativize(dir).toString());
        if (!ltsf.isV3LTSF(dir.getFileName().toString().toLowerCase())) {
            addDummyLTPF(cv, reportedFile);
        }
        LOG.log(Level.FINE, "File ''{0}'' was added", new Object[]{dir.toString()});
    }

    /**
     * Add a dummy long term preservation file
     */
    private void addDummyLTPF(CreateVEO cv, Path file) {
        Path p;

        // add the dummy LTPF to the VEO if we haven't already done so
        /*
        if (!addedDummyLTPF) {
            p = job.dummyContentFile;
            try {
                cv.addContentFile("DummyContent/DummyLTSF.txt", p);
            } catch (VEOError ve) {
                LOG.log(Level.WARNING, "Cannot add dummy LTSF {0} because: {1}", new Object[]{p.toString(), ve.getMessage()});
                return;
            }
            addedDummyLTPF = true;
        }
         */
        // add the content file to the current information piece
        if (job.includeDummyContent && job.dummyContentFile != null) {
            try {
                cv.addContentFile("DummyContent/DummyLTSF.txt", job.dummyContentFile);
            } catch (VEOError ve) {
                LOG.log(Level.WARNING, "Cannot add ''DummyContent/DummyLTSF.txt'' because: {0}", new Object[]{ve.getMessage()});
            }
        }
        LOG.log(Level.WARNING, "WARNING: {0} is not a valid long term preservation format", file.toString());
    }

    /**
     * Recursively delete a directory
     */
    private boolean deleteDirectory(Path directory) {
        DirectoryStream<Path> ds;
        boolean failed;

        failed = false;
        try {
            if (!Files.exists(directory)) {
                return true;
            }
            ds = Files.newDirectoryStream(directory);
            for (Path p : ds) {
                if (!Files.isDirectory(p)) {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        failed = true;
                    }
                } else {
                    failed |= !deleteDirectory(p);
                }
            }
            ds.close();
            if (!failed) {
                Files.delete(directory);
            }
        } catch (IOException e) {
            failed = true;
        }
        return !failed;
    }

    /**
     * versDateTime
     *
     * Returns a date and time in the standard VERS format (see PROS 99/007
     * (Version 2), Specification 2, p146
     *
     * @param fssafe true if we want a date/time that is file system safe
     * @param ms	milliseconds since the epoch (if zero, return current
     * date/time)
     */
    private String versDateTime(boolean fssafe, long ms) {
        Date d;
        SimpleDateFormat sdf;
        TimeZone tz;
        String s;

        tz = TimeZone.getDefault();
        if (fssafe) {
            sdf = new SimpleDateFormat("yyyy-MM-dd'T'HHmmssZ");
        } else {
            sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        }
        sdf.setTimeZone(tz);
        if (ms == 0) {
            d = new Date();
        } else {
            d = new Date(ms);
        }
        s = sdf.format(d);
        if (!fssafe) {
            s = s.substring(0, 22) + ":" + s.substring(22, 24);
        }
        return s;
    }

    private String versTime(boolean fssafe, long ms) {
        Date d;
        SimpleDateFormat sdf;
        TimeZone tz;
        String s;

        tz = TimeZone.getDefault();
        if (fssafe) {
            sdf = new SimpleDateFormat("HHmmss");
        } else {
            sdf = new SimpleDateFormat("HH:mm:ss");
        }
        sdf.setTimeZone(tz);
        if (ms == 0) {
            d = new Date();
        } else {
            d = new Date(ms);
        }
        s = sdf.format(d);
        if (!fssafe) {
            s = s.substring(0, 22) + ":" + s.substring(22, 24);
        }
        return s;
    }

    /**
     * Add a description of the harvest paramenters to the root information
     * object
     */
    private void addHarvestDescription(CreateVEO cv, Path baseDirectory) throws VEOError {
        StringBuilder sb;
        Iterator<String> it;
        Iterator<Pattern> itp;
        Pattern pat;
        String s;

        sb = new StringBuilder();

        sb.append("<rdf:RDF ");
        sb.append("xmlns:ex=\"http://www.agls.gov.au/agls/terms#\"\n");
        sb.append(">");
        sb.append("<rdf:Description rdf:about=\"http://www.example.org/124\">\n");
        sb.append(" <ex:baseDirectory >");
        sb.append(XMLParser.xmlEncode(baseDirectory.toString()));
        sb.append("</ex:baseDirectory>\n");

        itp = ignorePatterns.iterator();
        if (!itp.hasNext()) {
            sb.append(" <ex:ignoredPatterns/>\n");
        } else {
            // sb.append(" <fsharvest:ignoredPatterns rdf:parseType=\"Literal\">\n");
            while (itp.hasNext()) {
                pat = itp.next();
                sb.append("  <ex:ignoredPattern>");
                sb.append(XMLParser.xmlEncode(pat.pattern()));
                sb.append("</ex:ignoredPattern>\n");
            }
            // sb.append(" </fsharvest:ignoredPatterns>\n");
        }

        it = directoriesIgnored.keySet().iterator();
        if (!it.hasNext()) {
            sb.append(" <ex:ignoredDirectories/>\n");
        } else {
            // sb.append(" <fsharvest:ignoredDirectories rdf:parseType=\"Literal\">\n");
            while (it.hasNext()) {
                s = directoriesIgnored.get(it.next());
                sb.append("  <ex:ignoredDirectory>");
                sb.append(XMLParser.xmlEncode(s));
                sb.append("</ex:ignoredDirectory>\n");
            }
            // sb.append(" </fsharvest:ignoredDirectories>\n");
        }
        sb.append("</rdf:Description>\n");
        sb.append("</rdf:RDF>\n");
        cv.addMetadataPackage("http://prov.vic.gov.au/vers/schema/FileHarvestDesc", "http://www.w3.org/1999/02/22-rdf-syntax-ns", sb);
    }

    /**
     * Report
     */
    private String harvestReport() {
        Iterator<String> it;
        String s;
        StringBuilder sb = new StringBuilder();

        try {
            sb.append("\tVEO created from '" + job.sourceDir.toRealPath().toString() + "'\n");
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, "Failure when converting the base directory ''{0}'' to a real path: ''{1}''", new Object[]{job.sourceDir, ioe.getMessage()});
        }
        sb.append("\tThe following directories have not been included due to the software configuration:\n");
        it = directoriesIgnored.keySet().iterator();
        if (!it.hasNext()) {
            sb.append(" None\n");
        } else {
            sb.append("\n");
            while (it.hasNext()) {
                s = directoriesIgnored.get(it.next());
                sb.append("\t'" + s + "'\n");
            }
        }
        if (ignoreFileWithNoExtension) {
            sb.append("\tFiles without a file extension have not been included due to the software configuration\n");
        }
        return sb.toString();
    }

    /**
     * Main program
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        EasyVEOCreate fh;

        try {
            fh = new EasyVEOCreate(args);
            // fha.report();
            fh.doTask();
        } catch (Exception e) {
            System.out.println("Fatal error: " + e.getMessage());
        }
    }
}
