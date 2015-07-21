package com.gitblit.plugin.gitblitrepoposixpermission;

import org.apache.log4j.Logger;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Properties;

public class GitBlitRepoPosixPermissionTest
{
    private static final boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    private static final Properties prop = new Properties();

    private static void modDirectoryRecursive(File currentDir) throws IOException
    {

        //Recursive search for *.git dir
        File[] currentFileNames = currentDir.listFiles();
        if (currentFileNames == null)
        {
            System.out.println("We hit a file: " + currentDir.getAbsolutePath());
            return; // just a file
        }
        System.out.println("Entering " + currentDir.getAbsolutePath());

        if (currentDir.isDirectory())
        {
            for (File file : currentFileNames)
            {
                if (file.getName().matches(".*\\.git")) //Found a git repo
                {
                    //Add configs.
                    PosixFileAttributes attrs = Files.getFileAttributeView(file.toPath(), PosixFileAttributeView.class).readAttributes();
                    System.out.println(GitBlitRepoPosixPermissionTest.class.getName() + ": Found a repo: " + file.getName());
                    Repository localRepo = new FileRepository(file.getPath());
                    StoredConfig localConfig = localRepo.getConfig();

                    //Configs based on 'git config -l' on hand configured repository
//                    gitblit.owner=OWNER_NAME
                    localConfig.setString("gitblit", null, "owner", attrs.owner().getName());
//                    gitblit.acceptnewpatchsets=true
                    localConfig.setBoolean("gitblit", null, "acceptnewpatchsets",
                            Boolean.parseBoolean(prop.getProperty("gitblit.acceptnewpatchsets")));
//                    gitblit.acceptnewtickets=true
                    localConfig.setBoolean("gitblit", null, "acceptnewtickets",
                            Boolean.parseBoolean(prop.getProperty("gitblit.acceptnewtickets")));
//                    gitblit.useincrementalpushtags=false
                    localConfig.setBoolean("gitblit", null, "useincrementalpushtags",
                            Boolean.parseBoolean(prop.getProperty("gitblit.useincrementalpushtags")));
//                    gitblit.allowforks=true
                    localConfig.setBoolean("gitblit", null, "allowforks",
                            Boolean.parseBoolean(prop.getProperty("gitblit.allowforks")));
//                    gitblit.accessrestriction=VIEW
                    localConfig.setString("gitblit", null, "accessrestriction",
                            prop.getProperty("gitblit.accessrestriction"));
//                    gitblit.authorizationcontrol=NAMED
                    localConfig.setString("gitblit", null, "authorizationcontrol",
                            prop.getProperty("gitblit.authorizationcontrol"));
//                    gitblit.verifycommitter=false
                    localConfig.setBoolean("gitblit", null, "verifycommitter",
                            Boolean.parseBoolean(prop.getProperty("gitblit.verifycommitter")));
//                    gitblit.showremotebranches=false
                    localConfig.setBoolean("gitblit", null, "showremotebranches",
                            Boolean.parseBoolean(prop.getProperty("gitblit.showremotebranches")));
//                    gitblit.isfrozen=false
                    localConfig.setBoolean("gitblit", null, "isfrozen",
                            Boolean.parseBoolean(prop.getProperty("gitblit.isfrozen")));
//                    gitblit.skipsizecalculation=false
                    localConfig.setBoolean("gitblit", null, "skipsizecalculation",
                            Boolean.parseBoolean(prop.getProperty("gitblit.skipsizecalculation")));
//                    gitblit.skipsummarymetrics=false
                    localConfig.setBoolean("gitblit", null, "skipsummarymetrics",
                            Boolean.parseBoolean(prop.getProperty("gitblit.skipsummarymetrics")));
//                    gitblit.federationstrategy=FEDERATE_THIS
                    localConfig.setString("gitblit", null, "federationstrategy",
                            prop.getProperty("gitblit.federationstrategy"));
//                    gitblit.isfederated=false
                    localConfig.setBoolean("gitblit", null, "isfederated",
                            Boolean.parseBoolean(prop.getProperty("gitblit.isfederated")));
//                    gitblit.gcthreshold=500k
                    localConfig.setString("gitblit", null, "gcthreshold",
                            prop.getProperty("gitblit.gcthreshold"));
//                    gitblit.lastgc=1969-12-31T16:00:00-0800
                    localConfig.setString("gitblit", null, "lastgc",
                            prop.getProperty("gitblit.lastgc"));

                    //Save and close repo
                    localConfig.save();
                    localRepo.close();
                } else
                {
                    //Enter dir.
                    modDirectoryRecursive(new File(file.getPath()));
                }
            }
            //else skip
        }
    }

    public static void main(String[] args) throws IOException
    {
        if (isPosix)
        {
            System.out.println("System is POSIX");
            File cwd = new File(System.getProperty("user.dir") + "/teststuff2");
            prop.load(new FileInputStream("config.properties"));
            System.out.println("Current dir = " + cwd.getPath());

            modDirectoryRecursive(cwd);

            //Testing stuff
//            for (File file : currentFileNames)
//            {
//                PosixFileAttributes attrs = Files.getFileAttributeView(file.toPath(),
//                        PosixFileAttributeView.class).readAttributes();
//                System.out.print(PosixFilePermissions.toString(attrs.permissions()) +
//                        "    " + attrs.owner() + ":" + attrs.group() + "    ");
//
//                System.out.print(attrs.isDirectory() ? "dir\t" : "file\t");
//                System.out.println(file.getName());
//            }
        } else
        {
            System.out.println("System is not POSIX");
        }


        System.out.println("Done.");
        System.exit(0);
    }
}
