package com.gitblit.plugin.gitblitrepoposixpermission;

import com.gitblit.Constants;
import com.gitblit.extensions.LifeCycleListener;
import com.gitblit.manager.IRepositoryManager;
import com.gitblit.manager.IRuntimeManager;
import com.gitblit.manager.IUserManager;
import com.gitblit.models.TeamModel;
import com.gitblit.servlet.GitblitContext;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.Extension;

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
  private IRuntimeManager runtimeManager = null;
  private IRepositoryManager repositoryManager = null;
  private IUserManager userManager = null;

  public GitBlitRepoPosixListener()
  {
    super();
    //I guess this needs to be here?
    this.runtimeManager = GitblitContext.getManager(IRuntimeManager.class);
    this.repositoryManager = GitblitContext.getManager(IRepositoryManager.class);
    this.userManager = GitblitContext.getManager(IUserManager.class);
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
        modAllReposByString();
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
   * Helper function to set all repo permissions.
   *
   * @throws IOException from File errors
   */
  private void modAllReposByString() throws IOException
  {
    for(String currentRepName : repositoryManager.getRepositoryList())
    {
      Repository localRepo = repositoryManager.getRepository(currentRepName);
      File currentFolder = localRepo.getDirectory();
      log.debug(this.getClass().getName() + ": Found a repo: " + currentRepName);
      PosixFileAttributes attrs = Files.getFileAttributeView(currentFolder.toPath(),
              PosixFileAttributeView.class).readAttributes();
      log.debug(this.getClass().getName() + ": Configuring owner and group to: " +
              attrs.owner() + ":" + attrs.group());

      StoredConfig localConfig = localRepo.getConfig();

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
      log.info(String.format("Finding TeamModel for group=%s: %s",
              attrs.group().getName(), userManager.getTeamModel(attrs.group().getName())));
      TeamModel localTeam = userManager.getTeamModel(attrs.group().getName());
      if(localTeam != null)
      {
        log.info(String.format("Adding %s permissions of repo: %s to group: %s",
                Constants.AccessPermission.DELETE, currentRepName, attrs.group().getName()));
        localTeam.setRepositoryPermission(currentRepName, Constants.AccessPermission.DELETE);
      }
      else
      {
        log.warn("TeamModel for group " + attrs.group().getName() + " does not exist.");
      }
      userManager.updateTeamModel(attrs.group().getName(), localTeam);
    }
  }

}