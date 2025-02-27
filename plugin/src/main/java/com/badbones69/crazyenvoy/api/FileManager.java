package com.badbones69.crazyenvoy.api;

import com.badbones69.crazyenvoy.multisupport.Version;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author BadBones69
 * @version v1.0
 *
 */
public class FileManager {
    
    private static FileManager instance = new FileManager();
    private boolean log = false;
    private HashMap<Files, File> files = new HashMap<>();
    private ArrayList<String> homeFolders = new ArrayList<>();
    private ArrayList<CustomFile> customFiles = new ArrayList<>();
    /**
     * For allowing files to be grabbed from a folder with a different name then it will be put in.
     */
    private HashMap<String, String> jarHomeFolders = new HashMap<>();
    private HashMap<String, String> autoGenerateFiles = new HashMap<>();
    private HashMap<Files, FileConfiguration> configurations = new HashMap<>();
    
    public static FileManager getInstance() {
        return instance;
    }
    
    /**
     * Sets up the plugin and loads all necessary files.
     */
    public void setup() {
        if (!CrazyManager.getJavaPlugin().getDataFolder().exists()) CrazyManager.getJavaPlugin().getDataFolder().mkdirs();
        files.clear();
        customFiles.clear();
        //Loads all the normal static files.
        for (Files file : Files.values()) {
            File newFile = new File(CrazyManager.getJavaPlugin().getDataFolder(), file.getFileLocation());
            if (log) CrazyManager.getJavaPlugin().getLogger().info("Loading the " + file.getFileName());
            if (!newFile.exists()) {
                try {
                    String fileLocation = file.getFileLocation();
                    //Switch between 1.12.2- and 1.13+ config version.
                    if (file == Files.CONFIG) {
                        if (Version.isOlder(Version.v1_13_R2)) {
                            fileLocation = "config1.12.2-Down.yml";
                        } else {
                            fileLocation = "config1.13-Up.yml";
                        }
                    }
                    File serverFile = new File(CrazyManager.getJavaPlugin().getDataFolder(), "/" + file.getFileLocation());
                    InputStream jarFile = getClass().getResourceAsStream("/" + fileLocation);
                    copyFile(jarFile, serverFile);
                } catch (Exception e) {
                    if (log) CrazyManager.getJavaPlugin().getLogger().info("Failed to load " + file.getFileName());
                    e.printStackTrace();
                    continue;
                }
            }
            files.put(file, newFile);
            configurations.put(file, YamlConfiguration.loadConfiguration(newFile));
            if (log) CrazyManager.getJavaPlugin().getLogger().info("Successfully loaded " + file.getFileName());
        }
        //Starts to load all the custom files.
        if (homeFolders.size() > 0) {
            if (log) CrazyManager.getJavaPlugin().getLogger().info("Loading custom files.");
            for (String homeFolder : homeFolders) {
                File homeFile = new File(CrazyManager.getJavaPlugin().getDataFolder(), "/" + homeFolder);
                if (homeFile.exists()) {
                    String[] list = homeFile.list();
                    if (list != null) {
                        for (String name : list) {
                            if (name.endsWith(".yml")) {
                                CustomFile file = new CustomFile(name, homeFolder);
                                if (file.exists()) {
                                    customFiles.add(file);
                                    if (log) CrazyManager.getJavaPlugin().getLogger().info("Loaded new custom file: " + homeFolder + "/" + name + ".");
                                }
                            }
                        }
                    }
                } else {
                    homeFile.mkdir();
                    if (log) CrazyManager.getJavaPlugin().getLogger().info("The folder " + homeFolder + "/ was not found so it was created.");
                    for (String fileName : autoGenerateFiles.keySet()) {
                        if (autoGenerateFiles.get(fileName).equalsIgnoreCase(homeFolder)) {
                            homeFolder = autoGenerateFiles.get(fileName);
                            try {
                                File serverFile = new File(CrazyManager.getJavaPlugin().getDataFolder(), homeFolder + "/" + fileName);
                                InputStream jarFile = getClass().getResourceAsStream((jarHomeFolders.getOrDefault(fileName, homeFolder)) + "/" + fileName);
                                copyFile(jarFile, serverFile);
                                if (fileName.toLowerCase().endsWith(".yml")) {
                                    customFiles.add(new CustomFile(fileName, homeFolder));
                                }
                                if (log) CrazyManager.getJavaPlugin().getLogger().info("Created new default file: " + homeFolder + "/" + fileName + ".");
                            } catch (Exception e) {
                                if (log) CrazyManager.getJavaPlugin().getLogger().info("Failed to create new default file: " + homeFolder + "/" + fileName + "!");
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            if (log) CrazyManager.getJavaPlugin().getLogger().info("Finished loading custom files.");
        }
    }
    
    /**
     * Turn on the logger system for the FileManager.
     * @param log True to turn it on and false for it to be off.
     */
    public FileManager logInfo(Boolean log) {
        this.log = log;
        return this;
    }
    
    /**
     * Check if the logger is logging in console.
     * @return True if it is and false if it isn't.
     */
    public boolean isLogging() {
        return log;
    }
    
    /**
     * Register a folder that has custom files in it. Make sure to have a "/" in front of the folder name.
     * @param homeFolder The folder that has custom files in it.
     */
    public FileManager registerCustomFilesFolder(String homeFolder) {
        homeFolders.add(homeFolder);
        return this;
    }
    
    /**
     * Unregister a folder that has custom files in it. Make sure to have a "/" in front of the folder name.
     * @param homeFolder The folder with custom files in it.
     */
    public FileManager unregisterCustomFilesFolder(String homeFolder) {
        homeFolders.remove(homeFolder);
        return this;
    }
    
    /**
     * Register a file that needs to be generated when it's home folder doesn't exist. Make sure to have a "/" in front of the home folder's name.
     * @param fileName The name of the file you want to auto-generate when the folder doesn't exist.
     * @param homeFolder The folder that has custom files in it.
     */
    public FileManager registerDefaultGenerateFiles(String fileName, String homeFolder) {
        autoGenerateFiles.put(fileName, homeFolder);
        return this;
    }
    
    /**
     * Register a file that needs to be generated when it's home folder doesn't exist. Make sure to have a "/" in front of the home folder's name.
     * @param fileName The name of the file you want to auto-generate when the folder doesn't exist.
     * @param homeFolder The folder that has custom files in it.
     * @param jarHomeFolder The folder that the file is found in the jar.
     */
    public FileManager registerDefaultGenerateFiles(String fileName, String homeFolder, String jarHomeFolder) {
        autoGenerateFiles.put(fileName, homeFolder);
        jarHomeFolders.put(fileName, jarHomeFolder);
        return this;
    }
    
    /**
     * Unregister a file that doesn't need to be generated when it's home folder doesn't exist. Make sure to have a "/" in front of the home folder's name.
     * @param fileName The file that you want to remove from auto-generating.
     */
    public FileManager unregisterDefaultGenerateFiles(String fileName) {
        autoGenerateFiles.remove(fileName);
        jarHomeFolders.remove(fileName);
        return this;
    }
    
    /**
     * Gets the file from the system.
     * @return The file from the system.
     */
    public FileConfiguration getFile(Files file) {
        return configurations.get(file);
    }
    
    /**
     * Get a custom file from the loaded custom files instead of a hardcoded one.
     * This allows you to get custom files like Per player data files.
     * @param name Name of the crate you want. (Without the .yml)
     * @return The custom file you wanted otherwise if not found will return null.
     */
    public CustomFile getFile(String name) {
        for (CustomFile file : customFiles) {
            if (file.getName().equalsIgnoreCase(name)) return file;
        }
        return null;
    }
    
    /**
     * Get a list of all the custom files.
     */
    public List<CustomFile> getCustomFiles() {
        return customFiles;
    }
    
    /**
     * Saves the file from the loaded state to the file system.
     */
    public void saveFile(Files file) {
        try {
            configurations.get(file).save(files.get(file));
        } catch (IOException e) {
            CrazyManager.getJavaPlugin().getLogger().info("Could not save " + file.getFileName() + "!");
            e.printStackTrace();
        }
    }
    
    /**
     * Save a custom file.
     * @param name The name of the custom file.
     */
    public void saveFile(String name) {
        CustomFile file = getFile(name);
        if (file != null) {
            try {
                file.getFile().save(new File(CrazyManager.getJavaPlugin().getDataFolder(), file.getHomeFolder() + "/" + file.getFileName()));
                if (log) CrazyManager.getJavaPlugin().getLogger().info("Successfuly saved the " + file.getFileName() + ".");
            } catch (Exception e) {
                CrazyManager.getJavaPlugin().getLogger().info("Could not save " + file.getFileName() + "!");
                e.printStackTrace();
            }
        } else {
            if (log) CrazyManager.getJavaPlugin().getLogger().info("The file " + name + ".yml could not be found!");
        }
    }
    
    /**
     * Save a custom file.
     * @param file The custom file you are saving.
     * @return True if the file saved correct and false if there was an error.
     */
    public boolean saveFile(CustomFile file) {
        return file.saveFile();
    }
    
    /**
     * Overrides the loaded state file and loads the file systems file.
     */
    public void reloadFile(Files file) {
        configurations.put(file, YamlConfiguration.loadConfiguration(files.get(file)));
    }
    
    /**
     * Overrides the loaded state file and loads the file systems file.
     */
    public void reloadFile(String name) {
        CustomFile file = getFile(name);
        if (file != null) {
            try {
                file.file = YamlConfiguration.loadConfiguration(new File(CrazyManager.getJavaPlugin().getDataFolder(), "/" + file.getHomeFolder() + "/" + file.getFileName()));
                if (log) CrazyManager.getJavaPlugin().getLogger().info("Successfuly reloaded the " + file.getFileName() + ".");
            } catch (Exception e) {
                CrazyManager.getJavaPlugin().getLogger().info("Could not reload the " + file.getFileName() + "!");
                e.printStackTrace();
            }
        } else {
            if (log) CrazyManager.getJavaPlugin().getLogger().info("The file " + name + ".yml could not be found!");
        }
    }
    
    /**
     * Overrides the loaded state file and loads the filesystems file.
     * @return True if it reloaded correct and false if the file wasn't found.
     */
    public boolean reloadFile(CustomFile file) {
        return file.reloadFile();
    }
    
    /**
     * Was found here: https://bukkit.org/threads/extracting-file-from-jar.16962
     */
    private void copyFile(InputStream in, File out) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[1024];
            int i;
            while ((i = in.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
    
    public enum Files {
        
        //ENUM_NAME("FileName.yml", "FilePath.yml"),
        CONFIG("config.yml", "config.yml"),
        MESSAGES("messages.yml", "messages.yml"),
        DATA("data.yml", "data.yml");
        
        private final String fileName;
        private final String fileLocation;
        
        /**
         * The files that the server will try and load.
         * @param fileName The file name that will be in the plugin's folder.
         * @param fileLocation The location the file is in while in the Jar.
         */
        private Files(String fileName, String fileLocation) {
            this.fileName = fileName;
            this.fileLocation = fileLocation;
        }
        
        /**
         * Get the name of the file.
         * @return The name of the file.
         */
        public String getFileName() {
            return fileName;
        }
        
        /**
         * The location the jar it is at.
         * @return The location in the jar the file is in.
         */
        public String getFileLocation() {
            return fileLocation;
        }
        
        /**
         * Gets the file from the system.
         * @return The file from the system.
         */
        public FileConfiguration getFile() {
            return getInstance().getFile(this);
        }
        
        /**
         * Saves the file from the loaded state to the file system.
         */
        public void saveFile() {
            getInstance().saveFile(this);
        }
        
        /**
         * Overrides the loaded state file and loads the file systems file.
         */
        public void reloadFile() {
            getInstance().reloadFile(this);
        }
    }
    
    public class CustomFile {
        
        private final String name;
        private final String fileName;
        private final String homeFolder;
        private FileConfiguration file;
        
        /**
         * A custom file that is being made.
         * @param name Name of the file.
         * @param homeFolder The home folder of the file.
         */
        public CustomFile(String name, String homeFolder) {
            this.name = name.replace(".yml", "");
            this.fileName = name;
            this.homeFolder = homeFolder;
            if (new File(CrazyManager.getJavaPlugin().getDataFolder(), "/" + homeFolder).exists()) {
                if (new File(CrazyManager.getJavaPlugin().getDataFolder(), "/" + homeFolder + "/" + name).exists()) {
                    file = YamlConfiguration.loadConfiguration(new File(CrazyManager.getJavaPlugin().getDataFolder(), "/" + homeFolder + "/" + name));
                } else {
                    file = null;
                }
            } else {
                new File(CrazyManager.getJavaPlugin().getDataFolder(), "/" + homeFolder).mkdir();
                if (log) CrazyManager.getJavaPlugin().getLogger().info("The folder " + homeFolder + "/ was not found so it was created.");
                file = null;
            }
        }
        
        /**
         * Get the name of the file without the .yml part.
         * @return The name of the file without the .yml.
         */
        public String getName() {
            return name;
        }
        
        /**
         * Get the full name of the file.
         * @return Full name of the file.
         */
        public String getFileName() {
            return fileName;
        }
        
        /**
         * Get the name of the home folder of the file.
         * @return The name of the home folder the files are in.
         */
        public String getHomeFolder() {
            return homeFolder;
        }
        
        /**
         * Get the ConfigurationFile.
         * @return The ConfigurationFile of this file.
         */
        public FileConfiguration getFile() {
            return file;
        }
        
        /**
         * Check if the file actually exists in the file system.
         * @return True if it does and false if it doesn't.
         */
        public boolean exists() {
            return file != null;
        }
        
        /**
         * Save the custom file.
         * @return True if it saved correct and false if something went wrong.
         */
        public boolean saveFile() {
            if (file != null) {
                try {
                    file.save(new File(CrazyManager.getJavaPlugin().getDataFolder(), homeFolder + "/" + fileName));
                    if (log) CrazyManager.getJavaPlugin().getLogger().info("Successfuly saved the " + fileName + ".");
                    return true;
                } catch (Exception e) {
                    CrazyManager.getJavaPlugin().getLogger().info("Could not save " + fileName + "!");
                    e.printStackTrace();
                    return false;
                }
            } else {
                if (log) CrazyManager.getJavaPlugin().getLogger().info("There was a null custom file that could not be found!");
            }
            return false;
        }
        
        /**
         * Overrides the loaded state file and loads the filesystems file.
         * @return True if it reloaded correct and false if the file wasn't found or errored.
         */
        public boolean reloadFile() {
            if (file != null) {
                try {
                    file = YamlConfiguration.loadConfiguration(new File(CrazyManager.getJavaPlugin().getDataFolder(), "/" + homeFolder + "/" + fileName));
                    if (log) CrazyManager.getJavaPlugin().getLogger().info("Successfuly reloaded the " + fileName + ".");
                    return true;
                } catch (Exception e) {
                    CrazyManager.getJavaPlugin().getLogger().info("Could not reload the " + fileName + "!");
                    e.printStackTrace();
                }
            } else {
                if (log) CrazyManager.getJavaPlugin().getLogger().info("There was a null custom file that was not found!");
            }
            return false;
        }
        
    }
    
}