package com.gitblit.plugin.gitblitrepoposixpermission;

import com.gitblit.Constants;
import com.gitblit.extensions.LifeCycleListener;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.manager.IUserManager;
import com.gitblit.models.TeamModel;
//import com.gitblit.servlet.GitblitContext;
import com.google.inject.Inject;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.Extension;

//import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Properties;

/**
 * Manages users and groups of all repos base on posix folder attributes of the git repository.
 *
 * @author William Cork
 */
@Extension
public class GitBlitRepoPosixListener extends LifeCycleListener
{
  private static final boolean ISPOSIX = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
  private static final Properties prop = new Properties();

  final Logger log = LoggerFactory.getLogger(getClass());
  final IRuntimeManager runtimeManager;
  final IRepositoryManager repositoryManager;
  final IUserManager userManager;

  //TODO: This is possibly where the error occurs. No idea what's going on here...
  @Inject
  public GitBlitRepoPosixListener(IRuntimeManager runtimeManager,
                                  IRepositoryManager repositoryManager,
                                  IUserManager userManager)
  {
    this.runtimeManager = runtimeManager;
    this.repositoryManager = repositoryManager;
    this.userManager = userManager;
  }

  @Override
  public void onStartup()
  {
    if (ISPOSIX)
    {
      try
      {
        //Get properties
        prop.load(new FileInputStream("GitBlitRepoPosix.properties"));
        //Modify repos to owner:group
        modDirectoryRecursive(runtimeManager.getBaseFolder());
        log.info(this.getClass().getName() + ": Completed posix repository permission settings");
      } catch (IOException e)
      {
        log.warn("GitBlitDirectoryManager failure: " + e);
      }
      //Reset the repo cache
      repositoryManager.resetRepositoryListCache();
    } else
    {
      log.warn(this.getClass().getName() + ": System is not POSIX. Cannot manage repo permissions.");
    }
  }

  @Override
  public void onShutdown()
  {
    //do nothing
  }

  /**
   * Helper function to traverse a given folder recursively.
   *
   * @param currentDir the starting directory.
   * @throws IOException from File errors
   */
  private void modDirectoryRecursive(File currentDir) throws IOException
  {
    //Recursive search for *.git dir
    File[] currentFileNames = currentDir.listFiles();
    if (currentFileNames == null) return; // just a file

    if (currentDir.isDirectory())
    {
      for (File file : currentFileNames)
      {
        if (file.getName().matches(".*\\.git")) //Found a git repo
        {
          //Add configs.
          PosixFileAttributes attrs = Files.getFileAttributeView(file.toPath(),
                  PosixFileAttributeView.class).readAttributes();
          log.debug(this.getClass().getName() + ": Found a repo: " + file.getAbsolutePath());
          Repository localRepo = new FileRepository(file.getPath());
          StoredConfig localConfig = localRepo.getConfig();

          log.debug(this.getClass().getName() + ": Configuring owner and group to: " +
                  attrs.owner() + ":" + attrs.group());

          //Configs given from GitBlitRepoPosix.properties
          localConfig.setString("gitblit", null, "owner", attrs.owner().getName());
          localConfig.setBoolean("gitblit", null, "acceptnewpatchsets",
                  Boolean.parseBoolean(prop.getProperty("gitblit.acceptnewpatchsets")));
          localConfig.setBoolean("gitblit", null, "acceptnewtickets",
                  Boolean.parseBoolean(prop.getProperty("gitblit.acceptnewtickets")));
          localConfig.setBoolean("gitblit", null, "useincrementalpushtags",
                  Boolean.parseBoolean(prop.getProperty("gitblit.useincrementalpushtags")));
          localConfig.setBoolean("gitblit", null, "allowforks",
                  Boolean.parseBoolean(prop.getProperty("gitblit.allowforks")));
          localConfig.setString("gitblit", null, "accessrestriction",
                  prop.getProperty("gitblit.accessrestriction"));
          localConfig.setString("gitblit", null, "authorizationcontrol",
                  prop.getProperty("gitblit.authorizationcontrol"));
          localConfig.setBoolean("gitblit", null, "verifycommitter",
                  Boolean.parseBoolean(prop.getProperty("gitblit.verifycommitter")));
          localConfig.setBoolean("gitblit", null, "showremotebranches",
                  Boolean.parseBoolean(prop.getProperty("gitblit.showremotebranches")));
          localConfig.setBoolean("gitblit", null, "isfrozen",
                  Boolean.parseBoolean(prop.getProperty("gitblit.isfrozen")));
          localConfig.setBoolean("gitblit", null, "skipsizecalculation",
                  Boolean.parseBoolean(prop.getProperty("gitblit.skipsizecalculation")));
          localConfig.setBoolean("gitblit", null, "skipsummarymetrics",
                  Boolean.parseBoolean(prop.getProperty("gitblit.skipsummarymetrics")));
          localConfig.setString("gitblit", null, "federationstrategy",
                  prop.getProperty("gitblit.federationstrategy"));
          localConfig.setBoolean("gitblit", null, "isfederated",
                  Boolean.parseBoolean(prop.getProperty("gitblit.isfederated")));
          localConfig.setString("gitblit", null, "gcthreshold",
                  prop.getProperty("gitblit.gcthreshold"));
          localConfig.setString("gitblit", null, "lastgc",
                  prop.getProperty("gitblit.lastgc"));

          //Save and close repo
          localConfig.save();
          localRepo.close();

          //Add repo to group(attrs.group().getName()) in users.conf via java API
          TeamModel localTeam = userManager.getTeamModel(attrs.group().getName());
          localTeam.setRepositoryPermission(file.getPath(), Constants.AccessPermission.DELETE);
          userManager.updateTeamModel(attrs.group().getName(), localTeam);

        } else
        {
          //Not a repo. Enter dir.
          modDirectoryRecursive(file);
        }
      }
      //else skip
    }
  }
}